package io.github.domi04151309.home.hue

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.*
import org.json.JSONObject

class HueConnectActivity : AppCompatActivity() {

    private var enabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.setNoActionBar(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hue_connect)

        val queue = Volley.newRequestQueue(this)
        val deviceId = intent.getStringExtra("deviceId") ?: ""
        val url = Devices(this).getDeviceById(deviceId).address + "api"
        val jsonRequestObject = JSONObject("{\"devicetype\":\"HomeApp#" + getHostName() + "\"}")
        val requestToRegisterUser = CustomJsonArrayRequest(Request.Method.POST, url, jsonRequestObject,
                Response.Listener { response ->
                    val responseObject = response.getJSONObject(0)
                    if (responseObject.has("error") && enabled)
                        Toast.makeText(this, R.string.try_again, Toast.LENGTH_LONG).show()
                    else if (responseObject.has("success")) {
                        val username = responseObject.getJSONObject("success").getString("username")
                        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(deviceId, username).apply()
                        finish()
                    }
                    else if (!enabled) enabled = true
                    else Toast.makeText(this, R.string.err, Toast.LENGTH_LONG).show()
                },
                Response.ErrorListener { error ->
                    Toast.makeText(this, R.string.err, Toast.LENGTH_LONG).show()
                    Log.e(Global.LOG_TAG, error.toString())
                }
        )

        queue.add(requestToRegisterUser)

        findViewById<Button>(R.id.continue_btn).setOnClickListener {
            queue.add(requestToRegisterUser)
        }
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
