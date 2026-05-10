package dev.mslalith.focuslauncher.core.domain.appdrawer

import com.google.common.truth.Truth.assertThat
import dev.mslalith.focuslauncher.core.data.repository.AppDrawerRepo
import dev.mslalith.focuslauncher.core.data.repository.HiddenAppsRepo
import dev.mslalith.focuslauncher.core.model.app.App
import dev.mslalith.focuslauncher.core.testing.AppRobolectricTestRunner
import dev.mslalith.focuslauncher.core.testing.CoroutineTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AppRobolectricTestRunner::class)
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
class GetAppDrawerAppsUseCaseTest : CoroutineTest() {

    private lateinit var useCase: GetAppDrawerAppsUseCase

    private val appDrawerRepo = TestAppDrawerRepo()
    private val hiddenAppsRepo = TestHiddenAppsRepo()
    private val searchQueryFlow = MutableStateFlow("")

    @Before
    fun setup() {
        useCase = GetAppDrawerAppsUseCase(
            appDrawerRepo = appDrawerRepo,
            hiddenAppsRepo = hiddenAppsRepo
        )
    }

    @Test
    fun `01 - when query matches mid-word, app is returned`() = runCoroutineTest {
        val clashRoyale = app(name = "Clash Royale", packageName = "com.supercell.clashroyale")
        appDrawerRepo.addApps(
            apps = listOf(
                clashRoyale,
                app(name = "Chrome", packageName = "com.android.chrome")
            )
        )

        assertThat(useCase(searchQueryFlow = searchQueryFlow).first()).hasSize(2)
        searchQueryFlow.value = "Royale"
        assertThat(useCase(searchQueryFlow = searchQueryFlow).first()).containsExactly(clashRoyale)
    }

    @Test
    fun `02 - when query has surrounding spaces, it is trimmed`() = runCoroutineTest {
        val clashRoyale = app(name = "Clash Royale", packageName = "com.supercell.clashroyale")
        appDrawerRepo.addApps(apps = listOf(clashRoyale))

        assertThat(useCase(searchQueryFlow = searchQueryFlow).first()).hasSize(1)
        searchQueryFlow.value = "  Royale  "
        assertThat(useCase(searchQueryFlow = searchQueryFlow).first()).containsExactly(clashRoyale)
    }

    private fun app(name: String, packageName: String): App = App(
        name = name,
        packageName = packageName,
        isSystem = false
    )

    private class TestAppDrawerRepo : AppDrawerRepo {
        private val appsFlow = MutableStateFlow(emptyList<App>())
        override val allAppsFlow = appsFlow

        override suspend fun getAppBy(packageName: String): App? = appsFlow.value.firstOrNull { it.packageName == packageName }
        override suspend fun addApps(apps: List<App>) = appsFlow.update { it + apps }
        override suspend fun addApp(app: App) = appsFlow.update { it + app }
        override suspend fun removeApp(app: App) = appsFlow.update { it - app }
        override suspend fun clearApps() = appsFlow.update { emptyList() }
        override suspend fun updateDisplayName(app: App, displayName: String) = appsFlow.update { apps ->
            apps.map { if (it.packageName == app.packageName) it.copy(displayName = displayName) else it }
        }
        override suspend fun areAppsEmptyInDatabase(): Boolean = appsFlow.value.isEmpty()
    }

    private class TestHiddenAppsRepo : HiddenAppsRepo {
        private val hiddenAppsFlow = MutableStateFlow(emptyList<App>())
        override val onlyHiddenAppsFlow = hiddenAppsFlow

        override suspend fun addToHiddenApps(app: App) = hiddenAppsFlow.update { it + app }
        override suspend fun addToHiddenApps(apps: List<App>) = hiddenAppsFlow.update { it + apps }
        override suspend fun removeFromHiddenApps(packageName: String) = hiddenAppsFlow.update { apps ->
            apps.filterNot { it.packageName == packageName }
        }
        override suspend fun clearHiddenApps() = hiddenAppsFlow.update { emptyList() }
        override suspend fun isHidden(packageName: String): Boolean = hiddenAppsFlow.value.any { it.packageName == packageName }
    }
}
