package io.github.domi04151309.home.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import android.net.Uri
import io.github.domi04151309.home.R
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.P
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.Theme

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, GeneralPreferenceFragment())
                .commit()
    }

    class GeneralPreferenceFragment : PreferenceFragmentCompat() {
        private val prefsChangedListener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == P.PREF_THEME) requireActivity().recreate()
                }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(
                    prefsChangedListener
            )
        }

        override fun onDestroy() {
            super.onDestroy()
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(
                    prefsChangedListener
            )
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_general)
            findPreference<Preference>("devices")?.setOnPreferenceClickListener {
                startActivity(Intent(context, DevicesActivity::class.java))
                true
            }
            findPreference<Preference>("devices_json")?.setOnPreferenceClickListener {
                Devices.reloadFromPreferences()
                true
            }
            findPreference<Preference>("reset_json")?.setOnPreferenceClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.pref_reset)
                    .setMessage(R.string.pref_reset_question)
                    .setPositiveButton(R.string.str_delete) { _, _ ->
                        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putString("devices_json", Global.DEFAULT_JSON).apply()
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
                startActivity(Intent(context, WebActivity::class.java).putExtra("URI", uri).putExtra("title", resources.getString(R.string.pref_info_wiki)))
                true
            }
            findPreference<Preference>("header")?.setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://unsplash.com/photos/mx4mSkK9zeo")))
                true
            }
        }
    }
}
