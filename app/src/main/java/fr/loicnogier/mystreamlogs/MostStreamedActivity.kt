package fr.loicnogier.mystreamlogs

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MostStreamedActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Apply saved language configuration
        super.attachBaseContext(SettingsActivity.applyLanguage(newBase))
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var mostStreamedAdapter: MostStreamedAdapter
    private lateinit var emptyTextView: TextView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var yearFilterLayout: TextInputLayout
    private lateinit var yearFilterAutoComplete: AutoCompleteTextView
    private lateinit var monthFilterLayout: TextInputLayout
    private lateinit var monthFilterAutoComplete: AutoCompleteTextView

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
        yearFilterLayout = findViewById(R.id.yearFilterLayout)
        yearFilterAutoComplete = findViewById(R.id.yearFilterAutoComplete)
        monthFilterLayout = findViewById(R.id.monthFilterLayout)
        monthFilterAutoComplete = findViewById(R.id.monthFilterAutoComplete)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        setupRecyclerView()
        setupFilters()
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
        mostStreamedAdapter = MostStreamedAdapter { trackTitle, artistName ->
            mostStreamedViewModel.deleteTrack(trackTitle, artistName)
        }
        recyclerView.adapter = mostStreamedAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupFilters() {
        setupYearFilter()
        setupMonthFilter()
    }

    private fun setupYearFilter() {
        // Generate a list of years (from 2020 to current year)
        val years = generateYearsList()

        // Add an "All" option at the beginning
        val allYearsOption = getString(R.string.filter_all_years)
        val adapter = ArrayAdapter(
            this, 
            android.R.layout.simple_dropdown_item_1line, 
            listOf(allYearsOption) + years
        )
        yearFilterAutoComplete.setAdapter(adapter)

        // Set the default selection to "All"
        yearFilterAutoComplete.setText(allYearsOption, false)

        // Handle selection
        yearFilterAutoComplete.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                // "All" option selected
                mostStreamedViewModel.setSelectedYear(null)
            } else {
                // A specific year selected
                val selectedYear = years[position - 1] // -1 because of the "All" option
                mostStreamedViewModel.setSelectedYear(selectedYear)
            }
        }
    }

    private fun setupMonthFilter() {
        // Generate a list of all 12 months
        val months = generateMonthsList()

        // Add an "All" option at the beginning
        val allMonthsOption = getString(R.string.filter_all_months)
        val adapter = ArrayAdapter(
            this, 
            android.R.layout.simple_dropdown_item_1line, 
            listOf(allMonthsOption) + months.map { it.second }
        )
        monthFilterAutoComplete.setAdapter(adapter)

        // Set the default selection to "All"
        monthFilterAutoComplete.setText(allMonthsOption, false)

        // Handle selection
        monthFilterAutoComplete.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                // "All" option selected
                mostStreamedViewModel.setSelectedMonth(null)
            } else {
                // A specific month selected
                val selectedMonth = months[position - 1].first // -1 because of the "All" option
                mostStreamedViewModel.setSelectedMonth(selectedMonth)
            }
        }
    }

    private fun generateYearsList(): List<String> {
        val years = mutableListOf<String>()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Generate years from 2020 to current year
        for (year in 2020..currentYear) {
            years.add(year.toString())
        }

        return years.reversed() // Most recent years first
    }

    private fun generateMonthsList(): List<Pair<String, String>> {
        val months = mutableListOf<Pair<String, String>>()
        val monthFormat = SimpleDateFormat("MM", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Set to January
        calendar.set(Calendar.MONTH, Calendar.JANUARY)

        // Generate all 12 months
        for (i in 0 until 12) {
            val date = calendar.time
            val monthKey = monthFormat.format(date) // Format as "MM" for the query
            val monthDisplay = displayFormat.format(date) // Format as "Month" for display
            months.add(Pair(monthKey, monthDisplay))
            calendar.add(Calendar.MONTH, 1)
        }

        return months
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
