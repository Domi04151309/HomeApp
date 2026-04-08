package io.github.domi04151309.home.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
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

class SettingsActivity : BaseActivity() {
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
            findPreference<Preference>("devices_json")?.setOnPreferenceClickListener {
                Devices.reloadFromPreferences()
                true
            }
            findPreference<Preference>("reset_json")?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_reset)
                    .setMessage(R.string.pref_reset_question)
                    .setPositiveButton(R.string.str_delete) { _, _ ->
                        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                            putString("devices_json", Global.DEFAULT_JSON)
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
                    val flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
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
        }
    }
}
