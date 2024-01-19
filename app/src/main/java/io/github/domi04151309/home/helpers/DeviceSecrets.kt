package io.github.domi04151309.home.helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject

class DeviceSecrets(context: Context, private val id: String) {
    private val masterKeyAlias =
        MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val preferences: SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            "device_secrets",
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    private val secrets =
        JSONObject(
            preferences.getString(id, DEFAULT_JSON)
                ?: DEFAULT_JSON,
        )

    var username: String
        get() = secrets.optString("username")
        set(value) {
            secrets.put("username", value)
        }

    var password: String
        get() = secrets.optString("password")
        set(value) {
            secrets.put("password", value)
        }

    fun updateDeviceSecrets() {
        preferences.edit().putString(id, secrets.toString()).apply()
    }

    fun deleteDeviceSecrets() {
        preferences.edit().remove(id).apply()
    }

    companion object {
        private const val DEFAULT_JSON = """{ "username": "", "password": "" }"""
    }
}
