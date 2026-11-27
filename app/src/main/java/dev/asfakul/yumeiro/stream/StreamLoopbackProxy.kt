package dev.asfakul.yumeiro.stream

import android.content.Context
import android.net.Uri
import android.util.Log
import dev.asfakul.yumeiro.network.NetworkClient
import dev.asfakul.yumeiro.online.ProviderCircuitBreaker
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object StreamLoopbackProxy {
    private const val TAG = "StreamLoopbackProxy"
    private const val MAX_ENTRIES = 256
    private const val MAX_HEADER_RETRIES = 2
    private const val MAX_MIDSTREAM_RETRIES = 3
    private const val IO_BUFFER_SIZE = 32 * 1024

    data class Entry(
        val url: String,
        val headers: Map<String, String>,
        val createdAt: Long = System.currentTimeMillis(),
    )

    private val running = AtomicBoolean(false)
    private val entries = ConcurrentHashMap<String, Entry>()
    private val executor = Executors.newCachedThreadPool { r ->
        Thread(r, "StreamProxy-Worker").apply { isDaemon = true }
    }

    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var port: Int = -1
    @Volatile private var appContext: Context? = null

    private val URI_ATTR_REGEX = Regex("""URI="([^"]+)"""")

    fun ensureStarted(context: Context): Int {
        appContext = context.applicationContext
        if (running.get() && port > 0) return port
        synchronized(this) {
            if (running.get() && port > 0) return port
            val loopback = InetAddress.getByName("127.0.0.1")
            val ss = ServerSocket(0, 48, loopback)
            serverSocket = ss
            port = ss.localPort
            running.set(true)
            executor.execute { acceptLoop(ss) }
            Log.i(TAG, "Started on 127.0.0.1:$port")
            return port
        }
    }

    fun register(context: Context, url: String, headers: Map<String, String>): String {
        val p = ensureStarted(context)
        evictIfNeeded()
        val token = UUID.randomUUID().toString().replace("-", "")
        entries[token] = Entry(url, headers)
        val extHint = Uri.parse(url).lastPathSegment?.substringAfterLast('.', "")
            ?.takeIf { it.length in 2..5 && it.all(Char::isLetterOrDigit) }
        val suffix = if (extHint != null) ".$extHint" else ""
        return "http://127.0.0.1:$p/s/$token$suffix"
    }

    fun clearSessions() = entries.clear()

    fun shutdown() {
        running.set(false)
        runCatching { serverSocket?.close() }
        serverSocket = null
        port = -1
        entries.clear()
    }

    private fun evictIfNeeded() {
        if (entries.size < MAX_ENTRIES) return
        entries.entries.sortedBy { it.value.createdAt }
            .take(entries.size - MAX_ENTRIES / 2)
            .forEach { entries.remove(it.key) }
    }

    private fun acceptLoop(ss: ServerSocket) {
        while (running.get() && !ss.isClosed) {
            val client = runCatching { ss.accept() }.getOrNull() ?: break
            executor.execute { handleClient(client) }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { sock ->
            sock.soTimeout = 25_000
            val reader = BufferedReader(InputStreamReader(sock.getInputStream()))
            val out = BufferedOutputStream(sock.getOutputStream(), IO_BUFFER_SIZE)

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(' ')
            if (parts.size < 2) return
            val method = parts[0].uppercase()
            if (method != "GET" && method != "HEAD") {
                writeStatus(out, 405, "Method Not Allowed"); return
            }

            val reqHeaders = HashMap<String, String>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val idx = line.indexOf(':')
                if (idx > 0) reqHeaders[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
            }

            val rawPath = parts[1].substringBefore('?')
            val token = rawPath.removePrefix("/s/").substringBefore('.')
            val entry = entries[token]
            if (entry == null) {
                writeStatus(out, 404, "Not Found"); return
            }

            val client = NetworkClient.get(appContext)
            val rangeHeader = reqHeaders["range"]
            val host = runCatching { URI(entry.url).host.orEmpty() }.getOrDefault("")
            val t0 = System.currentTimeMillis()

            val response = executeWithRetry(client, entry, method, rangeHeader, host) ?: run {
                writeStatus(out, 502, "Bad Gateway"); return
            }

            response.use { res ->
                val latency = System.currentTimeMillis() - t0
                if (res.isSuccessful) {
                    ProviderCircuitBreaker.recordSuccess(host, latency)
                } else {
                    ProviderCircuitBreaker.recordFailure(host, res.code)
                }

                val contentType = res.header("Content-Type").orEmpty()
                val finalUrl = res.request.url.toString()
                val isHls = contentType.contains("mpegurl", ignoreCase = true) ||
                    finalUrl.substringBefore('?').endsWith(".m3u8", ignoreCase = true)

                if (isHls && res.isSuccessful && method == "GET") {
                    val body = res.body?.string().orEmpty()
                    val rewritten = rewriteHlsManifest(body, finalUrl, entry.headers).toByteArray(Charsets.UTF_8)
                    val headerBlock = buildString {
                        append("HTTP/1.1 200 OK\r\n")
                        append("Content-Type: application/vnd.apple.mpegurl\r\n")
                        append("Content-Length: ${rewritten.size}\r\n")
                        append("Connection: close\r\n\r\n")
                    }
                    out.write(headerBlock.toByteArray(Charsets.US_ASCII))
                    out.write(rewritten)
                    out.flush()
                    return
                }

                streamMediaResponse(client, entry, res, method, rangeHeader, host, out)
            }
        }
    }

    private fun executeWithRetry(
        client: OkHttpClient,
        entry: Entry,
        method: String,
        rangeHeader: String?,
        host: String,
    ): Response? {
        var lastResponse: Response? = null
        for (attempt in 0..MAX_HEADER_RETRIES) {
            val req = buildUpstreamRequest(entry, method, rangeHeader)
            try {
                val res = client.newCall(req).execute()
                if (res.code == 429 || res.code in 500..599) {
                    lastResponse?.close()
                    lastResponse = res
                    if (attempt < MAX_HEADER_RETRIES) {
                        Thread.sleep(250L * (1 shl attempt))
                        continue
                    }
                    return res
                }
                lastResponse?.close()
                return res
            } catch (e: IOException) {
                ProviderCircuitBreaker.recordFailure(host, -1)
                if (attempt == MAX_HEADER_RETRIES) return lastResponse
                runCatching { Thread.sleep(250L * (1 shl attempt)) }
            }
        }
        return lastResponse
    }

    private fun buildUpstreamRequest(entry: Entry, method: String, rangeHeader: String?): Request {
        val b = Request.Builder().url(entry.url)
        if (method == "HEAD") b.head() else b.get()
        entry.headers.forEach { (k, v) ->
            if (!k.equals("Host", true) && !k.equals("Range", true) && !k.equals("Accept-Encoding", true)) {
                b.header(k, v)
            }
        }
        if (rangeHeader != null) b.header("Range", rangeHeader)
        b.header("Accept-Encoding", "identity")
        return b.build()
    }

    private fun streamMediaResponse(
        client: OkHttpClient,
        entry: Entry,
        initial: Response,
        method: String,
        initialRange: String?,
        host: String,
        out: OutputStream,
    ) {
        val code = initial.code
        val msg = initial.message.ifBlank { if (code == 206) "Partial Content" else "OK" }
        val contentLength = initial.body?.contentLength() ?: -1L
        val contentRange = initial.header("Content-Range")
        val contentType = initial.header("Content-Type") ?: "application/octet-stream"
        val acceptRanges = initial.header("Accept-Ranges") ?: if (code == 206) "bytes" else null

        val headerBlock = buildString {
            append("HTTP/1.1 $code $msg\r\n")
            append("Content-Type: $contentType\r\n")
            if (contentLength >= 0) append("Content-Length: $contentLength\r\n")
            if (contentRange != null) append("Content-Range: $contentRange\r\n")
            if (acceptRanges != null) append("Accept-Ranges: $acceptRanges\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(headerBlock.toByteArray(Charsets.US_ASCII))
        out.flush()
        if (method == "HEAD" || !initial.isSuccessful) return

        val startOffset = parseRangeStart(contentRange, initialRange)
        val canResume = acceptRanges.equals("bytes", ignoreCase = true) || code == 206
        var totalWritten = 0L
        var currentRes: Response? = initial
        var midRetries = 0
        val buf = ByteArray(IO_BUFFER_SIZE)

        while (true) {
            val stream = currentRes?.body?.byteStream() ?: break
            var dropError: IOException? = null
            try {
                val buffered = BufferedInputStream(stream, IO_BUFFER_SIZE)
                while (true) {
                    val read = buffered.read(buf)
                    if (read == -1) break
                    out.write(buf, 0, read)
                    totalWritten += read
                }
                out.flush()
                break
            } catch (e: IOException) {
                dropError = e
            } finally {
                if (currentRes !== initial) runCatching { currentRes?.close() }
            }

            val completed = contentLength in 1..totalWritten
            if (dropError == null || completed || !canResume || midRetries >= MAX_MIDSTREAM_RETRIES) break

            midRetries++
            val nextStart = startOffset + totalWritten
            Log.w(TAG, "Mid-stream drop on $host at byte $nextStart (retry $midRetries/$MAX_MIDSTREAM_RETRIES)")
            runCatching { Thread.sleep(200L * midRetries) }
            val nextReq = buildUpstreamRequest(entry, "GET", "bytes=$nextStart-")
            currentRes = runCatching { client.newCall(nextReq).execute() }.getOrNull()
                ?.takeIf { it.code == 206 } ?: break
        }
    }

    private fun parseRangeStart(contentRange: String?, rangeHeader: String?): Long {
        if (contentRange != null) {
            Regex("""bytes\s+(\d+)-""").find(contentRange)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }
        }
        if (rangeHeader != null) {
            Regex("""bytes=(\d+)-""").find(rangeHeader)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }
        }
        return 0L
    }

    private fun rewriteHlsManifest(manifest: String, baseUrl: String, headers: Map<String, String>): String {
        val ctx = appContext ?: return manifest
        val base = runCatching { URI(baseUrl) }.getOrNull() ?: return manifest
        return manifest.lineSequence().joinToString("\n") { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> raw
                line.startsWith("#") -> {
                    if ("URI=\"" in line) {
                        URI_ATTR_REGEX.replace(raw) { m ->
                            val resolved = resolveUrl(base, m.groupValues[1])
                            val proxied = register(ctx, resolved, headers)
                            """URI="$proxied""""
                        }
                    } else raw
                }
                else -> register(ctx, resolveUrl(base, line), headers)
            }
        }
    }

    private fun resolveUrl(base: URI, target: String): String =
        runCatching { base.resolve(target).toString() }.getOrDefault(target)

    private fun writeStatus(out: OutputStream, code: Int, message: String) {
        val resp = "HTTP/1.1 $code $message\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
        runCatching {
            out.write(resp.toByteArray(Charsets.US_ASCII))
            out.flush()
        }
    }
}
