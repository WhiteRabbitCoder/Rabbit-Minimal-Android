package dev.mslalith.focuslauncher.screens.launcher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.internal.BackHandler
import dagger.hilt.components.SingletonComponent
import dev.mslalith.focuslauncher.core.screens.LauncherScreen
import dev.mslalith.focuslauncher.core.ui.controller.toggleStatusBars
import dev.mslalith.focuslauncher.core.ui.effects.OnLifecycleEventChange
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
    var isAppDrawerVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.showStatusBar) {
        systemUiController.toggleStatusBars(show = state.showStatusBar)
    }

    OnLifecycleEventChange { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            isAppDrawerVisible = false
            if (pagerState.currentPage != 1) {
                coroutineScope.launch { pagerState.animateScrollToPage(page = 1) }
            }
        }
    }

    BackHandler {
        when {
            isAppDrawerVisible -> isAppDrawerVisible = false
            pagerState.currentPage != 1 -> coroutineScope.launch {
                pagerState.animateScrollToPage(page = 1)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        AnimatedContent(
            targetState = isAppDrawerVisible,
            transitionSpec = {
                if (targetState) {
                    slideInVertically(initialOffsetY = { it }) togetherWith
                        slideOutVertically(targetOffsetY = { -it })
                } else {
                    slideInVertically(initialOffsetY = { -it }) togetherWith
                        slideOutVertically(targetOffsetY = { it })
                }
            },
            label = "LauncherContent",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = paddingValues)
                .consumeWindowInsets(paddingValues = paddingValues)
        ) { appDrawerVisible ->
            if (appDrawerVisible) {
                AppDrawerPage(
                    state = state.appDrawerPageState,
                    isVisible = true,
                    onDismissRequest = { isAppDrawerVisible = false },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    beyondBoundsPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> DiscoveryPage()
                        1 -> HomePage(
                            state = state.homePageState,
                            onNavigateToAiScreen = { state.eventSink(LauncherUiEvent.NavigateToAiScreen) },
                            onNavigateToSettings = { state.eventSink(LauncherUiEvent.NavigateToSettings) },
                            onNavigateToAppDrawer = { isAppDrawerVisible = true },
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscoveryPage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Discovery",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
