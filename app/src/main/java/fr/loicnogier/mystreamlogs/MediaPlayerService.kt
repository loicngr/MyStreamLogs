package fr.loicnogier.mystreamlogs

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MediaPlayerService : NotificationListenerService() {

    override fun attachBaseContext(base: Context) {
        // Apply saved language configuration
        super.attachBaseContext(SettingsActivity.applyLanguage(base))
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val trackHistoryDao by lazy { database.trackHistoryDao() }
    private var lastTrackTitle: String? = null
    private var lastArtistName: String? = null
    private val tidalPackageName = "com.aspiro.tidal"
    private val CHANNEL_ID = "TRACK_HISTORY_CHANNEL"
    private val NOTIFICATION_ID = 1
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeControllers = mutableMapOf<String, MediaController>()
    private val sessionCallback = SessionCallback()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initMediaSessionManager()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i("MediaPlayerService", "Service connected")
        lastTrackTitle = null
        lastArtistName = null
        initMediaSessionManager()
    }

    private fun initMediaSessionManager() {
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            updateActiveSessions()
        } catch (e: Exception) {
            Log.e("MediaPlayerService", "Error initializing MediaSessionManager", e)
        }
    }

    private fun updateActiveSessions() {
        mediaSessionManager?.let { manager ->
            try {
                val activeSessions = manager.getActiveSessions(ComponentName(this, MediaPlayerService::class.java))
                
                // Register callbacks for new sessions
                for (controller in activeSessions) {
                    if (!activeControllers.containsKey(controller.packageName)) {
                        controller.registerCallback(sessionCallback)
                        activeControllers[controller.packageName] = controller
                        
                        // Check if this is Tidal and process current metadata
                        if (controller.packageName == tidalPackageName) {
                            processMetadata(controller.metadata)
                        }
                    }
                }
                
                Log.d("MediaPlayerService", "Active sessions updated. Count: ${activeControllers.size}")
            } catch (e: SecurityException) {
                Log.e("MediaPlayerService", "Security exception getting active sessions", e)
            } catch (e: Exception) {
                Log.e("MediaPlayerService", "Error getting active sessions", e)
            }
        }
    }

    private inner class SessionCallback : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            processMetadata(metadata)
        }
    }

    private fun processMetadata(metadata: MediaMetadata?) {
        metadata ?: return
        
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim()
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim()
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim()
        val albumArtUri = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
        
        Log.d("MediaPlayerService", "Metadata received: Title='$title', Artist='$artist', Album='$album'")
        
        if (!title.isNullOrBlank() && !artist.isNullOrBlank() &&
            (title != lastTrackTitle || artist != lastArtistName)) {
            
            Log.i("MediaPlayerService", "New track detected: '$title' by '$artist'")
            
            lastTrackTitle = title
            lastArtistName = artist
            
            val trackHistory = TrackHistory(
                trackTitle = title,
                artistName = artist,
                albumName = album,
                albumArtUrl = albumArtUri
            )
            
            serviceScope.launch {
                try {
                    trackHistoryDao.insert(trackHistory)
                    Log.i("MediaPlayerService", "Track saved in the database.")
                    showTrackSavedNotification(title, artist)
                } catch (e: Exception) {
                    Log.e("MediaPlayerService", "Error during insertion in the DB", e)
                }
            }
        } else {
            Log.d("MediaPlayerService", "Metadata ignored (no title/artist or identical to the previous one).")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("MediaPlayerService", "Notification channel created.")
        }
    }

    private fun showTrackSavedNotification(title: String, artist: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("MediaPlayerService", "Permission POST_NOTIFICATIONS not granted.")
                return
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText("$title - $artist")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
            Log.d("MediaPlayerService", "Notification displayed for '$title' with PendingIntent.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister all callbacks
        for (controller in activeControllers.values) {
            controller.unregisterCallback(sessionCallback)
        }
        activeControllers.clear()
        serviceJob.cancel()
        Log.i("MediaPlayerService", "Service killed")
    }
}