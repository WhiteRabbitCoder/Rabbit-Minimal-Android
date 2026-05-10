package dev.mslalith.focuslauncher.feature.appdrawerpage.apps.list

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mslalith.focuslauncher.core.model.appdrawer.AppDrawerItem

@Composable
internal fun AppDrawerListItem(
    appDrawerItem: AppDrawerItem,
    usageMinutes: Long,
    onClick: (AppDrawerItem) -> Unit,
    onLongClick: (AppDrawerItem) -> Unit
) {
    val usageText = when {
        usageMinutes >= 60 -> "${usageMinutes / 60}h ${usageMinutes % 60}m"
        usageMinutes > 0 -> "${usageMinutes}m"
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(appDrawerItem) {
                detectTapGestures(
                    onTap = { onClick(appDrawerItem) },
                    onLongPress = { onLongClick(appDrawerItem) }
                )
            }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = appDrawerItem.app.displayName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (usageText != null) {
            Text(
                text = usageText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
