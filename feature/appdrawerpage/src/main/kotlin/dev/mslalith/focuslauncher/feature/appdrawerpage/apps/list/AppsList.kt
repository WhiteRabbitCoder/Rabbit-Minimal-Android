package dev.mslalith.focuslauncher.feature.appdrawerpage.apps.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mslalith.focuslauncher.core.model.appdrawer.AppDrawerItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppsList(
    groupedApps: ImmutableMap<Char, ImmutableList<AppDrawerItem>>,
    listState: LazyListState = rememberLazyListState(),
    usageMap: Map<String, Long>,
    showAppGroupHeader: Boolean,
    onAppClick: (AppDrawerItem) -> Unit,
    onAppLongClick: (AppDrawerItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 24.dp)
    ) {
        groupedApps.forEach { (character, apps) ->
            item(key = character) {
                GroupedAppsList(
                    apps = apps,
                    character = character,
                    usageMap = usageMap,
                    showAppGroupHeader = showAppGroupHeader && groupedApps.size != 1,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    modifier = Modifier
                )
            }
        }
    }
}
