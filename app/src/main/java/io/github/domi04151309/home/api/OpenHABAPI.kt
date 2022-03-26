package io.github.domi04151309.home.api

import android.content.Context
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface

class OpenHABAPI(
    c: Context,
    deviceId: String,
    recyclerViewInterface: HomeRecyclerViewHelperInterface?
) : UnifiedAPI(c, deviceId, recyclerViewInterface) {

    init {
        dynamicSummaries = false
    }

    override fun loadList(callback: CallbackInterface) {
        //TODO: implement
        callback.onItemsLoaded(
            UnifiedRequestCallback(
                arrayListOf(
                    ListViewItem(
                        "Not yet implemented!",
                        "Not yet implemented!",
                        icon = R.drawable.ic_device_clock
                    )
                ),
                deviceId
            ),
            recyclerViewInterface
        )
    }

    override fun loadStates(callback: RealTimeStatesCallback, offset: Int) {
        //TODO: implement
        callback.onStatesLoaded(
            arrayListOf(null),
            offset,
            dynamicSummaries
        )
    }

    override fun execute(path: String, callback: CallbackInterface) {
        //TODO: implement
        callback.onExecuted(
            "Not yet implemented!",
            false
        )
    }

    override fun changeSwitchState(id: String, state: Boolean) {
        //TODO: implement
    }
}