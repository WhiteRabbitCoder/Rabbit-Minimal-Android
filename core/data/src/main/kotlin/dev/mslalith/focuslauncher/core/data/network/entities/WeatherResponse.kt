package dev.mslalith.focuslauncher.core.data.network.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WeatherResponse(
    @SerialName("current") val current: CurrentWeather
) {
    @Serializable
    data class CurrentWeather(
        @SerialName("temperature_2m") val temperatureCelsius: Double
    )
}
