package io.github.domi04151309.home.services

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.CommandAction
import android.service.controls.actions.ControlAction
import android.widget.Toast
import androidx.annotation.RequiresApi
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
    private var updateSubscriber: Flow.Subscriber<in Control>? = null
    private var finishedRequests = 0

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> =
        Flow.Publisher { subscriber ->
            updateSubscriber = subscriber
            if (!Global.checkNetwork(this)) {
                subscriber.onComplete()
                @Suppress("LabeledExpression")
                return@Publisher
            }
            val devices = Devices(this)
            val relevantDevices = mutableListOf<DeviceItem>()
            for (i in 0 until devices.length) {
                val currentDevice = devices.getDeviceByIndex(i)
                if (
                    !currentDevice.hide &&
                    Global.POWER_MENU_MODES.contains(currentDevice.mode)
                ) {
                    relevantDevices.add(currentDevice)
                }
            }
            finishedRequests = 0
            for (index in 0 until relevantDevices.size) {
                Global.getCorrectAPI(this, relevantDevices[index].mode, relevantDevices[index].id)
                    .loadList(
                        getAllAvailableCallback(
                            subscriber,
                            relevantDevices,
                            index,
                        ),
                    )
            }
        }

    private fun getAllAvailableCallback(
        subscriber: Flow.Subscriber<in Control>,
        relevantDevices: MutableList<DeviceItem>,
        index: Int,
    ): UnifiedAPI.CallbackInterface =
        object : UnifiedAPI.CallbackInterface {
            override fun onItemsLoaded(
                holder: UnifiedRequestCallback,
                recyclerViewInterface: HomeRecyclerViewHelperInterface?,
            ) {
                for (it in holder.response ?: emptyList()) {
                    subscriber.onNext(
                        ControlBuilders.buildGenericControl(this@ControlService, it, relevantDevices[index]),
                    )
                }
                finishedRequests++
                if (finishedRequests == relevantDevices.size) subscriber.onComplete()
            }

            override fun onExecuted(
                result: String,
                shouldRefresh: Boolean,
            ) {
                // Do nothing.
            }
        }

    private fun loadStatefulControl(
        subscriber: Flow.Subscriber<in Control>?,
        id: String,
    ) {
        val device = Devices(this).getDeviceById(id.substring(0, id.indexOf('@')))
        if (Global.checkNetwork(this)) {
            Global
                .getCorrectAPI(this, device.mode, device.id)
                .loadList(getStatefulControlsCallback(device, id, subscriber))
        } else {
            subscriber?.onNext(ControlBuilders.buildUnreachableControl(this, id, device))
        }
    }

    private fun getStatefulControlsCallback(
        device: DeviceItem,
        id: String,
        subscriber: Flow.Subscriber<in Control>?,
    ) = object : UnifiedAPI.CallbackInterface {
        override fun onItemsLoaded(
            holder: UnifiedRequestCallback,
            recyclerViewInterface: HomeRecyclerViewHelperInterface?,
        ) {
            if (holder.response == null) {
                subscriber?.onNext(ControlBuilders.buildUnreachableControl(this@ControlService, id, device))
                return
            }
            for (it in holder.response) {
                if (device.id + '@' + it.hidden != id) continue
                subscriber?.onNext(ControlBuilders.buildStatefulControl(this@ControlService, id, it, device))
            }
        }

        override fun onExecuted(
            result: String,
            shouldRefresh: Boolean,
        ) {
            // Do nothing.
        }
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> =
        Flow.Publisher { subscriber ->
            updateSubscriber = subscriber
            subscriber.onSubscribe(
                object : Flow.Subscription {
                    override fun request(n: Long) {
                        // Do nothing.
                    }

                    override fun cancel() {
                        // Do nothing.
                    }
                },
            )
            for (id in controlIds) {
                loadStatefulControl(subscriber, id)
            }
        }

    override fun performControlAction(
        controlId: String,
        action: ControlAction,
        consumer: Consumer<Int>,
    ) {
        if (Global.checkNetwork(this)) {
            val device =
                Devices(this)
                    .getDeviceById(controlId.substring(0, controlId.indexOf('@')))
            val api = Global.getCorrectAPI(this, device.mode, device.id)
            if (action is BooleanAction) {
                api.changeSwitchState(controlId.substring(device.id.length + 1), action.newState)
            } else if (action is CommandAction) {
                api.execute(
                    controlId.substring(device.id.length + 1),
                    object : UnifiedAPI.CallbackInterface {
                        override fun onItemsLoaded(
                            holder: UnifiedRequestCallback,
                            recyclerViewInterface: HomeRecyclerViewHelperInterface?,
                        ) {
                            // Do nothing.
                        }

                        override fun onExecuted(
                            result: String,
                            shouldRefresh: Boolean,
                        ) {
                            Toast.makeText(this@ControlService, result, Toast.LENGTH_LONG).show()
                        }
                    },
                )
            }
            consumer.accept(ControlAction.RESPONSE_OK)
            Handler(Looper.getMainLooper()).postDelayed({
                loadStatefulControl(updateSubscriber, controlId)
            }, UPDATE_DELAY)
        } else {
            consumer.accept(ControlAction.RESPONSE_FAIL)
        }
    }

    companion object {
        private const val UPDATE_DELAY = 100L
    }
}
