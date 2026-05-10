package dev.mslalith.focuslauncher.core.domain.launcherapps

import dev.mslalith.focuslauncher.core.data.repository.AppDrawerRepo
import kotlinx.coroutines.flow.firstOrNull
import dev.mslalith.focuslauncher.core.launcherapps.manager.launcherapps.LauncherAppsManager
import javax.inject.Inject

class LoadAllAppsUseCase @Inject constructor(
    private val launcherAppsManager: LauncherAppsManager,
    private val appDrawerRepo: AppDrawerRepo
) {
    suspend operator fun invoke(forceLoad: Boolean = false) {
        if (!forceLoad && !appDrawerRepo.areAppsEmptyInDatabase()) {
            val existingPackages = appDrawerRepo.allAppsFlow.firstOrNull()
                .orEmpty()
                .mapTo(hashSetOf()) { it.packageName }

            val missingApps = launcherAppsManager.loadAllApps()
                .map { it.app }
                .filterNot { it.packageName in existingPackages }

            if (missingApps.isNotEmpty()) {
                appDrawerRepo.addApps(apps = missingApps)
            }
            return
        }

        appDrawerRepo.addApps(apps = launcherAppsManager.loadAllApps().map { it.app })
    }
}
