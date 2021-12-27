package io.github.domi04151309.home.api

import android.content.Context
import android.content.res.Resources
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface
import org.json.JSONObject

open class UnifiedAPI(
    protected val c: Context,
    val deviceId: String,
    protected val recyclerViewInterface: HomeRecyclerViewHelperInterface?
) {

    interface CallbackInterface {
        fun onItemsLoaded(holder: UnifiedRequestCallback, recyclerViewInterface: HomeRecyclerViewHelperInterface?)
        fun onExecuted(result: String, shouldRefresh: Boolean = false)
    }

    interface RealTimeStatesCallback {
        fun onStatesLoaded(states: ArrayList<Boolean?>)
    }

    var dynamicSummaries: Boolean = true

    protected val url: String = Devices(c).getDeviceById(deviceId).address
    protected val queue: RequestQueue = Volley.newRequestQueue(c)

    open fun loadList(callback: CallbackInterface) {}
    open fun loadStates(callback: RealTimeStatesCallback) {}
    open fun execute(path: String, callback: CallbackInterface) {}
    open fun changeSwitchState(id: String, state: Boolean) {}

    open class Parser(protected val resources: Resources) {
        open fun parseResponse(response: JSONObject): ArrayList<ListViewItem> = arrayListOf()
    }
}