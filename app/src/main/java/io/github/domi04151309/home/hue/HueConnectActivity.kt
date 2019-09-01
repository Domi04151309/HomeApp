package io.github.domi04151309.home.hue

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.*
import org.json.JSONObject


class HueConnectActivity : AppCompatActivity() {

    private var running = false
    private var activityStarted = false
    private var queue: RequestQueue? = null
    private var requestToRegisterUser: CustomJsonArrayRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.setNoActionBar(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hue_connect)

        queue = Volley.newRequestQueue(this)
        val deviceId = intent.getStringExtra("deviceId") ?: ""
        val url = Devices(this).getDeviceById(deviceId).address + "api"
        val jsonRequestObject = JSONObject("{\"devicetype\":\"HomeApp#" + getHostName() + "\"}")
        requestToRegisterUser = CustomJsonArrayRequest(Request.Method.POST, url, jsonRequestObject,
                Response.Listener { response ->
                    val responseObject = response.getJSONObject(0)
                    if (responseObject.has("success")) {
                        running = false
                        val username = responseObject.getJSONObject("success").getString("username")
                        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(deviceId, username).apply()
                        if (!activityStarted) {
                            startActivity(
                                    Intent(this, MainActivity::class.java)
                                            .putExtra("device", deviceId)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            )
                            activityStarted = true
                        }
                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(this, R.string.err, Toast.LENGTH_LONG).show()
                    Log.e(Global.LOG_TAG, error.toString())
                }
        )

        queue!!.add(requestToRegisterUser)

        findViewById<Button>(R.id.cancel_btn).setOnClickListener {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        running = false
    }

    override fun onResume() {
        super.onResume()
        running = true
        startHandler()
    }

    private fun startHandler() {
        val permissionHandler = Handler()
        val handlerDelay = 1000L
        permissionHandler.postDelayed(object : Runnable {
            override fun run() {
                queue!!.add(requestToRegisterUser)
                Log.e(Global.LOG_TAG, "Sent!")
                if (running) permissionHandler.postDelayed(this, handlerDelay)
            }
        }, handlerDelay)
    }

    private fun getHostName(): String {
        return try {
            val getString = Build::class.java.getDeclaredMethod("getString", String::class.java)
            getString.isAccessible = true
            getString.invoke(null, "net.hostname").toString()
        } catch (ex: Exception) {
            "unknown"
        }
    }
}
