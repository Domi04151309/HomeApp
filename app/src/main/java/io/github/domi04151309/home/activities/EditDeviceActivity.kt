package io.github.domi04151309.home.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.IconSpinnerAdapter
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.helpers.DeviceSecrets
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.Theme

class EditDeviceActivity : AppCompatActivity() {
    companion object {
        private val SUPPORTS_DIRECT_VIEW =
            arrayOf(
                "ESP Easy",
                "Hue API",
                "Shelly Gen 1",
                "Shelly Gen 2",
                "SimpleHome API",
                "Tasmota",
            )
        private val HAS_CONFIG =
            arrayOf(
                "Hue API",
                "ESP Easy",
                "Node-RED",
                "Shelly Gen 1",
                "Shelly Gen 2",
            )
        private val HAS_INFO =
            arrayOf(
                "Hue API",
                "Shelly Gen 2",
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_device)

        val devices = Devices(this)
        var deviceId = intent.getStringExtra("deviceId")
        val editing =
            if (deviceId == null) {
                deviceId = devices.generateNewId()
                false
            } else {
                true
            }

        val deviceSecrets = DeviceSecrets(this, deviceId)

        val deviceIcn = findViewById<ImageView>(R.id.deviceIcn)
        val nameTxt = findViewById<TextView>(R.id.nameTxt)
        val nameBox = findViewById<TextInputLayout>(R.id.nameBox)
        val addressBox = findViewById<TextInputLayout>(R.id.addressBox)
        val iconSpinner = findViewById<TextInputLayout>(R.id.iconSpinner).editText as AutoCompleteTextView
        val modeSpinner = findViewById<TextInputLayout>(R.id.modeSpinner).editText as AutoCompleteTextView
        val specialDivider = findViewById<View>(R.id.specialDivider)
        val specialSection = findViewById<LinearLayout>(R.id.specialSection)
        val usernameBox = findViewById<TextInputLayout>(R.id.usernameBox)
        val passwordBox = findViewById<TextInputLayout>(R.id.passwordBox)
        val configHide = findViewById<CheckBox>(R.id.configHide)
        val configDirectView = findViewById<CheckBox>(R.id.configDirectView)
        val configBtn = findViewById<Button>(R.id.configBtn)
        val infoBtn = findViewById<Button>(R.id.infoBtn)

        findViewById<TextView>(R.id.idTxt).text = (resources.getString(R.string.pref_add_id, deviceId))

        iconSpinner.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {}

                override fun onTextChanged(
                    s: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                    deviceIcn.setImageResource(Global.getIcon(s.toString()))
                }
            },
        )
        modeSpinner.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {}

                override fun onTextChanged(
                    s: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                    val string = s.toString()
                    val specialVisibility = if (string == "Fritz! Auto-Login" || string == "Shelly Gen 1") View.VISIBLE else View.GONE
                    val usernameVisibility = if (string == "Shelly Gen 1") View.VISIBLE else View.GONE
                    specialDivider.visibility = specialVisibility
                    specialSection.visibility = specialVisibility
                    usernameBox.visibility = usernameVisibility

                    if (SUPPORTS_DIRECT_VIEW.contains(string)) {
                        configDirectView.isEnabled = true
                    } else {
                        configDirectView.isEnabled = false
                        configDirectView.isChecked = false
                    }

                    if (editing) {
                        configBtn.visibility =
                            if (HAS_CONFIG.contains(string)) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                        infoBtn.visibility =
                            if (HAS_INFO.contains(string)) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                    }
                }
            },
        )
        nameBox.editText?.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {}

                override fun onTextChanged(
                    s: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                    val string = s.toString()
                    if (string == "") {
                        nameTxt.text = resources.getString(R.string.pref_add_name_empty)
                    } else {
                        nameTxt.text = string
                    }
                }
            },
        )

        if (editing) {
            title = resources.getString(R.string.pref_edit_device)
            val deviceObj = devices.getDeviceById(deviceId)
            nameBox.editText?.setText(deviceObj.name)
            addressBox.editText?.setText(deviceObj.address)
            iconSpinner.setText(deviceObj.iconName)
            modeSpinner.setText(deviceObj.mode)
            usernameBox.editText?.setText(deviceSecrets.username)
            passwordBox.editText?.setText(deviceSecrets.password)
            configHide.isChecked = deviceObj.hide
            configDirectView.isChecked = deviceObj.directView

            configBtn.setOnClickListener {
                when (modeSpinner.text.toString()) {
                    "ESP Easy", "Shelly Gen 1", "Shelly Gen 2" -> {
                        startActivity(
                            Intent(this, WebActivity::class.java)
                                .putExtra("URI", addressBox.editText?.text.toString())
                                .putExtra("title", resources.getString(R.string.pref_device_config)),
                        )
                    }
                    "Node-RED" -> {
                        startActivity(
                            Intent(this, WebActivity::class.java)
                                .putExtra("URI", formatNodeREDAddress(addressBox.editText?.text.toString()))
                                .putExtra("title", resources.getString(R.string.pref_device_config)),
                        )
                    }
                    "Hue API" -> {
                        val huePackageName = "com.philips.lighting.hue2"
                        val launchIntent = packageManager.getLaunchIntentForPackage(huePackageName)
                        if (launchIntent == null) {
                            try {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=$huePackageName"),
                                    ),
                                )
                            } catch (e: ActivityNotFoundException) {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=$huePackageName"),
                                    ),
                                )
                            }
                        } else {
                            startActivity(launchIntent)
                        }
                    }
                }
            }

            infoBtn.setOnClickListener {
                startActivity(Intent(this, DeviceInfoActivity::class.java).putExtra("device", deviceId))
            }

            findViewById<Button>(R.id.shortcutBtn).setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val shortcutManager = this.getSystemService(ShortcutManager::class.java)
                    if (shortcutManager != null) {
                        if (shortcutManager.isRequestPinShortcutSupported) {
                            val shortcut =
                                ShortcutInfo.Builder(this, deviceId)
                                    .setShortLabel(
                                        deviceObj.name.ifEmpty {
                                            resources.getString(R.string.pref_add_name_empty)
                                        },
                                    )
                                    .setLongLabel(
                                        deviceObj.name.ifEmpty {
                                            resources.getString(R.string.pref_add_name_empty)
                                        },
                                    )
                                    .setIcon(Icon.createWithResource(this, deviceObj.iconId))
                                    .setIntent(
                                        Intent(this, MainActivity::class.java)
                                            .putExtra("device", deviceId)
                                            .setAction(Intent.ACTION_MAIN)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                                    )
                                    .build()
                            shortcutManager.requestPinShortcut(shortcut, null)
                        } else {
                            Toast.makeText(this, R.string.pref_add_shortcut_failed, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.pref_add_shortcut_failed, Toast.LENGTH_LONG).show()
                }
            }

            findViewById<Button>(R.id.deleteBtn).setOnClickListener {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.str_delete)
                    .setMessage(R.string.pref_delete_device_question)
                    .setPositiveButton(R.string.str_delete) { _, _ ->
                        devices.deleteDevice(deviceId)
                        finish()
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
            }
        } else {
            iconSpinner.setText(resources.getStringArray(R.array.pref_icons)[0])
            modeSpinner.setText(resources.getStringArray(R.array.pref_add_mode_array)[0])
            findViewById<View>(R.id.editDivider).visibility = View.GONE
            findViewById<LinearLayout>(R.id.editSection).visibility = View.GONE
        }

        iconSpinner.setAdapter(IconSpinnerAdapter(resources.getStringArray(R.array.pref_icons)))
        modeSpinner.setAdapter(ArrayAdapter(this, R.layout.dropdown_item, resources.getStringArray(R.array.pref_add_mode_array)))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            val name = nameBox.editText?.text.toString()
            if (name == "") {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.err_missing_name)
                    .setMessage(R.string.err_missing_name_summary)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .show()
                return@setOnClickListener
            } else if (addressBox.editText?.text.toString() == "") {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.err_missing_address)
                    .setMessage(R.string.err_missing_address_summary)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .show()
                return@setOnClickListener
            }

            val tempAddress =
                if (modeSpinner.text.toString() == "Node-RED") {
                    formatNodeREDAddress(addressBox.editText?.text.toString())
                } else {
                    addressBox.editText?.text.toString()
                }

            val newItem = DeviceItem(deviceId)
            newItem.name = name
            newItem.address = tempAddress
            newItem.mode = modeSpinner.text.toString()
            newItem.iconName = iconSpinner.text.toString()
            newItem.hide = configHide.isChecked
            newItem.directView = configDirectView.isChecked
            devices.addDevice(newItem)
            deviceSecrets.username = usernameBox.editText?.text.toString()
            deviceSecrets.password = passwordBox.editText?.text.toString()
            deviceSecrets.updateDeviceSecrets()
            finish()
        }
    }

    private fun formatNodeREDAddress(url: String): String {
        var result = url
        if (!result.contains(":1880")) {
            if (result.endsWith('/')) result = result.dropLast(1)
            result += ":1880/"
        }
        return result
    }
}
