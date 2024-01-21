package io.github.domi04151309.home.activities

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.SimpleListAdapter
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.SimpleListItem
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface

class ShortcutDeviceActivity : BaseActivity(), RecyclerViewHelperInterface {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        val devices = Devices(this)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        val listItems: ArrayList<SimpleListItem> = ArrayList(devices.length)
        var currentDevice: DeviceItem
        for (i in 0 until devices.length) {
            currentDevice = devices.getDeviceByIndex(i)
            listItems +=
                SimpleListItem(
                    title = currentDevice.name,
                    summary = currentDevice.address,
                    hidden = currentDevice.id,
                    icon = currentDevice.iconId,
                )
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SimpleListAdapter(listItems, this)
    }

    override fun onItemClicked(
        view: View,
        position: Int,
    ) {
        val device = Devices(this).getDeviceByIndex(position)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = this.getSystemService(ShortcutManager::class.java)
            if (shortcutManager != null) {
                setResult(
                    RESULT_OK,
                    shortcutManager.createShortcutResultIntent(
                        ShortcutInfo.Builder(this, device.id)
                            .setShortLabel(
                                device.name.ifEmpty {
                                    resources.getString(R.string.pref_add_name_empty)
                                },
                            )
                            .setLongLabel(
                                device.name.ifEmpty {
                                    resources.getString(R.string.pref_add_name_empty)
                                },
                            )
                            .setIcon(Icon.createWithResource(this, device.iconId))
                            .setIntent(
                                Intent(this, MainActivity::class.java)
                                    .putExtra(Devices.INTENT_EXTRA_DEVICE, device.id)
                                    .setAction(Intent.ACTION_MAIN)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                            )
                            .build(),
                    ),
                )
                finish()
            }
        } else {
            Toast.makeText(this, R.string.pref_add_shortcut_failed, Toast.LENGTH_LONG).show()
        }
    }
}
