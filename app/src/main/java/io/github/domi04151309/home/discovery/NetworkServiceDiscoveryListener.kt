package io.github.domi04151309.home.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.appcompat.app.AppCompatActivity

class NetworkServiceDiscoveryListener(
    context: Context,
    private val resolveListener: NetworkServiceResolveListener,
) : NsdManager.DiscoveryListener {
    private val nsdManager = context.getSystemService(AppCompatActivity.NSD_SERVICE) as NsdManager

    override fun onStartDiscoveryFailed(
        p0: String?,
        p1: Int,
    ) {
        nsdManager.stopServiceDiscovery(this)
    }

    override fun onStopDiscoveryFailed(
        p0: String?,
        p1: Int,
    ) {
        nsdManager.stopServiceDiscovery(this)
    }

    override fun onDiscoveryStarted(p0: String?) {
        // Do nothing.
    }

    override fun onDiscoveryStopped(p0: String?) {
        // Do nothing.
    }

    override fun onServiceFound(service: NsdServiceInfo) {
        val serviceName = service.serviceName.lowercase()
        if ((serviceName.startsWith("shelly") && !serviceName.startsWith("shellybutton1")) ||
            service.serviceType.equals("_simplehome._tcp.")
        ) {
            if (resolveListener.isBusy.compareAndSet(false, true)) {
                nsdManager.resolveService(service, resolveListener)
            } else {
                resolveListener.pendingServices.add(service)
            }
        }
    }

    override fun onServiceLost(service: NsdServiceInfo) {
        val iterator = resolveListener.pendingServices.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().serviceName == service.serviceName) {
                iterator.remove()
            }
        }
    }
}
