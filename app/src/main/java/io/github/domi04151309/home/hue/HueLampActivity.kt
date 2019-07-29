package io.github.domi04151309.home.hue

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.*
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.Devices
import io.github.domi04151309.home.Global
import io.github.domi04151309.home.Global.volleyError
import io.github.domi04151309.home.R
import io.github.domi04151309.home.Theme
import java.lang.Exception

class HueLampActivity : AppCompatActivity() {

    private var isRoom: Boolean = false
    private var queue: RequestQueue? = null
    private var lightDataRequest: JsonObjectRequest? = null
    private var roomDataRequest: JsonObjectRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hue_lamp)

        val internId = intent.getStringExtra("ID")
        val id: String
        if (internId.startsWith("room#")) {
            id = internId.substring(internId.lastIndexOf("#") + 1)
            isRoom = true
        } else {
            id = internId
            isRoom = false
        }
        val device = intent.getStringExtra("Device")
        val address = Devices(PreferenceManager.getDefaultSharedPreferences(this)).getAddress(device)
        val hueAPI = HueAPI(this, device)
        queue = Volley.newRequestQueue(this)

        title = device
        val briBar = findViewById<SeekBar>(R.id.briBar)
        val ctBar = findViewById<SeekBar>(R.id.ctBar)
        val gridView = findViewById<View>(R.id.scenes) as GridView

        //Get scenes
        fun setProgress(seekBar: SeekBar, value: Int) {
            if (Build.VERSION.SDK_INT >= 24)
                seekBar.setProgress(value, true)
            else
                seekBar.progress = value
        }

        lightDataRequest = JsonObjectRequest(Request.Method.GET,  address + "api/" + hueAPI.getUsername() + "/lights/" + id, null,
                Response.Listener { response ->
                    findViewById<TextView>(R.id.nameTxt).text = response.getString("name")
                    try {
                        setProgress(briBar, response.getJSONObject("state").getInt("bri"))
                    } catch (e: Exception) {
                        findViewById<TextView>(R.id.briTxt).visibility = View.GONE
                        briBar.visibility = View.GONE
                    }
                    try {
                        setProgress(ctBar, response.getJSONObject("state").getInt("ct") - 153)
                    } catch (e: Exception) {
                        findViewById<TextView>(R.id.ctTxt).visibility = View.GONE
                        ctBar.visibility = View.GONE
                    }
                },
                Response.ErrorListener { error ->
                    finish()
                    Toast.makeText(this, volleyError(this, error), Toast.LENGTH_LONG).show()
                }
        )

        roomDataRequest = JsonObjectRequest(Request.Method.GET, address + "api/" + hueAPI.getUsername() + "/groups/" + id, null,
                Response.Listener { response ->
                    findViewById<TextView>(R.id.nameTxt).text = response.getString("name")
                    try {
                        setProgress(briBar, response.getJSONObject("action").getInt("bri"))
                    } catch (e: Exception) {
                        findViewById<TextView>(R.id.briTxt).visibility = View.GONE
                        briBar.visibility = View.GONE
                    }
                    try {
                        setProgress(ctBar, response.getJSONObject("action").getInt("ct") - 153)
                    } catch (e: Exception) {
                        findViewById<TextView>(R.id.ctTxt).visibility = View.GONE
                        ctBar.visibility = View.GONE
                    }
                },
                Response.ErrorListener { error ->
                    finish()
                    Toast.makeText(this, volleyError(this, error), Toast.LENGTH_LONG).show()
                }
        )
        val scenesRequest = JsonObjectRequest(Request.Method.GET, address + "api/" + hueAPI.getUsername() + "/scenes/", null,
                Response.Listener { response ->
                    try {
                        val count = response.length()
                        val drawables: ArrayList<Int> = arrayListOf()
                        val names: ArrayList<String> = arrayListOf()
                        val ids: ArrayList<String> = arrayListOf()
                        var i = 0
                        while (i < count) {
                            val currentObjectName = response.names().getString(i)
                            val currentObject = response.getJSONObject(currentObjectName)
                            if (currentObject.getString("group") == id) {
                                if (currentObject.getString("group") == id) {
                                    drawables.add(R.drawable.ic_hue_scene)
                                    names.add(currentObject.getString("name"))
                                    ids.add(currentObjectName)
                                }
                            }
                            i++
                        }
                        val adapter = HueScenesGridAdapter(this, drawables.toIntArray(), names.toTypedArray(), ids.toTypedArray())
                        gridView.adapter = adapter
                    } catch (e: Exception){
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                },
                Response.ErrorListener { error ->
                    finish()
                    Toast.makeText(this, volleyError(this, error), Toast.LENGTH_LONG).show()
                }
        )
        if (isRoom) queue!!.add(scenesRequest)

        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            hueAPI.activateSceneOfGroup(id, view.findViewById<TextView>(R.id.hidden).text.toString())
            Handler().postDelayed({
                queue!!.add(roomDataRequest)
            }, 250)
        }

        // Selected item is a whole room
        if (isRoom) {
            queue!!.add(roomDataRequest)

            findViewById<Button>(R.id.onBtn).setOnClickListener {
                hueAPI.switchGroupByID(id, true)
            }

            findViewById<Button>(R.id.offBtn).setOnClickListener {
                hueAPI.switchGroupByID(id, false)
            }

            briBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    hueAPI.changeBrightnessOfGroup(id, seekBar.progress)
                }
            })

            ctBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    hueAPI.changeColorTemperatureOfGroup(id, seekBar.progress + 153)
                }
            })
        }

        // Selected item is a single light
        else {
            queue!!.add(lightDataRequest)

            findViewById<Button>(R.id.onBtn).setOnClickListener {
                hueAPI.switchLightByID(id, true)
            }

            findViewById<Button>(R.id.offBtn).setOnClickListener {
                hueAPI.switchLightByID(id, false)
            }

            briBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    hueAPI.changeBrightness(id, seekBar.progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })

            ctBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    hueAPI.changeColorTemperature(id, seekBar.progress + 153)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        }
    }

    override fun onResume() {
        super.onResume()
        if(isRoom) queue!!.add(roomDataRequest)
        else queue!!.add(lightDataRequest)
    }
}
