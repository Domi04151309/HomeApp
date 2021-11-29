package io.github.domi04151309.home.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.R
import io.github.domi04151309.home.activities.HueLampActivity
import io.github.domi04151309.home.activities.HueSceneActivity
import io.github.domi04151309.home.adapters.HueSceneGridAdapter
import io.github.domi04151309.home.custom.CustomJsonArrayRequest
import io.github.domi04151309.home.data.SceneGridItem
import io.github.domi04151309.home.helpers.ColorUtils
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.HueAPI
import io.github.domi04151309.home.helpers.HueUtils
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface
import org.json.JSONObject

class HueScenesFragment : Fragment(R.layout.fragment_hue_scenes), RecyclerViewHelperInterface {

    companion object {
        var scenesChanged: Boolean = false
    }

    private var scenesRequest: JsonObjectRequest? = null
    private var selectedScene: CharSequence = ""
    private var selectedSceneName: CharSequence = ""
    private lateinit var c: Context
    private lateinit var lampData: HueLampActivity
    private lateinit var hueAPI: HueAPI
    private lateinit var queue: RequestQueue

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        c = context ?: throw IllegalStateException()
        lampData = context as HueLampActivity
        hueAPI = HueAPI(c, lampData.deviceId)
        queue = Volley.newRequestQueue(context)

        val recyclerView = (super.onCreateView(inflater, container, savedInstanceState) ?: throw IllegalStateException()) as RecyclerView
        val adapter = HueSceneGridAdapter(this, this)
        recyclerView.layoutManager = GridLayoutManager(c, 3)
        recyclerView.adapter = adapter

        scenesRequest = JsonObjectRequest(Request.Method.GET, lampData.addressPrefix + "/scenes/", null,
                { response ->
                    try {
                        val gridItems: ArrayList<SceneGridItem> = ArrayList(response.length())
                        val sceneIds: ArrayList<String> = ArrayList(response.length() / 2)
                        val sceneNames: ArrayList<String> = ArrayList(response.length() / 2)
                        var currentObjectName: String
                        var currentObject: JSONObject
                        for (i in 0 until response.length()) {
                            currentObjectName = response.names()?.getString(i) ?: ""
                            currentObject = response.getJSONObject(currentObjectName)
                            if (currentObject.getString("group") == lampData.id) {
                                sceneIds.add(currentObjectName)
                                sceneNames.add(currentObject.getString("name"))
                            }
                        }
                        if (sceneIds.size > 0) {
                            var completedRequests = 0
                            for (i in 0 until sceneIds.size) {
                                queue.add(JsonObjectRequest(
                                    Request.Method.GET,
                                    lampData.addressPrefix + "/scenes/" + sceneIds[i],
                                    null,
                                    { sceneResponse ->
                                        val states = sceneResponse.getJSONObject("lightstates")
                                        val currentSceneValues = ArrayList<Int>(states.length())
                                        var lampObject: JSONObject
                                        for (j in 0 until states.length()) {
                                            lampObject = states.getJSONObject(
                                                states.names()?.getString(j) ?: break
                                            )
                                            if (lampObject.getBoolean("on")) {
                                                if (lampObject.has("hue") && lampObject.has("sat")) {
                                                    currentSceneValues.clear()
                                                    currentSceneValues.add(
                                                        HueUtils.hueSatToRGB(
                                                            lampObject.getInt("hue"),
                                                            lampObject.getInt("sat")
                                                        )
                                                    )
                                                } else if (lampObject.has("xy")) {
                                                    val xyArray = lampObject.getJSONArray("xy")
                                                    currentSceneValues.clear()
                                                    currentSceneValues.add(
                                                        ColorUtils.xyToRGB(
                                                            xyArray.getDouble(0),
                                                            xyArray.getDouble(1)
                                                        )
                                                    )
                                                } else if (lampObject.has("ct")) {
                                                    currentSceneValues.add(
                                                        HueUtils.ctToRGB(lampObject.getInt("ct"))
                                                    )
                                                }
                                            }
                                        }
                                        gridItems += SceneGridItem(
                                            name = sceneNames[i],
                                            hidden = sceneIds[i],
                                            color = if (currentSceneValues.size > 0) currentSceneValues[0] else Color.WHITE
                                        )
                                        completedRequests++
                                        if (completedRequests == sceneIds.size) {
                                            val sortedItems = gridItems.sortedWith(compareBy { it.color }).toMutableList()
                                            sortedItems += SceneGridItem(
                                                name = resources.getString(R.string.hue_add_scene),
                                                hidden = "add"
                                            )
                                            adapter.updateData(sortedItems)
                                        }
                                    },
                                    { error ->
                                        Log.e(Global.LOG_TAG, error.toString())
                                    }
                                ))
                            }
                        } else {
                            adapter.updateData(mutableListOf(SceneGridItem(
                                name = resources.getString(R.string.hue_add_scene),
                                hidden = "add"
                            )))
                        }
                    } catch (e: Exception){
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                },
                { error ->
                    Toast.makeText(c, Global.volleyError(c, error), Toast.LENGTH_LONG).show()
                }
        )
        queue.add(scenesRequest)
        return recyclerView
    }

    override fun onItemClicked(view: View, position: Int) {
        val hiddenText = view.findViewById<TextView>(R.id.hidden).text.toString()
        if (hiddenText == "add") {
            startActivity(Intent(c, HueSceneActivity::class.java).putExtra("deviceId", lampData.deviceId).putExtra("room", lampData.id))
        } else {
            hueAPI.activateSceneOfGroup(lampData.id, hiddenText)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        selectedScene = v.findViewById<TextView>(R.id.hidden).text
        selectedSceneName = v.findViewById<TextView>(R.id.title).text
        if (selectedScene != "add") {
            MenuInflater(c).inflate(R.menu.activity_hue_lamp_context, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.title) {
            resources.getString(R.string.str_rename) -> {
                val nullParent: ViewGroup? = null
                val view = layoutInflater.inflate(R.layout.dialog_input, nullParent, false)
                val input = view.findViewById<EditText>(R.id.input)
                input.setText(selectedSceneName)
                AlertDialog.Builder(c)
                    .setTitle(R.string.str_rename)
                    .setMessage(R.string.hue_rename_scene)
                    .setView(view)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val requestObject = "{\"name\":\"" + input.text.toString() + "\"}"
                        val renameSceneRequest = CustomJsonArrayRequest(Request.Method.PUT, lampData.addressPrefix + "/scenes/$selectedScene", JSONObject(requestObject),
                            { queue.add(scenesRequest) },
                            { e -> Log.e(Global.LOG_TAG, e.toString()) }
                        )
                        queue.add(renameSceneRequest)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
                true
            }
            resources.getString(R.string.str_delete) -> {
                AlertDialog.Builder(c)
                    .setTitle(R.string.str_delete)
                    .setMessage(R.string.hue_delete_scene)
                    .setPositiveButton(R.string.str_delete) { _, _ ->
                        val deleteSceneRequest = CustomJsonArrayRequest(Request.Method.DELETE, lampData.addressPrefix + "/scenes/" + selectedScene, null,
                            { queue.add(scenesRequest) },
                            { e -> Log.e(Global.LOG_TAG, e.toString()) }
                        )
                        queue.add(deleteSceneRequest)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
                true
            }
            else -> {
                super.onContextItemSelected(item)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (scenesChanged) {
            scenesChanged = false
            queue.add(scenesRequest)
        }
    }
}