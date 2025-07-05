package fr.loicnogier.mystreamlogs

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var deleteDataButton: Button
    private lateinit var checkPermissionsButton: Button
    private lateinit var languageToggleGroup: MaterialButtonToggleGroup
    private lateinit var systemLanguageButton: MaterialButton
    private lateinit var englishLanguageButton: MaterialButton
    private lateinit var frenchLanguageButton: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val trackHistoryDao by lazy { database.trackHistoryDao() }

    companion object {
        private const val PREFS_NAME = "MyStreamLogsPrefs"
        private const val PREF_LANGUAGE = "language"
        private const val LANGUAGE_SYSTEM = "system"
        private const val LANGUAGE_ENGLISH = "en"
        private const val LANGUAGE_FRENCH = "fr"

        // Apply the saved language to the context
        fun applyLanguage(context: Context): Context {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val language = prefs.getString(PREF_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM

            return if (language == LANGUAGE_SYSTEM) {
                context // Use system default
            } else {
                // Change configuration
                val locale = Locale(language)
                Locale.setDefault(locale)

                val config = Configuration(context.resources.configuration)
                config.setLocale(locale)

                context.createConfigurationContext(config)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("SettingsActivity", "Permission POST_NOTIFICATIONS granted.")
            } else {
                Log.w("SettingsActivity", "Permission POST_NOTIFICATIONS denied.")
            }
        }

    override fun attachBaseContext(newBase: Context) {
        // Apply saved language configuration
        super.attachBaseContext(applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        toolbar = findViewById(R.id.toolbar)
        deleteDataButton = findViewById(R.id.deleteDataButton)
        checkPermissionsButton = findViewById(R.id.checkPermissionsButton)
        languageToggleGroup = findViewById(R.id.languageToggleGroup)
        systemLanguageButton = findViewById(R.id.systemLanguageButton)
        englishLanguageButton = findViewById(R.id.englishLanguageButton)
        frenchLanguageButton = findViewById(R.id.frenchLanguageButton)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        setupDeleteButton()
        setupCheckPermissionsButton()
        setupLanguageSelector()
    }

    private fun setupLanguageSelector() {
        // Set the selected language based on saved preference
        val savedLanguage = sharedPreferences.getString(PREF_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM

        // Check the appropriate button based on the saved language
        val buttonId = when (savedLanguage) {
            LANGUAGE_ENGLISH -> englishLanguageButton.id
            LANGUAGE_FRENCH -> frenchLanguageButton.id
            else -> systemLanguageButton.id
        }
        languageToggleGroup.check(buttonId)

        // Set up the listener for language selection
        languageToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val selectedLanguage = when (checkedId) {
                    englishLanguageButton.id -> LANGUAGE_ENGLISH
                    frenchLanguageButton.id -> LANGUAGE_FRENCH
                    else -> LANGUAGE_SYSTEM
                }

                // Save the selected language
                if (selectedLanguage != savedLanguage) {
                    sharedPreferences.edit().putString(PREF_LANGUAGE, selectedLanguage).apply()

                    // Restart the entire application to apply the new language
                    recreateActivity()
                }
            }
        }
    }

    private fun recreateActivity() {
        // Create an intent to restart the entire application
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        finish()
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // Ferme cette activité et retourne à la précédente
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupDeleteButton() {
        deleteDataButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_delete_confirmation_dialog_title)
            .setMessage(R.string.settings_delete_confirmation_dialog_message)
            .setPositiveButton(R.string.settings_delete_confirmation_dialog_confirm) { _, _ ->
                deleteAllUserData()
            }
            .setNegativeButton(R.string.settings_delete_confirmation_dialog_cancel, null)
            .show()
    }

    private fun deleteAllUserData() {
        lifecycleScope.launch {
            trackHistoryDao.deleteAll()
            Toast.makeText(this@SettingsActivity, getString(R.string.data_deleted_toast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCheckPermissionsButton() {
        checkPermissionsButton.setOnClickListener {
            showPermissionsDialog()
        }
    }

    private fun showPermissionsDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permissions_check_title)
            .setMessage(R.string.permissions_check_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                checkAndRequestAllPermissions()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun checkAndRequestAllPermissions() {
        // Check and request notification listener permission
        checkNotificationListenerPermission()

        // Check and request POST_NOTIFICATIONS permission (for Android 13+)
        requestPostNotificationsPermission()

        // Check and request battery optimization permission
        requestBatteryOptimizationPermission()
    }

    private fun checkNotificationListenerPermission() {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val componentName = ComponentName(this, NotificationListener::class.java).flattenToString()
        val isEnabled = enabledListeners != null && enabledListeners.contains(componentName)

        Log.d("SettingsActivity", "Notification Listener Enabled: $isEnabled")

        if (!isEnabled) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Could not open Notification Listener Settings", e)
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
                    Log.d("SettingsActivity", "Permission POST_NOTIFICATIONS already granted.")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionRationaleDialog()
                }
                else -> {
                    Log.d("SettingsActivity", "Requesting POST_NOTIFICATIONS permission.")
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
                Log.d("SettingsActivity", "Requesting battery optimization exception")
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Could not open battery optimization settings", e)
            }
        } else {
            Log.d("SettingsActivity", "Battery optimization already disabled for this app")
        }
    }
}
