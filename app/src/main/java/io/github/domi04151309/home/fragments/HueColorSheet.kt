package io.github.domi04151309.home.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
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
import io.github.domi04151309.home.helpers.SliderUtils
import io.github.domi04151309.home.interfaces.HueLampInterface
import java.lang.Exception

class HueColorSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "HueColorSheet"
    }

    //TODO: make work in edit scenes
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val c = context ?: throw IllegalStateException()
        val lampInterface = context as HueLampInterface
        val hueAPI = HueAPI(c, lampInterface.device.id)

        val view = inflater.inflate(R.layout.fragment_hue_bri_color, container, false)
        val colorPickerView = view.findViewById<ColorPickerView>(R.id.colorPickerView)
        val ctText = view.findViewById<TextView>(R.id.ctTxt)
        val ctBar = view.findViewById<Slider>(R.id.ctBar)
        val hueSatText = view.findViewById<TextView>(R.id.hueSatTxt)
        val hueBar = view.findViewById<Slider>(R.id.hueBar)
        val satBar = view.findViewById<Slider>(R.id.satBar)
        val briText = view.findViewById<TextView>(R.id.briTxt)
        val briBar = view.findViewById<Slider>(R.id.briBar)

        val availableInputs = arrayOf<View>(colorPickerView, hueBar, satBar, ctBar, briBar)
        val ctViews = arrayOf<View>(ctText, ctBar)
        val hueSatViews = arrayOf<View>(colorPickerView, hueSatText, hueBar, satBar)
        val briViews = arrayOf<View>(briText, briBar)

        //Load colors
        Volley.newRequestQueue(c)
            .add(JsonObjectRequest(Request.Method.GET,
                "${lampInterface.addressPrefix}/lights/${lampInterface.id}",
                null,
                { response ->
                    val state = response.getJSONObject("state")

                    if (!state.has("ct")) {
                        ctViews.forEach {
                            it.visibility = View.GONE
                        }
                    } else {
                        ctViews.forEach {
                            it.visibility = View.VISIBLE
                        }
                        SliderUtils.setProgress(ctBar, state.getInt("ct") - 153)
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
                                state.getInt("sat")
                            )
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
                },
                { error ->
                    Toast.makeText(c, Global.volleyError(c, error), Toast.LENGTH_LONG).show()
                }
            ))

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
        briBar.setLabelFormatter { value: Float ->
            HueUtils.briToPercent(value.toInt())
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
                hueAPI.changeColorTemperature(lampInterface.id, value.toInt() + 153)
                lampInterface.onColorChanged(HueUtils.ctToRGB(value.toInt() + 153))
            }
        }

        hueBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val color = HueUtils.hueSatToRGB(value.toInt(), satBar.value.toInt())
                hueAPI.changeHue(lampInterface.id, value.toInt())
                colorPickerView.selectByHsvColor(color)
                lampInterface.onColorChanged(color)
            }
            SliderUtils.setSliderGradientNow(
                satBar, intArrayOf(
                    Color.WHITE,
                    HueUtils.hueToRGB(
                        value.toInt()
                    )
                )
            )
        }

        satBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val color = HueUtils.hueSatToRGB(hueBar.value.toInt(), value.toInt())
                hueAPI.changeSaturation(lampInterface.id, value.toInt())
                colorPickerView.selectByHsvColor(color)
                lampInterface.onColorChanged(color)
            }
        }

        colorPickerView.setColorListener(ColorListener { color, fromUser ->
            if (fromUser) {
                val hueSat = HueUtils.rgbToHueSat(color)
                hueAPI.changeHueSat(lampInterface.id, hueSat[0], hueSat[1])
                hueBar.value = hueSat[0].toFloat()
                satBar.value = hueSat[1].toFloat()
                lampInterface.onColorChanged(color)
                lampInterface.onColorChanged(color)
            }
        })

        briBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) hueAPI.changeBrightness(lampInterface.id, value.toInt())
            lampInterface.onBrightnessChanged(HueUtils.briToPercent(value.toInt()))
        }

        return view
    }
}