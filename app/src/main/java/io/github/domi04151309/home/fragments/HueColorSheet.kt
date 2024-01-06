package io.github.domi04151309.home.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorListener
import io.github.domi04151309.home.R
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.HueUtils
import io.github.domi04151309.home.helpers.HueUtils.MIN_COLOR_TEMPERATURE
import io.github.domi04151309.home.helpers.SliderUtils
import io.github.domi04151309.home.interfaces.HueAdvancedLampInterface
import org.json.JSONObject

class HueColorSheet(private val lampInterface: HueAdvancedLampInterface) :
    BottomSheetDialogFragment(),
    Response.Listener<JSONObject> {
    private lateinit var colorPickerView: ColorPickerView
    private lateinit var ctText: TextView
    private lateinit var ctBar: Slider
    private lateinit var hueSatText: TextView
    private lateinit var hueBar: Slider
    private lateinit var satBar: Slider
    private lateinit var briText: TextView
    private lateinit var briBar: Slider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val hueAPI = HueAPI(requireContext(), lampInterface.device.id)

        val view = inflater.inflate(R.layout.fragment_hue_bri_color, container, false)
        colorPickerView = view.findViewById(R.id.colorPickerView)
        ctText = view.findViewById(R.id.ctTxt)
        ctBar = view.findViewById(R.id.ctBar)
        hueSatText = view.findViewById(R.id.hueSatTxt)
        hueBar = view.findViewById(R.id.hueBar)
        satBar = view.findViewById(R.id.satBar)
        briText = view.findViewById(R.id.briTxt)
        briBar = view.findViewById(R.id.briBar)

        Volley.newRequestQueue(requireContext())
            .add(
                JsonObjectRequest(
                    Request.Method.GET,
                    "${lampInterface.addressPrefix}/lights/${lampInterface.id}",
                    null,
                    this,
                ) { error ->
                    Toast.makeText(
                        requireContext(),
                        Global.volleyError(requireContext(), error),
                        Toast.LENGTH_LONG,
                    ).show()
                },
            )

        setupColorControls(hueAPI)
        setupTemperatureControls(hueAPI)
        setupBrightnessControls(hueAPI)

        return view
    }

    override fun onResponse(response: JSONObject) {
        val availableInputs = arrayOf(colorPickerView, hueBar, satBar, ctBar, briBar)
        val ctViews = arrayOf(ctText, ctBar)
        val hueSatViews = arrayOf(colorPickerView, hueSatText, hueBar, satBar)
        val briViews = arrayOf(briText, briBar)
        val state = response.getJSONObject("state")

        if (!state.has("ct")) {
            ctViews.forEach {
                it.visibility = View.GONE
            }
        } else {
            ctViews.forEach {
                it.visibility = View.VISIBLE
            }
            SliderUtils.setProgress(ctBar, state.getInt("ct") - MIN_COLOR_TEMPERATURE)
        }
        if (!state.has("hue") && !state.has("sat")) {
            hueSatViews.forEach {
                it.visibility = View.GONE
            }
        } else {
            hueSatViews.forEach {
                it.visibility = View.VISIBLE
            }
            colorPickerView.selectByHsvColor(
                HueUtils.hueSatToRGB(
                    state.getInt("hue"),
                    state.getInt("sat"),
                ),
            )
            SliderUtils.setProgress(hueBar, state.getInt("hue"))
            SliderUtils.setProgress(satBar, state.getInt("sat"))
        }
        if (!state.has("bri")) {
            briViews.forEach {
                it.visibility = View.GONE
            }
        } else {
            briViews.forEach {
                it.visibility = View.VISIBLE
            }
            SliderUtils.setProgress(briBar, state.getInt("bri"))
        }
        availableInputs.forEach {
            it.isEnabled = state.optBoolean("on")
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
                hueAPI.changeHue(lampInterface.id, value.toInt())
                colorPickerView.selectByHsvColor(color)
                lampInterface.onColorChanged(color)
                lampInterface.onHueSatChanged(value.toInt(), satBar.value.toInt())
            }
            SliderUtils.setSliderGradientNow(
                satBar,
                intArrayOf(
                    Color.WHITE,
                    HueUtils.hueToRGB(
                        value.toInt(),
                    ),
                ),
            )
        }

        satBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val color = HueUtils.hueSatToRGB(hueBar.value.toInt(), value.toInt())
                hueAPI.changeSaturation(lampInterface.id, value.toInt())
                colorPickerView.selectByHsvColor(color)
                lampInterface.onColorChanged(color)
                lampInterface.onHueSatChanged(hueBar.value.toInt(), value.toInt())
            }
        }

        colorPickerView.setColorListener(
            ColorListener { color, fromUser ->
                if (fromUser) {
                    val hueSat = HueUtils.rgbToHueSat(color)
                    hueAPI.changeHueSat(lampInterface.id, hueSat[0], hueSat[1])
                    hueBar.value = hueSat[0].toFloat()
                    satBar.value = hueSat[1].toFloat()
                    lampInterface.onColorChanged(color)
                    lampInterface.onColorChanged(color)
                    lampInterface.onHueSatChanged(hueSat[0], hueSat[1])
                }
            },
        )
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
                hueAPI.changeColorTemperature(lampInterface.id, value.toInt() + MIN_COLOR_TEMPERATURE)
                lampInterface.onColorChanged(HueUtils.ctToRGB(value.toInt() + MIN_COLOR_TEMPERATURE))
                lampInterface.onCtChanged(value.toInt() + MIN_COLOR_TEMPERATURE)
            }
        }
    }

    private fun setupBrightnessControls(hueAPI: HueAPI) {
        briBar.setLabelFormatter { value: Float ->
            HueUtils.briToPercent(value.toInt())
        }

        briBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) hueAPI.changeBrightness(lampInterface.id, value.toInt())
            lampInterface.onBrightnessChanged(value.toInt())
        }
    }
}
