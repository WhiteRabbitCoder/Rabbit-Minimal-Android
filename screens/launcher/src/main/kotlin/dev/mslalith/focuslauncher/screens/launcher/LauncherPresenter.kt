package dev.mslalith.focuslauncher.screens.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.collectAsRetainedState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.components.SingletonComponent
import dev.mslalith.focuslauncher.core.data.repository.settings.GeneralSettingsRepo
import dev.mslalith.focuslauncher.core.domain.launcherapps.LoadAllAppsUseCase
import dev.mslalith.focuslauncher.core.model.Constants.Defaults.Settings.General.DEFAULT_STATUS_BAR
import dev.mslalith.focuslauncher.core.screens.AiScreen
import dev.mslalith.focuslauncher.core.screens.LauncherScreen
import dev.mslalith.focuslauncher.core.screens.SettingsPageScreen
import dev.mslalith.focuslauncher.feature.appdrawerpage.AppDrawerPagePresenter
import dev.mslalith.focuslauncher.feature.homepage.HomePagePresenter

class LauncherPresenter @AssistedInject constructor(
    @Assisted private val navigator: Navigator,
    private val homePagePresenterFactory: HomePagePresenter.Factory,
    private val appDrawerPagePresenter: AppDrawerPagePresenter,
    private val loadAllAppsUseCase: LoadAllAppsUseCase,
    private val generalSettingsRepo: GeneralSettingsRepo
) : Presenter<LauncherState> {

    @CircuitInject(LauncherScreen::class, SingletonComponent::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): LauncherPresenter
    }

    private val homePagePresenter by lazy { homePagePresenterFactory.create(navigator = navigator) }

    @Composable
    override fun present(): LauncherState {
        val homePageState = homePagePresenter.present()
        val appDrawerPageState = appDrawerPagePresenter.present()
        val showStatusBar by generalSettingsRepo.statusBarVisibilityFlow
            .collectAsRetainedState(initial = DEFAULT_STATUS_BAR)

        LaunchedEffect(key1 = Unit) {
            loadAllAppsUseCase(forceLoad = false)
        }

        return LauncherState(
            homePageState = homePageState,
            appDrawerPageState = appDrawerPageState,
            showStatusBar = showStatusBar,
            eventSink = { event ->
                when (event) {
                    LauncherUiEvent.NavigateToAiScreen -> navigator.goTo(AiScreen)
                    LauncherUiEvent.NavigateToSettings -> navigator.goTo(SettingsPageScreen)
                }
            }
        )
    }
}
