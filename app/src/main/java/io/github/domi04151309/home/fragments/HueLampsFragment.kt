package io.github.domi04151309.home.fragments

import android.content.Context
import android.content.Intent
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
import io.github.domi04151309.home.adapters.ListViewAdapter
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.RequestCallbackObject
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.HueAPI
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class HueLampsFragment : Fragment(R.layout.fragment_hue_lamps) {

    private lateinit var c: Context
    private lateinit var lampData: HueLampActivity
    private lateinit var hueAPI: HueAPI
    private lateinit var queue: RequestQueue

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

        hueAPI.loadLightsByIDs(lampData.lights ?: JSONArray(), object : HueAPI.RequestCallBack {
            override fun onGroupLoaded(holder: RequestCallbackObject) {}
            override fun onGroupsLoaded(holder: RequestCallbackObject) {}
            override fun onLightsLoaded(holder: RequestCallbackObject) {
                if (holder.response != null) {
                    try {
                        var currentObjectName: String
                        var currentObject: JSONObject
                        val listItems: ArrayList<ListViewItem> = arrayListOf()
                        for (i in 1 until (holder.response.length() + 1)) {
                            try {
                                currentObjectName = holder.response.names()?.getString(i - 1) ?: ""
                                currentObject = holder.response.getJSONObject(currentObjectName)
                                listItems += ListViewItem(
                                        title = currentObject.getString("name"),
                                        summary = resources.getString(R.string.hue_tap),
                                        hidden = currentObjectName,
                                        icon = R.drawable.ic_device_lamp,
                                        state = currentObject.getJSONObject("state").getBoolean("on"),
                                        stateListener = hueLampStateListener
                                )
                            } catch (e: JSONException) {
                                Log.e(Global.LOG_TAG, e.toString())
                            }
                        }
                        listView.adapter = ListViewAdapter(c, listItems, false)
                    } catch (e: Exception) {
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                }
            }
        })

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            startActivity(
                    Intent(c, HueLampActivity::class.java)
                            .putExtra("ID", view.findViewById<TextView>(R.id.hidden).text.toString())
                            .putExtra("Device", lampData.deviceId)
            )
        }
        return view
    }
}