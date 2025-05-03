package fr.loicnogier.mystreamlogs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class MostStreamedAdapter : ListAdapter<
        TrackStreamCount,
        MostStreamedAdapter.MostStreamedViewHolder
        >(MostStreamedDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MostStreamedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_most_streamed, parent, false)
        return MostStreamedViewHolder(view)
    }

    override fun onBindViewHolder(holder: MostStreamedViewHolder, position: Int) {
        val trackCount = getItem(position)
        holder.bind(trackCount, position + 1)
    }

    class MostStreamedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rankTextView: TextView = itemView.findViewById(R.id.rankTextView)
        private val titleTextView: TextView = itemView.findViewById(R.id.trackTitleTextView)
        private val artistTextView: TextView = itemView.findViewById(R.id.artistNameTextView)
        private val countTextView: TextView = itemView.findViewById(R.id.streamCountTextView)
        private var currentItem: TrackStreamCount? = null
        private val logTag = "MostStreamedAdapter"

        init {
            itemView.setOnClickListener {
                currentItem?.let { item ->
                    itemView.context.openTidalOrWebSearch(
                        artistName = item.artistName,
                        trackTitle = item.trackTitle,
                        logTag = logTag
                    )
                }
            }
        }

        fun bind(trackCount: TrackStreamCount, rank: Int) {
            currentItem = trackCount
            rankTextView.text = rank.toString()
            titleTextView.text = trackCount.trackTitle
            artistTextView.text = trackCount.artistName
            countTextView.text = itemView.context.resources.getQuantityString(
                R.plurals.stream_count_text, trackCount.streamCount, trackCount.streamCount
            )
        }
    }

    class MostStreamedDiffCallback : DiffUtil.ItemCallback<TrackStreamCount>() {
        override fun areItemsTheSame(oldItem: TrackStreamCount, newItem: TrackStreamCount): Boolean {
            return oldItem.trackTitle == newItem.trackTitle && oldItem.artistName == newItem.artistName
        }

        override fun areContentsTheSame(oldItem: TrackStreamCount, newItem: TrackStreamCount): Boolean {
            return oldItem == newItem
        }
    }
}