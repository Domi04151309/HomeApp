package com.rine.upnpdiscovery

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

@Suppress("MagicNumber")
class UPnPDiscovery : AsyncTask<Activity, UPnPDiscovery.OnDiscoveryListener, Void> {
    private val devices = HashSet<UPnPDevice>()

    @SuppressLint("StaticFieldLeak")
    private val mContext: Context
    private var mThreadsCount: Int = 0
    private val mCustomQuery: String
    private val mInternetAddress: String
    private val mPort: Int

    private val mListener: OnDiscoveryListener

    interface OnDiscoveryListener {
        fun onStart()

        fun onFoundNewDevice(device: UPnPDevice)

        fun onFinish(devices: HashSet<UPnPDevice>)

        fun onError(e: Exception)
    }

    private constructor(activity: Activity, listener: OnDiscoveryListener) {
        mContext = activity.applicationContext
        mListener = listener
        mThreadsCount = 0
        mCustomQuery = DEFAULT_QUERY
        mInternetAddress = DEFAULT_ADDRESS
        mPort = 1900
    }

    private constructor(
        activity: Activity,
        listener: OnDiscoveryListener,
        customQuery: String,
        address: String,
        port: Int,
    ) {
        mContext = activity.applicationContext
        mListener = listener
        mThreadsCount = 0
        mCustomQuery = customQuery
        mInternetAddress = address
        mPort = port
    }

    override fun doInBackground(vararg p0: Activity?): Void? {
        mListener.onStart()
        val wifi = mContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("The Lock")
        lock.acquire()
        var socket: DatagramSocket? = null
        try {
            val group = InetAddress.getByName(mInternetAddress)
            val port = mPort
            val query = mCustomQuery
            socket = DatagramSocket(null)
            socket.reuseAddress = true
            socket.broadcast = true
            socket.bind(InetSocketAddress(port))

            val datagramPacketRequest = DatagramPacket(query.toByteArray(), query.length, group, port)
            socket.send(datagramPacketRequest)

            val time = System.currentTimeMillis()
            var curTime = System.currentTimeMillis()

            while (curTime - time < 1000) {
                val datagramPacket = DatagramPacket(ByteArray(1024), 1024)
                socket.receive(datagramPacket)
                val response = String(datagramPacket.data, 0, datagramPacket.length)
                if (response.substring(0, 12).uppercase() == "HTTP/1.1 200") {
                    val device = UPnPDevice(datagramPacket.address.hostAddress ?: continue, response)
                    mThreadsCount++
                    getData(device.location, device)
                }
                curTime = System.currentTimeMillis()
            }
        } catch (e: IOException) {
            mListener.onError(e)
        } finally {
            socket?.close()
        }
        lock.release()
        return null
    }

    private fun getData(
        url: String,
        device: UPnPDevice,
    ) {
        val stringRequest =
            StringRequest(
                Request.Method.GET,
                url,
                { response ->
                    device.update(response)
                    mListener.onFoundNewDevice(device)
                    devices.add(device)
                    mThreadsCount--
                    if (mThreadsCount == 0) {
                        mListener.onFinish(devices)
                    }
                },
                {
                    mThreadsCount--
                    Log.e(TAG, "URL: $url get content error!")
                },
            )
        stringRequest.tag = TAG + "SSDP description request"
        Volley.newRequestQueue(mContext).add(stringRequest)
    }

    companion object {
        internal val TAG: String = UPnPDiscovery::class.java.simpleName

        private const val DISCOVER_TIMEOUT = 1500
        private const val LINE_END = "\r\n"
        private const val DEFAULT_QUERY =
            "M-SEARCH * HTTP/1.1" + LINE_END +
                "HOST: 239.255.255.250:1900" + LINE_END +
                "MAN: \"ssdp:discover\"" + LINE_END +
                "MX: 1" + LINE_END +
                "ST: ssdp:all" + LINE_END +
                LINE_END
        private const val DEFAULT_ADDRESS = "239.255.255.250"

        fun discoveryDevices(
            activity: Activity,
            listener: OnDiscoveryListener,
        ): Boolean {
            val discover = UPnPDiscovery(activity, listener)
            discover.execute()
            return try {
                Thread.sleep(DISCOVER_TIMEOUT.toLong())
                true
            } catch (e: InterruptedException) {
                false
            }
        }

        fun discoveryDevices(
            activity: Activity,
            listener: OnDiscoveryListener,
            customQuery: String,
            address: String,
            port: Int,
        ): Boolean {
            val discover = UPnPDiscovery(activity, listener, customQuery, address, port)
            discover.execute()
            return try {
                Thread.sleep(DISCOVER_TIMEOUT.toLong())
                true
            } catch (e: InterruptedException) {
                false
            }
        }
    }
}
