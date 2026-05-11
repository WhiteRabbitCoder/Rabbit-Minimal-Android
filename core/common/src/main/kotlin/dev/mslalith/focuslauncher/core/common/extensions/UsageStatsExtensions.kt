package dev.mslalith.focuslauncher.core.common.extensions

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.Calendar

fun getStartOfTodayMillis(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

/**
 * Returns launchable app package names (ACTION_MAIN + CATEGORY_LAUNCHER) as a set for fast filtering.
 */
fun PackageManager.getLaunchablePackages(): Set<String> {
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        queryIntentActivities(launcherIntent, 0)
    }
    return resolveInfos.mapNotNull { it.activityInfo?.packageName }.toSet()
}

/**
 * Returns true only for user-launchable, non-system apps excluding the launcher app itself.
 */
fun PackageManager.isUserLaunchableApp(
    packageName: String,
    selfPackageName: String,
    launchablePackages: Set<String>
): Boolean {
    if (packageName == selfPackageName || packageName !in launchablePackages) return false
    val appInfo = runCatching { getApplicationInfo(packageName, 0) }.getOrNull() ?: return false
    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    return !isSystemApp
}
