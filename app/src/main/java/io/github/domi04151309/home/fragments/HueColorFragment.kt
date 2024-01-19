package io.github.domi04151309.home.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.slider.Slider
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorListener
import io.github.domi04151309.home.R
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.data.LightStates
import io.github.domi04151309.home.helpers.HueUtils
import io.github.domi04151309.home.helpers.HueUtils.MIN_COLOR_TEMPERATURE
import io.github.domi04151309.home.helpers.SliderUtils
import io.github.domi04151309.home.interfaces.HueRoomInterface

class HueColorFragment : Fragment(R.layout.fragment_hue_color) {
    private lateinit var lampInterface: HueRoomInterface
    private lateinit var hueAPI: HueAPI
    private lateinit var colorPickerView: ColorPickerView
    private lateinit var ctText: TextView
    private lateinit var ctBar: Slider
    private lateinit var hueSatText: TextView
    private lateinit var hueBar: Slider
    private lateinit var satBar: Slider

    private class OnSliderTouchListener(
        private val fragment: HueColorFragment,
        private val action: (slider: Slider) -> Unit,
    ) : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) {
            fragment.pauseUpdates()
        }

        override fun onStopTrackingTouch(slider: Slider) {
            action(slider)
            fragment.resumeUpdates()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        lampInterface = context as HueRoomInterface
        hueAPI = HueAPI(requireContext(), lampInterface.device.id)

        val view =
            super.onCreateView(inflater, container, savedInstanceState)
                ?: error("View does not exist yet.")
        colorPickerView = view.findViewById(R.id.colorPickerView)
        ctText = view.findViewById(R.id.ctTxt)
        ctBar = view.findViewById(R.id.ctBar)
        hueSatText = view.findViewById(R.id.hueSatTxt)
        hueBar = view.findViewById(R.id.hueBar)
        satBar = view.findViewById(R.id.satBar)

        val availableInputs = arrayOf(colorPickerView, ctBar, hueBar, satBar)
        val ctViews = arrayOf(ctText, ctBar)
        val hueSatViews = arrayOf(colorPickerView, hueSatText, hueBar, satBar)

        setupColorControls(hueAPI)
        setupTemperatureControls(hueAPI)

        fun updateFunction(data: LightStates.Light) {
            if (lampInterface.canReceiveRequest) {
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
            view.postDelayed({
                colorPickerView.selectByHsvColor(
                    HueUtils.hueSatToRGB(
                        lampInterface.lampData.state.hue,
                        lampInterface.lampData.state.sat,
                    ),
                )
            }, LOADING_DELAY)
            updateFunction(lampInterface.lampData.state)
            lampInterface.lampData.addOnDataChangedListener(::updateFunction)
        }

        return view
    }

    internal fun pauseUpdates() {
        lampInterface.canReceiveRequest = false
    }

    internal fun resumeUpdates() {
        Handler(Looper.getMainLooper()).postDelayed({
            lampInterface.canReceiveRequest = true
        }, UPDATE_DELAY)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupColorPicker(hueAPI: HueAPI) {
        colorPickerView.setColorListener(
            ColorListener { color, fromUser ->
                if (fromUser) {
                    val hueSat = HueUtils.rgbToHueSat(color)
                    hueBar.value = hueSat[0].toFloat()
                    satBar.value = hueSat[1].toFloat()
                    lampInterface.onColorChanged(color)
                }
            },
        )
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
    }

    private fun setupColorControls(hueAPI: HueAPI) {
        hueBar.setLabelFormatter { value: Float ->
            HueUtils.hueToDegree(value.toInt())
        }
        satBar.setLabelFormatter { value: Float ->
            HueUtils.satToPercent(value.toInt())
        }

        SliderUtils.setSliderGradient(
            hueBar,
            HueUtils.defaultColors(),
        )
        SliderUtils.setSliderGradient(
            satBar,
            intArrayOf(
                Color.WHITE,
                Color.RED,
            ),
        )

        hueBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val color = HueUtils.hueSatToRGB(value.toInt(), satBar.value.toInt())
                colorPickerView.selectByHsvColor(color)
                lampInterface.onColorChanged(color)
            }
            SliderUtils.setSliderGradientNow(
                satBar,
                intArrayOf(
                    Color.WHITE,
                    HueUtils.hueToRGB(value.toInt()),
                ),
            )
        }
        hueBar.addOnSliderTouchListener(
            OnSliderTouchListener(this) { slider ->
                hueAPI.changeHueOfGroup(lampInterface.id, slider.value.toInt())
            },
        )

        satBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val color = HueUtils.hueSatToRGB(hueBar.value.toInt(), value.toInt())
                colorPickerView.selectByHsvColor(color)
                lampInterface.onColorChanged(color)
            }
        }
        satBar.addOnSliderTouchListener(
            OnSliderTouchListener(this) { slider ->
                hueAPI.changeSaturationOfGroup(lampInterface.id, slider.value.toInt())
            },
        )

        setupColorPicker(hueAPI)
    }

    private fun setupTemperatureControls(hueAPI: HueAPI) {
        ctBar.setLabelFormatter { value: Float ->
            HueUtils.ctToKelvin(value.toInt() + MIN_COLOR_TEMPERATURE)
        }

        SliderUtils.setSliderGradient(
            ctBar,
            intArrayOf(
                Color.WHITE,
                Color.parseColor("#FF8B16"),
            ),
        )

        ctBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                lampInterface.onColorChanged(HueUtils.ctToRGB(value.toInt() + MIN_COLOR_TEMPERATURE))
            }
        }
        ctBar.addOnSliderTouchListener(
            OnSliderTouchListener(this) { slider ->
                hueAPI.changeColorTemperatureOfGroup(lampInterface.id, slider.value.toInt() + MIN_COLOR_TEMPERATURE)
            },
        )
    }

    companion object {
        private const val LOADING_DELAY = 200L
        private const val UPDATE_DELAY = 5000L
    }
}
