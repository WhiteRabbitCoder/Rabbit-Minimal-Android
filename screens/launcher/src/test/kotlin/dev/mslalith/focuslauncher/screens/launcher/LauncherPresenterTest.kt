package dev.mslalith.focuslauncher.screens.launcher

import app.cash.turbine.ReceiveTurbine
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dev.mslalith.focuslauncher.core.common.model.LoadingState
import dev.mslalith.focuslauncher.core.common.model.getOrNull
import dev.mslalith.focuslauncher.core.data.repository.AppDrawerRepo
import dev.mslalith.focuslauncher.core.data.repository.settings.GeneralSettingsRepo
import dev.mslalith.focuslauncher.core.domain.launcherapps.LoadAllAppsUseCase
import dev.mslalith.focuslauncher.core.launcherapps.manager.launcherapps.test.TestLauncherAppsManager
import dev.mslalith.focuslauncher.core.model.app.App
import dev.mslalith.focuslauncher.core.model.appdrawer.AppDrawerItem
import dev.mslalith.focuslauncher.core.testing.AppRobolectricTestRunner
import dev.mslalith.focuslauncher.core.testing.TestApps
import dev.mslalith.focuslauncher.core.testing.circuit.PresenterTest
import dev.mslalith.focuslauncher.core.testing.disableAsSystem
import dev.mslalith.focuslauncher.core.testing.toAppsWithComponents
import dev.mslalith.focuslauncher.core.testing.toPackageNamed
import dev.mslalith.focuslauncher.feature.appdrawerpage.AppDrawerPagePresenter
import dev.mslalith.focuslauncher.feature.homepage.HomePagePresenter
import kotlinx.collections.immutable.ImmutableList
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AppRobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
class LauncherPresenterTest : PresenterTest<LauncherPresenter, LauncherState>() {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var homePagePresenterFactory: HomePagePresenter.Factory

    @Inject
    lateinit var appDrawerPagePresenter: AppDrawerPagePresenter

    @Inject
    lateinit var appDrawerRepo: AppDrawerRepo

    @Inject
    lateinit var generalSettingsRepo: GeneralSettingsRepo

    private val testLauncherAppsManager = TestLauncherAppsManager()

    private val allApps by lazy { TestApps.all.toPackageNamed().disableAsSystem() }

    @Before
    fun setup() {
        hiltRule.inject()
    }

    override fun presenterUnderTest() = LauncherPresenter(
        navigator = navigator,
        homePagePresenterFactory = homePagePresenterFactory,
        appDrawerPagePresenter = appDrawerPagePresenter,
        loadAllAppsUseCase = LoadAllAppsUseCase(
            launcherAppsManager = testLauncherAppsManager,
            appDrawerRepo = appDrawerRepo
        ),
        generalSettingsRepo = generalSettingsRepo
    )

    @Test
    fun `01 - when queried, all apps must be retrieved`() {
        testLauncherAppsManager.setAllApps(apps = allApps.toAppsWithComponents())
        runPresenterTest {
            assertThat(awaitItem().appDrawerPageState.allAppsState.getOrNull()).isNull()
            assertDrawerApps(expected = allApps)
        }
    }

    context (ReceiveTurbine<LauncherState>)
    private suspend fun assertDrawerApps(
        expected: List<App>
    ) = assertFor(expected = expected) { it.appDrawerPageState.allAppsState.toTestApps() }
}

private fun LoadingState<ImmutableList<AppDrawerItem>>.toTestApps(): List<App>? = getOrNull()?.map { it.app }
