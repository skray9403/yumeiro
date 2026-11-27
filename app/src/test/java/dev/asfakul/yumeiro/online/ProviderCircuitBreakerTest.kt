package dev.asfakul.yumeiro.online

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProviderCircuitBreakerTest {
    @Before
    fun reset() {
        ProviderCircuitBreaker.reset()
    }

    @Test
    fun extractsHostAndNormalizesCase() {
        assertEquals("media.example", ProviderCircuitBreaker.hostOf("https://Media.Example/watch?id=1"))
    }

    @Test
    fun startsUnknownAndDegradesAfterFailure() {
        val url = "https://media.example/video.mp4"
        assertEquals(ProviderCircuitBreaker.Status.UNKNOWN, ProviderCircuitBreaker.snapshot(url).status)

        ProviderCircuitBreaker.recordFailure("media.example", 500)

        assertEquals(ProviderCircuitBreaker.Status.DEGRADED, ProviderCircuitBreaker.snapshot(url).status)
        assertFalse(ProviderCircuitBreaker.isTripped(url))
    }

    @Test
    fun tripsAfterThresholdAndSuccessResetsHost() {
        val url = "https://media.example/video.mp4"
        ProviderCircuitBreaker.recordFailure("media.example", 500)
        ProviderCircuitBreaker.recordFailure("media.example", 500)
        ProviderCircuitBreaker.recordFailure("media.example", 500)

        assertTrue(ProviderCircuitBreaker.isTripped(url))
        assertEquals(ProviderCircuitBreaker.Status.TRIPPED, ProviderCircuitBreaker.snapshot(url).status)

        ProviderCircuitBreaker.recordSuccess("media.example", 420)

        assertFalse(ProviderCircuitBreaker.isTripped(url))
        assertEquals(ProviderCircuitBreaker.Status.HEALTHY, ProviderCircuitBreaker.snapshot(url).status)
        assertEquals(420L, ProviderCircuitBreaker.snapshot(url).avgLatencyMs)
    }

    @Test
    fun authorizationAndMissingResourcesCarryHigherFailureWeight() {
        val url = "https://media.example/video.mp4"
        ProviderCircuitBreaker.recordFailure("media.example", 403)
        ProviderCircuitBreaker.recordFailure("media.example", 500)

        assertTrue(ProviderCircuitBreaker.isTripped(url))
    }

    @Test
    fun resetCanClearOneHostWithoutTouchingOthers() {
        ProviderCircuitBreaker.recordFailure("one.example", 500)
        ProviderCircuitBreaker.recordFailure("two.example", 500)

        ProviderCircuitBreaker.reset("one.example")

        assertEquals(ProviderCircuitBreaker.Status.UNKNOWN, ProviderCircuitBreaker.snapshot("https://one.example/a").status)
        assertEquals(ProviderCircuitBreaker.Status.DEGRADED, ProviderCircuitBreaker.snapshot("https://two.example/a").status)
    }
}
