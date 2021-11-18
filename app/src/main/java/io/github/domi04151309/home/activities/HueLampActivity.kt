package io.github.domi04151309.home.activities

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.HueDetailsTabAdapter
import io.github.domi04151309.home.helpers.*
import io.github.domi04151309.home.helpers.Global.volleyError
import io.github.domi04151309.home.helpers.Theme
import org.json.JSONArray
import android.graphics.Shader.TileMode
import android.graphics.LinearGradient
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.PaintDrawable
import android.view.ViewTreeObserver.OnGlobalLayoutListener

class HueLampActivity : AppCompatActivity() {

    var address: String = ""
    var deviceId: String = ""
    var id: String = ""
    var lights: JSONArray? = null
    var canReceiveRequest: Boolean = false
    private var isRoom: Boolean = false
    private var lightDataRequest: JsonObjectRequest? = null
    private var roomDataRequest: JsonObjectRequest? = null
    internal lateinit var hueAPI: HueAPI
    private lateinit var queue: RequestQueue

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    internal fun setSliderGradientNow(view: View, colors: IntArray) {
        val gradient = PaintDrawable()
        gradient.setCornerRadius(dpToPx(16).toFloat())
        gradient.paint.shader = LinearGradient(
            0f, 0f,
            view.width.toFloat(), 0f,
            colors,
            null,
            TileMode.CLAMP
        )

        val layers = LayerDrawable(arrayOf(gradient))
        layers.setLayerInset(
            0,
            dpToPx(14),
            dpToPx(22),
            dpToPx(14),
            dpToPx(22)
        )
        view.background = layers
    }

    private fun setSliderGradient(view: View, colors: IntArray) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                setSliderGradientNow(view, colors)
            }
        })
    }

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
        val briBar = findViewById<Slider>(R.id.briBar)
        val ctBar = findViewById<Slider>(R.id.ctBar)
        val hueBar = findViewById<Slider>(R.id.hueBar)
        val satBar = findViewById<Slider>(R.id.satBar)
        val tabBar = findViewById<TabLayout>(R.id.tabBar)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val availableSliders = arrayOf(briBar, ctBar, hueBar, satBar)

        //Slider labels
        briBar.setLabelFormatter { value: Float ->
            HueUtils.briToPercent(value.toInt())
        }
        ctBar.setLabelFormatter { value: Float ->
            HueUtils.ctToKelvin(value.toInt() + 153)
        }
        hueBar.setLabelFormatter { value: Float ->
            HueUtils.hueToDegree(value.toInt())
        }
        satBar.setLabelFormatter { value: Float ->
            HueUtils.satToPercent(value.toInt())
        }

        //Slider tints
        setSliderGradient(ctBar, intArrayOf(
            Color.WHITE,
            Color.parseColor("#FF8B16")
        ))
        setSliderGradient(hueBar, intArrayOf(
            Color.HSVToColor(floatArrayOf(0f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(60f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(120f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(180f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(240f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(300f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(360f, 1f, 1f))
        ))
        setSliderGradient(satBar, intArrayOf(
            Color.WHITE,
            Color.RED
        ))

        //Default tint
        ImageViewCompat.setImageTintList(
            lampIcon,
            ColorStateList.valueOf(Color.WHITE)
        )

        //Smooth seekBars
        fun setProgress(slider: Slider, value: Int) {
            val animation = ObjectAnimator.ofFloat(slider, "value", value.toFloat())
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

                        val isOn = response.getJSONObject("state").getBoolean("any_on")
                        availableSliders.forEach {
                            it.isEnabled = isOn
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

            briBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(slider: Slider) {
                    hueAPI.changeBrightnessOfGroup(id, slider.value.toInt())
                    canReceiveRequest = true
                }
            })

            ctBar.addOnChangeListener { _, value, _ ->
                ImageViewCompat.setImageTintList(
                    lampIcon,
                    ColorStateList.valueOf(HueUtils.ctToRGB(value.toInt() + 153))
                )
            }
            ctBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(slider: Slider) {
                    hueAPI.changeColorTemperatureOfGroup(id, slider.value.toInt() + 153)
                    canReceiveRequest = true
                }
            })

            hueBar.addOnChangeListener { _, value, _ ->
                ImageViewCompat.setImageTintList(
                    lampIcon,
                    ColorStateList.valueOf(HueUtils.hueSatToRGB(value.toInt(), satBar.value.toInt()))
                )
                setSliderGradientNow(satBar, intArrayOf(
                    Color.WHITE,
                    HueUtils.hueToRGB(value.toInt())
                ))
            }
            hueBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(slider: Slider) {
                    hueAPI.changeHueOfGroup(id, slider.value.toInt())
                    canReceiveRequest = true
                }
            })

            satBar.addOnChangeListener { _, value, _ ->
                ImageViewCompat.setImageTintList(
                    lampIcon,
                    ColorStateList.valueOf(HueUtils.hueSatToRGB(hueBar.value.toInt(), value.toInt()))
                )
            }
            satBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(slider: Slider) {
                    hueAPI.changeSaturationOfGroup(id, slider.value.toInt())
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

                        val isOn = state.getBoolean("on")
                        availableSliders.forEach {
                            it.isEnabled = isOn
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

            briBar.addOnChangeListener { _, value, fromUser ->
                if (fromUser) hueAPI.changeBrightness(id, value.toInt())
            }
            briBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(slider: Slider) {
                    canReceiveRequest = true
                }
            })

            ctBar.addOnChangeListener { _, value, fromUser ->
                if (fromUser) hueAPI.changeColorTemperature(id, value.toInt() + 153)
                ImageViewCompat.setImageTintList(
                    lampIcon,
                    ColorStateList.valueOf(HueUtils.ctToRGB(value.toInt() + 153))
                )
            }
            ctBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(slider: Slider) {
                    canReceiveRequest = true
                }
            })

            hueBar.addOnChangeListener { _, value, fromUser ->
                if (fromUser) hueAPI.changeHue(id, value.toInt())
                ImageViewCompat.setImageTintList(
                    lampIcon,
                    ColorStateList.valueOf(HueUtils.hueSatToRGB(value.toInt(), satBar.value.toInt()))
                )
                setSliderGradientNow(satBar, intArrayOf(
                    Color.WHITE,
                    HueUtils.hueToRGB(value.toInt())
                ))
            }
            hueBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(slider: Slider) {
                    canReceiveRequest = true
                }
            })

            satBar.addOnChangeListener { _, value, fromUser ->
                if (fromUser) hueAPI.changeSaturation(id, value.toInt())
                ImageViewCompat.setImageTintList(
                    lampIcon,
                    ColorStateList.valueOf(HueUtils.hueSatToRGB(hueBar.value.toInt(), value.toInt()))
                )
            }
            satBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    canReceiveRequest = false
                }
                override fun onStopTrackingTouch(slider: Slider) {
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
