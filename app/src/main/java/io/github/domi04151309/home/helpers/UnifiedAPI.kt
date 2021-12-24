package io.github.domi04151309.home.helpers

import android.content.Context
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface

open class UnifiedAPI(
    protected val c: Context,
    protected val deviceId: String,
    protected val recyclerViewInterface: HomeRecyclerViewHelperInterface?
) {

    interface CallbackInterface {
        fun onItemsLoaded(holder: UnifiedRequestCallback, recyclerViewInterface: HomeRecyclerViewHelperInterface?)
        fun onExecuted(result: String, deviceId: String = "", shouldRefresh: Boolean = false)
    }

    protected val url: String = Devices(c).getDeviceById(deviceId).address
    protected val queue: RequestQueue = Volley.newRequestQueue(c)

    open fun loadList(callback: CallbackInterface) {}
    open fun execute(url: String, callback: CallbackInterface) {}
    open fun changeSwitchState(id: Int, state: Boolean) {}
}