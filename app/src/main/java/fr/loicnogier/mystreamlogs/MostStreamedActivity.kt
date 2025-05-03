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

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val trackHistoryDao by lazy { database.trackHistoryDao() }

    private val mostStreamedViewModel: MostStreamedViewModel by viewModels {
        MostStreamedViewModelFactory(trackHistoryDao)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_most_streamed)

        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.mostStreamedRecyclerView)
        emptyTextView = findViewById(R.id.emptyMostStreamedText)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        setupRecyclerView()
        observeMostStreamedTracks()
    }

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

    private fun observeMostStreamedTracks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mostStreamedViewModel.mostStreamedTracks.collect { tracks ->
                    mostStreamedAdapter.submitList(tracks)
                    emptyTextView.isVisible = tracks.isEmpty()
                    recyclerView.isVisible = tracks.isNotEmpty()
                }
            }
        }
    }
}