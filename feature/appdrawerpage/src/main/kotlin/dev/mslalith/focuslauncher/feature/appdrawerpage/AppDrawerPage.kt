package dev.mslalith.focuslauncher.feature.appdrawerpage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import dagger.hilt.components.SingletonComponent
import dev.mslalith.focuslauncher.core.circuitoverlay.bottomsheet.showBottomSheet
import dev.mslalith.focuslauncher.core.circuitoverlay.bottomsheet.showBottomSheetWithResult
import dev.mslalith.focuslauncher.core.common.extensions.groupByImmutable
import dev.mslalith.focuslauncher.core.common.extensions.isAlphabet
import dev.mslalith.focuslauncher.core.common.extensions.launchApp
import dev.mslalith.focuslauncher.core.common.model.LoadingState
import dev.mslalith.focuslauncher.core.model.appdrawer.AppDrawerItem
import dev.mslalith.focuslauncher.core.screens.AppDrawerPageScreen
import dev.mslalith.focuslauncher.core.screens.AppMoreOptionsBottomSheetScreen
import dev.mslalith.focuslauncher.core.screens.BottomSheetScreen
import dev.mslalith.focuslauncher.core.screens.UpdateAppDisplayNameBottomSheetScreen
import dev.mslalith.focuslauncher.core.ui.DotWaveLoader
import dev.mslalith.focuslauncher.core.ui.effects.OnDayChangeListener
import dev.mslalith.focuslauncher.core.ui.effects.OnLifecycleEventChange
import dev.mslalith.focuslauncher.core.ui.providers.LocalLauncherPagerState
import dev.mslalith.focuslauncher.feature.appdrawerpage.apps.list.AlphabetIndex
import dev.mslalith.focuslauncher.feature.appdrawerpage.apps.list.AppsList
import java.util.Calendar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@CircuitInject(AppDrawerPageScreen::class, SingletonComponent::class)
@Composable
fun AppDrawerPage(
    state: AppDrawerPageState,
    modifier: Modifier = Modifier
) {
    val eventSink = state.eventSink
    val scope = rememberCoroutineScope()
    val overlayHost = LocalOverlayHost.current

    fun showBottomSheet(screen: BottomSheetScreen<Unit>) {
        scope.launch { overlayHost.showBottomSheet(screen) }
    }

    fun showAppMoreOptionsBottomSheetScreen(appDrawerItem: AppDrawerItem) {
        scope.launch {
            when (overlayHost.showBottomSheetWithResult(AppMoreOptionsBottomSheetScreen(appDrawerItem = appDrawerItem))) {
                is AppMoreOptionsBottomSheetScreen.Result.ShowUpdateAppDisplayBottomSheet ->
                    showBottomSheet(screen = UpdateAppDisplayNameBottomSheetScreen(app = appDrawerItem.app))
                null -> Unit
            }
        }
    }

    AppDrawerPageKeyboardAware(
        modifier = modifier,
        state = state,
        onSearchQueryChange = { eventSink(AppDrawerPageUiEvent.UpdateSearchQuery(query = it)) },
        reloadIconPack = { eventSink(AppDrawerPageUiEvent.ReloadIconPack) },
        showAppMoreOptions = ::showAppMoreOptionsBottomSheetScreen
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppDrawerPageKeyboardAware(
    state: AppDrawerPageState,
    onSearchQueryChange: (String) -> Unit,
    reloadIconPack: () -> Unit,
    showAppMoreOptions: (AppDrawerItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val pagerState = LocalLauncherPagerState.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var shouldRequestKeyboardFocus by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.targetPage }.collectLatest { (currentPage, targetPage) ->
            if (targetPage == APP_DRAWER_PAGE) {
                shouldRequestKeyboardFocus = true
            }
            if (currentPage != APP_DRAWER_PAGE && targetPage != APP_DRAWER_PAGE) {
                onSearchQueryChange("")
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        }
    }

    LaunchedEffect(key1 = pagerState, key2 = shouldRequestKeyboardFocus) {
        snapshotFlow { pagerState.currentPage to pagerState.targetPage }
            .collectLatest { (_, targetPage) ->
                if (!shouldRequestKeyboardFocus) return@collectLatest
                if (targetPage != APP_DRAWER_PAGE) return@collectLatest

                focusRequester.requestFocus()
                keyboardController?.show()
                shouldRequestKeyboardFocus = false
            }
    }

    // Auto-launch when filtered results narrow to exactly one app
    val allAppsState = state.allAppsState
    LaunchedEffect(allAppsState, state.searchBarQuery) {
        if (state.searchBarQuery.isEmpty()) return@LaunchedEffect
        val loaded = allAppsState as? LoadingState.Loaded ?: return@LaunchedEffect
        if (loaded.value.size == 1) {
            focusManager.clearFocus()
            keyboardController?.hide()
            context.launchApp(app = loaded.value.first().app)
            onSearchQueryChange("")
        }
    }

    fun onAppClick(appDrawerItem: AppDrawerItem) {
        focusManager.clearFocus()
        context.launchApp(app = appDrawerItem.app)
        onSearchQueryChange("")
    }

    fun onAppLongClick(appDrawerItem: AppDrawerItem) {
        focusManager.clearFocus()
        showAppMoreOptions(appDrawerItem)
    }

    fun onAppPointerDown() {
        shouldRequestKeyboardFocus = false
        focusManager.clearFocus(force = true)
    }

    AppDrawerPageInternal(
        modifier = modifier,
        appDrawerPageState = state,
        focusRequester = focusRequester,
        onSearchQueryChange = onSearchQueryChange,
        onAppPointerDown = ::onAppPointerDown,
        onAppClick = ::onAppClick,
        onAppLongClick = ::onAppLongClick,
        reloadIconPack = reloadIconPack
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppDrawerPageInternal(
    appDrawerPageState: AppDrawerPageState,
    focusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onAppPointerDown: () -> Unit,
    onAppClick: (AppDrawerItem) -> Unit,
    onAppLongClick: (AppDrawerItem) -> Unit,
    reloadIconPack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var usageMap by remember { mutableStateOf(getUsageMap(context)) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    OnDayChangeListener { reloadIconPack() }

    OnLifecycleEventChange { event ->
        if (event == Lifecycle.Event.ON_RESUME) usageMap = getUsageMap(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        DrawerSearchField(
            query = appDrawerPageState.searchBarQuery,
            onQueryChange = onSearchQueryChange,
            focusRequester = focusRequester,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp)
        )

        when (val allAppsState = appDrawerPageState.allAppsState) {
            is LoadingState.Loaded -> {
                val groupedApps by remember(key1 = allAppsState.value) {
                    derivedStateOf {
                        allAppsState.value.groupByImmutable { app ->
                            app.app.displayName.first()
                                .let { if (it.isAlphabet()) it.uppercaseChar() else '#' }
                        }
                    }
                }
                val characters = remember(groupedApps) { groupedApps.keys.toList() }
                val charToIndex = remember(characters) {
                    characters.mapIndexed { i, c -> c to i }.toMap()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AppsList(
                        groupedApps = groupedApps,
                        listState = listState,
                        usageMap = usageMap,
                        showAppGroupHeader = false,
                        onAppPointerDown = onAppPointerDown,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 24.dp, end = 52.dp)
                    )
                    AlphabetIndex(
                        characters = characters,
                        onCharacterTap = { char ->
                            val index = charToIndex[char] ?: return@AlphabetIndex
                            scope.launch { listState.scrollToItem(index) }
                        }
                    )
                }
            }

            LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    DotWaveLoader()
                }
            }
        }
    }
}

@Composable
private fun DrawerSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            color = MaterialTheme.colorScheme.onBackground
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            autoCorrect = false,
            imeAction = ImeAction.Search
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Buscar aplicaciones",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        }
    )
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

private const val APP_DRAWER_PAGE = 2
