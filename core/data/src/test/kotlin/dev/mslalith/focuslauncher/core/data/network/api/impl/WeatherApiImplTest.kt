package dev.mslalith.focuslauncher.core.data.network.api.impl

import com.google.common.truth.Truth.assertThat
import dev.mslalith.focuslauncher.core.testing.KtorApiTest
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.serialization.JsonConvertException
import io.mockk.mockk
import io.ktor.client.call.DoubleReceiveException
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
internal class WeatherApiImplTest : KtorApiTest() {

    private val weatherApi = WeatherApiImpl(httpClient = client)

    private val url = "https://api.open-meteo.com/v1/forecast"

    @Test
    fun `01 - when valid coordinates, temperature is parsed correctly`() = runCoroutineTest {
        onRequestTo(url = url) {
            successResponse(content = successJson(temperatureCelsius = 20.6))
        }

        val result = weatherApi.getTemperatureCelsius(latitude = 40.4168, longitude = -3.7038)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(21) // 20.6 rounds to 21
    }

    @Test
    fun `02 - when temperature is negative, value is parsed correctly`() = runCoroutineTest {
        onRequestTo(url = url) {
            successResponse(content = successJson(temperatureCelsius = -5.3))
        }

        val result = weatherApi.getTemperatureCelsius(latitude = 0.0, longitude = 0.0)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(-5)
    }

    @Test
    fun `03 - when server returns malformed JSON, result is failure`() = runCoroutineTest {
        onRequestTo(url = url) {
            throw JsonConvertException(message = "Test exception")
        }

        val result = weatherApi.getTemperatureCelsius(latitude = 40.0, longitude = -3.0)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `04 - when timeout occurs, result is failure`() = runCoroutineTest {
        onRequestTo(url = url) {
            throw HttpRequestTimeoutException(url = url, timeoutMillis = null)
        }

        val result = weatherApi.getTemperatureCelsius(latitude = 40.0, longitude = -3.0)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `05 - when double receive exception, result is failure`() = runCoroutineTest {
        onRequestTo(url = url) {
            throw DoubleReceiveException(call = mockk())
        }

        val result = weatherApi.getTemperatureCelsius(latitude = 40.0, longitude = -3.0)

        assertThat(result.isFailure).isTrue()
    }
}

private fun successJson(temperatureCelsius: Double): String = """
    {
      "latitude": 40.4375,
      "longitude": -3.6875,
      "generationtime_ms": 0.021,
      "utc_offset_seconds": 0,
      "timezone": "GMT",
      "timezone_abbreviation": "GMT",
      "elevation": 666.0,
      "current_units": {
        "time": "iso8601",
        "interval": "seconds",
        "temperature_2m": "°C"
      },
      "current": {
        "time": "2026-05-10T18:45",
        "interval": 900,
        "temperature_2m": $temperatureCelsius
      }
    }
""".trimIndent()
