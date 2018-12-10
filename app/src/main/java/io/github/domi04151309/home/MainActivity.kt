package io.github.domi04151309.home

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
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
import android.widget.Toast
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

    //val testTitles = arrayOf("One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten")
    //val testSummaries = arrayOf("Online", "Online", "Online", "Online", "Online", "Online", "Online", "Offline", "Offline", "Offline")

    private var prefs: SharedPreferences? = null
    private var listView: ListView? = null
    private var ips: Array<String?>? = null
    private var level = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            startActivity(Intent(this, DevicesActivity::class.java))
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
        nav_view.setCheckedItem(R.id.nav_devices)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        listView = findViewById<View>(R.id.listView) as ListView
        loadDevices()

        listView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            if (level == 1)
                loadCommands(view)
            else if (level == 2)
                execute(view)
        }
    }

    private fun loadDevices(){
        //Example: {"devices": {"Arduino MKR1000": "192.168.20.45", "Raspberry Pi": "192.168.20.??"}}
        val jsonString = prefs!!.getString("devices_json", "{\"devices\":{}}")
        val jsonDevices = JSONObject(jsonString).getJSONObject("devices")
        val titles: Array<String?>?
        val summaries: Array<String?>?
        var i = 0
        if (jsonDevices.length() == 0) {
            titles = arrayOfNulls(1)
            summaries = arrayOfNulls(1)
            ips = arrayOfNulls(1)
            titles[i] = resources.getString(R.string.main_no_devices)
            summaries[i] = resources.getString(R.string.main_no_devices_summary)
            ips!![i] = formatURL("null")
        } else {
            val deviceList = jsonDevices.names()
            titles = arrayOfNulls(deviceList.length())
            summaries = arrayOfNulls(deviceList.length())
            ips = arrayOfNulls(deviceList.length())
            val count = deviceList.length()
            while (i < count) {
                try {
                    val mJsonString = deviceList.getString(i)
                    if (mJsonString.toString() == "null")
                        titles[i] = resources.getString(R.string.main_no_devices)
                    else
                        titles[i] = mJsonString.toString()
                    if (jsonDevices.getString(mJsonString) == "null")
                        summaries[i] = resources.getString(R.string.main_no_devices_summary)
                    else
                        summaries[i] = resources.getString(R.string.main_tap_to_connect)
                    ips!![i] = formatURL(jsonDevices.getString(mJsonString))
                } catch (e: JSONException) {
                    Log.e("Home", e.toString())
                }
                i++
            }
        }
        Log.d("Home", Arrays.toString(titles) + Arrays.toString(summaries))

        val adapter = ListAdapter(this, titles, summaries, ips)
        listView!!.adapter = adapter
        level = 1
    }

    private fun loadCommands(view: View){
        val summary = view.findViewById(R.id.summary) as TextView
        val ip = view.findViewById(R.id.hidden) as TextView
        if (ip.text.toString() == "http://null/") return
        val url = ip.text.toString() + "commands"
        Log.d("Home", url)
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    Log.d("Home", response.toString())
                    val jsonCommands = response.getJSONObject("commands")
                    val commandsList = jsonCommands.names()
                    val titles = arrayOfNulls<String>(commandsList.length())
                    val summaries = arrayOfNulls<String>(commandsList.length())
                    val commands = arrayOfNulls<String>(commandsList.length())
                    var i = 0
                    val count = commandsList.length()
                    while (i < count) {
                        try {
                            val mJsonString = commandsList.getString(i)
                            titles[i] = jsonCommands.getJSONObject(mJsonString).getString("title")
                            summaries[i] = jsonCommands.getJSONObject(mJsonString).getString("summary")
                            commands[i] = ip.text.toString() + mJsonString
                        } catch (e: JSONException) {
                            Log.e("Home", e.toString())
                        }
                        i++
                    }

                    val adapter = ListAdapter(this, titles, summaries, commands)
                    listView!!.adapter = adapter
                    level = 2
                },
                Response.ErrorListener { error ->
                    summary.text = resources.getString(R.string.main_device_unavailable)
                    Log.w("Home", error.toString())
                }
        )
        queue.add(jsonObjectRequest)
    }

    private fun execute(view: View) {
        val ip = view.findViewById(R.id.hidden) as TextView
        val url = ip.text.toString()
        Log.d("Home", url)
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    Log.d("Home", response.toString())
                    Toast.makeText(this, response.getString("toast"), Toast.LENGTH_LONG).show()
                },
                Response.ErrorListener { error ->
                    Toast.makeText(this, resources.getString(R.string.main_execution_failed), Toast.LENGTH_LONG).show()
                    Log.w("Home", error.toString())
                }
        )
        queue.add(jsonObjectRequest)
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
        } else if (level == 2) {
            loadDevices()
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_devices -> {
                loadDevices()
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
        loadDevices()
    }
}
