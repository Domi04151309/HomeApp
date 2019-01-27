package io.github.domi04151309.home

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.AppBarLayout
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONException
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var devices: Devices? = null
    private var listView: ListView? = null
    private var ips: Array<String?>? = null
    private var level = 1
    private var reset = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devices = Devices(PreferenceManager.getDefaultSharedPreferences(this))
        listView = findViewById<View>(R.id.listView) as ListView
        val appBar = findViewById<AppBarLayout>(R.id.app_bar)

        listView!!.viewTreeObserver.addOnScrollChangedListener({
            if (Global.getScroll(listView!!) > 0)
                appBar.elevation = 16f
            else
                appBar.elevation = 0f
        })

        fab.setOnClickListener { view ->
            reset = true
            startActivity(Intent(this, DevicesActivity::class.java))
        }

        val menuButton = findViewById<View>(R.id.menu_icon) as ImageView
        menuButton.setOnClickListener({
            drawer_layout.openDrawer(GravityCompat.START)
        })

        nav_view.setNavigationItemSelectedListener(this)
        nav_view.setCheckedItem(R.id.nav_devices)

        loadDevices()

        listView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            if (level == 1)
                loadCommands(view)
            else if (level == 2)
                execute(view)
        }
    }

    private fun loadDevices(){
        var titles: Array<String?>?
        var summaries: Array<String?>?
        var drawables: IntArray?
        var i = 0
        try {
            if (devices!!.length() == 0) {
                titles = arrayOfNulls(1)
                summaries = arrayOfNulls(1)
                ips = arrayOfNulls(1)
                drawables = IntArray(1)
                titles[i] = resources.getString(R.string.main_no_devices)
                summaries[i] = resources.getString(R.string.main_no_devices_summary)
                ips!![i] = Global.formatURL("null")
                drawables[i] = R.drawable.ic_info
            } else {
                val count = devices!!.length()
                titles = arrayOfNulls(count)
                summaries = arrayOfNulls(count)
                drawables = IntArray(count)
                ips = arrayOfNulls(count)
                while (i < count) {
                    try {
                        val name = devices!!.getName(i)
                        titles[i] = name
                        summaries[i] = resources.getString(R.string.main_tap_to_connect)
                        ips!![i] = Global.formatURL(devices!!.getAddress(name))
                        drawables[i] = Global.getIconId(devices!!.getIcon(name))
                    } catch (e: JSONException) {
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                    i++
                }
            }
        } catch (e: Exception){
            titles = arrayOfNulls(1)
            summaries = arrayOfNulls(1)
            ips = arrayOfNulls(1)
            drawables = IntArray(1)
            titles[i] = resources.getString(R.string.err_wrong_format)
            summaries[i] = resources.getString(R.string.err_wrong_format_summary)
            drawables[i] = R.drawable.ic_warning
            ips!![i] = Global.formatURL("null")
            Log.e(Global.LOG_TAG, e.toString())
        }
        Log.d(Global.LOG_TAG, Arrays.toString(titles) + Arrays.toString(summaries))

        val adapter = ListAdapter(this, titles, summaries, ips, drawables)
        listView!!.adapter = adapter
        setLevelOne()
    }

    private fun loadCommands(view: View){
        val title = view.findViewById<TextView>(R.id.title).text
        val summary = view.findViewById(R.id.summary) as TextView
        val ip = view.findViewById(R.id.hidden) as TextView
        if (ip.text.toString() == "http://null/") return
        summary.text = resources.getString(R.string.main_connecting)
        val url = ip.text.toString() + "commands"
        Log.d(Global.LOG_TAG, url)
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    try {
                        Log.d(Global.LOG_TAG, response.toString())
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
                                Log.e(Global.LOG_TAG, e.toString())
                            }
                            i++
                        }

                        val adapter = ListAdapter(this, titles, summaries, commands)
                        listView!!.adapter = adapter
                        setLevelTwo(view.findViewById<ImageView>(R.id.drawable).drawable, title)
                    } catch (e: Exception) {
                        summary.text = resources.getString(R.string.err_wrong_format_summary)
                        setLevelOne()
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                },
                Response.ErrorListener { error ->
                    summary.text = resources.getString(R.string.main_device_unavailable)
                    Log.w(Global.LOG_TAG, error.toString())
                }
        )
        queue.add(jsonObjectRequest)
    }

    private fun execute(view: View) {
        val ip = view.findViewById(R.id.hidden) as TextView
        val url = ip.text.toString()
        Log.d(Global.LOG_TAG, url)
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    Log.d(Global.LOG_TAG, response.toString())
                    try {
                        Toast.makeText(this, response.getString("toast"), Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, resources.getString(R.string.err_wrong_format_summary), Toast.LENGTH_LONG).show()
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(this, resources.getString(R.string.main_execution_failed), Toast.LENGTH_LONG).show()
                    Log.w(Global.LOG_TAG, error.toString())
                }
        )
        queue.add(jsonObjectRequest)
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
        when (item.itemId) {
            R.id.nav_devices -> {
                loadDevices()
                reset = false
            }
            R.id.nav_wiki -> {
                val uri = "https://github.com/Domi04151309/HomeApp/wiki"
                startActivity(Intent(this, WebActivity::class.java).putExtra("URI", uri).putExtra("title", resources.getString(R.string.nav_wiki)))
                reset = true
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, Preferences::class.java))
                reset = true
            }
            R.id.nav_source -> {
                val uri = "https://github.com/Domi04151309/HomeApp"
                startActivity(Intent(this, WebActivity::class.java).putExtra("URI", uri).putExtra("title", resources.getString(R.string.nav_source)))
                reset = true
            }
            R.id.nav_google_home -> {
                val intent = Intent()
                intent.component = ComponentName("com.google.android.apps.chromecast.app", "com.google.android.apps.chromecast.app.DiscoveryActivity")
                startActivity(intent)
                reset = true
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setLevelOne() {
        deviceIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_home))
        deviceName.text = resources.getString(R.string.main_device_name)
        fab.show()
        level = 1
    }

    private fun setLevelTwo(icon: Drawable, title: CharSequence) {
        fab.hide()
        deviceIcon.setImageDrawable(icon)
        deviceName.text = title
        level = 2
    }

    override fun onResume() {
        super.onResume()
        if(reset) {
            nav_view.setCheckedItem(R.id.nav_devices)
            loadDevices()
            reset = false
        }
    }
}
