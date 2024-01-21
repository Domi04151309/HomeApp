package io.github.domi04151309.home.discovery

import android.app.Activity
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.DeviceDiscoveryListAdapter
import io.github.domi04151309.home.api.ShellyAPI
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.helpers.Devices
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class NetworkServiceResolveListener(
    private val activity: Activity,
    private val adapter: DeviceDiscoveryListAdapter,
) : NsdManager.ResolveListener {
    private val devices = Devices(activity)
    private val queue = Volley.newRequestQueue(activity)
    private val nsdManager = activity.getSystemService(AppCompatActivity.NSD_SERVICE) as NsdManager

    var isBusy: AtomicBoolean = AtomicBoolean(false)
    var pendingServices: ConcurrentLinkedQueue<NsdServiceInfo> = ConcurrentLinkedQueue<NsdServiceInfo>()

    override fun onResolveFailed(
        service: NsdServiceInfo,
        p1: Int,
    ) {
        resolveNextInQueue()
    }

    override fun onServiceResolved(service: NsdServiceInfo) {
        activity.runOnUiThread {
            if (service.serviceType.equals("._simplehome._tcp")) {
                val url =
                    service.attributes["url"]?.decodeToString()
                        ?: service.host.hostAddress
                adapter.add(
                    ListViewItem(
                        title = service.serviceName,
                        summary = url,
                        hidden = "SimpleHome API#Raspberry Pi",
                        icon = R.drawable.ic_device_raspberry_pi,
                        state = devices.addressExists(url),
                    ),
                )
            } else {
                val pos =
                    adapter.add(
                        ListViewItem(
                            title = service.serviceName,
                            summary = service.host.hostAddress ?: "",
                            hidden = "Shelly Gen ${
                                service.attributes["gen"]?.decodeToString() ?: "1"
                            }#Lamp",
                            icon = R.drawable.ic_device_lamp,
                            state = devices.addressExists(service.host.hostAddress ?: ""),
                        ),
                    )

                queue.add(
                    ShellyAPI.loadName(
                        "http://" + service.host.hostAddress + "/",
                        service.attributes["gen"]?.decodeToString()?.toInt() ?: 1,
                    ) { name ->
                        if (name.isNotEmpty()) adapter.changeTitle(pos, name)
                    },
                )
            }
        }
        resolveNextInQueue()
    }

    private fun resolveNextInQueue() {
        val nextNsdService = pendingServices.poll()
        if (nextNsdService != null) {
            nsdManager.resolveService(nextNsdService, this)
        } else {
            isBusy.set(false)
        }
    }
}
