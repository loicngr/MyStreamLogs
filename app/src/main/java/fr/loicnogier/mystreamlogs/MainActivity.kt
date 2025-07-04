package fr.loicnogier.mystreamlogs

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var permissionGroup: Group
    private lateinit var permissionButton: Button
    private lateinit var emptyHistoryText: TextView
    private lateinit var searchView: SearchView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var yearFilterLayout: TextInputLayout
    private lateinit var yearFilterAutoComplete: AutoCompleteTextView
    private lateinit var monthFilterLayout: TextInputLayout
    private lateinit var monthFilterAutoComplete: AutoCompleteTextView

    private lateinit var historyContentGroup: List<View>

    override fun attachBaseContext(newBase: Context) {
        // Apply saved language configuration
        super.attachBaseContext(SettingsActivity.applyLanguage(newBase))
    }


    private val database by lazy { AppDatabase.getDatabase(this) }
    private val trackHistoryDao by lazy { database.trackHistoryDao() }

    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(trackHistoryDao)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("MainActivity", "Permission POST_NOTIFICATIONS granted.")
            } else {
                Log.w("MainActivity", "Permission POST_NOTIFICATIONS denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.historyRecyclerView)
        permissionGroup = findViewById(R.id.permissionGroup)
        permissionButton = findViewById(R.id.permissionButton)
        emptyHistoryText = findViewById(R.id.emptyHistoryText)
        searchView = findViewById(R.id.searchView)
        bottomNavigationView = findViewById(R.id.bottomNavigation)
        yearFilterLayout = findViewById(R.id.yearFilterLayout)
        yearFilterAutoComplete = findViewById(R.id.yearFilterAutoComplete)
        monthFilterLayout = findViewById(R.id.monthFilterLayout)
        monthFilterAutoComplete = findViewById(R.id.monthFilterAutoComplete)
        val mainContentLayout = findViewById<View>(R.id.main)

        historyContentGroup = listOf(recyclerView, searchView, emptyHistoryText, yearFilterLayout, monthFilterLayout)

        ViewCompat.setOnApplyWindowInsetsListener(mainContentLayout) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                0
            )

            val bottomNavParams = bottomNavigationView.layoutParams as ViewGroup.MarginLayoutParams
            bottomNavParams.bottomMargin = imeInsets.bottom
            bottomNavigationView.layoutParams = bottomNavParams

            WindowInsetsCompat.CONSUMED
        }


        setupRecyclerView()
        setupPermissionButton()
        setupSearchView()
        setupFilters()
        setupBottomNavigation()

        observeHistory()
        requestPostNotificationsPermission()
        checkNotificationListenerPermission()
        requestBatteryOptimizationPermission()
    }


    override fun onResume() {
        super.onResume()
        currentFocus?.clearFocus()
        checkNotificationListenerPermission()
        bottomNavigationView.selectedItemId = R.id.navigation_history
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter { id ->
            historyViewModel.deleteTrack(id)
        }
        recyclerView.adapter = historyAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupPermissionButton() {
        permissionButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Could not open Notification Listener Settings", e)
            }
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus() // Hide keyboard on submit
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                historyViewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })
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
                historyViewModel.setSelectedYear(null)
            } else {
                // A specific year selected
                val selectedYear = years[position - 1] // -1 because of the "All" option
                historyViewModel.setSelectedYear(selectedYear)
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
                historyViewModel.setSelectedMonth(null)
            } else {
                // A specific month selected
                val selectedMonth = months[position - 1].first // -1 because of the "All" option
                historyViewModel.setSelectedMonth(selectedMonth)
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

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_history -> {
                    // History is already handled by this activity, ensure view is visible
                    showHistoryView(true)
                    true
                }
                R.id.navigation_most_streamed -> {
                    val intent = Intent(this, MostStreamedActivity::class.java)
                    startActivity(intent)
                    false
                }
                R.id.navigation_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    false
                }
                else -> false
            }
        }

         bottomNavigationView.selectedItemId = R.id.navigation_history
    }

     private fun showHistoryView(show: Boolean) {
         historyContentGroup.forEach { it.isVisible = show }
         if (show) {
             updateHistoryVisibility(historyAdapter.currentList)
         }
     }


    private fun checkNotificationListenerPermission() {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val componentName = ComponentName(this, MediaPlayerService::class.java).flattenToString()
        val isEnabled = enabledListeners != null && enabledListeners.contains(componentName)

        Log.d("MainActivity", "Media Player Service Enabled: $isEnabled")

        permissionGroup.isVisible = !isEnabled
        showHistoryView(isEnabled)
        bottomNavigationView.isVisible = isEnabled
    }

    private fun observeHistory() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                historyViewModel.history.collect { historyList ->
                    Log.d("MainActivity", "History updated with ${historyList.size} items.")
                    historyAdapter.submitList(historyList)
                    // Only update visibility if the permission is granted and history tab is active
                    if (permissionGroup.isGone && bottomNavigationView.selectedItemId == R.id.navigation_history) {
                       updateHistoryVisibility(historyList)
                    }
                }
            }
        }
    }

    // Helper to manage visibility of RecyclerView vs Empty Text
    private fun updateHistoryVisibility(historyList: List<TrackHistory>) {
         val hasPermission = permissionGroup.isGone
         if (!hasPermission) {
             // Ensure views are hidden if permission is not granted
             recyclerView.isVisible = false
             emptyHistoryText.isVisible = false
             searchView.isVisible = false // Make sure search is hidden too
             return
         }

         // Permission is granted, proceed with visibility logic
         val isListEmpty = historyList.isEmpty()
         // Check if there is an active search query in the SearchView
         val isQueryEmpty = searchView.query.isNullOrEmpty()

         // Show RecyclerView if the list is NOT empty
         recyclerView.isVisible = !isListEmpty

         // Show the "History Empty" text ONLY if the list is empty AND the user is NOT searching
         emptyHistoryText.isVisible = isListEmpty && isQueryEmpty

         // Ensure SearchView remains visible as long as permission is granted
         searchView.isVisible = true
    }


    private fun requestPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Permission POST_NOTIFICATIONS already granted.")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionRationaleDialog()
                }
                else -> {
                    Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }


    private fun showPermissionRationaleDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.permission_post_notifications_title))
                .setMessage(getString(R.string.permission_post_notifications_message))
                .setPositiveButton(getString(R.string.permission_post_notifications_button_grant)) { _, _ ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                .setNegativeButton(getString(R.string.permission_post_notifications_button_deny), null)
                .show()
        }
    }

    private fun requestBatteryOptimizationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = "package:$packageName".toUri()
            try {
                startActivity(intent)
                Log.d("MainActivity", "Requesting battery optimization exception")
            } catch (e: Exception) {
                Log.e("MainActivity", "Could not open battery optimization settings", e)
            }
        } else {
            Log.d("MainActivity", "Battery optimization already disabled for this app")
        }
    }
}
