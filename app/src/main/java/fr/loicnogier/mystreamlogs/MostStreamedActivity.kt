package fr.loicnogier.mystreamlogs

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class MostStreamedActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var mostStreamedAdapter: MostStreamedAdapter
    private lateinit var emptyTextView: TextView
    private lateinit var toolbar: MaterialToolbar

    // Get the DAO instance (could also be injected)
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val trackHistoryDao by lazy { database.trackHistoryDao() }

    // Get the ViewModel instance using the factory
    private val mostStreamedViewModel: MostStreamedViewModel by viewModels {
        MostStreamedViewModelFactory(trackHistoryDao)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enable edge-to-edge display
        setContentView(R.layout.activity_most_streamed)

        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.mostStreamedRecyclerView)
        emptyTextView = findViewById(R.id.emptyMostStreamedText)

        // Set up the toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back button

        // Adjust padding for system bars for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        setupRecyclerView()
        observeMostStreamedTracks()
    }

    // Handle the back button in the toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // Close this activity and return to the previous one
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        mostStreamedAdapter = MostStreamedAdapter()
        recyclerView.adapter = mostStreamedAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    // Observe the data from the ViewModel
    private fun observeMostStreamedTracks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mostStreamedViewModel.mostStreamedTracks.collect { tracks ->
                    mostStreamedAdapter.submitList(tracks)
                    // Show/hide empty text based on the list content
                    emptyTextView.isVisible = tracks.isEmpty()
                    recyclerView.isVisible = tracks.isNotEmpty()
                }
            }
        }
    }
}