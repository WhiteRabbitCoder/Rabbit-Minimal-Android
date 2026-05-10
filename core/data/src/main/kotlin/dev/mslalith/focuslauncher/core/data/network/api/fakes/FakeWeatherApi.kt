package dev.mslalith.focuslauncher.core.data.network.api.fakes

import dev.mslalith.focuslauncher.core.data.network.api.WeatherApi

class FakeWeatherApi : WeatherApi {
    override suspend fun getTemperatureCelsius(latitude: Double, longitude: Double): Result<Int> =
        Result.success(22)
}
