package fr.loicnogier.mystreamlogs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

// Adapter for the RecyclerView displaying most streamed tracks
class MostStreamedAdapter : ListAdapter<
        TrackStreamCount,
        MostStreamedAdapter.MostStreamedViewHolder
        >(MostStreamedDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MostStreamedViewHolder {
        // Inflate the layout for each list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_most_streamed, parent, false)
        return MostStreamedViewHolder(view)
    }

    override fun onBindViewHolder(holder: MostStreamedViewHolder, position: Int) {
        val trackCount = getItem(position)
        // Bind data to the ViewHolder, passing the position + 1 as rank
        holder.bind(trackCount, position + 1)
    }

    // ViewHolder holds the views for each list item
    class MostStreamedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rankTextView: TextView = itemView.findViewById(R.id.rankTextView)
        private val titleTextView: TextView = itemView.findViewById(R.id.trackTitleTextView)
        private val artistTextView: TextView = itemView.findViewById(R.id.artistNameTextView)
        private val countTextView: TextView = itemView.findViewById(R.id.streamCountTextView)

        // Bind the TrackStreamCount data to the views
        fun bind(trackCount: TrackStreamCount, rank: Int) {
            rankTextView.text = rank.toString() // Display rank
            titleTextView.text = trackCount.trackTitle
            artistTextView.text = trackCount.artistName
            // Display the stream count, using plural string resource for better localization
            countTextView.text = itemView.context.resources.getQuantityString(
                R.plurals.stream_count_text, trackCount.streamCount, trackCount.streamCount
            )
            // TODO: Add click listener similar to HistoryAdapter if needed
        }
    }

    // DiffUtil callback to efficiently update the list
    class MostStreamedDiffCallback : DiffUtil.ItemCallback<TrackStreamCount>() {
        override fun areItemsTheSame(oldItem: TrackStreamCount, newItem: TrackStreamCount): Boolean {
            // Items are the same if title and artist match
            return oldItem.trackTitle == newItem.trackTitle && oldItem.artistName == newItem.artistName
        }

        override fun areContentsTheSame(oldItem: TrackStreamCount, newItem: TrackStreamCount): Boolean {
            // Contents are the same if the whole object matches (including count)
            return oldItem == newItem
        }
    }
}