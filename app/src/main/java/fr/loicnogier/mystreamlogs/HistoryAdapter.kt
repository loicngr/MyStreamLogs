package fr.loicnogier.mystreamlogs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        private val albumArtImageView: ImageView = itemView.findViewById(R.id.albumArtImageView)
        private val titleTextView: TextView = itemView.findViewById(R.id.trackTitleTextView)
        private val artistTextView: TextView = itemView.findViewById(R.id.artistNameTextView)
        private val albumTextView: TextView = itemView.findViewById(R.id.albumNameTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

        fun bind(track: TrackHistory) {
            titleTextView.text = track.trackTitle
            artistTextView.text = track.artistName

            if (track.albumName.isNullOrBlank()) {
                albumTextView.visibility = View.GONE
            } else {
                albumTextView.text = track.albumName
                albumTextView.visibility = View.VISIBLE
            }

            timestampTextView.text = dateFormatter.format(Date(track.timestamp))

            if (track.albumArtUrl.isNullOrBlank()) {
                albumArtImageView.setImageResource(android.R.drawable.ic_menu_gallery)
            } else {
                albumArtImageView.load(track.albumArtUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                }
            }
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