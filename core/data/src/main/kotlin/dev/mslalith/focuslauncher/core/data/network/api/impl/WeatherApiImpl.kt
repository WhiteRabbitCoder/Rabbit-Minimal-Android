package dev.mslalith.focuslauncher.core.data.network.api.impl

import dev.mslalith.focuslauncher.core.data.network.api.WeatherApi
import dev.mslalith.focuslauncher.core.data.network.entities.WeatherResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject
import kotlin.math.roundToInt

internal class WeatherApiImpl @Inject constructor(
    private val httpClient: HttpClient
) : WeatherApi {

    override suspend fun getTemperatureCelsius(latitude: Double, longitude: Double): Result<Int> =
        runCatching {
            httpClient.get(urlString = "https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("current", "temperature_2m")
                parameter("temperature_unit", "celsius")
            }.body<WeatherResponse>().current.temperatureCelsius.roundToInt()
        }
}
