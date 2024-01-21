package io.github.domi04151309.home.discovery

import android.content.Context
import android.util.Log
import com.rine.upnpdiscovery.UPnPDevice
import com.rine.upnpdiscovery.UPnPDiscovery
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.DeviceDiscoveryListAdapter
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.helpers.Devices

class UPnPListener(
    context: Context,
    private val adapter: DeviceDiscoveryListAdapter,
) : UPnPDiscovery.OnDiscoveryListener {
    private val devices = Devices(context)
    private val addresses = mutableListOf<String>()

    override fun onStart() {
        // Do nothing.
    }

    override fun onFoundNewDevice(device: UPnPDevice) {
        if (device.server.contains("IpBridge") && !addresses.contains(device.hostAddress)) {
            adapter.add(
                ListViewItem(
                    title = device.friendlyName,
                    summary = device.hostAddress,
                    hidden = "Hue API#Lamp",
                    icon = R.drawable.ic_device_lamp,
                    state = devices.addressExists(device.hostAddress),
                ),
            )
            addresses += device.hostAddress
        }
        if (device.friendlyName.startsWith("FRITZ!") && !addresses.contains(device.hostAddress)) {
            adapter.add(
                ListViewItem(
                    title = device.friendlyName,
                    summary = device.hostAddress,
                    hidden = "Website#Router",
                    icon = R.drawable.ic_device_router,
                    state = devices.addressExists(device.hostAddress),
                ),
            )
            addresses += device.hostAddress
        }
        if (device.server.contains("SimpleHome") && !addresses.contains(device.hostAddress)) {
            adapter.add(
                ListViewItem(
                    title = device.friendlyName,
                    summary = device.hostAddress,
                    hidden = "SimpleHome API#Raspberry Pi",
                    icon = R.drawable.ic_device_raspberry_pi,
                    state = devices.addressExists(device.hostAddress),
                ),
            )
            addresses += device.hostAddress
        }
    }

    override fun onFinish(devices: HashSet<UPnPDevice>) {
        // Do nothing.
    }

    override fun onError(e: Exception) {
        Log.e(this::class.simpleName, e.toString())
    }
}
