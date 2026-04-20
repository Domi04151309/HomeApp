package io.github.domi04151309.home.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.domi04151309.home.R
import io.github.domi04151309.home.fragments.PreferenceFragment
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.P
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : BaseActivity() {
    private val exportLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri: Uri? ->
            if (uri != null) {
                exportConfiguration(uri)
            }
        }

    private val importLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            if (uri != null) {
                importConfiguration(uri)
            }
        }

    private fun exportConfiguration(uri: Uri) {
        try {
            val devicesJson =
                PreferenceManager
                    .getDefaultSharedPreferences(this)
                    .getString(P.PREF_DEVICES_JSON, P.PREF_DEVICES_JSON_DEFAULT)
                    ?: P.PREF_DEVICES_JSON_DEFAULT

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(devicesJson)
                }
            }

            Toast.makeText(this, R.string.pref_export_success, Toast.LENGTH_LONG).show()
        } catch (error: IllegalStateException) {
            Log.w(Global.LOG_TAG, error)
            Toast.makeText(
                this,
                getString(R.string.pref_export_failed),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun importConfiguration(uri: Uri) {
        try {
            val jsonString =
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readText()
                    }
                } ?: error("Could not read file.")

            val json = JSONObject(jsonString)

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pref_import)
                .setMessage(R.string.pref_import_confirmation)
                .setPositiveButton(R.string.str_import) { _, _ ->
                    PreferenceManager.getDefaultSharedPreferences(this).edit {
                        putString(P.PREF_DEVICES_JSON, json.toString())
                    }
                    Devices.reloadFromPreferences()
                    Toast.makeText(this, R.string.pref_import_success, Toast.LENGTH_LONG).show()
                }
                .setNegativeButton(R.string.str_cancel, null)
                .show()
        } catch (error: IllegalStateException) {
            Log.w(Global.LOG_TAG, error)
            Toast.makeText(
                this,
                getString(R.string.pref_import_failed),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, GeneralPreferenceFragment())
            .commit()
    }

    fun openExportLauncher() {
        val timestamp = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(Date())
        exportLauncher.launch("home_app_$timestamp.json")
    }

    fun openImportLauncher() {
        importLauncher.launch(arrayOf("application/json"))
    }

    class GeneralPreferenceFragment : PreferenceFragment() {
        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?,
        ) {
            addPreferencesFromResource(R.xml.pref_general)
            findPreference<Preference>(P.PREF_CONTROLS_AUTH)?.isVisible =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            findPreference<Preference>("devices")?.setOnPreferenceClickListener {
                startActivity(Intent(context, DevicesActivity::class.java))
                true
            }
            findPreference<Preference>("export_config")?.setOnPreferenceClickListener {
                (activity as? SettingsActivity)?.openExportLauncher()
                true
            }
            findPreference<Preference>("import_config")?.setOnPreferenceClickListener {
                (activity as? SettingsActivity)?.openImportLauncher()
                true
            }
            findPreference<Preference>("reset_json")?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_reset)
                    .setMessage(R.string.pref_reset_question)
                    .setPositiveButton(R.string.str_delete) { _, _ ->
                        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                            putString(P.PREF_DEVICES_JSON, P.PREF_DEVICES_JSON_DEFAULT)
                        }
                        Toast.makeText(context, R.string.pref_reset_toast, Toast.LENGTH_LONG).show()
                        Devices.reloadFromPreferences()
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
                true
            }
            findPreference<Preference>("about")?.setOnPreferenceClickListener {
                startActivity(Intent(context, AboutActivity::class.java))
                true
            }
            findPreference<Preference>("wiki")?.setOnPreferenceClickListener {
                val uri = "https://github.com/Domi04151309/HomeApp/wiki"
                startActivity(
                    Intent(context, WebActivity::class.java).putExtra("URI", uri),
                )
                true
            }
            findPreference<Preference>("header")?.setOnPreferenceClickListener {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://unsplash.com/photos/mx4mSkK9zeo".toUri(),
                    ),
                )
                true
            }
        }
    }
}
