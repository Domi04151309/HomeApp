package io.github.domi04151309.home.activities

import android.content.Context
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rine.upnpdiscovery.UPnPDiscovery
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.DeviceDiscoveryListAdapter
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.discovery.NetworkServiceDiscoveryListener
import io.github.domi04151309.home.discovery.NetworkServiceResolveListener
import io.github.domi04151309.home.discovery.UPnPListener
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface

class SearchDevicesActivity : BaseActivity(), RecyclerViewHelperInterface {
    private lateinit var adapter: DeviceDiscoveryListAdapter
    private lateinit var devices: Devices
    private lateinit var nsdManager: NsdManager
    private lateinit var discoveryListenerHttp: NsdManager.DiscoveryListener
    private lateinit var discoveryListenerSimpleHome: NsdManager.DiscoveryListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter =
            DeviceDiscoveryListAdapter(
                mutableListOf(
                    ListViewItem(
                        title = resources.getString(R.string.pref_add_search),
                        summary = resources.getString(R.string.pref_add_search_summary),
                        icon = R.drawable.ic_search,
                    ),
                ),
                this,
            )
        devices = Devices(this)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Device variables
        val manager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val routerIp = intToIp(manager.dhcpInfo.gateway)

        Thread {
            // Add Router
            adapter.add(
                ListViewItem(
                    title = resources.getString(R.string.pref_device_router),
                    summary = routerIp,
                    hidden = "Website#Router",
                    icon = R.drawable.ic_device_router,
                    state = devices.addressExists(routerIp),
                ),
            )

            // Get compatible devices
            UPnPDiscovery.discoveryDevices(
                this,
                UPnPListener(this, adapter),
            )
        }.start()

        val resolveListener = NetworkServiceResolveListener(this, adapter)
        nsdManager = getSystemService(NSD_SERVICE) as NsdManager
        discoveryListenerHttp = NetworkServiceDiscoveryListener(this, resolveListener)
        discoveryListenerSimpleHome = NetworkServiceDiscoveryListener(this, resolveListener)
        nsdManager.discoverServices("_http._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListenerHttp)
        nsdManager.discoverServices("_simplehome._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListenerSimpleHome)
    }

    @Suppress("MagicNumber")
    private fun intToIp(address: Int): String =
        (address and 0xFF).toString() + "." + (address shr 8 and 0xFF) + "." +
            (address shr 16 and 0xFF) + "." + (address shr 24 and 0xFF)

    override fun onItemClicked(
        view: View,
        position: Int,
    ) {
        val name = view.findViewById<TextView>(R.id.title).text.toString()
        val hidden = view.findViewById<TextView>(R.id.hidden).text.toString()
        if (hidden != "") {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pref_add_dialog)
                .setMessage(resources.getString(R.string.pref_add_dialog_message, name))
                .setPositiveButton(R.string.str_add) { _, _ ->
                    val newItem =
                        DeviceItem(
                            devices.generateNewId(),
                            name,
                            hidden.substring(0, hidden.indexOf('#')),
                            hidden.substring(hidden.lastIndexOf('#') + 1),
                        )
                    newItem.address = view.findViewById<TextView>(R.id.summary).text.toString()
                    devices.addDevice(newItem)
                    adapter.changeState(position, true)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nsdManager.stopServiceDiscovery(discoveryListenerHttp)
        nsdManager.stopServiceDiscovery(discoveryListenerSimpleHome)
    }
}
