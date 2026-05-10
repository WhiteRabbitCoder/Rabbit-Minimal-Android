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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import dev.mslalith.focuslauncher.feature.clock24.widget.ClockWidgetUiComponent
import dev.mslalith.focuslauncher.feature.favorites.FavoritesListUiComponent

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
        modifier = modifier
    )
}

@Composable
private fun HomePageContent(
    state: HomePageState,
    onClockWidgetClick: () -> Unit,
    onNavigateToAiScreen: () -> Unit,
    onNavigateToSettings: () -> Unit,
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
        ) {
            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

            HomeProgressSection(
                dayProgress = state.dayProgress,
                yearProgress = state.yearProgress
            )

            Spacer(modifier = Modifier.height(12.dp))

            HomeContextRow(weatherText = state.weatherText)

            Spacer(modifier = Modifier.weight(1f))

            FavoritesListUiComponent(
                state = state.favoritesListUiComponentState,
                contentPadding = 0.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            HomeBottomBar(
                onNavigateToAiScreen = onNavigateToAiScreen,
                onNavigateToSettings = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
