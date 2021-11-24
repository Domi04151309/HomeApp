package io.github.domi04151309.home.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.android.material.slider.Slider
import com.skydoves.colorpickerview.ColorPickerView
import io.github.domi04151309.home.R
import io.github.domi04151309.home.activities.HueLampActivity
import io.github.domi04151309.home.helpers.HueAPI
import io.github.domi04151309.home.helpers.HueUtils
import io.github.domi04151309.home.helpers.UpdateHandler
import com.skydoves.colorpickerview.listeners.ColorListener
import io.github.domi04151309.home.helpers.Global

class HueColorFragment : Fragment(R.layout.fragment_hue_color) {
    //TODO: fix swiping not working
    //TODO: test for single light control

    companion object {
        private const val UPDATE_DELAY = 1000L
    }

    private var shouldBeUpdated = true
    private lateinit var c: Context
    private lateinit var lampData: HueLampActivity
    private lateinit var hueAPI: HueAPI
    private lateinit var queue: RequestQueue
    private lateinit var colorPickerView: ColorPickerView
    private lateinit var ctText: TextView
    private lateinit var ctBar: Slider
    private lateinit var hueText: TextView
    private lateinit var hueBar: Slider
    private lateinit var satText: TextView
    private lateinit var satBar: Slider
    private lateinit var availableSliders: Array<Slider>
    private lateinit var ctViews: Array<View>
    private lateinit var hueSatViews: Array<View>
    private val updateHandler: UpdateHandler = UpdateHandler()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        c = context ?: throw IllegalStateException()
        lampData = context as HueLampActivity
        hueAPI = HueAPI(c, lampData.deviceId)
        queue = Volley.newRequestQueue(context)

        val view = super.onCreateView(inflater, container, savedInstanceState) ?: throw IllegalStateException()
        colorPickerView = view.findViewById(R.id.colorPickerView)
        ctText = view.findViewById(R.id.ctTxt)
        ctBar = view.findViewById(R.id.ctBar)
        hueText = view.findViewById(R.id.hueTxt)
        hueBar = view.findViewById(R.id.hueBar)
        satText = view.findViewById(R.id.satTxt)
        satBar = view.findViewById(R.id.satBar)

        availableSliders = arrayOf(ctBar, hueBar, satBar)
        ctViews = arrayOf(ctText, ctBar)
        hueSatViews = arrayOf(colorPickerView, hueText, hueBar, satText, satBar)

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
        HueLampActivity.setSliderGradient(
            resources, ctBar, intArrayOf(
                Color.WHITE,
                Color.parseColor("#FF8B16")
            )
        )
        HueLampActivity.setSliderGradient(
            resources, hueBar, intArrayOf(
                Color.HSVToColor(floatArrayOf(0f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(60f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(120f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(180f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(240f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(300f, 1f, 1f)),
                Color.HSVToColor(floatArrayOf(360f, 1f, 1f))
            )
        )
        HueLampActivity.setSliderGradient(
            resources, satBar, intArrayOf(
                Color.WHITE,
                Color.RED
            )
        )

        if (lampData.isRoom) {
            ctBar.addOnChangeListener { _, value, _ ->
                ImageViewCompat.setImageTintList(
                    lampData.lampIcon,
                    ColorStateList.valueOf(HueUtils.ctToRGB(value.toInt() + 153))
                )
            }
            ctBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    pauseUpdates()
                }
                override fun onStopTrackingTouch(slider: Slider) {
                    hueAPI.changeColorTemperatureOfGroup(lampData.id, slider.value.toInt() + 153)
                    resumeUpdates()
                }
            })

            hueBar.addOnChangeListener { _, value, _ ->
                val color = HueUtils.hueSatToRGB(value.toInt(), satBar.value.toInt())
                ImageViewCompat.setImageTintList(
                    lampData.lampIcon,
                    ColorStateList.valueOf(color)
                )
                HueLampActivity.setSliderGradientNow(
                    resources, satBar, intArrayOf(
                        Color.WHITE,
                        HueUtils.hueToRGB(value.toInt())
                    )
                )
                colorPickerView.selectByHsvColor(color)
            }
            hueBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    pauseUpdates()
                }
                override fun onStopTrackingTouch(slider: Slider) {
                    hueAPI.changeHueOfGroup(lampData.id, slider.value.toInt())
                    resumeUpdates()
                }
            })

            satBar.addOnChangeListener { _, value, _ ->
                val color = HueUtils.hueSatToRGB(hueBar.value.toInt(), value.toInt())
                ImageViewCompat.setImageTintList(
                    lampData.lampIcon,
                    ColorStateList.valueOf(color)
                )
                colorPickerView.selectByHsvColor(color)
            }
            satBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    pauseUpdates()
                }
                override fun onStopTrackingTouch(slider: Slider) {
                    hueAPI.changeSaturationOfGroup(lampData.id, slider.value.toInt())
                    resumeUpdates()
                }
            })

            colorPickerView.setColorListener(ColorListener { color, fromUser ->
                if (fromUser) ImageViewCompat.setImageTintList(
                    lampData.lampIcon,
                    ColorStateList.valueOf(color)
                )
            })
            colorPickerView.setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    pauseUpdates()
                } else if (event.action == MotionEvent.ACTION_UP) {
                    val hueSat = HueUtils.rgbToHueSat(colorPickerView.color)
                    hueAPI.changeHueSatOfGroup(lampData.id, hueSat[0], hueSat[1])
                    resumeUpdates()
                }
                view.performClick()
            }
        } else {
            ctBar.addOnChangeListener { _, value, fromUser ->
                if (fromUser) hueAPI.changeColorTemperature(lampData.id, value.toInt() + 153)
                ImageViewCompat.setImageTintList(
                    lampData.lampIcon,
                    ColorStateList.valueOf(HueUtils.ctToRGB(value.toInt() + 153))
                )
            }
            ctBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    pauseUpdates()
                }
                override fun onStopTrackingTouch(slider: Slider) {
                    resumeUpdates()
                }
            })

            hueBar.addOnChangeListener { _, value, fromUser ->
                if (fromUser) hueAPI.changeHue(lampData.id, value.toInt())
                val color = HueUtils.hueSatToRGB(value.toInt(), satBar.value.toInt())
                ImageViewCompat.setImageTintList(
                    lampData.lampIcon,
                    ColorStateList.valueOf(color)
                )
                HueLampActivity.setSliderGradientNow(
                    resources, satBar, intArrayOf(
                        Color.WHITE,
                        HueUtils.hueToRGB(value.toInt()
                    )
                ))
                colorPickerView.selectByHsvColor(color)
            }
            hueBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    pauseUpdates()
                }
                override fun onStopTrackingTouch(slider: Slider) {
                    resumeUpdates()
                }
            })

            satBar.addOnChangeListener { _, value, fromUser ->
                if (fromUser) hueAPI.changeSaturation(lampData.id, value.toInt())
                val color = HueUtils.hueSatToRGB(hueBar.value.toInt(), value.toInt())
                ImageViewCompat.setImageTintList(
                    lampData.lampIcon,
                    ColorStateList.valueOf(color)
                )
                colorPickerView.selectByHsvColor(color)
            }
            satBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    pauseUpdates()
                }
                override fun onStopTrackingTouch(slider: Slider) {
                    resumeUpdates()
                }
            })

            colorPickerView.setColorListener(ColorListener { color, fromUser ->
                if (fromUser) {
                    val hueSat = HueUtils.rgbToHueSat(color)
                    hueAPI.changeHueSat(lampData.id, hueSat[0], hueSat[1])
                    ImageViewCompat.setImageTintList(
                        lampData.lampIcon,
                        ColorStateList.valueOf(color)
                    )
                }
            })
            colorPickerView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    pauseUpdates()
                } else if (event.action == MotionEvent.ACTION_UP) {
                    resumeUpdates()
                }
                view.performClick()
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        updateHandler.setUpdateFunction {
            if (shouldBeUpdated) {
                if (lampData.hueCt == -1) {
                    ctViews.forEach {
                        it.visibility = View.GONE
                    }
                } else {
                    ctViews.forEach {
                        it.visibility = View.VISIBLE
                    }
                    HueLampActivity.setProgress(ctBar, lampData.hueCt)
                }
                if (lampData.hueHue == -1 || lampData.hueSat == -1) {
                    hueSatViews.forEach {
                        it.visibility = View.GONE
                    }
                } else {
                    hueSatViews.forEach {
                        it.visibility = View.VISIBLE
                    }
                    colorPickerView.selectByHsvColor(HueUtils.hueSatToRGB(lampData.hueHue, lampData.hueSat))
                    HueLampActivity.setProgress(hueBar, lampData.hueHue)
                    HueLampActivity.setProgress(satBar, lampData.hueSat)
                }
                availableSliders.forEach {
                    it.isEnabled = lampData.hueOn
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        updateHandler.stop()
    }

    internal fun pauseUpdates() {
        shouldBeUpdated = false
        lampData.canReceiveRequest = false
    }

    internal fun resumeUpdates() {
        lampData.canReceiveRequest = true
        Handler(Looper.getMainLooper()).postDelayed({
            shouldBeUpdated = true
        }, UPDATE_DELAY)
    }
}