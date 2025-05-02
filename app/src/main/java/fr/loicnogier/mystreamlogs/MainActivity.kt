package fr.loicnogier.mystreamlogs

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var permissionGroup: Group
    private lateinit var permissionButton: Button
    private lateinit var emptyHistoryText: TextView
    private lateinit var searchView: SearchView

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val trackHistoryDao by lazy { database.trackHistoryDao() }

    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(trackHistoryDao)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("MainActivity", "Permission POST_NOTIFICATIONS accordée.")
            } else {
                Log.w("MainActivity", "Permission POST_NOTIFICATIONS refusée.")
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
        val mainContentLayout = findViewById<View>(R.id.main)


        ViewCompat.setOnApplyWindowInsetsListener(mainContentLayout) { view, insets ->

            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())





            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,

                maxOf(systemBarsInsets.bottom, imeInsets.bottom)
            )



            WindowInsetsCompat.CONSUMED


        }


        setupRecyclerView()
        setupPermissionButton()
        setupSearchView()

        observeHistory()


        requestPostNotificationsPermission()
    }


    override fun onResume() {
        super.onResume()
        checkNotificationListenerPermission()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        recyclerView.adapter = historyAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupPermissionButton() {
        permissionButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Impossible d'ouvrir les paramètres d'accès aux notifications", e)
            }
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                historyViewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })
    }

    private fun checkNotificationListenerPermission() {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val componentName = ComponentName(this, NotificationListener::class.java).flattenToString()
        val isEnabled = enabledListeners != null && enabledListeners.contains(componentName)

        Log.d("MainActivity", "Accès notification activé: $isEnabled")

        permissionGroup.isVisible = !isEnabled
        recyclerView.visibility = if (isEnabled) View.VISIBLE else View.GONE
        searchView.visibility = if (isEnabled) View.VISIBLE else View.GONE

        if (!isEnabled) {
            emptyHistoryText.visibility = View.GONE
        }
    }

    private fun observeHistory() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                historyViewModel.history.collect { historyList ->
                    Log.d("MainActivity", "Mise à jour de l'historique avec ${historyList.size} éléments.")
                    historyAdapter.submitList(historyList)

                    if (permissionGroup.isGone) {
                        emptyHistoryText.isVisible = historyList.isEmpty()
                        recyclerView.isVisible = historyList.isNotEmpty()
                    } else {
                        recyclerView.visibility = View.GONE
                        emptyHistoryText.visibility = View.GONE
                    }
                }
            }
        }
    }


    private fun requestPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Permission POST_NOTIFICATIONS déjà accordée.")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionRationaleDialog()
                }
                else -> {
                    Log.d("MainActivity", "Demande de la permission POST_NOTIFICATIONS.")
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
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton(
                    getString(R.string.permission_post_notifications_button_deny),
                    null
                )
                .show()
        }
    }
}