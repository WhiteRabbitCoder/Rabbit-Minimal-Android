package dev.mslalith.focuslauncher.screens.launcher

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import dev.mslalith.focuslauncher.feature.appdrawerpage.AppDrawerPageState
import dev.mslalith.focuslauncher.feature.homepage.HomePageState

data class LauncherState(
    val homePageState: HomePageState,
    val appDrawerPageState: AppDrawerPageState,
    val showStatusBar: Boolean,
    val eventSink: (LauncherUiEvent) -> Unit = {}
) : CircuitUiState

sealed interface LauncherUiEvent : CircuitUiEvent {
    data object NavigateToAiScreen : LauncherUiEvent
    data object NavigateToSettings : LauncherUiEvent
}
