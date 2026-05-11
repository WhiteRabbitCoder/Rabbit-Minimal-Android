package dev.mslalith.focuslauncher.core.data.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MediaNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {}
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}
