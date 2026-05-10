package dev.mslalith.focuslauncher.feature.homepage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Process
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import dev.mslalith.focuslauncher.core.ui.effects.OnLifecycleEventChange
import dev.mslalith.focuslauncher.core.ui.extensions.clickableNoRipple
import kotlin.math.roundToInt

private const val SCREEN_TIME_LIMIT_MINUTES = 240L // 4 hours
private const val BAR_CHARS = 20

@Composable
internal fun HomeProgressSection(
    dayProgress: Float,
    yearProgress: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var screenTimeMinutes by remember { mutableStateOf(getTodayScreenTimeMinutes(context)) }

    OnLifecycleEventChange { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            screenTimeMinutes = getTodayScreenTimeMinutes(context)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
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
        }
    }
}

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
        AsciiProgressBar(
            progress = progress,
            color = barColor
        )
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

@Composable
internal fun HomeContextRow(
    weatherText: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (batteryLevel, isCharging) = remember(context) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).roundToInt() else 0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        pct to charging
    }

    val batteryText = if (isCharging) "+$batteryLevel%" else "$batteryLevel%"

    Text(
        text = "$batteryText   $weatherText",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

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
            text = "pix",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickableNoRipple(onClick = onNavigateToAiScreen)
        )
    }
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
