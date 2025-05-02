package fr.loicnogier.mystreamlogs

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val trackHistoryDao by lazy { database.trackHistoryDao() }
    private var lastTrackTitle: String? = null
    private var lastArtistName: String? = null
    private val tidalPackageName = "com.aspiro.tidal"
    private val CHANNEL_ID = "TRACK_HISTORY_CHANNEL"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

     override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i("NotificationListener", "Service connecté")
        lastTrackTitle = null
        lastArtistName = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        if (sbn.packageName != tidalPackageName) {
            return
        }

        val notification = sbn.notification ?: return
        val extras: Bundle = notification.extras ?: return

        val title = extras.getString(Notification.EXTRA_TITLE)?.trim()
        val artist = extras.getString(Notification.EXTRA_TEXT)?.trim()
        val album = extras.getString(Notification.EXTRA_SUB_TEXT)?.trim()
        val albumArtUrl: String? = null

        Log.d("NotificationListener", "Notification reçue: Titre='$title', Artiste='$artist', Album='$album'")

        if (!title.isNullOrBlank() && !artist.isNullOrBlank() &&
            (title != lastTrackTitle || artist != lastArtistName)) {

            Log.i("NotificationListener", "Nouvelle piste détectée : '$title' par '$artist'")

            lastTrackTitle = title
            lastArtistName = artist

            val trackHistory = TrackHistory(
                trackTitle = title,
                artistName = artist,
                albumName = album,
                albumArtUrl = albumArtUrl
            )

            serviceScope.launch {
                try {
                    trackHistoryDao.insert(trackHistory)
                    Log.i("NotificationListener", "Piste enregistrée dans la base de données.")
                    showTrackSavedNotification(title, artist)
                } catch (e: Exception) {
                    Log.e("NotificationListener", "Erreur lors de l'insertion dans la DB", e)
                }
            }
        } else {
             Log.d("NotificationListener", "Notification ignorée (pas de titre/artiste ou identique à la précédente).")
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
            Log.d("NotificationListener", "Canal de notification créé.")
        }
    }

    private fun showTrackSavedNotification(title: String, artist: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                 Log.w("NotificationListener", "Permission POST_NOTIFICATIONS non accordée.")
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
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO
            .setContentTitle(getString(R.string.notification_title))
            .setContentText("$title - $artist")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
            Log.d("NotificationListener", "Notification affichée pour '$title' avec PendingIntent.")
        }
    }

     override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
         if (sbn?.packageName == tidalPackageName) {
             val notification = sbn.notification
             val extras: Bundle? = notification?.extras
             val title = extras?.getString(Notification.EXTRA_TITLE)?.trim()
             val artist = extras?.getString(Notification.EXTRA_TEXT)?.trim()

             if(title == lastTrackTitle && artist == lastArtistName){
                 Log.d("NotificationListener", "Current track notification removed, resetting.")
                 lastTrackTitle = null
                 lastArtistName = null
             }
         }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.i("NotificationListener", "Service killed")
    }
}