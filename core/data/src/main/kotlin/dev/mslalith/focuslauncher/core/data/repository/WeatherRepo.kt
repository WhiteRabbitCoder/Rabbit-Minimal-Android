package dev.mslalith.focuslauncher.core.data.repository

import dev.mslalith.focuslauncher.core.data.network.api.WeatherApi
import javax.inject.Inject

class WeatherRepo @Inject constructor(
    private val weatherApi: WeatherApi
) {
    suspend fun getTemperatureCelsius(latitude: Double, longitude: Double): Int? =
        weatherApi.getTemperatureCelsius(latitude, longitude).getOrNull()
}
