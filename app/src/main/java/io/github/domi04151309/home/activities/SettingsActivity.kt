package io.github.domi04151309.home.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import io.github.domi04151309.home.helpers.LocaleHelper
import io.github.domi04151309.home.helpers.P
import io.github.domi04151309.home.helpers.Rooms
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : BaseActivity() {

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            exportConfiguration(uri)
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importConfiguration(uri)
        }
    }

    private fun exportConfiguration(uri: Uri) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val devicesJson = prefs.getString(PREF_DEVICES_JSON, Global.DEFAULT_JSON) ?: Global.DEFAULT_JSON
            val roomsJson = prefs.getString(PREF_ROOMS_JSON, DEFAULT_ROOMS_JSON) ?: DEFAULT_ROOMS_JSON

            // Create pretty printed JSON with 4-space indentation
            val exportData = JSONObject().apply {
                put("export_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("devices", JSONObject(devicesJson).optJSONObject("devices") ?: JSONObject())
                put("rooms", JSONObject(roomsJson).optJSONObject("rooms") ?: JSONObject())
            }.toString(4)

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(exportData)
                }
            }
            Toast.makeText(this, "Configuration exported successfully", Toast.LENGTH_LONG).show()
        } catch (e: IllegalStateException) {
            Toast.makeText(this, getString(R.string.pref_export_failed, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun importConfiguration(uri: Uri) {
        try {
            val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: throw IllegalStateException("Could not read file")

            val json = JSONObject(jsonString)

            // Validate the JSON structure
            if (!json.has("devices") || !json.has("rooms")) {
                throw Exception("Invalid configuration file")
            }

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pref_import)
                .setMessage(R.string.pref_import_confirm_message)
                .setPositiveButton(R.string.str_import) { _, _ ->
                    try {
                        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                        prefs.edit {
                            putString(PREF_DEVICES_JSON, "{\"devices\":${json.getJSONObject("devices").toString()}}")
                            putString(PREF_ROOMS_JSON, "{\"rooms\":${json.getJSONObject("rooms").toString()}}")
                        }
                        Devices.reloadFromPreferences()
                        Rooms.reloadFromPreferences()
                        Toast.makeText(this, R.string.pref_import_success, Toast.LENGTH_LONG).show()
                    } catch (e: IllegalStateException) {
                        Toast.makeText(this, getString(R.string.pref_import_failed, e.message), Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } catch (e: IllegalStateException) {
            Toast.makeText(this, getString(R.string.pref_import_failed, e.message), Toast.LENGTH_LONG).show()
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
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        exportLauncher.launch("homeapp_config_$timestamp.json")
    }

    fun openImportLauncher() {
        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
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
                            putString(PREF_DEVICES_JSON, Global.DEFAULT_JSON)
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
            setupLanguagePreference()
        }

        private fun setupLanguagePreference() {
            findPreference<Preference>(P.PREF_LANGUAGE)?.setOnPreferenceChangeListener { _, newValue ->
                val newLang = newValue as String
                LocaleHelper.setNewLocale(requireContext(), newLang)
                Toast.makeText(context, R.string.pref_language_restart, Toast.LENGTH_SHORT).show()
                restartApp()
                true
            }
        }

        private fun restartApp() {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val packageManager = requireContext().packageManager
                val packageName = requireContext().packageName
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                intent?.let {
                    val flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    it.addFlags(flags)
                    it.putExtra("language_changed", true)
                    startActivity(it)
                }
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(0)
            }, RESTART_DELAY_MS)
        }

        companion object {
            private const val RESTART_DELAY_MS = 500L
            private const val PREF_DEVICES_JSON = "devices_json"
            private const val PREF_ROOMS_JSON = "rooms_json"
            private const val DEFAULT_ROOMS_JSON = "{\"rooms\":{}}"
        }
    }
}
