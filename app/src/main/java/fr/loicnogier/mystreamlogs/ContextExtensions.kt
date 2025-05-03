package fr.loicnogier.mystreamlogs

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri

/**
 * Tries to open a track search in Tidal app via URI scheme, falling back to web search.
 * @param artistName The name of the artist.
 * @param trackTitle The title of the track.
 * @param logTag The tag to use for logging messages.
 */
fun Context.openTidalOrWebSearch(artistName: String, trackTitle: String, logTag: String) {
    if (artistName.isBlank() || trackTitle.isBlank()) {
        Log.w(logTag, "Artist name or track title is blank, cannot perform search.")
        return
    }

    val searchQuery = "$artistName $trackTitle"
    val encodedQuery = Uri.encode(searchQuery) // Encode once for use in URIs

    // 1. Try Tidal URI Scheme
    val tidalUri = "tidal://search?query=$encodedQuery".toUri()
    val intent = Intent(Intent.ACTION_VIEW, tidalUri)

    try {
        Log.d(logTag, "Attempting search '$searchQuery' in Tidal via URI scheme: $tidalUri")
        this.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w(
            logTag,
            "Tidal app doesn't handle 'tidal://search' URI scheme or is not installed. Falling back to web search.",
            e
        )

        // 2. Fallback: Try Web Search URL (might open app via App Links or browser)
        val webSearchUri = "https://listen.tidal.com/search?q=$encodedQuery".toUri()
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