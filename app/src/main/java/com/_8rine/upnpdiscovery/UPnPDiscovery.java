package com._8rine.upnpdiscovery;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;

public class UPnPDiscovery extends AsyncTask {

    private static final String TAG = UPnPDiscovery.class.getSimpleName();

    private static final int DISCOVER_TIMEOUT = 1500;
    private static final String LINE_END = "\r\n";
    private static final String DEFAULT_QUERY = "M-SEARCH * HTTP/1.1" + LINE_END +
            "HOST: 239.255.255.250:1900" + LINE_END +
            "MAN: \"ssdp:discover\"" + LINE_END +
            "MX: 1"+ LINE_END +
            "ST: ssdp:all" + LINE_END +
            LINE_END;
    private static final String DEFAULT_ADDRESS = "239.255.255.250";

    private final HashSet<UPnPDevice> devices = new HashSet<>();
    private final Context mContext;
    private final Activity mActivity;
    private int mThreadsCount;
    private final String mCustomQuery;
    private final String mInternetAddress;
    private final int mPort;

    public interface OnDiscoveryListener {
        void OnStart();
        void OnFoundNewDevice(UPnPDevice device);
        void OnFinish(HashSet<UPnPDevice> devices);
        void OnError(Exception e);
    }

    private final OnDiscoveryListener mListener;

    private UPnPDiscovery(Activity activity, OnDiscoveryListener listener) {
        mContext = activity.getApplicationContext();
        mActivity = activity;
        mListener = listener;
        mThreadsCount = 0;
        mCustomQuery = DEFAULT_QUERY;
        mInternetAddress = DEFAULT_ADDRESS;
        mPort = 1900;
    }

    private UPnPDiscovery(Activity activity, OnDiscoveryListener listener, String customQuery, String address, int port) {
        mContext = activity.getApplicationContext();
        mActivity = activity;
        mListener = listener;
        mThreadsCount = 0;
        mCustomQuery = customQuery;
        mInternetAddress = address;
        mPort = port;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mListener.OnStart();
            }
        });
        WifiManager wifi = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifi != null) {
            WifiManager.MulticastLock lock = wifi.createMulticastLock("The Lock");
            lock.acquire();
            DatagramSocket socket = null;
            try {
                InetAddress group = InetAddress.getByName(mInternetAddress);
                int port = mPort;
                String query = mCustomQuery;
                socket = new DatagramSocket(port);
                socket.setReuseAddress(true);

                DatagramPacket datagramPacketRequest = new DatagramPacket(query.getBytes(), query.length(), group, port);
                socket.send(datagramPacketRequest);

                long time = System.currentTimeMillis();
                long curTime = System.currentTimeMillis();

                while (curTime - time < 1000) {
                    DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
                    socket.receive(datagramPacket);
                    String response = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                    if (response.substring(0, 12).toUpperCase().equals("HTTP/1.1 200")) {
                        UPnPDevice device = new UPnPDevice(datagramPacket.getAddress().getHostAddress(), response);
                        mThreadsCount++;
                        getData(device.getLocation(), device);
                    }
                    curTime = System.currentTimeMillis();
                }

            } catch (final IOException e) {
                e.printStackTrace();
                mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        mListener.OnError(e);
                    }
                });
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
            lock.release();
        }
        return null;
    }

    private void getData(final String url, final UPnPDevice device) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        device.update(response);
                        mListener.OnFoundNewDevice(device);
                        devices.add(device);
                        mThreadsCount--;
                        if (mThreadsCount == 0) {
                            mActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    mListener.OnFinish(devices);
                                }
                            });
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mThreadsCount--;
                Log.d(TAG, "URL: " + url + " get content error!");
            }
        });
        stringRequest.setTag(TAG + "SSDP description request");
        Volley.newRequestQueue(mContext).add(stringRequest);
    }

    public static boolean discoveryDevices(Activity activity, OnDiscoveryListener listener) {
        UPnPDiscovery discover = new UPnPDiscovery(activity, listener);
        discover.execute();
        try {
            Thread.sleep(DISCOVER_TIMEOUT);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public static boolean discoveryDevices(Activity activity, OnDiscoveryListener listener, String customQuery, String address, int port) {
        UPnPDiscovery discover = new UPnPDiscovery(activity, listener, customQuery, address, port);
        discover.execute();
        try {
            Thread.sleep(DISCOVER_TIMEOUT);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

}