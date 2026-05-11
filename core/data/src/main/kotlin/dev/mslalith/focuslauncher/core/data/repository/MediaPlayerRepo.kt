package dev.mslalith.focuslauncher.core.data.repository

import kotlinx.coroutines.flow.Flow

data class MediaState(
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val showWidget: Boolean = false,
    val hasPermission: Boolean = false,
    val packageName: String = ""
)

interface MediaPlayerRepo {
    val mediaState: Flow<MediaState>
    fun play()
    fun pause()
    fun skipToNext()
    fun skipToPrevious()
    fun checkPermissionAndInitialize()
}
