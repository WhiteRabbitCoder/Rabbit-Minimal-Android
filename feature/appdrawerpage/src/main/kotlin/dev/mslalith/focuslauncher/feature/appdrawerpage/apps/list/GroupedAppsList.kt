package dev.mslalith.focuslauncher.feature.appdrawerpage.apps.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mslalith.focuslauncher.core.model.appdrawer.AppDrawerItem
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun GroupedAppsList(
    modifier: Modifier,
    apps: ImmutableList<AppDrawerItem>,
    character: Char,
    usageMap: Map<String, Long>,
    showAppGroupHeader: Boolean,
    onAppClick: (AppDrawerItem) -> Unit,
    onAppLongClick: (AppDrawerItem) -> Unit
) {
    Column(modifier = modifier.padding(top = 8.dp)) {
        if (showAppGroupHeader) {
            CharacterHeader(character = character)
        }
        apps.forEach { app ->
            AppDrawerListItem(
                appDrawerItem = app,
                usageMinutes = usageMap[app.app.packageName] ?: 0L,
                onClick = onAppClick,
                onLongClick = onAppLongClick
            )
        }
    }
}
