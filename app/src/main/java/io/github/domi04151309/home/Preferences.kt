package io.github.domi04151309.home

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
import io.github.domi04151309.home.objects.Global
import io.github.domi04151309.home.objects.Theme

class Preferences : AppCompatActivity() {

    private val spChanged = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "theme") {
            startActivity(Intent(this@Preferences, MainActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, GeneralPreferenceFragment())
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(spChanged)
    }

    class GeneralPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_general)
            findPreference<Preference>("devices")!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                startActivity(Intent(context, DevicesActivity::class.java))
                true
            }
            findPreference<Preference>("reset_json")!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                AlertDialog.Builder(requireContext())
                        .setTitle(R.string.pref_reset)
                        .setMessage(R.string.pref_reset_question)
                        .setPositiveButton(R.string.str_reset) { _, _ ->
                            PreferenceManager.getDefaultSharedPreferences(context).edit().putString("devices_json", Global.DEFAULT_JSON).apply()
                            Toast.makeText(context, R.string.pref_reset_toast, Toast.LENGTH_LONG).show()
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                        .show()
                true
            }
            findPreference<Preference>("about")!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                startActivity(Intent(context, AboutActivity::class.java))
                true
            }
            findPreference<Preference>("header")!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://unsplash.com/photos/mx4mSkK9zeo")))
                true
            }
        }
    }
}
