package io.github.domi04151309.home.services

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.CommandAction
import android.service.controls.actions.ControlAction
import android.service.controls.templates.ControlButton
import android.service.controls.templates.StatelessTemplate
import android.service.controls.templates.ToggleTemplate
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import io.github.domi04151309.home.R
import io.github.domi04151309.home.activities.MainActivity
import io.github.domi04151309.home.api.UnifiedAPI
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.P
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface
import java.util.concurrent.Flow
import java.util.function.Consumer

@RequiresApi(Build.VERSION_CODES.R)
class ControlService : ControlsProviderService() {
    private var updateSubscriber: Flow.Subscriber<in Control>? = null
    private var finishedRequests = 0

    internal fun getPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

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
                        Control.StatelessBuilder(
                            relevantDevices[index].id + '@' + it.hidden,
                            getPendingIntent(),
                        )
                            .setTitle(it.title)
                            .setSubtitle(relevantDevices[index].name)
                            .setZone(relevantDevices[index].name)
                            .setStructure(resources.getString(R.string.app_name))
                            .setDeviceType(Global.getDeviceType(relevantDevices[index].iconName))
                            .build(),
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

    internal fun getUnreachableControl(
        id: String,
        device: DeviceItem,
    ): Control =
        Control.StatefulBuilder(id, getPendingIntent())
            .setTitle(device.name)
            .setZone(device.name)
            .setStructure(resources.getString(R.string.app_name))
            .setDeviceType(Global.getDeviceType(device.iconName))
            .setStatus(Control.STATUS_DISABLED)
            .setStatusText(resources.getString(R.string.str_unreachable))
            .build()

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
            subscriber?.onNext(getUnreachableControl(id, device))
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
                subscriber?.onNext(getUnreachableControl(id, device))
                return
            }
            for (it in holder.response) {
                if (device.id + '@' + it.hidden != id) continue
                val controlBuilder =
                    Control.StatefulBuilder(id, getPendingIntent())
                        .setTitle(it.title)
                        .setSubtitle(device.name)
                        .setZone(device.name)
                        .setStructure(resources.getString(R.string.app_name))
                        .setDeviceType(Global.getDeviceType(device.iconName))
                        .setStatus(Control.STATUS_OK)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    controlBuilder.setAuthRequired(
                        PreferenceManager.getDefaultSharedPreferences(this@ControlService)
                            .getBoolean(
                                P.PREF_CONTROLS_AUTH,
                                P.PREF_CONTROLS_AUTH_DEFAULT,
                            ),
                    )
                }
                if (it.state != null) {
                    controlBuilder.setControlTemplate(
                        ToggleTemplate(
                            id,
                            ControlButton(
                                it.state ?: false,
                                it.state.toString(),
                            ),
                        ),
                    )
                    controlBuilder.setStatusText(
                        resources.getString(
                            if (it.state == true) {
                                R.string.str_on
                            } else {
                                R.string.str_off
                            },
                        ),
                    )
                }
                if (device.mode == Global.TASMOTA) {
                    controlBuilder.setControlTemplate(
                        StatelessTemplate(id),
                    )
                }
                subscriber?.onNext(controlBuilder.build())
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
