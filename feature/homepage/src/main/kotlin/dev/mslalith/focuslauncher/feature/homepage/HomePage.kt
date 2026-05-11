package dev.mslalith.focuslauncher.feature.homepage

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.provider.AlarmClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.slack.circuit.codegen.annotations.CircuitInject
import dagger.hilt.components.SingletonComponent
import dev.mslalith.focuslauncher.core.common.extensions.openNotificationShade
import dev.mslalith.focuslauncher.core.screens.HomePageScreen
import dev.mslalith.focuslauncher.core.ui.effects.OnLifecycleEventChange
import dev.mslalith.focuslauncher.core.ui.extensions.onSwipeDown
import dev.mslalith.focuslauncher.core.ui.extensions.onSwipeUp
import dev.mslalith.focuslauncher.core.ui.extensions.onHorizontalSwipe
import dev.mslalith.focuslauncher.feature.clock24.widget.ClockWidgetUiComponent
import dev.mslalith.focuslauncher.feature.favorites.FavoritesListUiComponent
import dev.mslalith.focuslauncher.feature.homepage.widget.MediaPlayerWidget

@CircuitInject(HomePageScreen::class, SingletonComponent::class)
@Composable
fun HomePage(
    state: HomePageState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) sendLocation(context, state)
    }

    OnLifecycleEventChange { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                sendLocation(context, state)
            } else if (!permissionGranted) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            
            state.eventSink(HomePageUiEvent.CheckMediaPermission)
        }
    }

    fun openClockApp() {
        runCatching {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            context.startActivity(intent)
        }
    }

    HomePageContent(
        state = state,
        onClockWidgetClick = ::openClockApp,
        onNavigateToAiScreen = { state.eventSink(HomePageUiEvent.NavigateToAiScreen) },
        onNavigateToSettings = { state.eventSink(HomePageUiEvent.NavigateToSettings) },
        onNavigateToAppDrawer = {},
        modifier = modifier
    )
}

@SuppressLint("MissingPermission")
private fun sendLocation(context: Context, state: HomePageState) {
    runCatching {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val lastKnown = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if (lastKnown != null) {
            state.eventSink(HomePageUiEvent.LocationGranted(lastKnown.latitude, lastKnown.longitude))
            return
        }

        // No cached location — request a fresh one from the first enabled provider
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        for (provider in providers) {
            if (lm.isProviderEnabled(provider)) {
                lm.requestLocationUpdates(provider, 0L, 0f, object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        state.eventSink(HomePageUiEvent.LocationGranted(loc.latitude, loc.longitude))
                        lm.removeUpdates(this)
                    }
                })
                break
            }
        }
    }
}

// Second overload used by Launcher.kt
@Composable
fun HomePage(
    state: HomePageState,
    onNavigateToAiScreen: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAppDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) sendLocation(context, state)
    }

    OnLifecycleEventChange { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                sendLocation(context, state)
            } else if (!permissionGranted) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

            state.eventSink(HomePageUiEvent.CheckMediaPermission)
        }
    }

    fun openClockApp() {
        runCatching {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            context.startActivity(intent)
        }
    }

    HomePageContent(
        state = state,
        onClockWidgetClick = ::openClockApp,
        onNavigateToAiScreen = onNavigateToAiScreen,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToAppDrawer = onNavigateToAppDrawer,
        modifier = modifier
    )
}

@Composable
private fun HomePageContent(
    state: HomePageState,
    onClockWidgetClick: () -> Unit,
    onNavigateToAiScreen: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAppDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .onSwipeDown(enabled = state.isPullDownNotificationShadeEnabled) {
                    context.openNotificationShade()
                }
                .onSwipeUp {
                    onNavigateToAppDrawer()
                }
                .onHorizontalSwipe(
                    onSwipeRight = {
                        runCatching {
                            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.googlequicksearchbox")
                            if (intent != null) {
                                context.startActivity(intent)
                            } else {
                                val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
                                context.startActivity(browserIntent)
                            }
                        }
                    }
                )
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            ClockWidgetUiComponent(
                state = state.clockWidgetUiComponentState,
                horizontalPadding = 0.dp,
                onClick = onClockWidgetClick
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = state.date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(12.dp))

            HomeProgressSection(
                dayProgress = state.dayProgress,
                yearProgress = state.yearProgress,
                pomodoroTimeLeft = state.pomodoroTimeLeft,
                pomodoroIsRunning = state.pomodoroIsRunning,
                onTogglePomodoro = { state.eventSink(HomePageUiEvent.TogglePomodoro) },
                onResetPomodoro = { state.eventSink(HomePageUiEvent.ResetPomodoro) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            HomeContextRow(
                weatherText = state.weatherText,
                lunarCalendarState = state.lunarCalendarUiComponentState
            )

            Spacer(modifier = Modifier.weight(0.325f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                MediaPlayerWidget(
                    state = state.mediaState,
                    onPlayPauseClick = { state.eventSink(HomePageUiEvent.ToggleMediaPlayback) },
                    onNextClick = { state.eventSink(HomePageUiEvent.SkipMediaToNext) },
                    onPrevClick = { state.eventSink(HomePageUiEvent.SkipMediaToPrevious) },
                    onOpenApp = {
                        val pkg = state.mediaState.packageName
                        if (pkg.isNotEmpty()) {
                            runCatching {
                                context.packageManager.getLaunchIntentForPackage(pkg)
                                    ?.let { context.startActivity(it) }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!state.mediaState.hasPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.Text(
                    text = "Grant Media Permission",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clickable {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.weight(0.325f))

            TopUsedAppsSection()

            Spacer(modifier = Modifier.weight(0.35f))

            HomeBottomBar(
                onNavigateToAiScreen = onNavigateToAiScreen,
                onNavigateToSettings = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
