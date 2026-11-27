package dev.asfakul.yumeiro.online

import java.net.URI
import java.util.concurrent.ConcurrentHashMap

object ProviderCircuitBreaker {
    private const val TRIP_THRESHOLD = 3
    private const val COOLDOWN_MS = 5 * 60_000L

    enum class Status { HEALTHY, DEGRADED, TRIPPED, UNKNOWN }

    data class Snapshot(
        val status: Status,
        val avgLatencyMs: Long,
        val consecutiveFailures: Int,
    )

    private class HostState {
        var failures: Int = 0
        var trippedUntil: Long = 0L
        var ewmaLatencyMs: Long = 0L
        var samples: Int = 0
    }

    private val hosts = ConcurrentHashMap<String, HostState>()

    fun hostOf(url: String): String =
        runCatching { URI(url).host?.lowercase().orEmpty() }.getOrDefault("")

    fun recordSuccess(host: String, latencyMs: Long) {
        if (host.isBlank()) return
        val s = hosts.computeIfAbsent(host) { HostState() }
        synchronized(s) {
            s.failures = 0
            s.trippedUntil = 0L
            s.samples++
            s.ewmaLatencyMs = if (s.samples == 1) latencyMs else (s.ewmaLatencyMs * 7 + latencyMs * 3) / 10
        }
    }

    fun recordFailure(host: String, httpCode: Int = -1) {
        if (host.isBlank()) return
        val s = hosts.computeIfAbsent(host) { HostState() }
        synchronized(s) {
            val weight = if (httpCode == 403 || httpCode == 404 || httpCode == 410) 2 else 1
            s.failures += weight
            if (s.failures >= TRIP_THRESHOLD) {
                s.trippedUntil = System.currentTimeMillis() + COOLDOWN_MS
            }
        }
    }

    fun isTripped(urlOrHost: String, now: Long = System.currentTimeMillis()): Boolean {
        val host = if ("://" in urlOrHost) hostOf(urlOrHost) else urlOrHost.lowercase()
        val s = hosts[host] ?: return false
        synchronized(s) { return s.trippedUntil > now }
    }

    fun snapshot(url: String, now: Long = System.currentTimeMillis()): Snapshot {
        val host = hostOf(url)
        val s = hosts[host] ?: return Snapshot(Status.UNKNOWN, 0L, 0)
        synchronized(s) {
            val status = when {
                s.trippedUntil > now -> Status.TRIPPED
                s.failures > 0 || s.ewmaLatencyMs > 2_500L -> Status.DEGRADED
                s.samples > 0 -> Status.HEALTHY
                else -> Status.UNKNOWN
            }
            return Snapshot(status, s.ewmaLatencyMs, s.failures)
        }
    }

    fun rankBonus(url: String): Int {
        val snap = snapshot(url)
        return when (snap.status) {
            Status.TRIPPED -> -1000
            Status.DEGRADED -> -100
            Status.HEALTHY -> (150 - (snap.avgLatencyMs / 40L).toInt()).coerceIn(20, 150)
            Status.UNKNOWN -> 0
        }
    }

    fun reset(host: String? = null) {
        if (host == null) hosts.clear() else hosts.remove(host.lowercase())
    }
}
