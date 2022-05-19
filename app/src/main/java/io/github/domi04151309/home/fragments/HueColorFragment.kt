package io.github.domi04151309.home.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.slider.Slider
import com.skydoves.colorpickerview.ColorPickerView
import io.github.domi04151309.home.R
import com.skydoves.colorpickerview.listeners.ColorListener
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.helpers.*
import io.github.domi04151309.home.interfaces.HueRoomInterface

class HueColorFragment : Fragment(R.layout.fragment_hue_color) {

    companion object {
        private const val UPDATE_DELAY = 5000L
    }

    private var shouldBeUpdated = true
    private lateinit var c: Context
    private lateinit var lampInterface: HueRoomInterface
    private lateinit var lampData: HueLampData
    private lateinit var hueAPI: HueAPI

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        c = context ?: throw IllegalStateException()
        lampInterface = context as HueRoomInterface
        lampData = lampInterface.lampData
        hueAPI = HueAPI(c, lampInterface.device.id)

        val view = super.onCreateView(inflater, container, savedInstanceState) ?: throw IllegalStateException()
        val colorPickerView = view.findViewById<ColorPickerView>(R.id.colorPickerView)
        val ctText = view.findViewById<TextView>(R.id.ctTxt)
        val ctBar = view.findViewById<Slider>(R.id.ctBar)
        val hueSatText = view.findViewById<TextView>(R.id.hueSatTxt)
        val hueBar = view.findViewById<Slider>(R.id.hueBar)
        val satBar = view.findViewById<Slider>(R.id.satBar)

        val availableInputs = arrayOf<View>(colorPickerView, ctBar, hueBar, satBar)
        val ctViews = arrayOf<View>(ctText, ctBar)
        val hueSatViews = arrayOf<View>(colorPickerView, hueSatText, hueBar, satBar)

        //Slider labels
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
        SliderUtils.setSliderGradient(
            ctBar, intArrayOf(
                Color.WHITE,
                Color.parseColor("#FF8B16")
            )
        )
        SliderUtils.setSliderGradient(
            hueBar, intArrayOf(
                Color.HSVToColor(floatArrayOf(0f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(60f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(120f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(180f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(240f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(300f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(360f, 1f, 1f))
            )
        )
        SliderUtils.setSliderGradient(
            satBar, intArrayOf(
                Color.WHITE,
                Color.RED
            )
        )

        ctBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                lampInterface.onColorChanged(HueUtils.ctToRGB(value.toInt() + 153))
            }
        }
        ctBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                pauseUpdates()
            }
            override fun onStopTrackingTouch(slider: Slider) {
                hueAPI.changeColorTemperatureOfGroup(lampInterface.id, slider.value.toInt() + 153)
                resumeUpdates()
            }
        })

        hueBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val color = HueUtils.hueSatToRGB(value.toInt(), satBar.value.toInt())
                colorPickerView.selectByHsvColor(color)
                lampInterface.onColorChanged(color)
            }
            SliderUtils.setSliderGradientNow(
                satBar, intArrayOf(
                    Color.WHITE,
                    HueUtils.hueToRGB(value.toInt())
                )
            )
        }
        hueBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                pauseUpdates()
            }
            override fun onStopTrackingTouch(slider: Slider) {
                hueAPI.changeHueOfGroup(lampInterface.id, slider.value.toInt())
                resumeUpdates()
            }
        })

        satBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val color = HueUtils.hueSatToRGB(hueBar.value.toInt(), value.toInt())
                colorPickerView.selectByHsvColor(color)
                lampInterface.onColorChanged(color)
            }
        }
        satBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                pauseUpdates()
            }
            override fun onStopTrackingTouch(slider: Slider) {
                hueAPI.changeSaturationOfGroup(lampInterface.id, slider.value.toInt())
                resumeUpdates()
            }
        })

        colorPickerView.setColorListener(ColorListener { color, fromUser ->
            if (fromUser) {
                val hueSat = HueUtils.rgbToHueSat(color)
                hueBar.value = hueSat[0].toFloat()
                satBar.value = hueSat[1].toFloat()
                lampInterface.onColorChanged(color)
            }
        })
        colorPickerView.setOnTouchListener { innerView, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                pauseUpdates()
            } else if (event.action == MotionEvent.ACTION_UP) {
                val hueSat = HueUtils.rgbToHueSat(colorPickerView.color)
                hueAPI.changeHueSatOfGroup(lampInterface.id, hueSat[0], hueSat[1])
                resumeUpdates()
            }
            innerView.performClick()
        }

        fun updateFunction(data: HueLampData) {
            if (shouldBeUpdated) {
                if (data.ct == -1) {
                    ctViews.forEach {
                        it.visibility = View.GONE
                    }
                } else {
                    ctViews.forEach {
                        it.visibility = View.VISIBLE
                    }
                    SliderUtils.setProgress(ctBar, data.ct)
                }
                if (data.hue == -1 || data.sat == -1) {
                    hueSatViews.forEach {
                        it.visibility = View.GONE
                    }
                } else {
                    hueSatViews.forEach {
                        it.visibility = View.VISIBLE
                    }
                    colorPickerView.selectByHsvColor(HueUtils.hueSatToRGB(data.hue, data.sat))
                    SliderUtils.setProgress(hueBar, data.hue)
                    SliderUtils.setProgress(satBar, data.sat)
                }
                availableInputs.forEach {
                    it.isEnabled = data.on
                }
            }
        }

        view.post {
            updateFunction(lampData)
            lampData.addOnDataChangedListener(::updateFunction)
        }

        return view
    }

    internal fun pauseUpdates() {
        shouldBeUpdated = false
        lampInterface.canReceiveRequest = false
    }

    internal fun resumeUpdates() {
        lampInterface.canReceiveRequest = true
        Handler(Looper.getMainLooper()).postDelayed({
            shouldBeUpdated = true
        }, UPDATE_DELAY)
    }
}