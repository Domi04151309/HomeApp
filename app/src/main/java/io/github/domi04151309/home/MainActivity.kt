package io.github.domi04151309.home

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
        nav_view.setCheckedItem(R.id.nav_devices)

        val testTitles = arrayOf("One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten")
        val testSummaries = arrayOf("Online", "Online", "Online", "Online", "Online", "Online", "Online", "Offline", "Offline", "Offline")

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val listView = findViewById<View>(R.id.listView) as ListView
        //Example: {"devices": {"Arduino MKR1000": "192.168.20.45", "Raspberry Pi": "192.168.20.??"}}
        loadDevices(prefs, listView)

        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val summary = view.findViewById(R.id.summary) as TextView
            val hidden = view.findViewById(R.id.hidden) as TextView
            val url = hidden.text.toString() + "commands"
            Log.d("Home", url)
            val queue = Volley.newRequestQueue(this)
            val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                    Response.Listener { response ->
                        summary.text = response.toString()
                    },
                    Response.ErrorListener { error ->
                        summary.text = resources.getString(R.string.main_device_unavailable)
                        Log.e("Home", error.toString())
                    }
            )
            queue.add(jsonObjectRequest)

            //adapter = ListAdapter(this, testTitles, summaries)
            //listView.adapter = adapter

            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

    private fun loadDevices(prefs: SharedPreferences, listView: ListView){
        //Example: {"devices": {"Arduino MKR1000": "192.168.20.45", "Raspberry Pi": "192.168.20.??"}}
        val jsonString = prefs.getString("devices_json", "{\"devices\":{\"No devices found\":\"null\"}}")
        val jsonDevices = JSONObject(jsonString).getJSONObject("devices")
        val deviceList = jsonDevices.names()
        val titles = arrayOfNulls<String>(deviceList.length())
        val summaries = arrayOfNulls<String>(deviceList.length())
        val ips = arrayOfNulls<String>(deviceList.length())
        var i = 0
        val count = deviceList.length()
        while (i < count) {
            try {
                val mJsonString = deviceList.getString(i)
                titles[i] = mJsonString.toString()
                if (jsonDevices.getString(mJsonString) == "null")
                    summaries[i] = "Add a device in the settings"
                else
                    summaries[i] = jsonDevices.getString(mJsonString)
                ips[i] = formatURL(jsonDevices.getString(mJsonString))
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            i++
        }
        Log.d("Home", Arrays.toString(titles) + Arrays.toString(summaries))

        val adapter = ListAdapter(this, titles, summaries, ips)
        listView.adapter = adapter
    }

    private fun formatURL(url: String): String {
        var _url = url
        if (!(_url.startsWith("https://") || _url.startsWith("http://")))
            _url = "http://$_url"
        if (!_url.endsWith("/"))
            _url += "/"
        return _url
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_devices -> {
                // Handle the action
            }
            R.id.nav_wiki -> {
                val uri = "https://github.com/Domi04151309/home/wiki"
                startActivity(Intent(this, WebActivity::class.java).putExtra("URI", uri).putExtra("title", resources.getString(R.string.nav_wiki)))
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, Preferences::class.java))
            }
            R.id.nav_source -> {
                val uri = "https://github.com/Domi04151309/home"
                startActivity(Intent(this, WebActivity::class.java).putExtra("URI", uri).putExtra("title", resources.getString(R.string.nav_source)))
            }
            R.id.nav_google_home -> {
                val intent = Intent()
                intent.component = ComponentName("com.google.android.apps.chromecast.app", "com.google.android.apps.chromecast.app.DiscoveryActivity")
                startActivity(intent)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onResume() {
        super.onResume()
        nav_view.setCheckedItem(R.id.nav_devices)
        loadDevices(prefs = PreferenceManager.getDefaultSharedPreferences(this),listView = findViewById<View>(R.id.listView) as ListView)
    }
}
