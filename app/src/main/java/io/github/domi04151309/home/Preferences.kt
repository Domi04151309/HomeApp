package io.github.domi04151309.home

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.Preference
import android.preference.PreferenceManager
import android.widget.Toast
import android.support.v7.app.AlertDialog

class Preferences : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, GeneralPreferenceFragment())
                .commit()
    }

    private fun setupActionBar() {
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class GeneralPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_general)
            findPreference("version").summary = BuildConfig.VERSION_NAME
            findPreference("reset_json").onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val builder = AlertDialog.Builder(context)
                builder.setTitle(resources.getString(R.string.pref_reset))
                builder.setMessage(resources.getString(R.string.pref_reset_question))
                builder.setPositiveButton(resources.getString(android.R.string.ok), { _, _ ->
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putString("devices_json", Global.DEFAULT_JSON).apply()
                    Toast.makeText(context, resources.getString(R.string.pref_reset_toast), Toast.LENGTH_LONG).show()
                })
                builder.setNegativeButton(resources.getString(android.R.string.cancel), { dialog, _ -> dialog.cancel() })
                builder.show()
                true
            }
        }
    }
}
