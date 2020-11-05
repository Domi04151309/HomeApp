package io.github.domi04151309.home.activities

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import android.net.wifi.WifiManager
import android.util.Log
import com._8rine.upnpdiscovery.UPnPDevice
import com._8rine.upnpdiscovery.UPnPDiscovery
import android.os.Handler
import androidx.appcompat.app.AlertDialog
import android.widget.AdapterView
import android.widget.TextView
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.adapters.ListViewAdapter
import io.github.domi04151309.home.helpers.Theme

class SearchDevicesActivity : AppCompatActivity() {

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        listView = findViewById(R.id.listView)
        val devices = Devices(this)
        val addresses = mutableListOf<String>()

        //Countdown
        val waitItem = ListViewItem(
                title = resources.getString(R.string.pref_add_wait),
                summary = resources.getQuantityString(R.plurals.pref_add_wait_summary, 10, 10),
                icon = R.drawable.ic_info
        )
        listView.adapter = ListViewAdapter(this, arrayListOf(waitItem))
        Thread {
            Thread.sleep(1000L)
            val firstChildSummary = listView.getChildAt(0).findViewById<TextView>(R.id.summary)
            var count: Int
            for (i in 1 until 10) {
                runOnUiThread {
                    count = 10 - i
                    firstChildSummary.text = resources.getQuantityString(R.plurals.pref_add_wait_summary, count, count)
                }
                Thread.sleep(1000L)
            }
        }.start()

        //Device variables
        val listItems: ArrayList<ListViewItem> = arrayListOf()
        val manager = super.getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val routerIp = intToIp(manager.dhcpInfo.gateway)
        val customQuery = "M-SEARCH * HTTP/1.1" + "\r\n" +
                "HOST: 239.255.255.250:1900" + "\r\n" +
                "MAN: ssdp:discover" + "\r\n" +
                "MX: 10" + "\r\n" +
                "ST: ssdp:all" + "\r\n" +
                "\r\n"
        val customPort = 1900
        val customAddress = "239.255.255.250"

        Thread {
            //Get Hue Bridges
            UPnPDiscovery.discoveryDevices(this, object : UPnPDiscovery.OnDiscoveryListener {
                override fun onStart() {}
                override fun onFoundNewDevice(device: UPnPDevice) {
                    if (device.server.contains("IpBridge") && !addresses.contains(device.hostAddress)) {
                        listItems += ListViewItem(
                                title = device.friendlyName,
                                summary = device.hostAddress,
                                hidden = "Hue API#Lamp",
                                icon = R.drawable.ic_device_lamp
                        )
                        addresses += device.hostAddress
                    }
                }

                override fun onFinish(devices: HashSet<UPnPDevice>) {}
                override fun onError(e: Exception) {
                    Log.e("UPnPDiscovery", "Error: " + e.localizedMessage)
                }
            }, customQuery, customAddress, customPort)

            //Get compatible devices
            UPnPDiscovery.discoveryDevices(this, object : UPnPDiscovery.OnDiscoveryListener {
                override fun onStart() {}
                override fun onFoundNewDevice(device: UPnPDevice) {
                    val friendlyName = device.friendlyName
                    if (friendlyName.startsWith("FRITZ!") && !addresses.contains(device.hostAddress)) {
                        listItems += ListViewItem(
                                title = device.friendlyName,
                                summary = device.hostAddress,
                                hidden = "Website#Router",
                                icon = R.drawable.ic_device_router
                        )
                        addresses += device.hostAddress
                    }
                    if (device.server.contains("SimpleHome") && !addresses.contains(device.hostAddress)) {
                        listItems += ListViewItem(
                                title = device.friendlyName,
                                summary = device.hostAddress,
                                hidden = "SimpleHome API#Raspberry Pi",
                                icon = R.drawable.ic_device_raspberry_pi
                        )
                        addresses += device.hostAddress
                    }
                }

                override fun onFinish(devices: HashSet<UPnPDevice>) {}
                override fun onError(e: Exception) {
                    Log.e("UPnPDiscovery", "Error: " + e.localizedMessage)
                }
            })
        }.start()

        //Display found devices
        Handler().postDelayed({
            if (!addresses.contains(routerIp)) {
                listItems += ListViewItem(
                        title = resources.getString(R.string.pref_device_router),
                        summary = routerIp,
                        hidden = "Website#Router",
                        icon = R.drawable.ic_device_router
                )
                addresses += routerIp
            }
            listView.adapter = ListViewAdapter(this, listItems)
        }, 10000)

        //Handle clicks
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            val hidden =  view.findViewById<TextView>(R.id.hidden).text.toString()
            if (hidden != "") {
                val newItem = DeviceItem(devices.generateNewId())
                newItem.name = view.findViewById<TextView>(R.id.title).text.toString()
                newItem.address = view.findViewById<TextView>(R.id.summary).text.toString()
                newItem.mode = hidden.substring(0 , hidden.indexOf("#"))
                newItem.iconName = hidden.substring(hidden.lastIndexOf("#") + 1)
                devices.addDevice(newItem)
                AlertDialog.Builder(this)
                        .setTitle(R.string.pref_add_success)
                        .setMessage(R.string.pref_add_success_message)
                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                        .show()
            }
        }
    }

    private fun intToIp(address: Int): String {
        return (address and 0xFF).toString() + "." + (address shr 8 and 0xFF) + "." + (address shr 16 and 0xFF) + "." + (address shr 24 and 0xFF)
    }
}
