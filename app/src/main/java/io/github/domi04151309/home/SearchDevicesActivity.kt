package io.github.domi04151309.home

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import android.net.wifi.WifiManager
import android.util.Log
import com._8rine.upnpdiscovery.UPnPDevice
import com._8rine.upnpdiscovery.UPnPDiscovery
import android.os.Handler
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import io.github.domi04151309.home.data.ListViewItem

class SearchDevicesActivity : AppCompatActivity() {

    private var listView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        listView = findViewById<View>(R.id.listView) as ListView
        val devices = Devices(PreferenceManager.getDefaultSharedPreferences(this))
        val addresses = mutableListOf<String>()

        val waitItem = ListViewItem(resources.getString(R.string.pref_add_wait))
        waitItem.summary = resources.getString(R.string.pref_add_wait_summary)
        waitItem.icon = R.drawable.ic_info
        val firstAdapter = ListViewAdapter(this, arrayOf(waitItem))
        listView!!.adapter = firstAdapter
        var listItems: Array<ListViewItem> = arrayOf()

        //Get Router
        val manager = super.getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val routerIp = intToIp(manager.dhcpInfo.gateway)
        val routerItem = ListViewItem(resources.getString(R.string.pref_device_router))
        routerItem.summary = routerIp
        routerItem.hidden = "Website#Router"
        routerItem.icon = R.drawable.ic_device_router
        listItems += routerItem
        addresses += routerIp

        //Get Hue Bridges
        val customQuery = "M-SEARCH * HTTP/1.1" + "\r\n" +
                "HOST: 239.255.255.250:1900" + "\r\n" +
                "MAN: ssdp:discover" + "\r\n" +
                "MX: 10" + "\r\n" +
                "ST: ssdp:all" + "\r\n" +
                "\r\n"
        val customPort = 1900
        val customAddress = "239.255.255.250"
        UPnPDiscovery.discoveryDevices(this, object : UPnPDiscovery.OnDiscoveryListener {
            override fun OnStart() {}
            override fun OnFoundNewDevice(device: UPnPDevice) {
                Log.d("UPnPDiscovery", "Found new device: " + device.friendlyName)
                if (device.server.contains("IpBridge") && !addresses.contains(device.hostAddress)) {
                    val deviceItem = ListViewItem(device.friendlyName)
                    deviceItem.summary = device.hostAddress
                    deviceItem.hidden = "Hue API#Lamp"
                    deviceItem.icon = R.drawable.ic_device_lamp
                    listItems += deviceItem
                    addresses += device.hostAddress
                }
            }
            override fun OnFinish(devices: HashSet<UPnPDevice>) {}
            override fun OnError(e: Exception) {
                Log.d("UPnPDiscovery", "Error: " + e.localizedMessage)
            }
        }, customQuery, customAddress, customPort)

        //Get compatible routers
        UPnPDiscovery.discoveryDevices(this, object : UPnPDiscovery.OnDiscoveryListener {
            override fun OnStart() {}
            override fun OnFoundNewDevice(device: UPnPDevice) {
                val friendlyName = device.friendlyName
                Log.d("UPnPDiscovery", "Found new device: $friendlyName")
                if ((friendlyName.contains("FRITZ!Box") || friendlyName.contains("FRITZ!WLAN Repeater")) && !addresses.contains(device.hostAddress)) {
                    val deviceItem = ListViewItem(device.friendlyName)
                    deviceItem.summary = device.hostAddress
                    deviceItem.hidden = "Website#Router"
                    deviceItem.icon = R.drawable.ic_device_router
                    listItems += deviceItem
                    addresses += device.hostAddress
                }
            }
            override fun OnFinish(devices: HashSet<UPnPDevice>) {}
            override fun OnError(e: Exception) {
                Log.d("UPnPDiscovery", "Error: " + e.localizedMessage)
            }
        })

        //Display found devices
        Handler().postDelayed({
            val adapter = ListViewAdapter(this, listItems)
            listView!!.adapter = adapter
        }, 10000)

        //Handle clicks
        listView!!.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            val action =  view.findViewById<TextView>(R.id.hidden).text
            if (action != "") {
                val hidden = view.findViewById<TextView>(R.id.hidden).text.toString()
                devices.addDevice(
                        view.findViewById<TextView>(R.id.title).text.toString(),
                        view.findViewById<TextView>(R.id.summary).text.toString(),
                        hidden.substring(hidden.lastIndexOf("#") + 1),
                        hidden.substring(0 , hidden.indexOf("#"))
                )
                AlertDialog.Builder(this)
                        .setTitle(resources.getString(R.string.pref_add_success))
                        .setMessage(resources.getString(R.string.pref_add_success_message))
                        .setPositiveButton(resources.getString(android.R.string.ok)) { _, _ -> }
                        .show()
            }
        }
    }

    private fun intToIp(address: Int): String {
        return (address and 0xFF).toString() + "." + (address shr 8 and 0xFF) + "." + (address shr 16 and 0xFF) + "." + (address shr 24 and 0xFF)
    }
}
