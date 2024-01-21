package io.github.domi04151309.home.activities

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
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayoutMediator
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.HueDetailsTabAdapter
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.LightStates
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.HueLightListener
import io.github.domi04151309.home.helpers.HueUtils
import io.github.domi04151309.home.helpers.HueUtils.MIN_COLOR_TEMPERATURE
import io.github.domi04151309.home.helpers.SliderUtils
import io.github.domi04151309.home.helpers.UpdateHandler
import io.github.domi04151309.home.interfaces.HueRoomInterface
import org.json.JSONArray

class HueLampActivity : BaseActivity(), HueRoomInterface {
    override var addressPrefix: String = ""
    override var id: String = ""
    override var lights: JSONArray? = null
    override var canReceiveRequest: Boolean = false
    override var lampData: HueLightListener = HueLightListener()
    override lateinit var device: DeviceItem

    private var lampName: String = ""
    private var updateDataRequest: JsonObjectRequest? = null
    private var updateHandler: UpdateHandler = UpdateHandler()

    private lateinit var hueAPI: HueAPI
    private lateinit var queue: RequestQueue
    private lateinit var lampIcon: ImageView
    private lateinit var nameText: TextView
    private lateinit var brightnessText: TextView
    private lateinit var brightnessBar: Slider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hue_lamp)

        id = intent.getStringExtra("id") ?: "0"
        if (intent.hasExtra(Devices.INTENT_EXTRA_DEVICE)) {
            val extraDevice = intent.getStringExtra(Devices.INTENT_EXTRA_DEVICE) ?: ""
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
        nameText = findViewById(R.id.nameTxt)
        brightnessText = findViewById(R.id.briTxt)
        brightnessBar = findViewById(R.id.briBar)

        setupViews()

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.isUserInputEnabled = false
        viewPager.adapter = HueDetailsTabAdapter(this)
        viewPager.setCurrentItem(1, false)

        val tabIcons =
            arrayOf(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_color_palette, theme),
                ResourcesCompat.getDrawable(resources, R.drawable.ic_scene_white, theme),
                ResourcesCompat.getDrawable(resources, R.drawable.ic_device_lamp, theme),
            )
        TabLayoutMediator(findViewById(R.id.tabBar), viewPager) { tab, position ->
            tab.icon = tabIcons[position]
        }.attach()

        updateDataRequest = getUpdateRequest()
        updateHandler.setUpdateFunction {
            if (canReceiveRequest && hueAPI.readyForRequest) {
                queue.add(updateDataRequest)
            }
        }
    }

    private fun setupViews() {
        // Slider labels
        brightnessBar.setLabelFormatter { value: Float ->
            HueUtils.briToPercent(value.toInt())
        }

        // Lamp tint
        ImageViewCompat.setImageTintList(
            lampIcon,
            ColorStateList.valueOf(Color.WHITE),
        )
        lampData.addOnDataChangedListener {
            ImageViewCompat.setImageTintList(
                lampIcon,
                ColorStateList.valueOf(
                    if (it.hue != -1 && it.sat != -1) {
                        HueUtils.hueSatToRGB(it.hue, it.sat)
                    } else if (it.ct != -1) {
                        HueUtils.ctToRGB(it.ct + MIN_COLOR_TEMPERATURE)
                    } else {
                        Color.WHITE
                    },
                ),
            )
        }

        findViewById<Button>(R.id.onBtn).setOnClickListener {
            hueAPI.switchGroupById(id, true)
        }

        findViewById<Button>(R.id.offBtn).setOnClickListener {
            hueAPI.switchGroupById(id, false)
        }

        brightnessBar.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    canReceiveRequest = false
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    hueAPI.changeBrightnessOfGroup(id, slider.value.toInt())
                    canReceiveRequest = true
                }
            },
        )
    }

    private fun getUpdateRequest() =
        JsonObjectRequest(
            Request.Method.GET,
            "$addressPrefix/groups/$id",
            null,
            { response ->
                lights = response.getJSONArray("lights")
                lampName = response.getString("name")
                nameText.text = lampName
                val action = response.getJSONObject("action")
                val light = LightStates.Light()

                if (action.has("bri")) {
                    SliderUtils.setProgress(brightnessBar, action.getInt("bri"))
                } else {
                    brightnessText.visibility = View.GONE
                    brightnessBar.visibility = View.GONE
                }
                light.ct =
                    if (action.has("ct")) {
                        action.getInt("ct") - MIN_COLOR_TEMPERATURE
                    } else {
                        -1
                    }

                if (action.has("hue") && action.has("sat")) {
                    light.hue = action.getInt("hue")
                    light.sat = action.getInt("sat")
                } else {
                    light.hue = -1
                    light.sat = -1
                }

                light.on = response.getJSONObject("state").getBoolean("any_on")
                brightnessBar.isEnabled = light.on

                lampData.state = light
            },
            {
                canReceiveRequest = false
                updateHandler.stop()
                finish()
            },
        )

    override fun onStart() {
        super.onStart()
        canReceiveRequest = true
    }

    override fun onStop() {
        super.onStop()
        canReceiveRequest = false
    }

    override fun onDestroy() {
        super.onDestroy()
        updateHandler.stop()
    }

    override fun onColorChanged(color: Int) {
        ImageViewCompat.setImageTintList(
            lampIcon,
            ColorStateList.valueOf(color),
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_hue_lamp_actions, menu)
        return true
    }

    @Suppress("ReturnCount")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.action_add_shortcut) return super.onOptionsItemSelected(item)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = getSystemService(ShortcutManager::class.java) ?: return true
            if (!shortcutManager.isRequestPinShortcutSupported) {
                Toast.makeText(this, R.string.pref_add_shortcut_failed, Toast.LENGTH_LONG).show()
                return true
            }
            val shortcut =
                ShortcutInfo.Builder(this, device.id + lampName)
                    .setShortLabel(lampName)
                    .setLongLabel(lampName)
                    .setIcon(Icon.createWithResource(this, device.iconId))
                    .setIntent(
                        Intent(this, HueLampActivity::class.java)
                            .putExtra("id", id)
                            .putExtra(Devices.INTENT_EXTRA_DEVICE, device.id)
                            .setAction(Intent.ACTION_MAIN)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    )
                    .build()
            shortcutManager.requestPinShortcut(shortcut, null)
        } else {
            Toast.makeText(this, R.string.pref_add_shortcut_failed, Toast.LENGTH_LONG).show()
        }
        return true
    }
}
