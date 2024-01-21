package io.github.domi04151309.home.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.HueLampListAdapter
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.helpers.HueUtils
import io.github.domi04151309.home.helpers.UpdateHandler
import io.github.domi04151309.home.interfaces.HueAdvancedLampInterface
import io.github.domi04151309.home.interfaces.HueRoomInterface
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface
import org.json.JSONArray
import org.json.JSONObject

class HueLampsFragment :
    Fragment(R.layout.fragment_hue_lamps),
    RecyclerViewHelperInterface,
    HueAdvancedLampInterface,
    HueAPI.RequestCallback,
    CompoundButton.OnCheckedChangeListener {
    private lateinit var lampData: HueRoomInterface
    private lateinit var hueAPI: HueAPI
    private lateinit var queue: RequestQueue
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HueLampListAdapter
    private val updateHandler: UpdateHandler = UpdateHandler()

    override var id: String = ""
    override var canReceiveRequest: Boolean = true
    override lateinit var device: DeviceItem
    override lateinit var addressPrefix: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        lampData = context as HueRoomInterface
        hueAPI = HueAPI(requireContext(), lampData.device.id)
        queue = Volley.newRequestQueue(context)

        device = lampData.device
        addressPrefix = lampData.addressPrefix

        recyclerView = super.onCreateView(inflater, container, savedInstanceState) as RecyclerView

        adapter = HueLampListAdapter(this, this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        return recyclerView
    }

    override fun onStart() {
        super.onStart()
        updateHandler.setUpdateFunction {
            if (lampData.canReceiveRequest && hueAPI.readyForRequest) {
                hueAPI.loadLightsByIds(lampData.lights ?: JSONArray(), this)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        updateHandler.stop()
    }

    override fun onItemClicked(
        view: View,
        position: Int,
    ) {
        id = view.findViewById<TextView>(R.id.hidden).text.toString()
        HueColorSheet(this).show(
            requireActivity().supportFragmentManager,
            HueColorSheet::class.simpleName,
        )
    }

    override fun onLightsLoaded(response: JSONObject?) {
        if (response != null) {
            var currentObject: JSONObject
            var currentState: JSONObject
            val listItems: MutableList<ListViewItem> = mutableListOf()
            val colorArray: MutableList<Int> = mutableListOf()
            for (i in response.keys()) {
                currentObject = response.optJSONObject(i) ?: JSONObject()
                currentState = currentObject.optJSONObject("state") ?: JSONObject()
                colorArray +=
                    if (currentState.has("hue") && currentState.has("sat")) {
                        HueUtils.hueSatToRGB(
                            currentState.getInt("hue"),
                            currentState.getInt("sat"),
                        )
                    } else if (currentState.has("ct")) {
                        HueUtils.ctToRGB(currentState.getInt("ct"))
                    } else {
                        Color.parseColor("#FFFFFF")
                    }
                listItems +=
                    ListViewItem(
                        title = currentObject.optString("name"),
                        summary =
                            if (currentState.optBoolean("reachable")) {
                                resources.getString(R.string.hue_tap)
                            } else {
                                resources.getString(R.string.str_unreachable)
                            },
                        hidden = i,
                        state = currentState.optBoolean("on"),
                    )
            }
            adapter.updateData(recyclerView, listItems, colorArray)
        }
    }

    override fun onColorChanged(color: Int) {
        // Do nothing.
    }

    override fun onBrightnessChanged(brightness: Int) {
        // Do nothing.
    }

    override fun onHueSatChanged(
        hue: Int,
        sat: Int,
    ) {
        // Do nothing.
    }

    override fun onCtChanged(ct: Int) {
        // Do nothing.
    }

    override fun onCheckedChanged(
        compoundButton: CompoundButton,
        state: Boolean,
    ) {
        if (compoundButton.isPressed) {
            hueAPI.switchLightById(
                (compoundButton.parent as ViewGroup).findViewById<TextView>(R.id.hidden).text.toString(),
                state,
            )
        }
    }
}
