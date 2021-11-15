package io.github.domi04151309.home.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.*
import io.github.domi04151309.home.custom.CustomJsonArrayRequest
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.Theme
import org.json.JSONObject

class HueConnectActivity : AppCompatActivity() {

    private var activityStarted = false
    internal var running = false
    internal lateinit var queue: RequestQueue
    internal lateinit var requestToRegisterUser: CustomJsonArrayRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.setNoActionBar(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hue_connect)

        queue = Volley.newRequestQueue(this)
        val deviceId = intent.getStringExtra("deviceId") ?: ""
        val jsonRequestObject = JSONObject("{\"devicetype\":\"HomeApp#${android.os.Build.PRODUCT}\"}")
        requestToRegisterUser = CustomJsonArrayRequest(Request.Method.POST, Devices(this).getDeviceById(deviceId).address + "api", jsonRequestObject,
                { response ->
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
                { error ->
                    Toast.makeText(this, R.string.err, Toast.LENGTH_LONG).show()
                    Log.e(Global.LOG_TAG, error.toString())
                }
        )

        queue.add(requestToRegisterUser)

        findViewById<Button>(R.id.cancel_btn).setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        running = true
        startHandler()
    }

    override fun onStop() {
        super.onStop()
        running = false
    }

    private fun startHandler() {
        val permissionHandler = Handler(Looper.getMainLooper())
        val handlerDelay = 1000L
        permissionHandler.postDelayed(object : Runnable {
            override fun run() {
                queue.add(requestToRegisterUser)
                if (running) permissionHandler.postDelayed(this, handlerDelay)
            }
        }, handlerDelay)
    }
}
