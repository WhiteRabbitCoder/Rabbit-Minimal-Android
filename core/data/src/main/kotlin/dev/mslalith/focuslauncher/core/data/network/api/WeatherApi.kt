package dev.mslalith.focuslauncher.core.data.network.api

interface WeatherApi {
    suspend fun getTemperatureCelsius(latitude: Double, longitude: Double): Result<Int>
}
