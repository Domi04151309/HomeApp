package io.github.domi04151309.home.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.controls.Control
import android.service.controls.templates.ControlButton
import android.service.controls.templates.RangeTemplate
import android.service.controls.templates.StatelessTemplate
import android.service.controls.templates.ToggleRangeTemplate
import android.service.controls.templates.ToggleTemplate
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import io.github.domi04151309.home.R
import io.github.domi04151309.home.activities.MainActivity
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.P

@RequiresApi(Build.VERSION_CODES.R)
object ControlBuilders {
    private const val RANGE_MIN = 0f
    private const val RANGE_MAX = 100f
    private const val RANGE_STEP = 1f

    private fun getPendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun getControlButton(item: ListViewItem): ControlButton =
        ControlButton(
            item.state == true,
            item.state.toString(),
        )

    private fun getRangeTemplate(
        id: String,
        item: ListViewItem,
    ): RangeTemplate =
        RangeTemplate(
            id,
            RANGE_MIN,
            RANGE_MAX,
            item.percentage?.toFloat() ?: 0f,
            RANGE_STEP,
            "%.0f %%",
        )

    fun buildUnreachableControl(
        context: Context,
        id: String,
        device: DeviceItem,
    ): Control =
        Control.StatefulBuilder(id, getPendingIntent(context))
            .setTitle(device.name)
            .setZone(device.name)
            .setStructure(context.resources.getString(R.string.app_name))
            .setDeviceType(Global.getDeviceType(device.iconName))
            .setStatus(Control.STATUS_DISABLED)
            .setStatusText(context.resources.getString(R.string.str_unreachable))
            .build()

    fun buildGenericControl(
        context: Context,
        listItem: ListViewItem,
        device: DeviceItem,
    ): Control =
        Control.StatelessBuilder(
            device.id + '@' + listItem.hidden,
            getPendingIntent(context),
        )
            .setTitle(listItem.title)
            .setSubtitle(device.name)
            .setZone(device.name)
            .setStructure(context.resources.getString(R.string.app_name))
            .setDeviceType(Global.getDeviceType(device.iconName))
            .build()

    fun buildStatefulControl(
        context: Context,
        id: String,
        listItem: ListViewItem,
        device: DeviceItem,
    ): Control {
        val controlBuilder =
            Control.StatefulBuilder(id, getPendingIntent(context))
                .setTitle(listItem.title)
                .setSubtitle(device.name)
                .setZone(device.name)
                .setStructure(context.resources.getString(R.string.app_name))
                .setDeviceType(Global.getDeviceType(device.iconName))
                .setStatus(Control.STATUS_OK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            controlBuilder.setAuthRequired(
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(
                        P.PREF_CONTROLS_AUTH,
                        P.PREF_CONTROLS_AUTH_DEFAULT,
                    ),
            )
        }

        if (listItem.state != null) {
            controlBuilder.setStatusText(
                context.resources.getString(
                    if (listItem.state == true) {
                        R.string.str_on
                    } else {
                        R.string.str_off
                    },
                ),
            )
        }

        if (listItem.state != null && listItem.percentage != null) {
            controlBuilder.setControlTemplate(
                ToggleRangeTemplate(
                    id,
                    getControlButton(listItem),
                    getRangeTemplate(id, listItem),
                ),
            )
        } else if (listItem.state != null) {
            controlBuilder.setControlTemplate(
                ToggleTemplate(
                    id,
                    getControlButton(listItem),
                ),
            )
        } else if (listItem.percentage != null) {
            controlBuilder.setControlTemplate(
                getRangeTemplate(id, listItem),
            )
        }

        if (device.mode == Global.TASMOTA) {
            controlBuilder.setControlTemplate(
                StatelessTemplate(id),
            )
        }

        return controlBuilder.build()
    }
}
