package io.github.domi04151309.home.activities

import android.content.Context
import android.icu.lang.UCharacter
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.net.wifi.WifiManager
import android.util.Log
import com._8rine.upnpdiscovery.UPnPDevice
import com._8rine.upnpdiscovery.UPnPDiscovery
import android.view.View
import androidx.appcompat.app.AlertDialog
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.DeviceDiscoveryListAdapter
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.data.SimpleListItem
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.Theme
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface
import java.net.InetAddress

class SearchDevicesActivity : AppCompatActivity(), RecyclerViewHelperInterface {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeviceDiscoveryListAdapter
    private lateinit var devices: Devices

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        recyclerView = findViewById(R.id.recyclerView)
        adapter = DeviceDiscoveryListAdapter(arrayListOf(SimpleListItem(
            title = resources.getString(R.string.pref_add_search),
            summary = resources.getString(R.string.pref_add_search_summary),
            icon = R.drawable.ic_search
        )), this)
        devices = Devices(this)
        val addresses = mutableListOf<String>()

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        //Device variables
        val manager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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
            //Add Router
            adapter.add(SimpleListItem(
                title = resources.getString(R.string.pref_device_router),
                summary = routerIp,
                hidden = "Website#Router",
                icon = R.drawable.ic_device_router
            ))
            addresses += routerIp

            //Get Hue Bridges
            UPnPDiscovery.discoveryDevices(this, object : UPnPDiscovery.OnDiscoveryListener {
                override fun onStart() {}
                override fun onFoundNewDevice(device: UPnPDevice) {
                    if (device.server.contains("IpBridge") && !addresses.contains(device.hostAddress)) {
                        adapter.add(SimpleListItem(
                                title = device.friendlyName,
                                summary = device.hostAddress,
                                hidden = "Hue API#Lamp",
                                icon = R.drawable.ic_device_lamp
                        ))
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
                        adapter.add(SimpleListItem(
                                title = device.friendlyName,
                                summary = device.hostAddress,
                                hidden = "Website#Router",
                                icon = R.drawable.ic_device_router
                        ))
                        addresses += device.hostAddress
                    }
                    if (device.server.contains("SimpleHome") && !addresses.contains(device.hostAddress)) {
                        adapter.add(SimpleListItem(
                                title = device.friendlyName,
                                summary = device.hostAddress,
                                hidden = "SimpleHome API#Raspberry Pi",
                                icon = R.drawable.ic_device_raspberry_pi
                        ))
                        addresses += device.hostAddress
                    }
                }

                override fun onFinish(devices: HashSet<UPnPDevice>) {}
                override fun onError(e: Exception) {
                    Log.e("UPnPDiscovery", "Error: " + e.localizedMessage)
                }
            })
        }.start()

        val nsdManager = (getSystemService(NSD_SERVICE) as NsdManager)
        class mDNSresolve : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(Global.LOG_TAG, "Resolve failed: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.e(Global.LOG_TAG, "Resolve Succeeded. $serviceInfo")
                val host: String = serviceInfo.host.getHostAddress()
                Log.e(Global.LOG_TAG, "gen" + serviceInfo.attributes.get("gen").toString())
                runOnUiThread(java.lang.Runnable {
                    adapter.add(SimpleListItem(
                            title = serviceInfo.serviceName,
                            summary = host,
                            hidden = "Shelly Gen 2#Lamp",
                            icon = R.drawable.ic_device_lamp
                    ))
                })
            }
        }

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(p0: String?, p1: Int) {
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(p0: String?, p1: Int) {
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onDiscoveryStarted(p0: String?) {
            }

            override fun onDiscoveryStopped(p0: String?) {
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(Global.LOG_TAG, "found")
                if (service.serviceName.lowercase().startsWith("shelly")) {
                    nsdManager.resolveService(service, mDNSresolve())
                }
            }

            override fun onServiceLost(p0: NsdServiceInfo?) {
            }
        }
        nsdManager.discoverServices("_http._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun intToIp(address: Int): String {
        return (address and 0xFF).toString() + "." + (address shr 8 and 0xFF) + "." + (address shr 16 and 0xFF) + "." + (address shr 24 and 0xFF)
    }

    override fun onItemClicked(view: View, position: Int) {
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
