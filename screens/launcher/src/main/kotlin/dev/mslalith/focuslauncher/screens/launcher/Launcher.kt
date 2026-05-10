package dev.mslalith.focuslauncher.screens.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import dev.mslalith.focuslauncher.core.ui.effects.OnLifecycleEventChange
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.internal.BackHandler
import dagger.hilt.components.SingletonComponent
import dev.mslalith.focuslauncher.core.screens.LauncherScreen
import dev.mslalith.focuslauncher.core.ui.controller.toggleStatusBars
import dev.mslalith.focuslauncher.core.ui.providers.LocalLauncherPagerState
import dev.mslalith.focuslauncher.core.ui.providers.LocalSystemUiController
import dev.mslalith.focuslauncher.core.ui.providers.ProvideLauncherPagerState
import dev.mslalith.focuslauncher.feature.appdrawerpage.AppDrawerPage
import dev.mslalith.focuslauncher.feature.homepage.HomePage
import kotlinx.coroutines.launch

@CircuitInject(LauncherScreen::class, SingletonComponent::class)
@Composable
fun Launcher(
    state: LauncherState,
    modifier: Modifier = Modifier
) {
    ProvideLauncherPagerState {
        LauncherInternal(
            state = state,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherInternal(
    state: LauncherState,
    modifier: Modifier = Modifier
) {
    val pagerState = LocalLauncherPagerState.current
    val coroutineScope = rememberCoroutineScope()
    val systemUiController = LocalSystemUiController.current

    LaunchedEffect(state.showStatusBar) {
        systemUiController.toggleStatusBars(show = state.showStatusBar)
    }

    OnLifecycleEventChange { event ->
        if (event == Lifecycle.Event.ON_RESUME && pagerState.currentPage != 1) {
            coroutineScope.launch { pagerState.animateScrollToPage(page = 1) }
        }
    }

    BackHandler {
        when {
            pagerState.currentPage != 1 -> coroutineScope.launch {
                pagerState.animateScrollToPage(page = 1)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            beyondBoundsPageCount = 2,
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .consumeWindowInsets(paddingValues = paddingValues)
        ) { page ->
            when (page) {
                0 -> DiscoveryPage()
                1 -> HomePage(
                    state = state.homePageState,
                    onNavigateToAiScreen = { state.eventSink(LauncherUiEvent.NavigateToAiScreen) },
                    onNavigateToSettings = { state.eventSink(LauncherUiEvent.NavigateToSettings) }
                )
                2 -> AppDrawerPage(state = state.appDrawerPageState)
            }
        }
    }
}

@Composable
private fun DiscoveryPage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Discovery",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
