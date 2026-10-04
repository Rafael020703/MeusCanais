package rsv.squitv.data.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import rsv.squitv.util.NotificationHelper
import timber.log.Timber

class IptvMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM Token: %s", token)
        // Optionally send token to your server if needed for individual targeting
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("FCM Message from: %s", message.from)

        // Check if message contains a notification payload.
        message.notification?.let {
            Timber.d("Message Notification Body: %s", it.body)
            // Handle display notification
        }

        // Check if message contains a data payload.
        if (message.data.isNotEmpty()) {
            Timber.d("Message data payload: %s", message.data)
            
            val type = message.data["type"] // e.g., "new_content"
            val title = message.data["title"] ?: "Novidade no App"
            val body = message.data["body"] ?: "Confira o novo conteúdo disponível!"
            val streamId = message.data["streamId"]
            val contentType = message.data["contentType"] // movie, series, live

            NotificationHelper.showPushNotification(
                context = this,
                title = title,
                message = body,
                streamId = streamId?.toIntOrNull(),
                contentType = contentType
            )
        }
    }
}
