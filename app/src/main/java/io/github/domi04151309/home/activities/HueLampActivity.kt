package io.github.domi04151309.home.activities

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.HueDetailsTabAdapter
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.UpdateHandler
import io.github.domi04151309.home.helpers.HueAPI
import io.github.domi04151309.home.helpers.HueUtils
import io.github.domi04151309.home.helpers.Global.volleyError
import io.github.domi04151309.home.helpers.Theme
import org.json.JSONArray

class HueLampActivity : AppCompatActivity() {

    var address: String = ""
    var deviceId: String = ""
    var id: String = ""
    var lights: JSONArray? = null
    internal var canReceiveRequest: Boolean = false
    private var isRoom: Boolean = false
    private var lightDataRequest: JsonObjectRequest? = null
    private var roomDataRequest: JsonObjectRequest? = null
    internal lateinit var hueAPI: HueAPI
    private lateinit var queue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hue_lamp)

        val internId = intent.getStringExtra("ID") ?: "0"
        if (internId.startsWith("room#") || internId.startsWith("zone#")) {
            id = internId.substring(internId.lastIndexOf("#") + 1)
            isRoom = true
        } else {
            id = internId
            isRoom = false
        }
        deviceId = intent.getStringExtra("Device") ?: ""
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
        val tabBar = findViewById<TabLayout>(R.id.tabBar)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

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
                    { response ->
                        lights = response.getJSONArray("lights")
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
                    { error ->
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

            viewPager.adapter = HueDetailsTabAdapter(this)

            val tabIcons = arrayOf(
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_hue_scene_add, theme),
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_device_lamp, theme)
            )
            TabLayoutMediator(tabBar, viewPager) { tab, position ->
                tab.icon = tabIcons[position]
            }.attach()
        }

        // Selected item is a single light
        else {
            lightDataRequest = JsonObjectRequest(Request.Method.GET,  address + "api/" + hueAPI.getUsername() + "/lights/" + id, null,
                    { response ->
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
                    { error ->
                        finish()
                        Toast.makeText(this, volleyError(this, error), Toast.LENGTH_LONG).show()
                    }
            )

            tabBar.visibility = View.GONE
            viewPager.visibility = View.GONE

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

        val updateHandler = UpdateHandler()
        updateHandler.setUpdateFunction {
            if (canReceiveRequest && hueAPI.readyForRequest) {
                if(isRoom) queue.add(roomDataRequest)
                else queue.add(lightDataRequest)
            }
        }
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
