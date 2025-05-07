package fr.loicnogier.mystreamlogs

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var deleteDataButton: Button

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val trackHistoryDao by lazy { database.trackHistoryDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        toolbar = findViewById(R.id.toolbar)
        deleteDataButton = findViewById(R.id.deleteDataButton)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        setupDeleteButton()
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
}