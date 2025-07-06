package fr.loicnogier.mystreamlogs

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri

/**
 * Tries to open a track search in the appropriate music platform app via URI scheme, falling back to web search.
 * @param artistName The name of the artist.
 * @param trackTitle The title of the track.
 * @param platform The music platform (Tidal, Spotify, etc.).
 * @param logTag The tag to use for logging messages.
 */
fun Context.openMusicPlatformOrWebSearch(
    artistName: String, 
    trackTitle: String, 
    platform: String = "Tidal",
    logTag: String
) {
    if (artistName.isBlank() || trackTitle.isBlank()) {
        Log.w(logTag, "Artist name or track title is blank, cannot perform search.")
        return
    }

    val searchQuery = "$artistName $trackTitle"
    val encodedQuery = Uri.encode(searchQuery) // Encode once for use in URIs

    // Define URI schemes and web URLs for different platforms
    val (uriScheme, webUrl) = when (platform) {
        "Spotify" -> {
            Pair(
                "spotify:search:$encodedQuery",
                "https://open.spotify.com/search/$encodedQuery"
            )
        }
        "Deezer" -> {
            Pair(
                "deezer://www.deezer.com/search/$encodedQuery",
                "https://www.deezer.com/search/$encodedQuery"
            )
        }
        "Apple Music" -> {
            Pair(
                "music://music.apple.com/search?term=$encodedQuery",
                "https://music.apple.com/search?term=$encodedQuery"
            )
        }
        "YouTube Music" -> {
            Pair(
                "youtubemusic://search?q=$encodedQuery",
                "https://music.youtube.com/search?q=$encodedQuery"
            )
        }
        else -> { // Default to Tidal
            Pair(
                "tidal://search?query=$encodedQuery",
                "https://listen.tidal.com/search?q=$encodedQuery"
            )
        }
    }

    // 1. Try platform-specific URI Scheme
    val musicAppUri = uriScheme.toUri()
    val intent = Intent(Intent.ACTION_VIEW, musicAppUri)

    try {
        Log.d(logTag, "Attempting search '$searchQuery' in $platform via URI scheme: $uriScheme")
        this.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w(
            logTag,
            "$platform app doesn't handle URI scheme or is not installed. Falling back to web search.",
            e
        )

        // 2. Fallback: Try Web Search URL (might open app via App Links or browser)
        val webSearchUri = webUrl.toUri()
        val webAppIntent = Intent(Intent.ACTION_VIEW, webSearchUri)
        // No need for FLAG_ACTIVITY_NEW_TASK when called from an existing Activity/ViewHolder context

        try {
            Log.d(logTag, "Fallback 1: Attempting to open '$searchQuery' with web URI (App Link/Browser): $webSearchUri")
            this.startActivity(webAppIntent)
        } catch (eWeb: ActivityNotFoundException) {
            Log.e(
                logTag,
                "Fallback 2: Failed to open web search URI '$webSearchUri' even in a browser.",
                eWeb
            )

            Toast.makeText(this, R.string.error_opening_link, Toast.LENGTH_SHORT).show()
        }
    }
}
