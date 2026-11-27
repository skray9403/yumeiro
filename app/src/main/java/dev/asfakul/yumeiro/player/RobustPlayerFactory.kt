package dev.asfakul.yumeiro.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dev.asfakul.yumeiro.network.NetworkClient
import dev.asfakul.yumeiro.online.ProviderCircuitBreaker
import java.io.IOException
import java.net.SocketTimeoutException

@OptIn(UnstableApi::class)
object RobustPlayerFactory {
    class StreamLoadErrorPolicy(
        private val maxTransientRetries: Int = 4,
    ) : DefaultLoadErrorHandlingPolicy(maxTransientRetries) {

        override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
            val error = loadErrorInfo.exception
            val uri = loadErrorInfo.loadEventInfo.uri.toString()
            val host = ProviderCircuitBreaker.hostOf(uri)

            if (error is HttpDataSource.InvalidResponseCodeException) {
                val code = error.responseCode
                if (code == 401 || code == 403 || code == 404 || code == 410 || code == 451) {
                    ProviderCircuitBreaker.recordFailure(host, code)
                    return C.TIME_UNSET
                }
                if (code == 408 || code == 429 || code in 500..599) {
                    ProviderCircuitBreaker.recordFailure(host, code)
                    return backoffMs(loadErrorInfo.errorCount)
                }
            }

            if (error is SocketTimeoutException || error is HttpDataSource.HttpDataSourceException || error is IOException) {
                if (loadErrorInfo.errorCount <= maxTransientRetries) {
                    return backoffMs(loadErrorInfo.errorCount)
                }
                ProviderCircuitBreaker.recordFailure(host, -1)
            }

            return C.TIME_UNSET
        }

        override fun getMinimumLoadableRetryCount(dataType: Int): Int = maxTransientRetries

        private fun backoffMs(errorCount: Int): Long =
            (400L * (1L shl (errorCount - 1).coerceIn(0, 4))).coerceAtMost(5_000L)
    }

    fun build(
        context: Context,
        requestHeaders: Map<String, String> = emptyMap(),
        lowRamDevice: Boolean = dev.asfakul.yumeiro.DeviceClass.lowEnd,
    ): ExoPlayer {
        val appCtx = context.applicationContext

        val okHttpFactory = OkHttpDataSource.Factory(NetworkClient.get(appCtx)).apply {
            if (requestHeaders.isNotEmpty()) setDefaultRequestProperties(requestHeaders)
        }
        val dataSourceFactory = DefaultDataSource.Factory(appCtx, okHttpFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(StreamLoadErrorPolicy())

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                if (lowRamDevice) 12_000 else 22_000,
                if (lowRamDevice) 30_000 else 65_000,
                1_500,
                3_500,
            )
            .setBackBuffer(
                if (lowRamDevice) 5_000 else 15_000,
                true,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val extMode = when (VideoPrefs.decoder.value) {
            VideoPrefs.Decoder.HARDWARE_PLUS -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
            VideoPrefs.Decoder.SOFTWARE -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            VideoPrefs.Decoder.HARDWARE -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        }
        val renderersFactory = DefaultRenderersFactory(appCtx)
            .setExtensionRendererMode(extMode)
            .setEnableDecoderFallback(true)

        val trackSelector = DefaultTrackSelector(appCtx).apply {
            val maxHeight = VideoPrefs.quality.value.height
            setParameters(
                buildUponParameters()
                    .setExceedRendererCapabilitiesIfNecessary(true)
                    .setExceedVideoConstraintsIfNecessary(true)
                    .apply { if (maxHeight > 0) setMaxVideoSize(Int.MAX_VALUE, maxHeight) }
            )
        }

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(appCtx, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttrs, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
    }
}
