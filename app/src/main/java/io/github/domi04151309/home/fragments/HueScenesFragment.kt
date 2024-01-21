package io.github.domi04151309.home.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.domi04151309.home.R
import io.github.domi04151309.home.activities.HueSceneActivity
import io.github.domi04151309.home.adapters.HueSceneGridAdapter
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.custom.CustomJsonArrayRequest
import io.github.domi04151309.home.data.SceneGridItem
import io.github.domi04151309.home.helpers.ColorUtils
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.HueUtils
import io.github.domi04151309.home.interfaces.HueLampInterface
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface
import org.json.JSONException
import org.json.JSONObject

class HueScenesFragment :
    Fragment(R.layout.fragment_hue_scenes),
    RecyclerViewHelperInterface,
    Response.Listener<JSONObject> {
    private var scenesRequest: JsonObjectRequest? = null
    private var selectedScene: CharSequence = ""
    private var selectedSceneName: CharSequence = ""
    private lateinit var lampData: HueLampInterface
    private lateinit var hueAPI: HueAPI
    private lateinit var queue: RequestQueue
    private lateinit var adapter: HueSceneGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        lampData = context as HueLampInterface
        hueAPI = HueAPI(requireContext(), lampData.device.id)
        queue = Volley.newRequestQueue(context)

        val recyclerView = super.onCreateView(inflater, container, savedInstanceState) as RecyclerView
        adapter = HueSceneGridAdapter(this, this)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), COLUMNS)
        recyclerView.adapter = adapter

        scenesRequest =
            JsonObjectRequest(
                Request.Method.GET, lampData.addressPrefix + SCENES_PATH, null,
                this,
            ) { error ->
                Toast.makeText(
                    requireContext(),
                    Global.volleyError(requireContext(), error),
                    Toast.LENGTH_LONG,
                ).show()
            }
        queue.add(scenesRequest)
        return recyclerView
    }

    override fun onResponse(response: JSONObject) {
        try {
            val gridItems: ArrayList<SceneGridItem> = ArrayList(response.length())
            val scenes: List<Pair<String, String>> = getScenes(response)
            if (scenes.isNotEmpty()) {
                var completedRequests = 0
                for (i in scenes.indices) {
                    queue.add(
                        JsonObjectRequest(
                            Request.Method.GET,
                            lampData.addressPrefix + SCENES_PATH + scenes[i].first,
                            null,
                            { sceneResponse ->
                                gridItems +=
                                    SceneGridItem(
                                        name = scenes[i].second,
                                        hidden = scenes[i].first,
                                        color = getSceneColor(sceneResponse),
                                    )
                                completedRequests++
                                if (completedRequests == scenes.size) {
                                    val sortedItems =
                                        gridItems.sortedWith(compareBy { it.color })
                                            .toMutableList()
                                    sortedItems +=
                                        SceneGridItem(
                                            name = resources.getString(R.string.hue_add_scene),
                                            hidden = "add",
                                        )
                                    adapter.updateData(sortedItems)
                                }
                            },
                            { error ->
                                Log.e(Global.LOG_TAG, error.toString())
                            },
                        ),
                    )
                }
            } else {
                adapter.updateData(
                    mutableListOf(
                        SceneGridItem(
                            name = resources.getString(R.string.hue_add_scene),
                            hidden = "add",
                        ),
                    ),
                )
            }
        } catch (e: JSONException) {
            Log.e(Global.LOG_TAG, e.toString())
        }
    }

    private fun getScenes(response: JSONObject): List<Pair<String, String>> {
        val scenes: ArrayList<Pair<String, String>> =
            ArrayList(
                response.length() / SCENE_FRACTION_ESTIMATE,
            )
        var currentObject: JSONObject
        for (i in response.keys()) {
            currentObject = response.getJSONObject(i)
            if (currentObject.optString("group") == lampData.id) {
                scenes.add(Pair(i, currentObject.getString("name")))
            }
        }
        return scenes
    }

    private fun getSceneColor(response: JSONObject): Int {
        val states = response.getJSONObject("lightstates")
        val currentSceneValues = ArrayList<Int>(states.length())
        var lampObject: JSONObject
        for (j in states.keys()) {
            lampObject = states.getJSONObject(j)
            if (lampObject.getBoolean("on")) {
                if (lampObject.has("hue") && lampObject.has("sat")) {
                    currentSceneValues.clear()
                    currentSceneValues.add(
                        HueUtils.hueSatToRGB(
                            lampObject.getInt("hue"),
                            lampObject.getInt("sat"),
                        ),
                    )
                } else if (lampObject.has("xy")) {
                    val xyArray = lampObject.getJSONArray("xy")
                    currentSceneValues.clear()
                    currentSceneValues.add(
                        ColorUtils.xyToRGB(
                            xyArray.getDouble(0),
                            xyArray.getDouble(1),
                        ),
                    )
                } else if (lampObject.has("ct")) {
                    currentSceneValues.add(
                        HueUtils.ctToRGB(lampObject.getInt("ct")),
                    )
                }
            }
        }
        return if (currentSceneValues.size > 0) {
            currentSceneValues[0]
        } else {
            Color.WHITE
        }
    }

    override fun onItemClicked(
        view: View,
        position: Int,
    ) {
        val hiddenText = view.findViewById<TextView>(R.id.hidden).text.toString()
        if (hiddenText == "add") {
            startActivity(
                Intent(requireContext(), HueSceneActivity::class.java).putExtra(
                    "deviceId",
                    lampData.device.id,
                ).putExtra("room", lampData.id),
            )
        } else {
            hueAPI.activateSceneOfGroup(lampData.id, hiddenText)
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?,
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        selectedScene = v.findViewById<TextView>(R.id.hidden).text
        selectedSceneName = v.findViewById<TextView>(R.id.title).text
        if (selectedScene != "add") MenuInflater(requireContext()).inflate(R.menu.activity_hue_lamp_context, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean =
        when (item.title) {
            resources.getString(R.string.str_edit) -> {
                startActivity(
                    Intent(requireContext(), HueSceneActivity::class.java)
                        .putExtra("deviceId", lampData.device.id)
                        .putExtra("room", lampData.id)
                        .putExtra("scene", selectedScene),
                )
                true
            }
            resources.getString(R.string.str_delete) -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.str_delete)
                    .setMessage(R.string.hue_delete_scene)
                    .setPositiveButton(R.string.str_delete) { _, _ ->
                        val deleteSceneRequest =
                            CustomJsonArrayRequest(
                                Request.Method.DELETE,
                                lampData.addressPrefix + SCENES_PATH + selectedScene,
                                null,
                                { queue.add(scenesRequest) },
                                { e -> Log.e(Global.LOG_TAG, e.toString()) },
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

    override fun onStart() {
        super.onStart()
        if (scenesChanged) {
            scenesChanged = false
            queue.add(scenesRequest)
        }
    }

    companion object {
        private const val SCENE_FRACTION_ESTIMATE = 4
        private const val COLUMNS = 3
        private const val SCENES_PATH = "/scenes/"

        var scenesChanged: Boolean = false
    }
}
