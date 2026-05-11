package dev.mslalith.focuslauncher.core.data.repository.impl

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mslalith.focuslauncher.core.common.appcoroutinedispatcher.AppCoroutineDispatcher
import dev.mslalith.focuslauncher.core.data.repository.MediaPlayerRepo
import dev.mslalith.focuslauncher.core.data.repository.MediaState
import dev.mslalith.focuslauncher.core.data.service.MediaNotificationListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPlayerRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appCoroutineDispatcher: AppCoroutineDispatcher
) : MediaPlayerRepo {

    private val _mediaState = MutableStateFlow(MediaState())
    override val mediaState: StateFlow<MediaState> = _mediaState.asStateFlow()

    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val componentName = ComponentName(context, MediaNotificationListenerService::class.java)

    private var activeController: MediaController? = null

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers?.firstOrNull())
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateStateFromController()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateStateFromController()
        }
    }

    override fun checkPermissionAndInitialize() {
        val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        _mediaState.update { it.copy(hasPermission = hasPermission) }

        if (hasPermission) {
            try {
                mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
                val controllers = mediaSessionManager.getActiveSessions(componentName)
                updateActiveController(controllers.firstOrNull())
            } catch (e: SecurityException) {
                // Permission might have been revoked
                _mediaState.update { it.copy(hasPermission = false, showWidget = false) }
            }
        } else {
            _mediaState.update { it.copy(showWidget = false) }
            activeController?.unregisterCallback(controllerCallback)
            activeController = null
        }
    }

    private fun updateActiveController(controller: MediaController?) {
        activeController?.unregisterCallback(controllerCallback)
        activeController = controller
        activeController?.registerCallback(controllerCallback)
        updateStateFromController()
    }

    private fun updateStateFromController() {
        val controller = activeController
        if (controller == null) {
            _mediaState.update { it.copy(showWidget = false, isPlaying = false) }
            return
        }

        val playbackState = controller.playbackState
        val metadata = controller.metadata

        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"

        _mediaState.update {
            it.copy(
                isPlaying = isPlaying,
                title = title,
                artist = artist,
                packageName = controller.packageName ?: "",
                showWidget = isPlaying || playbackState?.state == PlaybackState.STATE_PAUSED
            )
        }
    }

    override fun play() {
        activeController?.transportControls?.play()
    }

    override fun pause() {
        activeController?.transportControls?.pause()
    }

    override fun skipToNext() {
        activeController?.transportControls?.skipToNext()
    }

    override fun skipToPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }
}
