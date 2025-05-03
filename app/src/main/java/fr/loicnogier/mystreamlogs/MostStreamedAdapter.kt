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

        init {
            itemView.setOnClickListener {
                currentItem?.let { item ->
                    val context = itemView.context
                    val searchQuery = "${item.artistName} ${item.trackTitle}"
                    val tidalUri = "tidal://search?query=${Uri.encode(searchQuery)}".toUri()
                    val intent = Intent(Intent.ACTION_VIEW, tidalUri)

                    try {
                        Log.d(
                            "MostStreamedAdapter",
                            "Tentative de recherche '$searchQuery' dans Tidal via URI scheme"
                        )
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Log.w(
                            "MostStreamedAdapter",
                            "Tidal ne semble pas gérer l'URI scheme 'tidal://search' ou n'est pas installé. Tentative avec lien web.",
                            e
                        )

                        val webSearchUri = "https://listen.tidal.com/search?q=${Uri.encode(searchQuery)}".toUri()
                        val webAppIntent = Intent(Intent.ACTION_VIEW, webSearchUri)

                        try {
                            Log.d(
                                "MostStreamedAdapter",
                                "Fallback 1: Tentative d'ouverture de '$searchQuery' dans Tidal (via App Link) avec l'URI: $webSearchUri"
                            )
                            context.startActivity(webAppIntent)
                        } catch (eWeb: ActivityNotFoundException) {
                            Log.w(
                                "MostStreamedAdapter",
                                "Impossible d'ouvrir via App Link non plus. Tentative avec le navigateur.",
                                eWeb
                            )

                            val browserIntent = Intent(Intent.ACTION_VIEW, webSearchUri)
                            try {
                                context.startActivity(browserIntent)
                            } catch (eBrowser: ActivityNotFoundException) {
                                Log.e(
                                    "MostStreamedAdapter",
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