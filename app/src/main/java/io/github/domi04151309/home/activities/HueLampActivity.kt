package io.github.domi04151309.home.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.helpers.*
import io.github.domi04151309.home.helpers.Global.volleyError
import io.github.domi04151309.home.helpers.Theme
import org.json.JSONArray

class HueLampActivity : AppCompatActivity() {

    companion object {
        fun setProgress(slider: Slider, value: Int) {
            val animation = ObjectAnimator.ofFloat(slider, "value", value.toFloat())
            animation.duration = 300
            animation.interpolator = DecelerateInterpolator()
            animation.start()
        }
    }

    var addressPrefix: String = ""
    var id: String = ""
    var lights: JSONArray? = null
    var canReceiveRequest: Boolean = false
    var lampData: HueLampData = HueLampData()
    var isRoom: Boolean = false
    private var internId: String = ""
    private var lampName: String = ""
    private var updateDataRequest: JsonObjectRequest? = null
    lateinit var device: DeviceItem
    lateinit var lampIcon: ImageView
    internal lateinit var hueAPI: HueAPI
    private lateinit var queue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hue_lamp)

        internId = intent.getStringExtra("id") ?: "0"
        if (internId.startsWith("room#") || internId.startsWith("zone#")) {
            id = internId.substring(internId.lastIndexOf('#') + 1)
            isRoom = true
        } else {
            id = internId
            isRoom = false
        }
        if (intent.hasExtra("device")) {
            val extraDevice = intent.getStringExtra("device") ?: ""
            val devices = Devices(this)
            if (devices.idExists(extraDevice)) {
                device = devices.getDeviceById(extraDevice)
            } else {
                Toast.makeText(this, R.string.main_device_nonexistent, Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }

        hueAPI = HueAPI(this, device.id)
        addressPrefix = device.address + "api/" + hueAPI.getUsername()
        queue = Volley.newRequestQueue(this)
        title = device.name
        lampIcon = findViewById(R.id.lampIcon)
        val nameTxt = findViewById<TextView>(R.id.nameTxt)
        val briBar = findViewById<Slider>(R.id.briBar)
        val tabBar = findViewById<TabLayout>(R.id.tabBar)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        viewPager.isUserInputEnabled = false
        viewPager.adapter = HueDetailsTabAdapter(this, isRoom)

        //Slider labels
        briBar.setLabelFormatter { value: Float ->
            HueUtils.briToPercent(value.toInt())
        }

        //Lamp tint
        ImageViewCompat.setImageTintList(
            lampIcon,
            ColorStateList.valueOf(Color.WHITE)
        )
        lampData.addOnDataChangedListener {
            ImageViewCompat.setImageTintList(
                lampIcon,
                ColorStateList.valueOf(
                    if (it.hue != -1 && it.sat != -1) HueUtils.hueSatToRGB(it.hue, it.sat)
                    else if (it.ct != -1) HueUtils.ctToRGB(it.ct + 153)
                    else Color.WHITE
                )
            )
        }

        // Selected item is a whole room
        if (isRoom) {
            updateDataRequest = JsonObjectRequest(Request.Method.GET, "$addressPrefix/groups/$id", null,
                    { response ->
                        lights = response.getJSONArray("lights")
                        lampName = response.getString("name")
                        nameTxt.text = lampName
                        val action = response.getJSONObject("action")

                        if (action.has("bri")) {
                            setProgress(briBar, action.getInt("bri"))
                        } else {
                            findViewById<TextView>(R.id.briTxt).visibility = View.GONE
                            briBar.visibility = View.GONE
                        }
                        lampData.ct =
                            if (action.has("ct")) action.getInt("ct") - 153
                            else -1

                        if (action.has("hue") && action.has("sat")) {
                            lampData.hue = action.getInt("hue")
                            lampData.sat = action.getInt("sat")
                        } else {
                            lampData.hue = -1
                            lampData.sat = -1
                        }

                        lampData.on = response.getJSONObject("state").getBoolean("any_on")
                        briBar.isEnabled = lampData.on

                        lampData.notifyDataChanged()
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

            viewPager.setCurrentItem(1, false)

            val tabIcons = arrayOf(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_color_palette, theme),
                ResourcesCompat.getDrawable(resources, R.drawable.ic_scene, theme),
                ResourcesCompat.getDrawable(resources, R.drawable.ic_device_lamp, theme)
            )
            TabLayoutMediator(tabBar, viewPager) { tab, position ->
                tab.icon = tabIcons[position]
            }.attach()
        }

        // Selected item is a single light
        else {
            updateDataRequest = JsonObjectRequest(Request.Method.GET, "$addressPrefix/lights/$id", null,
                    { response ->
                        lampName = response.getString("name")
                        nameTxt.text = lampName
                        val state = response.getJSONObject("state")

                        if (state.has("bri")) {
                            setProgress(briBar, state.getInt("bri"))
                        } else {
                            findViewById<TextView>(R.id.briTxt).visibility = View.GONE
                            briBar.visibility = View.GONE
                        }

                        lampData.ct =
                            if (state.has("ct")) state.getInt("ct") - 153
                            else -1

                        if (state.has("hue") && state.has("sat")) {
                            lampData.hue = state.getInt("hue")
                            lampData.sat = state.getInt("sat")
                        } else {
                            lampData.hue = -1
                            lampData.sat = -1
                        }

                        lampData.on = state.getBoolean("on")
                        briBar.isEnabled = lampData.on

                        lampData.notifyDataChanged()
                    },
                    { error ->
                        finish()
                        Toast.makeText(this, volleyError(this, error), Toast.LENGTH_LONG).show()
                    }
            )

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
        }

        val updateHandler = UpdateHandler()
        updateHandler.setUpdateFunction {
            if (canReceiveRequest && hueAPI.readyForRequest) {
                queue.add(updateDataRequest)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_hue_lamp_actions, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_add_shortcut) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val shortcutManager = this.getSystemService(ShortcutManager::class.java)
                if (shortcutManager != null) {
                    if (shortcutManager.isRequestPinShortcutSupported) {
                        val shortcut = ShortcutInfo.Builder(this, device.id + lampName)
                            .setShortLabel(lampName)
                            .setLongLabel(lampName)
                            .setIcon(Icon.createWithResource(this, device.iconId))
                            .setIntent(
                                Intent(this, HueLampActivity::class.java)
                                    .putExtra("id", internId)
                                    .putExtra("device", device.id)
                                    .setAction(Intent.ACTION_MAIN)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            )
                            .build()
                        shortcutManager.requestPinShortcut(shortcut, null)
                    } else
                        Toast.makeText(this, R.string.pref_add_shortcut_failed, Toast.LENGTH_LONG).show()
                }
            } else
                Toast.makeText(this, R.string.pref_add_shortcut_failed, Toast.LENGTH_LONG).show()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
