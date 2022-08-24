package io.github.domi04151309.home.activities

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.SimpleListAdapter
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.api.UnifiedAPI
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.SimpleListItem
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Theme
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface

class ShortcutHueRoomActivity : AppCompatActivity(), RecyclerViewHelperInterface {

    private var deviceId: String? = null
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        recyclerView = findViewById(R.id.recyclerView)

        val devices = Devices(this)
        val listItems: ArrayList<SimpleListItem> = ArrayList(devices.length)
        var currentDevice: DeviceItem
        for (i in 0 until devices.length) {
            currentDevice = devices.getDeviceByIndex(i)
            if (currentDevice.mode == "Hue API") listItems += SimpleListItem(
                title = currentDevice.name,
                summary = currentDevice.address,
                hidden = currentDevice.id,
                icon = currentDevice.iconId
            )
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SimpleListAdapter(listItems, this)
    }

    override fun onItemClicked(view: View, position: Int) {
        if (deviceId == null) {
            deviceId = view.findViewById<TextView>(R.id.hidden).text.toString()
            HueAPI(this, deviceId ?: throw IllegalStateException()).loadList(
                object : UnifiedAPI.CallbackInterface {
                    override fun onItemsLoaded(
                        holder: UnifiedRequestCallback,
                        recyclerViewInterface: HomeRecyclerViewHelperInterface?
                    ) {
                        if (holder.response != null) {
                            @Suppress("UNCHECKED_CAST")
                            recyclerView.adapter = SimpleListAdapter(
                                holder.response as ArrayList<SimpleListItem>,
                                this@ShortcutHueRoomActivity
                            )
                        } else {
                            deviceId = null
                            Toast.makeText(
                                this@ShortcutHueRoomActivity,
                                holder.errorMessage,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onExecuted(result: String, shouldRefresh: Boolean) {}
                }
            )
        } else {
            val device = Devices(this).getDeviceById(deviceId ?: throw IllegalStateException())
            val lampName = view.findViewById<TextView>(R.id.title).text
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val shortcutManager = this.getSystemService(ShortcutManager::class.java)
                if (shortcutManager != null) {
                    setResult(
                        RESULT_OK,
                        shortcutManager.createShortcutResultIntent(
                            ShortcutInfo.Builder(this, device.id + lampName)
                                .setShortLabel(lampName)
                                .setLongLabel(lampName)
                                .setIcon(Icon.createWithResource(this, device.iconId))
                                .setIntent(
                                    Intent(this, HueLampActivity::class.java)
                                        .putExtra("id", view.findViewById<TextView>(R.id.hidden).text)
                                        .putExtra("device", device.id)
                                        .setAction(Intent.ACTION_MAIN)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                )
                                .build()
                        )
                    )
                    finish()
                }
            } else
                Toast.makeText(this, R.string.pref_add_shortcut_failed, Toast.LENGTH_LONG).show()
        }
    }
}
