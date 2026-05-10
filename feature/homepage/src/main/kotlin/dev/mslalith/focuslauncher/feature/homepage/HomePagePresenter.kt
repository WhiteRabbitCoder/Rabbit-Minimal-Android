package dev.mslalith.focuslauncher.feature.homepage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.collectAsRetainedState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.components.SingletonComponent
import dev.mslalith.focuslauncher.core.data.repository.ClockRepo
import dev.mslalith.focuslauncher.core.data.repository.WeatherRepo
import dev.mslalith.focuslauncher.core.data.repository.settings.GeneralSettingsRepo
import dev.mslalith.focuslauncher.core.model.Constants.Defaults.Settings.General.DEFAULT_NOTIFICATION_SHADE
import dev.mslalith.focuslauncher.core.screens.AiScreen
import dev.mslalith.focuslauncher.core.screens.HomePageScreen
import dev.mslalith.focuslauncher.core.screens.SettingsPageScreen
import dev.mslalith.focuslauncher.feature.clock24.widget.ClockWidgetUiComponentPresenter
import dev.mslalith.focuslauncher.feature.favorites.FavoritesListUiComponentPresenter
import dev.mslalith.focuslauncher.feature.lunarcalendar.widget.LunarCalendarUiComponentPresenter
import dev.mslalith.focuslauncher.feature.quoteforyou.widget.QuoteForYouUiComponentPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class HomePagePresenter @AssistedInject constructor(
    @Assisted private val navigator: Navigator,
    private val generalSettingsRepo: GeneralSettingsRepo,
    private val clockRepo: ClockRepo,
    private val weatherRepo: WeatherRepo,
    private val clockWidgetUiComponentPresenter: ClockWidgetUiComponentPresenter,
    private val lunarCalendarUiComponentPresenter: LunarCalendarUiComponentPresenter,
    private val quoteForYouUiComponentPresenter: QuoteForYouUiComponentPresenter,
    private val favoritesListUiComponentPresenter: FavoritesListUiComponentPresenter
) : Presenter<HomePageState> {

    @CircuitInject(HomePageScreen::class, SingletonComponent::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): HomePagePresenter
    }

    @Composable
    override fun present(): HomePageState {
        val isPullDownNotificationShadeEnabled by generalSettingsRepo.notificationShadeFlow
            .collectAsRetainedState(initial = DEFAULT_NOTIFICATION_SHADE)

        val currentInstant by clockRepo.currentInstantStateFlow
            .collectAsRetainedState(initial = Clock.System.now())

        val localDateTime = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        val date = buildDateString(localDateTime)
        val dayProgress = (localDateTime.hour * 60 + localDateTime.minute) / 1440f
        val dayOfYear = localDateTime.date.dayOfYear
        val year = localDateTime.date.year
        val isLeap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        val daysInYear = if (isLeap) 366 else 365
        val yearProgress = dayOfYear / daysInYear.toFloat()

        var weatherText by remember { mutableStateOf("—°C") }

        val clockWidgetUiComponentState = clockWidgetUiComponentPresenter.present()
        val lunarCalendarUiComponentState = lunarCalendarUiComponentPresenter.present()
        val quoteForYouUiComponentState = quoteForYouUiComponentPresenter.present()
        val favoritesListUiComponentState = favoritesListUiComponentPresenter.present()

        return HomePageState(
            isPullDownNotificationShadeEnabled = isPullDownNotificationShadeEnabled,
            date = date,
            dayProgress = dayProgress,
            yearProgress = yearProgress,
            weatherText = weatherText,
            clockWidgetUiComponentState = clockWidgetUiComponentState,
            lunarCalendarUiComponentState = lunarCalendarUiComponentState,
            quoteForYouUiComponentState = quoteForYouUiComponentState,
            favoritesListUiComponentState = favoritesListUiComponentState,
            eventSink = { event ->
                when (event) {
                    HomePageUiEvent.NavigateToAiScreen -> navigator.goTo(AiScreen)
                    HomePageUiEvent.NavigateToSettings -> navigator.goTo(SettingsPageScreen)
                    is HomePageUiEvent.LocationGranted -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            val temp = weatherRepo.getTemperatureCelsius(event.latitude, event.longitude)
                            if (temp != null) weatherText = "${temp}°C"
                        }
                    }
                }
            }
        )
    }

    private fun buildDateString(localDateTime: kotlinx.datetime.LocalDateTime): String {
        val dayOfWeek = localDateTime.dayOfWeek.name
            .lowercase()
            .replaceFirstChar { it.uppercase() }
        val month = localDateTime.month.name
            .lowercase()
            .replaceFirstChar { it.uppercase() }
        val day = localDateTime.date.dayOfMonth
        val ordinal = when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$dayOfWeek, $day$ordinal $month"
    }
}
