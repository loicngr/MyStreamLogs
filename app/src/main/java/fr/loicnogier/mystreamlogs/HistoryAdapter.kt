package fr.loicnogier.mystreamlogs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter : ListAdapter<TrackHistory, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val track = getItem(position)
        holder.bind(track)
    }

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // private val albumArtImageView: ImageView = itemView.findViewById(R.id.albumArtImageView)
        private val titleTextView: TextView = itemView.findViewById(R.id.trackTitleTextView)
        private val artistTextView: TextView = itemView.findViewById(R.id.artistNameTextView)
        private val albumTextView: TextView = itemView.findViewById(R.id.albumNameTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        private var currentTrack: TrackHistory? = null

        init {
            itemView.setOnClickListener {
                currentTrack?.let { track ->
                    val context = itemView.context
                    val searchQuery = "${track.artistName} ${track.trackTitle}"
                    val tidalUri = "tidal://search?query=${searchQuery}".toUri()
                    val intent = Intent(Intent.ACTION_VIEW, tidalUri)


                    try {
                        Log.d(
                            "HistoryAdapter",
                            "Tentative de recherche '$searchQuery' dans Tidal via MEDIA_PLAY_FROM_SEARCH"
                        )
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Log.w(
                            "HistoryAdapter",
                            "Tidal ne semble pas gérer MEDIA_PLAY_FROM_SEARCH ou n'est pas installé. Tentative avec lien web.",
                            e
                        )

                        val webSearchUri = "https://listen.tidal.com/search?q=${Uri.encode(searchQuery)}".toUri()
                        val webAppIntent = Intent(Intent.ACTION_VIEW, webSearchUri)
                        webAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        try {
                            Log.d(
                                "HistoryAdapter",
                                "Fallback 1: Tentative d'ouverture de '$searchQuery' dans Tidal (via App Link) avec l'URI: $webSearchUri"
                            )
                            context.startActivity(webAppIntent)
                        } catch (eWeb: ActivityNotFoundException) {
                            Log.w(
                                "HistoryAdapter",
                                "Impossible d'ouvrir via App Link non plus. Tentative avec le navigateur.",
                                eWeb
                            )
                            val browserIntent = Intent(Intent.ACTION_VIEW, webSearchUri)
                            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(browserIntent)
                            } catch (eBrowser: ActivityNotFoundException) {
                                Log.e(
                                    "HistoryAdapter",
                                    "Impossible d'ouvrir le lien $webSearchUri, même dans un navigateur.",
                                    eBrowser
                                )
                                Toast.makeText(context, R.string.error_opening_link, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        fun bind(track: TrackHistory) {
            currentTrack = track
            titleTextView.text = track.trackTitle
            artistTextView.text = track.artistName

            if (track.albumName.isNullOrBlank()) {
                albumTextView.visibility = View.GONE
            } else {
                albumTextView.text = track.albumName
                albumTextView.visibility = View.VISIBLE
            }

            timestampTextView.text = dateFormatter.format(Date(track.timestamp))

            // --- La logique de chargement de l'image reste la même ---
            // TODO : À faire plus tard.
            // if (track.albumArtUrl.isNullOrBlank()) {
            //     albumArtImageView.setImageResource(R.drawable.ic_music_note) // Placeholder
            //     albumArtImageView.visibility = View.VISIBLE
            // } else {
            //    albumArtImageView.visibility = View.VISIBLE
            //    albumArtImageView.load(track.albumArtUrl) {
            //         crossfade(true)
            //         placeholder(R.drawable.ic_music_note)
            //         error(R.drawable.ic_broken_image)
            //     }
            // }
            // --- Fin de la logique de l'image ---
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<TrackHistory>() {
        override fun areItemsTheSame(oldItem: TrackHistory, newItem: TrackHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TrackHistory, newItem: TrackHistory): Boolean {
            return oldItem == newItem
        }
    }
}