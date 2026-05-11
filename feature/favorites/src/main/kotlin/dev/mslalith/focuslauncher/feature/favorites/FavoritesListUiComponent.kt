package dev.mslalith.focuslauncher.feature.favorites

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import com.slack.circuit.overlay.LocalOverlayHost
import dev.mslalith.focuslauncher.core.circuitoverlay.bottomsheet.showBottomSheet
import dev.mslalith.focuslauncher.core.common.extensions.launchApp
import dev.mslalith.focuslauncher.core.model.app.AppWithColor
import dev.mslalith.focuslauncher.core.screens.BottomSheetScreen
import dev.mslalith.focuslauncher.core.screens.FavoritesBottomSheetScreen
import dev.mslalith.focuslauncher.core.ui.effects.OnLifecycleEventChange
import java.util.Calendar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
fun FavoritesListUiComponent(
    state: FavoritesListUiComponentState,
    contentPadding: Dp,
    modifier: Modifier = Modifier
) {
    // Need to extract the eventSink out to a local val, so that the Compose Compiler
    // treats it as stable. See: https://issuetracker.google.com/issues/256100927
    val eventSink = state.eventSink

    FavoritesListUiComponent(
        modifier = modifier,
        favoritesList = state.favoritesList,
        addDefaultAppsToFavorites = { eventSink(FavoritesListUiComponentUiEvent.AddDefaultAppsIfRequired) },
        contentPadding = contentPadding
    )
}

@Composable
private fun FavoritesListUiComponent(
    favoritesList: ImmutableList<AppWithColor>,
    addDefaultAppsToFavorites: () -> Unit,
    contentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val overlayHost = LocalOverlayHost.current
    var usageMap by remember { mutableStateOf(getUsageMap(context)) }

    OnLifecycleEventChange { event ->
        if (event == Lifecycle.Event.ON_RESUME) usageMap = getUsageMap(context)
    }

    LaunchedEffect(key1 = favoritesList.isEmpty()) {
        if (favoritesList.isNotEmpty()) return@LaunchedEffect

        addDefaultAppsToFavorites()
    }

    fun showBottomSheet(
        screen: BottomSheetScreen<Unit>,
        skipPartiallyExpanded: Boolean = true
    ) {
        scope.launch {
            overlayHost.showBottomSheet(
                screen = screen,
                skipPartiallyExpanded = skipPartiallyExpanded
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = contentPadding)
    ) {
        favoritesList.take(4).forEach { favoriteAppWithColor ->
            ReusableContent(key = favoriteAppWithColor.app) {
                FavoriteItem(
                    appWithColor = favoriteAppWithColor,
                    usageMinutes = usageMap[favoriteAppWithColor.app.packageName] ?: 0L,
                    onClick = { context.launchApp(app = favoriteAppWithColor.app) },
                    onLongClick = { showBottomSheet(screen = FavoritesBottomSheetScreen, skipPartiallyExpanded = false) }
                )
            }
        }
    }
}

@Composable
private fun FavoriteItem(
    appWithColor: AppWithColor,
    usageMinutes: Long,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usageText = when {
        usageMinutes >= 60 -> "${usageMinutes / 60}h ${usageMinutes % 60}m"
        usageMinutes > 0 -> "${usageMinutes}m"
        else -> null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(appWithColor.app.packageName) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
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
            text = appWithColor.app.displayName,
            fontSize = 32.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (usageText != null) 12.dp else 0.dp)
        )
    }
}

private fun getUsageMap(context: Context): Map<String, Long> = runCatching {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    if (mode != AppOpsManager.MODE_ALLOWED) return@runCatching emptyMap()
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val cal = Calendar.getInstance()
    val endTime = cal.timeInMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startTime = cal.timeInMillis
    usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        .filter { it.totalTimeInForeground > 0 }
        .associate { it.packageName to (it.totalTimeInForeground / 60_000L) }
}.getOrDefault(emptyMap())
