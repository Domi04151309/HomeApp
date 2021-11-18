package io.github.domi04151309.home.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.R
import io.github.domi04151309.home.activities.HueLampActivity
import io.github.domi04151309.home.adapters.ListViewAdapterHue
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.RequestCallbackObject
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.HueAPI
import io.github.domi04151309.home.helpers.HueUtils
import io.github.domi04151309.home.helpers.UpdateHandler
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class HueLampsFragment : Fragment(R.layout.fragment_hue_lamps) {

    private lateinit var c: Context
    private lateinit var lampData: HueLampActivity
    private lateinit var hueAPI: HueAPI
    private lateinit var queue: RequestQueue
    private lateinit var requestCallBack: HueAPI.RequestCallBack
    private val updateHandler: UpdateHandler = UpdateHandler()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        c = context ?: throw IllegalStateException()
        lampData = context as HueLampActivity
        hueAPI = HueAPI(c, lampData.deviceId)
        queue = Volley.newRequestQueue(context)

        val view = super.onCreateView(inflater, container, savedInstanceState) ?: throw IllegalStateException()
        val listView = view.findViewById<ListView>(R.id.listView)

        val hueLampStateListener = CompoundButton.OnCheckedChangeListener { compoundButton, b ->
            if (compoundButton.isPressed) {
                val hidden = (compoundButton.parent as ViewGroup).findViewById<TextView>(R.id.hidden).text.toString()
                hueAPI.switchLightByID(hidden, b)
            }
        }

        requestCallBack = object : HueAPI.RequestCallBack {
            override fun onGroupLoaded(holder: RequestCallbackObject) {}
            override fun onGroupsLoaded(holder: RequestCallbackObject) {}
            override fun onLightsLoaded(holder: RequestCallbackObject) {
                if (holder.response != null) {
                    try {
                        var currentObjectName: String
                        var currentObject: JSONObject
                        var currentState: JSONObject
                        val listItems: ArrayList<ListViewItem> = arrayListOf()
                        val colorArray: ArrayList<Int> = arrayListOf()
                        for (i in 0 until (holder.response.length())) {
                            try {
                                currentObjectName = holder.response.names()?.getString(i) ?: ""
                                currentObject = holder.response.getJSONObject(currentObjectName)

                                currentState = currentObject.getJSONObject("state")
                                colorArray += if (currentState.has("hue") && currentState.has("sat")) {
                                    HueUtils.hueSatToRGB(currentState.getInt("hue"), currentState.getInt("sat"))
                                } else if (currentState.has("ct")) {
                                    HueUtils.ctToRGB(currentState.getInt("ct"))
                                } else {
                                    Color.parseColor("#FFFFFF")
                                }

                                listItems += ListViewItem(
                                    title = currentObject.getString("name"),
                                    summary =
                                        if (currentState.getBoolean("reachable")) resources.getString(R.string.hue_tap)
                                        else resources.getString(R.string.hue_unreachable),
                                    hidden = currentObjectName,
                                    state = currentState.getBoolean("on"),
                                    stateListener = hueLampStateListener
                                )
                            } catch (e: JSONException) {
                                Log.e(Global.LOG_TAG, e.toString())
                            }
                        }
                        listView.adapter = ListViewAdapterHue(c, listItems, colorArray)
                    } catch (e: Exception) {
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                }
            }
        }

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, element, _, _ ->
            startActivity(
                    Intent(c, HueLampActivity::class.java)
                            .putExtra("ID", element.findViewById<TextView>(R.id.hidden).text.toString())
                            .putExtra("Device", lampData.deviceId)
            )
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        updateHandler.setUpdateFunction {
            if (lampData.canReceiveRequest && hueAPI.readyForRequest) {
                hueAPI.loadLightsByIDs(lampData.lights ?: JSONArray(), requestCallBack)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        updateHandler.stop()
    }
}