package io.github.domi04151309.home

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import android.net.wifi.WifiManager
import android.util.Log
import com._8rine.upnpdiscovery.UPnPDevice
import com._8rine.upnpdiscovery.UPnPDiscovery
import org.json.JSONException
import android.os.Handler
import android.preference.PreferenceManager
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.widget.AdapterView
import android.widget.TextView

class SearchDevicesActivity : AppCompatActivity() {

    private var listView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        listView = findViewById<View>(R.id.listView) as ListView
        val devices = Devices(PreferenceManager.getDefaultSharedPreferences(this))
        val addresses = mutableListOf<String>()
        val modes = mutableListOf<String>()
        val icons = mutableListOf<String>()

        val firstTitle: Array<String?> = arrayOfNulls(1)
        val firstSummary: Array<String?> = arrayOfNulls(1)
        val firstDrawable = IntArray(1)
        firstTitle[0] = resources.getString(R.string.pref_add_wait)
        firstSummary[0] = resources.getString(R.string.pref_add_wait_summary)
        firstDrawable[0] = R.drawable.ic_info
        val firstAdapter = ListAdapter(this, firstTitle, firstSummary, firstDrawable)
        listView!!.adapter = firstAdapter

        //Get Router
        val manager = super.getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        addresses += intToIp(manager.dhcpInfo.gateway)
        modes += "Website"
        icons += "Router"

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
                    addresses += device.hostAddress
                    modes += "Hue Bridge"
                    icons += "Lamp"
                }
            }
            override fun OnFinish(devices: HashSet<UPnPDevice>) {}
            override fun OnError(e: Exception) {
                Log.d("UPnPDiscovery", "Error: " + e.localizedMessage)
            }
        }, customQuery, customAddress, customPort)

        //Display found devices
        Handler().postDelayed({
            val devicesNumber = addresses.size
            val titles: Array<String?> = arrayOfNulls(devicesNumber)
            val summaries: Array<String?> = arrayOfNulls(devicesNumber)
            val hidden: Array<String?> = arrayOfNulls(devicesNumber)
            val drawables = IntArray(devicesNumber)
            titles[0] = resources.getString(R.string.pref_device_router)
            summaries[0] = addresses[0]
            hidden[0] = modes[0] + "#" + icons[0]
            drawables[0] = R.drawable.ic_device_router
            var i = 1
            while (i < devicesNumber) {
                try {
                    titles[i] = resources.getString(R.string.pref_device_hue_bridge)
                    summaries[i] = addresses[i]
                    hidden[i] = modes[i] + "#" + icons[i]
                    drawables[i] = R.drawable.ic_device_lamp
                } catch (e: JSONException) {
                    Log.e(Global.LOG_TAG, e.toString())
                }
                i++
            }
            val adapter = ListAdapter(this, titles, summaries, hidden, drawables)
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
