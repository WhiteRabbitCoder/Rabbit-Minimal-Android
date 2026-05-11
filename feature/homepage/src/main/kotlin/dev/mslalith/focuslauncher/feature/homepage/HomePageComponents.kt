package dev.mslalith.focuslauncher.feature.homepage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import dev.mslalith.focuslauncher.core.common.model.getOrNull
import dev.mslalith.focuslauncher.core.model.lunarphase.LunarPhase
import dev.mslalith.focuslauncher.core.ui.effects.OnLifecycleEventChange
import dev.mslalith.focuslauncher.core.ui.extensions.clickableNoRipple
import dev.mslalith.focuslauncher.feature.lunarcalendar.widget.LunarCalendarUiComponentState
import kotlin.math.roundToInt

private const val SCREEN_TIME_LIMIT_MINUTES = 240L // 4 hours
private const val BAR_CHARS = 20

// ─── Progress Section ───────────────────────────────────────────────────────

@Composable
internal fun HomeProgressSection(
    dayProgress: Float,
    yearProgress: Float,
    pomodoroTimeLeft: Int,
    pomodoroIsRunning: Boolean,
    onTogglePomodoro: () -> Unit,
    onResetPomodoro: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var screenTimeMinutes by remember { mutableStateOf(getTodayScreenTimeMinutes(context)) }

    OnLifecycleEventChange { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            screenTimeMinutes = getTodayScreenTimeMinutes(context)
        }
    }

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProgressRow(
                label = "Año en curso ${(yearProgress * 100).roundToInt()}%",
                progress = yearProgress
            )
            ProgressRow(
                label = "Día en progreso ${(dayProgress * 100).roundToInt()}%",
                progress = dayProgress
            )
            if (screenTimeMinutes >= 0) {
                val h = screenTimeMinutes / 60
                val m = screenTimeMinutes % 60
                val screenProgress = (screenTimeMinutes / SCREEN_TIME_LIMIT_MINUTES.toFloat()).coerceIn(0f, 1f)
                ProgressRow(
                    label = "Pantalla hoy ${h}h ${m}m",
                    progress = screenProgress,
                    overLimit = screenTimeMinutes >= SCREEN_TIME_LIMIT_MINUTES
                )
            } else {
                // No permission — show a subtle prompt
                Text(
                    text = "[ activar tiempo de pantalla ]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickableNoRipple {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        PomodoroUiComponent(
            timeLeft = pomodoroTimeLeft,
            isRunning = pomodoroIsRunning,
            onToggle = onTogglePomodoro,
            onReset = onResetPomodoro
        )
    }
}

// ─── Pomodoro ───────────────────────────────────────────────────────────────

@Composable
private fun PomodoroUiComponent(
    timeLeft: Int,
    isRunning: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val mStr = minutes.toString().padStart(2, '0')
    val sStr = seconds.toString().padStart(2, '0')

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$mStr:$sStr",
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickableNoRipple(onClick = onToggle)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = if (isRunning) "[||]" else "[|>]",
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.clickableNoRipple(onClick = onToggle)
            )
            Text(
                text = "[X]",
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.clickableNoRipple(onClick = onReset)
            )
        }
    }
}

// ─── Progress Row & Bar ─────────────────────────────────────────────────────

@Composable
private fun ProgressRow(
    label: String,
    progress: Float,
    overLimit: Boolean = false
) {
    val barColor = if (overLimit) Color(0xFFF87171) else MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        AsciiProgressBar(progress = progress, color = barColor)
    }
}

@Composable
private fun AsciiProgressBar(
    progress: Float,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val filled = (BAR_CHARS * progress.coerceIn(0f, 1f)).roundToInt()
    val bar = "█".repeat(filled) + "░".repeat(BAR_CHARS - filled)

    Text(
        text = bar,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 0.sp,
        modifier = modifier
    )
}

// ─── Context Row (battery + weather + moon) ─────────────────────────────────

@Composable
internal fun HomeContextRow(
    weatherText: String,
    lunarCalendarState: LunarCalendarUiComponentState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var batteryStatus by remember(context) { mutableStateOf(context.readBatteryStatus()) }

    DisposableEffect(context) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                batteryStatus = context.readBatteryStatus(intent)
            }
        }
        val stickyIntent = context.registerReceiver(receiver, filter)
        batteryStatus = context.readBatteryStatus(stickyIntent)
        onDispose { context.unregisterReceiver(receiver) }
    }

    val (batteryLevel, isCharging) = batteryStatus
    val batteryText = if (isCharging) "+$batteryLevel%" else "$batteryLevel%"
    val lunarDetails = lunarCalendarState.lunarPhaseDetails.getOrNull()

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = modifier
    ) {
        Text(
            text = "$batteryText   $weatherText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alignByBaseline()
        )
        if (lunarDetails != null) {
            Text(
                text = lunarDetails.lunarPhase.toAsciiGlyph(),
                fontSize = 18.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}

private fun LunarPhase.toAsciiGlyph(): String = when (this) {
    LunarPhase.NEW_MOON        -> "○"
    LunarPhase.WAXING_CRESCENT -> "◔"
    LunarPhase.FIRST_QUARTER   -> "◑"
    LunarPhase.WAXING_GIBBOUS  -> "◕"
    LunarPhase.FULL_MOON       -> "●"
    LunarPhase.WANING_GIBBOUS  -> "◕"
    LunarPhase.LAST_QUARTER    -> "◐"
    LunarPhase.WANING_CRESCENT -> "◔"
}

// ─── Top 4 Most Used Apps ────────────────────────────────────────────────────

private data class TopApp(val packageName: String, val displayName: String, val minutes: Long)

@Composable
internal fun TopUsedAppsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var topApps by remember { mutableStateOf(getTop4UsedApps(context)) }

    OnLifecycleEventChange { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            topApps = getTop4UsedApps(context)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        topApps.forEach { app ->
            TopUsedAppItem(
                displayName = app.displayName,
                usageMinutes = app.minutes,
                onClick = {
                    runCatching {
                        context.packageManager.getLaunchIntentForPackage(app.packageName)
                            ?.let { context.startActivity(it) }
                    }
                }
            )
        }
    }
}

@Composable
private fun TopUsedAppItem(
    displayName: String,
    usageMinutes: Long,
    onClick: () -> Unit
) {
    val usageText = when {
        usageMinutes >= 60 -> "${usageMinutes / 60}h ${usageMinutes % 60}m"
        usageMinutes > 0   -> "${usageMinutes}m"
        else               -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        if (usageText != null) {
            Text(
                text = usageText,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
        Text(
            text = displayName,
            fontSize = 32.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (usageText != null) 12.dp else 0.dp)
        )
    }
}

private fun getTop4UsedApps(context: Context): List<TopApp> = runCatching {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    if (mode != AppOpsManager.MODE_ALLOWED) return@runCatching emptyList()

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val cal = java.util.Calendar.getInstance()
    val endTime = cal.timeInMillis
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    val startTime = cal.timeInMillis

    val pm = context.packageManager
    usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        .filter { it.totalTimeInForeground > 0 && it.packageName != context.packageName }
        .sortedByDescending { it.totalTimeInForeground }
        .take(4)
        .mapNotNull { stat ->
            pm.getLaunchIntentForPackage(stat.packageName) ?: return@mapNotNull null
            val appInfo = runCatching { pm.getApplicationInfo(stat.packageName, 0) }.getOrNull()
                ?: return@mapNotNull null
            val name = pm.getApplicationLabel(appInfo).toString()
            TopApp(stat.packageName, name, stat.totalTimeInForeground / 60_000L)
        }
}.getOrDefault(emptyList())

// ─── Bottom Bar ──────────────────────────────────────────────────────────────

@Composable
internal fun HomeBottomBar(
    onNavigateToAiScreen: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "...",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(40.dp)
                .clickableNoRipple(onClick = onNavigateToSettings)
        )
        Text(
            text = "Pix",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickableNoRipple(onClick = onNavigateToAiScreen)
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun Context.readBatteryStatus(intent: Intent? = null): Pair<Int, Boolean> {
    val batteryIntent = intent ?: registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val batteryLevel = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).roundToInt() else 0
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return batteryLevel to isCharging
}

private fun getTodayScreenTimeMinutes(context: Context): Long = runCatching {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    if (mode != AppOpsManager.MODE_ALLOWED) return@runCatching -1L

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val cal = java.util.Calendar.getInstance()
    val endTime = cal.timeInMillis
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    val startTime = cal.timeInMillis

    val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
    stats.sumOf { it.totalTimeInForeground } / 60_000L
}.getOrDefault(-1L)
