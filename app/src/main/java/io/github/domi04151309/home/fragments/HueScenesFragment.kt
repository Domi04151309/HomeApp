package io.github.domi04151309.home.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.R
import io.github.domi04151309.home.activities.HueLampActivity
import io.github.domi04151309.home.activities.HueSceneActivity
import io.github.domi04151309.home.adapters.HueScenesGridAdapter
import io.github.domi04151309.home.custom.CustomJsonArrayRequest
import io.github.domi04151309.home.data.ScenesGridItem
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.HueAPI
import org.json.JSONObject

class HueScenesFragment : Fragment(R.layout.fragment_hue_scenes) {

    private var scenesRequest: JsonObjectRequest? = null
    private var selectedScene: CharSequence = ""
    private var selectedSceneName: CharSequence = ""
    private lateinit var hueAPI: HueAPI
    private lateinit var queue: RequestQueue
    private lateinit var c: Context

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        c = context ?: throw IllegalStateException()
        hueAPI = HueAPI(c, HueLampActivity.deviceId)
        queue = Volley.newRequestQueue(context)

        val view = super.onCreateView(inflater, container, savedInstanceState)
        val gridView = view?.findViewById<GridView>(R.id.scenes) ?: throw IllegalStateException()

        scenesRequest = JsonObjectRequest(Request.Method.GET, HueLampActivity.address + "api/" + hueAPI.getUsername() + "/scenes/", null,
                { response ->
                    try {
                        val gridItems: ArrayList<ScenesGridItem> = ArrayList(response.length())
                        var currentObjectName: String
                        var currentObject: JSONObject
                        for (i in 0 until response.length()) {
                            currentObjectName = response.names()?.getString(i) ?: ""
                            currentObject = response.getJSONObject(currentObjectName)
                            if (currentObject.getString("group") == HueLampActivity.id) {
                                gridItems += ScenesGridItem(
                                        name = currentObject.getString("name"),
                                        hidden = currentObjectName,
                                        icon = R.drawable.ic_hue_scene
                                )
                            }
                        }
                        gridItems += ScenesGridItem(
                                name = resources.getString(R.string.hue_add_scene),
                                hidden = "add",
                                icon = R.drawable.ic_hue_scene_add
                        )
                        gridView.adapter = HueScenesGridAdapter(c, gridItems)
                    } catch (e: Exception){
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                },
                { error ->
                    Toast.makeText(c, Global.volleyError(c, error), Toast.LENGTH_LONG).show()
                }
        )
        queue.add(scenesRequest)

        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            val hiddenText = view.findViewById<TextView>(R.id.hidden).text.toString()
            if (hiddenText == "add") {
                startActivity(Intent(c, HueSceneActivity::class.java).putExtra("deviceId", HueLampActivity.deviceId).putExtra("room", HueLampActivity.id))
            } else {
                hueAPI.activateSceneOfGroup(HueLampActivity.id, hiddenText)
            }
        }
        registerForContextMenu(gridView)
        return view
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val info = menuInfo as AdapterView.AdapterContextMenuInfo
        val view = v as GridView
        selectedScene = view.getChildAt(info.position).findViewById<TextView>(R.id.hidden).text
        selectedSceneName = view.getChildAt(info.position).findViewById<TextView>(R.id.name).text
        if (selectedScene != "add") {
            MenuInflater(c).inflate(R.menu.activity_hue_lamp_context, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.title == resources.getString(R.string.str_rename)) {
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
                        val renameSceneRequest = CustomJsonArrayRequest(Request.Method.PUT, HueLampActivity.address + "api/" + hueAPI.getUsername() + "/scenes/$selectedScene", JSONObject(requestObject),
                                { queue.add(scenesRequest) },
                                { e -> Log.e(Global.LOG_TAG, e.toString()) }
                        )
                        queue.add(renameSceneRequest)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
        } else if (item.title == resources.getString(R.string.str_delete)) {
            AlertDialog.Builder(c)
                    .setTitle(R.string.str_delete)
                    .setMessage(R.string.hue_delete_scene)
                    .setPositiveButton(R.string.str_delete) { _, _ ->
                        val deleteSceneRequest = CustomJsonArrayRequest(Request.Method.DELETE, HueLampActivity.address + "api/" + hueAPI.getUsername() + "/scenes/" + selectedScene, null,
                                { queue.add(scenesRequest) },
                                { e -> Log.e(Global.LOG_TAG, e.toString()) }
                        )
                        queue.add(deleteSceneRequest)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
        }
        return super.onContextItemSelected(item)
    }
}