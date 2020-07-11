package io.github.domi04151309.home.hue

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.*
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ScenesGridItem
import io.github.domi04151309.home.helpers.CustomJsonArrayRequest
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.UpdateHandler
import io.github.domi04151309.home.objects.Global
import io.github.domi04151309.home.objects.Global.volleyError
import io.github.domi04151309.home.objects.Theme
import org.json.JSONObject

class HueLampActivity : AppCompatActivity() {

    private var canReceiveRequest: Boolean = false
    private var isRoom: Boolean = false
    private var lightDataRequest: JsonObjectRequest? = null
    private var roomDataRequest: JsonObjectRequest? = null
    private var scenesRequest: JsonObjectRequest? = null
    private var address: String = ""
    private var selectedScene: CharSequence = ""
    private var selectedSceneName: CharSequence = ""
    private lateinit var hueAPI: HueAPI
    private lateinit var queue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hue_lamp)

        val internId = intent.getStringExtra("ID") ?: "0"
        val id: String
        if (internId.startsWith("room#")) {
            id = internId.substring(internId.lastIndexOf("#") + 1)
            isRoom = true
        } else {
            id = internId
            isRoom = false
        }
        val deviceId = intent.getStringExtra("Device") ?: ""
        val device = Devices(this).getDeviceById(deviceId)
        address = device.address
        hueAPI = HueAPI(this, deviceId)
        queue = Volley.newRequestQueue(this)

        title = device.name
        val lampIcon = findViewById<ImageView>(R.id.lampIcon)
        val nameTxt = findViewById<TextView>(R.id.nameTxt)
        val briBar = findViewById<SeekBar>(R.id.briBar)
        val ctBar = findViewById<SeekBar>(R.id.ctBar)
        val hueBar = findViewById<SeekBar>(R.id.hueBar)
        val satBar = findViewById<SeekBar>(R.id.satBar)
        val gridView = findViewById<View>(R.id.scenes) as GridView

        //Reset tint
        DrawableCompat.setTint(
                DrawableCompat.wrap(lampIcon.drawable),
                Color.parseColor("#FBC02D")
        )

        //Smooth seekBars
        fun setProgress(seekBar: SeekBar, value: Int) {
            val animation = ObjectAnimator.ofInt(seekBar, "progress", value)
            animation.duration = 300
            animation.interpolator = DecelerateInterpolator()
            animation.start()
        }

        // Selected item is a whole room
        if (isRoom) {
            roomDataRequest = JsonObjectRequest(Request.Method.GET, address + "api/" + hueAPI.getUsername() + "/groups/" + id, null,
                    Response.Listener { response ->
                        nameTxt.text = response.getString("name")
                        val action = response.getJSONObject("action")
                        if (action.has("bri")) {
                            setProgress(briBar, action.getInt("bri"))
                        } else {
                            findViewById<TextView>(R.id.briTxt).visibility = View.GONE
                            briBar.visibility = View.GONE
                        }
                        if (action.has("ct")) {
                            setProgress(ctBar, action.getInt("ct") - 153)
                        } else {
                            findViewById<TextView>(R.id.ctTxt).visibility = View.GONE
                            ctBar.visibility = View.GONE
                        }
                        if (action.has("hue")) {
                            setProgress(hueBar, action.getInt("hue"))
                        } else {
                            findViewById<TextView>(R.id.hueTxt).visibility = View.GONE
                            hueBar.visibility = View.GONE
                        }
                        if (action.has("sat")) {
                            setProgress(satBar, action.getInt("sat"))
                        } else {
                            findViewById<TextView>(R.id.satTxt).visibility = View.GONE
                            satBar.visibility = View.GONE
                        }
                    },
                    Response.ErrorListener { error ->
                        finish()
                        Toast.makeText(this, volleyError(this, error), Toast.LENGTH_LONG).show()
                    }
            )

            scenesRequest = JsonObjectRequest(Request.Method.GET, address + "api/" + hueAPI.getUsername() + "/scenes/", null,
                    Response.Listener { response ->
                        try {
                            val gridItems: ArrayList<ScenesGridItem> = ArrayList(response.length())
                            var currentObjectName: String
                            var currentObject: JSONObject
                            for (i in 0 until response.length()) {
                                currentObjectName = response.names()?.getString(i) ?: ""
                                currentObject = response.getJSONObject(currentObjectName)
                                if (currentObject.getString("group") == id) {
                                    val scene = ScenesGridItem(currentObject.getString("name"))
                                    scene.hidden = currentObjectName
                                    scene.icon = R.drawable.ic_hue_scene
                                    gridItems += scene
                                }
                            }
                            val scene = ScenesGridItem(resources.getString(R.string.hue_add_scene))
                            scene.hidden = "add"
                            scene.icon = R.drawable.ic_hue_scene_add
                            gridItems += scene
                            gridView.adapter = HueScenesGridAdapter(this, gridItems)
                        } catch (e: Exception){
                            Log.e(Global.LOG_TAG, e.toString())
                        }
                    },
                    Response.ErrorListener { error ->
                        finish()
                        Toast.makeText(this, volleyError(this, error), Toast.LENGTH_LONG).show()
                    }
            )

            findViewById<Button>(R.id.onBtn).setOnClickListener {
                hueAPI.switchGroupByID(id, true)
            }

            findViewById<Button>(R.id.offBtn).setOnClickListener {
                hueAPI.switchGroupByID(id, false)
            }

            briBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    hueAPI.changeBrightnessOfGroup(id, seekBar.progress)
                    canReceiveRequest = true
                }
            })

            ctBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    DrawableCompat.setTint(
                            DrawableCompat.wrap(lampIcon.drawable),
                            HueUtils.ctToRGB(seekBar.progress + 153)
                    )
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    hueAPI.changeColorTemperatureOfGroup(id, seekBar.progress + 153)
                    canReceiveRequest = true
                }
            })

            hueBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    DrawableCompat.setTint(
                            DrawableCompat.wrap(lampIcon.drawable),
                            HueUtils.hueSatToRGB(seekBar.progress, satBar.progress)
                    )
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    hueAPI.changeHueOfGroup(id, seekBar.progress)
                    canReceiveRequest = true
                }
            })

            satBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    DrawableCompat.setTint(
                            DrawableCompat.wrap(lampIcon.drawable),
                            HueUtils.hueSatToRGB(hueBar.progress, seekBar.progress)
                    )
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    hueAPI.changeSaturationOfGroup(id, seekBar.progress)
                    canReceiveRequest = true
                }
            })

            gridView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
                val hiddenText = view.findViewById<TextView>(R.id.hidden).text.toString()
                if (hiddenText == "add") {
                    startActivity(Intent(this, HueSceneActivity::class.java).putExtra("deviceId", deviceId).putExtra("room", id))
                } else {
                    hueAPI.activateSceneOfGroup(id, hiddenText)
                    Handler().postDelayed({
                        queue.add(roomDataRequest)
                    }, 200)
                }
            }

            registerForContextMenu(gridView)
        }

        // Selected item is a single light
        else {
            lightDataRequest = JsonObjectRequest(Request.Method.GET,  address + "api/" + hueAPI.getUsername() + "/lights/" + id, null,
                Response.Listener { response ->
                    nameTxt.text = response.getString("name")
                    val state = response.getJSONObject("state")
                    if (state.has("bri")) {
                        setProgress(briBar, state.getInt("bri"))
                    } else {
                        findViewById<TextView>(R.id.briTxt).visibility = View.GONE
                        briBar.visibility = View.GONE
                    }
                    if (state.has("ct")) {
                        setProgress(ctBar, state.getInt("ct") - 153)
                    } else {
                        findViewById<TextView>(R.id.ctTxt).visibility = View.GONE
                        ctBar.visibility = View.GONE
                    }
                    if (state.has("hue")) {
                        setProgress(hueBar, state.getInt("hue"))
                    } else {
                        findViewById<TextView>(R.id.hueTxt).visibility = View.GONE
                        hueBar.visibility = View.GONE
                    }
                    if (state.has("sat")) {
                        setProgress(satBar, state.getInt("sat"))
                    } else {
                        findViewById<TextView>(R.id.satTxt).visibility = View.GONE
                        satBar.visibility = View.GONE
                    }
                },
                Response.ErrorListener { error ->
                    finish()
                    Toast.makeText(this, volleyError(this, error), Toast.LENGTH_LONG).show()
                }
            )

            gridView.visibility = View.GONE

            findViewById<Button>(R.id.onBtn).setOnClickListener {
                hueAPI.switchLightByID(id, true)
            }

            findViewById<Button>(R.id.offBtn).setOnClickListener {
                hueAPI.switchLightByID(id, false)
            }

            briBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) hueAPI.changeBrightness(id, seekBar.progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    canReceiveRequest = true
                }
            })

            ctBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) hueAPI.changeColorTemperature(id, seekBar.progress + 153)
                    DrawableCompat.setTint(
                            DrawableCompat.wrap(lampIcon.drawable),
                            HueUtils.ctToRGB(seekBar.progress + 153)
                    )
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    canReceiveRequest = true
                }
            })

            hueBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) hueAPI.changeHue(id, seekBar.progress)
                    DrawableCompat.setTint(
                            DrawableCompat.wrap(lampIcon.drawable),
                            HueUtils.hueSatToRGB(seekBar.progress, satBar.progress)
                    )
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    canReceiveRequest = true
                }
            })

            satBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) hueAPI.changeSaturation(id, seekBar.progress)
                    DrawableCompat.setTint(
                            DrawableCompat.wrap(lampIcon.drawable),
                            HueUtils.hueSatToRGB(hueBar.progress, seekBar.progress)
                    )
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    canReceiveRequest = true
                }
            })
        }

        if(isRoom) queue.add(scenesRequest)
        val updateHandler = UpdateHandler()
        updateHandler.setUpdateFunction {
            if (canReceiveRequest && hueAPI.readyForRequest) {
                if(isRoom) queue.add(roomDataRequest)
                else queue.add(lightDataRequest)
            }
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val info = menuInfo as AdapterView.AdapterContextMenuInfo
        val view = v as GridView
        selectedScene = view.getChildAt(info.position).findViewById<TextView>(R.id.hidden).text
        selectedSceneName = view.getChildAt(info.position).findViewById<TextView>(R.id.name).text
        if (selectedScene != "add") {
            menuInflater.inflate(R.menu.activity_hue_lamp_context, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.title == resources.getString(R.string.str_rename)) {
            val nullParent: ViewGroup? = null
            val view = layoutInflater.inflate(R.layout.dialog_input, nullParent, false)
            val input = view.findViewById<EditText>(R.id.input)
            input.setText(selectedSceneName)
            AlertDialog.Builder(this)
                    .setTitle(R.string.str_rename)
                    .setMessage(R.string.hue_rename_scene)
                    .setView(view)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val requestObject = "{\"name\":\"" + input.text.toString() + "\"}"
                        val renameSceneRequest = CustomJsonArrayRequest(Request.Method.PUT, address + "api/" + hueAPI.getUsername() + "/scenes/$selectedScene", JSONObject(requestObject),
                                Response.Listener { queue.add(scenesRequest) },
                                Response.ErrorListener { e -> Log.e(Global.LOG_TAG, e.toString()) }
                        )
                        queue.add(renameSceneRequest)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
        } else if (item.title == resources.getString(R.string.str_delete)) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.str_delete)
                    .setMessage(R.string.hue_delete_scene)
                    .setPositiveButton(R.string.str_delete) { _, _ ->
                        val deleteSceneRequest = CustomJsonArrayRequest(Request.Method.DELETE, address + "api/" + hueAPI.getUsername() + "/scenes/" + selectedScene, null,
                                Response.Listener { queue.add(scenesRequest) },
                                Response.ErrorListener { e -> Log.e(Global.LOG_TAG, e.toString()) }
                        )
                        queue.add(deleteSceneRequest)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
        }
        return super.onContextItemSelected(item)

    }

    override fun onStart() {
        super.onStart()
        canReceiveRequest = true
    }

    override fun onStop() {
        super.onStop()
        canReceiveRequest = false
    }
}
