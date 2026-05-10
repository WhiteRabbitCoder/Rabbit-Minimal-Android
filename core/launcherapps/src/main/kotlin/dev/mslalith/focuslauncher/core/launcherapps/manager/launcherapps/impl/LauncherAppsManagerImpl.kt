package dev.mslalith.focuslauncher.core.launcherapps.manager.launcherapps.impl

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Process
import android.provider.Telephony
import android.telecom.TelecomManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mslalith.focuslauncher.core.data.repository.AppDrawerRepo
import dev.mslalith.focuslauncher.core.launcherapps.manager.launcherapps.LauncherAppsManager
import dev.mslalith.focuslauncher.core.lint.kover.IgnoreInKoverReport
import dev.mslalith.focuslauncher.core.model.app.App
import dev.mslalith.focuslauncher.core.model.app.AppWithComponent
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@IgnoreInKoverReport
internal class LauncherAppsManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDrawerRepo: AppDrawerRepo
) : LauncherAppsManager {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    override suspend fun loadAllApps(): List<AppWithComponent> = buildList {
        val allApps = appDrawerRepo.allAppsFlow.firstOrNull().orEmpty()
        val launcherActivityInfos = launcherApps.getActivityList(null, Process.myUserHandle())
        val launcherPackages = launcherActivityInfos.mapTo(mutableSetOf()) { it.applicationInfo.packageName }

        for (launcherActivityInfo in launcherActivityInfos) {
            val applicationInfo = launcherActivityInfo.applicationInfo
            val localApp = allApps.find { it.packageName == applicationInfo.packageName }
            val appWithComponent = appWithComponent(
                localApp = localApp,
                packageName = applicationInfo.packageName,
                appName = applicationInfo.loadLabel(context.packageManager).toString(),
                componentName = launcherActivityInfo.componentName,
                applicationInfo = applicationInfo
            )
            add(appWithComponent)
        }

        queryLaunchableActivities()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName
                if (packageName in launcherPackages) return@mapNotNull null

                val applicationInfo = activityInfo.applicationInfo
                    ?: context.packageManager.getApplicationInfo(packageName, 0)
                val localApp = allApps.find { it.packageName == packageName }
                val appName = resolveInfo.loadLabel(context.packageManager).toString()

                appWithComponent(
                    localApp = localApp,
                    packageName = packageName,
                    appName = appName,
                    componentName = ComponentName(packageName, activityInfo.name),
                    applicationInfo = applicationInfo
                )
            }.forEach(::add)
    }

    override suspend fun loadApp(packageName: String): AppWithComponent? {
        val localApp = appDrawerRepo.getAppBy(packageName = packageName)
        val launcherActivityInfo = launcherApps.getActivityList(packageName, Process.myUserHandle()).firstOrNull()

        if (launcherActivityInfo != null) {
            return appWithComponent(
                localApp = localApp,
                packageName = packageName,
                appName = launcherActivityInfo.label.toString(),
                componentName = launcherActivityInfo.componentName,
                applicationInfo = launcherActivityInfo.applicationInfo
            )
        }

        val resolveInfo = queryLaunchableActivities(packageName).firstOrNull()
        return resolveInfo?.toAppWithComponent(localApp = localApp, packageName = packageName)
    }

    override suspend fun defaultFavoriteApps(): List<AppWithComponent> = listOfNotNull(defaultDialerApp(), defaultMessagingApp())

    private fun ApplicationInfo.isSystemApp() = try {
        (flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
    } catch (ex: PackageManager.NameNotFoundException) {
        false
    }

    private suspend fun defaultDialerApp(): AppWithComponent? {
        val manager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return manager.defaultDialerPackage?.let { loadApp(packageName = it) }
    }

    private suspend fun defaultMessagingApp(): AppWithComponent? {
        val packageName: String? = Telephony.Sms.getDefaultSmsPackage(context)
        return packageName?.let { loadApp(packageName = it) }
    }

    private fun appWithComponent(
        localApp: App?,
        packageName: String,
        appName: String,
        componentName: ComponentName,
        applicationInfo: ApplicationInfo
    ): AppWithComponent {
        if (localApp != null) {
            val displayName = if (localApp.name == localApp.displayName) appName else localApp.displayName
            return AppWithComponent(
                app = localApp.copy(
                    name = appName,
                    displayName = displayName
                ),
                componentName = componentName
            )
        }

        return AppWithComponent(
            app = App(
                name = appName,
                packageName = packageName,
                isSystem = applicationInfo.isSystemApp()
            ),
            componentName = componentName
        )
    }

    private fun queryLaunchableActivities(packageName: String? = null): List<ResolveInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageName?.let { launcherIntent.setPackage(it) }
        return context.packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
    }

    private fun ResolveInfo.toAppWithComponent(localApp: App?, packageName: String): AppWithComponent? {
        val activityInfo = activityInfo ?: return null
        val appName = loadLabel(context.packageManager).toString()
        val applicationInfo = activityInfo.applicationInfo ?: return null
        return appWithComponent(
            localApp = localApp,
            packageName = packageName,
            appName = appName,
            componentName = ComponentName(packageName, activityInfo.name),
            applicationInfo = applicationInfo
        )
    }
}
