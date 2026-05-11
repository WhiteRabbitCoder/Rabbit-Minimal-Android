package dev.mslalith.focuslauncher.feature.homepage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.BatteryManager
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import dev.mslalith.focuslauncher.core.common.model.getOrNull
import dev.mslalith.focuslauncher.core.model.lunarphase.LunarPhase
import dev.mslalith.focuslauncher.core.ui.effects.OnLifecycleEventChange
import dev.mslalith.focuslauncher.core.ui.extensions.clickableNoRipple
import dev.mslalith.focuslauncher.feature.lunarcalendar.widget.LunarCalendarUiComponentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private const val SCREEN_TIME_LIMIT_MINUTES = 240L // 4 hours
private const val BAR_CHARS = 20

private data class ScreenTimeAnalytics(
    val totalMillis: Long,
    val apps: List<ScreenTimeAppAnalytics>
)

private data class ScreenTimeAppAnalytics(
    val packageName: String,
    val displayName: String,
    val totalMillis: Long,
    val hourlyMillis: List<Long>
)

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
    var shouldShowAnalytics by remember { mutableStateOf(false) }
    var refreshAnalyticsKey by remember { mutableStateOf(0) }

    OnLifecycleEventChange { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            screenTimeMinutes = getTodayScreenTimeMinutes(context)
            refreshAnalyticsKey++
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
                    overLimit = screenTimeMinutes >= SCREEN_TIME_LIMIT_MINUTES,
                    onClick = { shouldShowAnalytics = true }
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

    if (shouldShowAnalytics && screenTimeMinutes >= 0) {
        ScreenTimeAnalyticsBottomSheet(
            refreshKey = refreshAnalyticsKey,
            onDismissRequest = { shouldShowAnalytics = false }
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
    overLimit: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val barColor = if (overLimit) Color(0xFFF87171) else MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickableNoRipple(onClick = onClick) else Modifier),
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

@Composable
private fun ScreenTimeAnalyticsBottomSheet(
    refreshKey: Int,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val analytics by produceState<ScreenTimeAnalytics?>(initialValue = null, key1 = refreshKey) {
        value = withContext(Dispatchers.IO) {
            getScreenTimeAnalytics(context)
        }
    }
    var selectedPackage by remember(refreshKey) { mutableStateOf<String?>(null) }

    val appList = analytics?.apps.orEmpty()
    LaunchedEffect(appList) {
        if (selectedPackage == null && appList.isNotEmpty()) {
            selectedPackage = appList.first().packageName
        }
    }
    val selectedApp = appList.firstOrNull { it.packageName == selectedPackage }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        when {
            analytics == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            appList.isEmpty() -> {
                Text(
                    text = "No hay datos de uso para hoy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }

            else -> {
                val totalMillis = analytics?.totalMillis ?: 0L
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Distribución diaria",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    appList.forEach { app ->
                        val iconBitmap = remember(app.packageName) {
                            runCatching {
                                context.packageManager.getApplicationIcon(app.packageName).toBitmap().asImageBitmap()
                            }.getOrNull()
                        }
                        val progress = if (totalMillis > 0L) {
                            (app.totalMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (selectedApp?.packageName == app.packageName) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickableNoRipple { selectedPackage = app.packageName }
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (iconBitmap != null) {
                                    Image(
                                        bitmap = iconBitmap,
                                        contentDescription = app.displayName,
                                        modifier = Modifier.size(28.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.displayName.firstOrNull()?.uppercase() ?: "?",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = formatDurationMillis(app.totalMillis),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

                    Text(
                        text = "Detalle horario",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (selectedApp == null) {
                        Text(
                            text = "Selecciona una aplicación para ver su uso por hora",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val maxHourMillis = selectedApp.hourlyMillis.maxOrNull()?.coerceAtLeast(1L) ?: 1L
                        val usedHours = selectedApp.hourlyMillis
                            .mapIndexedNotNull { hour, millis ->
                                if (millis > 0L) hour to millis else null
                            }

                        if (usedHours.isEmpty()) {
                            Text(
                                text = "Sin actividad horaria registrada para ${selectedApp.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            usedHours.forEach { (hour, millis) ->
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = buildHourLabel(hour),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = formatDurationMillis(millis),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (millis.toFloat() / maxHourMillis.toFloat()).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
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

private fun getScreenTimeAnalytics(context: Context): ScreenTimeAnalytics = runCatching {
    if (!hasUsageStatsPermission(context)) return@runCatching ScreenTimeAnalytics(0L, emptyList())

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val pm = context.packageManager
    val startTime = getStartOfTodayMillis()
    val endTime = System.currentTimeMillis()

    val totalByPackage = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        .filter { it.totalTimeInForeground > 0L }
        .associate { it.packageName to it.totalTimeInForeground }

    val hourlyByPackage = mutableMapOf<String, LongArray>()
    val activeSessions = mutableMapOf<String, Long>()
    val usageEvents = usm.queryEvents(startTime, endTime)
    val event = UsageEvents.Event()

    while (usageEvents.hasNextEvent()) {
        usageEvents.getNextEvent(event)
        val packageName = event.packageName ?: continue
        when (event.eventType) {
            UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                activeSessions[packageName] = event.timeStamp
            }

            UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                val start = activeSessions.remove(packageName) ?: continue
                if (event.timeStamp > start) {
                    val hourly = hourlyByPackage.getOrPut(packageName) { LongArray(24) }
                    addTimeToHourlyBuckets(start, event.timeStamp, hourly)
                }
            }
        }
    }

    activeSessions.forEach { (packageName, start) ->
        if (endTime > start) {
            val hourly = hourlyByPackage.getOrPut(packageName) { LongArray(24) }
            addTimeToHourlyBuckets(start, endTime, hourly)
        }
    }

    val appStats = totalByPackage.mapNotNull { (packageName, totalMillis) ->
        if (packageName == context.packageName) return@mapNotNull null
        val appInfo = runCatching { pm.getApplicationInfo(packageName, 0) }.getOrNull()
            ?: return@mapNotNull null
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        if (isSystemApp) return@mapNotNull null
        pm.getLaunchIntentForPackage(packageName) ?: return@mapNotNull null
        val appLabel = pm.getApplicationLabel(appInfo).toString()
        ScreenTimeAppAnalytics(
            packageName = packageName,
            displayName = appLabel,
            totalMillis = totalMillis,
            hourlyMillis = hourlyByPackage[packageName]?.toList() ?: List(24) { 0L }
        )
    }.sortedByDescending { it.totalMillis }

    ScreenTimeAnalytics(
        totalMillis = appStats.sumOf { it.totalMillis },
        apps = appStats
    )
}.getOrDefault(ScreenTimeAnalytics(0L, emptyList()))

private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun getStartOfTodayMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun addTimeToHourlyBuckets(
    startMillis: Long,
    endMillis: Long,
    hourlyBuckets: LongArray
) {
    var current = startMillis
    val currentCalendar = Calendar.getInstance()
    val nextBoundaryCalendar = Calendar.getInstance()
    while (current < endMillis) {
        currentCalendar.timeInMillis = current
        val hourOfDay = currentCalendar.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
        nextBoundaryCalendar.apply {
            timeInMillis = current
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, 1)
        }
        val nextHourBoundary = nextBoundaryCalendar.timeInMillis
        val segmentEnd = minOf(endMillis, nextHourBoundary)
        hourlyBuckets[hourOfDay] += (segmentEnd - current).coerceAtLeast(0L)
        current = segmentEnd
    }
}

private fun formatDurationMillis(durationMillis: Long): String {
    if (durationMillis < 60_000L) return "Menos de un minuto"
    val totalMinutes = durationMillis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0L -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

private fun buildHourLabel(hour: Int): String {
    val nextHour = (hour + 1) % 24
    return String.format(Locale.getDefault(), "%02d:00 - %02d:00", hour, nextHour)
}
