package io.github.domi04151309.home.services

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.actions.ControlAction
import android.service.controls.templates.ControlButton
import android.service.controls.templates.ToggleTemplate
import androidx.annotation.RequiresApi
import io.github.domi04151309.home.R
import io.github.domi04151309.home.api.UnifiedAPI
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface
import java.util.concurrent.Flow
import java.util.function.Consumer

@RequiresApi(Build.VERSION_CODES.R)
class ControlService : ControlsProviderService() {

    companion object {
        private const val CONTROL_REQUEST_CODE = 1
    }

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        return Flow.Publisher { subscriber ->
            val pi = PendingIntent.getActivity(
                baseContext, CONTROL_REQUEST_CODE, Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val devices = Devices(baseContext)
            val relevantDevices = arrayListOf<DeviceItem>()
            for (i in 0 until devices.length) {
                val currentDevice = devices.getDeviceByIndex(i)
                if (
                    !currentDevice.hide
                    && Global.POWER_MENU_MODES.contains(currentDevice.mode)
                    && Global.checkNetwork(this)
                ) relevantDevices.add(currentDevice)
            }
            var finishedRequests = 0
            for (i in 0 until relevantDevices.size) {
                Global.getCorrectAPI(this, relevantDevices[i].mode, relevantDevices[i].id)
                    ?.loadList(object : UnifiedAPI.CallbackInterface {
                        override fun onItemsLoaded(
                            holder: UnifiedRequestCallback,
                            recyclerViewInterface: HomeRecyclerViewHelperInterface?
                        ) {
                            if (holder.response != null) {
                                holder.response.forEach {
                                    subscriber.onNext(
                                        Control.StatelessBuilder(
                                            relevantDevices[i].id + '@' + it.hidden,
                                            pi
                                        )
                                            .setTitle(it.title)
                                            .setZone(relevantDevices[i].name)
                                            .setStructure(resources.getString(R.string.app_name))
                                            .setDeviceType(Global.getDeviceType(relevantDevices[i].iconName))
                                            .build()
                                    )
                                }
                            }
                            finishedRequests++
                            if (finishedRequests == relevantDevices.size) subscriber.onComplete()
                        }

                        override fun onExecuted(
                            result: String,
                            shouldRefresh: Boolean
                        ) {
                        }
                    })
            }
        }
    }

    //TODO: check network

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        return Flow.Publisher { subscriber ->
            subscriber.onSubscribe(object : Flow.Subscription {
                override fun request(n: Long) {}
                override fun cancel() {}
            })
            //TODO: Proper activity
            val pi = PendingIntent.getActivity(
                baseContext, CONTROL_REQUEST_CODE, Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            controlIds.forEach { id ->
                val device = Devices(baseContext).getDeviceById(id.substring(0, id.indexOf('@')))
                Global.getCorrectAPI(this, device.mode, device.id)
                    ?.loadList(object : UnifiedAPI.CallbackInterface {
                        override fun onItemsLoaded(
                            holder: UnifiedRequestCallback,
                            recyclerViewInterface: HomeRecyclerViewHelperInterface?
                        ) {
                            if (holder.response != null) {
                                holder.response.forEach {
                                    if (device.id + '@' + it.hidden == id) {
                                        val controlBuilder = Control.StatefulBuilder(id, pi)
                                            .setTitle(it.title)
                                            .setZone(device.name)
                                            .setStructure(resources.getString(R.string.app_name))
                                            .setDeviceType(Global.getDeviceType(device.iconName))
                                            .setStatus(Control.STATUS_OK)
                                        if (it.state != null) {
                                            controlBuilder.setControlTemplate(
                                                ToggleTemplate(
                                                    id,
                                                    ControlButton(
                                                        it.state ?: false,
                                                        it.state.toString()
                                                    )
                                                )
                                            )
                                            /*controlBuilder.setStatusText(
                                                if (it.state == true) resources.getString(R.string.str_on)
                                                else resources.getString(R.string.str_off)
                                            )*/
                                        }
                                        subscriber.onNext(controlBuilder.build())
                                    }
                                }
                            } else {
                                subscriber.onNext(
                                    Control.StatefulBuilder(id, pi)
                                        .setTitle(device.name)
                                        .setZone(device.name)
                                        .setStructure(resources.getString(R.string.app_name))
                                        .setDeviceType(Global.getDeviceType(device.iconName))
                                        /*.setStatus(Control.STATUS_NOT_FOUND).setStatusText(
                                            resources.getString(R.string.str_unreachable)
                                        )*/
                                        .build()
                                )
                            }
                        }

                        override fun onExecuted(
                            result: String,
                            shouldRefresh: Boolean
                        ) {
                        }
                    })
            }
        }
    }

    override fun performControlAction(p0: String, p1: ControlAction, p2: Consumer<Int>) {
        //TODO("Not yet implemented")
    }
}