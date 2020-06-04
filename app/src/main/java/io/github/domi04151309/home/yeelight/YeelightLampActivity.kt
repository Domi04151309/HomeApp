package io.github.domi04151309.home.yeelight

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.core.graphics.drawable.DrawableCompat
import android.view.animation.DecelerateInterpolator
import android.animation.ObjectAnimator
import android.widget.SeekBar
import cyanogenmod.util.ColorUtils
import io.github.domi04151309.home.*
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.objects.Theme

class YeelightLampActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yeelight_lamp)

        val deviceId = intent.getStringExtra("Device") ?: ""
        val device = Devices(this).getDeviceById(deviceId)
        val yeelightAPI = YeelightAPI(this, deviceId)

        title = device.name
        val lampIcon = findViewById<ImageView>(R.id.lampIcon)
        val nameTxt = findViewById<TextView>(R.id.nameTxt)
        val briBar = findViewById<SeekBar>(R.id.briBar)
        val ctBar = findViewById<SeekBar>(R.id.ctBar)
        val hueBar = findViewById<SeekBar>(R.id.hueBar)
        val satBar = findViewById<SeekBar>(R.id.satBar)

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

        //TODO: Load state

        findViewById<Button>(R.id.onBtn).setOnClickListener {
            yeelightAPI.turnOnLight()
        }

        findViewById<Button>(R.id.offBtn).setOnClickListener {
            yeelightAPI.turnOffLight()
        }

        briBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                yeelightAPI.changeBrightness(seekBar.progress)
            }
        })

        ctBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                DrawableCompat.setTint(
                        DrawableCompat.wrap(lampIcon.drawable),
                        ColorUtils.temperatureToRGB(seekBar.progress + YeelightUtils.CT_SHIFT)
                )
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                yeelightAPI.changeColorTemperature(seekBar.progress)
            }
        })

        hueBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                DrawableCompat.setTint(
                        DrawableCompat.wrap(lampIcon.drawable),
                        YeelightUtils.hueSatToRGB(seekBar.progress, satBar.progress)
                )
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                yeelightAPI.changeHue(seekBar.progress)
            }
        })

        satBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                DrawableCompat.setTint(
                        DrawableCompat.wrap(lampIcon.drawable),
                        YeelightUtils.hueSatToRGB(hueBar.progress, seekBar.progress)
                )
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                yeelightAPI.changeSaturation(seekBar.progress)
            }
        })
    }
}
