package fr.loicnogier.mystreamlogs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onDeleteClick: (id: Long) -> Unit
) : ListAdapter<TrackHistory, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_history, parent, false)
        return HistoryViewHolder(view, onDeleteClick)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val track = getItem(position)
        holder.bind(track)
    }

    class HistoryViewHolder(
        itemView: View,
        private val onDeleteClick: (id: Long) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        // private val albumArtImageView: ImageView = itemView.findViewById(R.id.albumArtImageView)
        private val titleTextView: TextView = itemView.findViewById(R.id.trackTitleTextView)
        private val artistTextView: TextView = itemView.findViewById(R.id.artistNameTextView)
        private val albumTextView: TextView = itemView.findViewById(R.id.albumNameTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val deleteButton: View = itemView.findViewById(R.id.deleteButton)
        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        private var currentTrack: TrackHistory? = null
        private val logTag = "HistoryAdapter"

        init {
            itemView.setOnClickListener {
                currentTrack?.let { track ->
                    itemView.context.openTidalOrWebSearch(
                        artistName = track.artistName,
                        trackTitle = track.trackTitle,
                        logTag = logTag
                    )
                }
            }

            deleteButton.setOnClickListener {
                currentTrack?.let { track ->
                    onDeleteClick(track.id)
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
