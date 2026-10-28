package io.github.domi04151309.home.api

import android.content.res.Resources
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import org.json.JSONObject

class TasmotaStatusParser(resources: Resources) : UnifiedAPI.Parser(resources) {
    override fun parseResponse(response: JSONObject): List<ListViewItem> {
        val unknown = resources.getString(R.string.tasmota_status_unknown)

        val statusSns = response.optJSONObject("StatusSNS")
        val sns = statusSns?.optJSONObject("TuyaSNS")
        val sts = response.optJSONObject("StatusSTS")
        val enum = response.optJSONObject("TuyaEnum")

        return listOf(
            airQualityItem(sns, unknown),
            modeItem(enum, unknown),
            fanItem(enum, unknown),
            childLockItem(statusSns, unknown),
            powerItem(sts, unknown),
        )
    }

    private fun airQualityItem(
        sns: JSONObject?,
        unknown: String,
    ): ListViewItem {
        val summary =
            if (sns != null && sns.has("PM")) {
                resources.getString(R.string.tasmota_status_pm_format, sns.optInt("PM"))
            } else {
                unknown
            }
        return ListViewItem(
            title = resources.getString(R.string.tasmota_status_air_quality),
            summary = summary,
            icon = R.drawable.ic_device_gauge,
        )
    }

    private fun modeItem(
        enum: JSONObject?,
        unknown: String,
    ): ListViewItem {
        val summary =
            when (enum?.takeIf { it.has("Enum1") }?.optInt("Enum1", -1)) {
                0 -> resources.getString(R.string.tasmota_mode_manual)
                1 -> resources.getString(R.string.tasmota_mode_auto)
                2 -> resources.getString(R.string.tasmota_mode_sleep)
                else -> unknown
            }
        return ListViewItem(
            title = resources.getString(R.string.tasmota_status_mode),
            summary = summary,
            icon = R.drawable.ic_device_clock,
        )
    }

    private fun fanItem(
        enum: JSONObject?,
        unknown: String,
    ): ListViewItem {
        // TuyaEnum2 is 0-based for the four manual fan speeds: 0->gear 1 .. 3->gear 4.
        val summary =
            when (enum?.takeIf { it.has("Enum2") }?.optInt("Enum2", -1)) {
                0 -> "1"
                1 -> "2"
                2 -> "3"
                3 -> "4"
                else -> unknown
            }
        return ListViewItem(
            title = resources.getString(R.string.tasmota_status_fan),
            summary = summary,
            icon = R.drawable.ic_device_gauge,
        )
    }

    private fun childLockItem(
        statusSns: JSONObject?,
        unknown: String,
    ): ListViewItem = booleanItem(statusSns, "Switch1", R.string.tasmota_status_child_lock, R.drawable.ic_do, unknown)

    private fun powerItem(
        sts: JSONObject?,
        unknown: String,
    ): ListViewItem = booleanItem(sts, "POWER", R.string.tasmota_status_power, R.drawable.ic_device_electricity, unknown)

    private fun booleanItem(
        source: JSONObject?,
        key: String,
        titleRes: Int,
        icon: Int,
        unknown: String,
    ): ListViewItem {
        val raw = if (source != null && source.has(key)) source.optString(key) else null
        val state: Boolean? =
            when (raw) {
                "ON" -> true
                "OFF" -> false
                else -> null
            }
        val summary =
            when (state) {
                true -> resources.getString(R.string.str_on)
                false -> resources.getString(R.string.str_off)
                null -> unknown
            }
        return ListViewItem(
            title = resources.getString(titleRes),
            summary = summary,
            icon = icon,
            state = state,
        )
    }
}
