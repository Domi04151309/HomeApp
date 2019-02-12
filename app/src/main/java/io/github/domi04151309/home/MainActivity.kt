package io.github.domi04151309.home

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.android.volley.*
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
    private var addresses: Array<String?>? = null
    private var level = 1
    private var reset = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.setNoActionBar(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devices = Devices(PreferenceManager.getDefaultSharedPreferences(this))
        listView = findViewById<View>(R.id.listView) as ListView

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
                addresses = arrayOfNulls(1)
                drawables = IntArray(1)
                titles[i] = resources.getString(R.string.main_no_devices)
                summaries[i] = resources.getString(R.string.main_no_devices_summary)
                addresses!![i] = Global.formatURL("null")
                drawables[i] = R.drawable.ic_info
            } else {
                val count = devices!!.length()
                titles = arrayOfNulls(count)
                summaries = arrayOfNulls(count)
                drawables = IntArray(count)
                addresses = arrayOfNulls(count)
                while (i < count) {
                    try {
                        val name = devices!!.getName(i)
                        titles[i] = name
                        summaries[i] = resources.getString(R.string.main_tap_to_connect)
                        addresses!![i] = Global.formatURL(devices!!.getAddress(name))
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
            addresses = arrayOfNulls(1)
            drawables = IntArray(1)
            titles[i] = resources.getString(R.string.err_wrong_format)
            summaries[i] = resources.getString(R.string.err_wrong_format_summary)
            drawables[i] = R.drawable.ic_warning
            addresses!![i] = Global.formatURL("null")
            Log.e(Global.LOG_TAG, e.toString())
        }
        Log.d(Global.LOG_TAG, Arrays.toString(titles) + Arrays.toString(summaries))

        val adapter = ListAdapter(this, titles, summaries, addresses, drawables)
        listView!!.adapter = adapter
        setLevelOne()
    }

    private fun loadCommands(view: View){
        val title = view.findViewById<TextView>(R.id.title).text
        val summary = view.findViewById<TextView>(R.id.summary)
        val address = view.findViewById<TextView>(R.id.hidden).text.toString()
        if (address == "http://null/") return
        summary.text = resources.getString(R.string.main_connecting)
        if (devices!!.getMode(title.toString()) == "Website"){
            startActivity(Intent(this, WebActivity::class.java).putExtra("URI", address).putExtra("title", title))
            reset = true
            return
        }
        val url = address + "commands"
        Log.d(Global.LOG_TAG, url)
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    try {
                        Log.d(Global.LOG_TAG, response.toString())
                        val jsonCommands = response.getJSONObject("commands")
                        val commandsList = jsonCommands.names()
                        val count = commandsList.length()
                        val titles = arrayOfNulls<String>(count)
                        val summaries = arrayOfNulls<String>(count)
                        val commands = arrayOfNulls<String>(count)
                        var i = 0
                        while (i < count) {
                            try {
                                val mJsonString = commandsList.getString(i)
                                titles[i] = jsonCommands.getJSONObject(mJsonString).getString("title")
                                summaries[i] = jsonCommands.getJSONObject(mJsonString).getString("summary")
                                commands[i] = address + mJsonString
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
                    if(error is TimeoutError || error is NoConnectionError) {
                        summary.text = resources.getString(R.string.main_device_unavailable)
                    } else if(error is ParseError) {
                        summary.text = resources.getString(R.string.main_parse_error)
                    } else if(error is ClientError) {
                        summary.text = resources.getString(R.string.main_device_client_error)
                    } else {
                        summary.text = resources.getString(R.string.main_device_unavailable)
                    }
                    Log.w(Global.LOG_TAG, error.toString())
                }
        )
        queue.add(jsonObjectRequest)
    }

    private fun execute(view: View) {
        val address = view.findViewById(R.id.hidden) as TextView
        val url = address.text.toString()
        Log.d(Global.LOG_TAG, url)
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    Log.d(Global.LOG_TAG, response.toString())
                    try {
                        Toast.makeText(this, response.getString("toast"), Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, resources.getString(R.string.main_execution_completed), Toast.LENGTH_LONG).show()
                        Log.w(Global.LOG_TAG, e.toString())
                    }
                },
                Response.ErrorListener { error ->
                    if(error is TimeoutError || error is NoConnectionError) {
                        Toast.makeText(this, resources.getString(R.string.main_device_unavailable), Toast.LENGTH_LONG).show()
                    } else if(error is ParseError) {
                        Toast.makeText(this, resources.getString(R.string.main_parse_error), Toast.LENGTH_LONG).show()
                    } else if(error is ClientError) {
                        Toast.makeText(this, resources.getString(R.string.main_command_client_error), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, resources.getString(R.string.main_execution_failed), Toast.LENGTH_LONG).show()
                    }
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
        val theme = resources.newTheme()
        theme.applyStyle(R.style.Dark, false)
        deviceIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_home, theme))
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
