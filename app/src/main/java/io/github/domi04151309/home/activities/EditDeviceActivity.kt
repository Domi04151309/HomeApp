package io.github.domi04151309.home.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.IconSpinnerAdapter
import io.github.domi04151309.home.custom.TextWatcher
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.helpers.DeviceSecrets
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.Rooms

class EditDeviceActivity : BaseActivity() {
    private lateinit var devices: Devices
    private lateinit var rooms: Rooms
    private lateinit var deviceId: String
    private var deviceSecrets: DeviceSecrets? = null
    private lateinit var deviceIcon: ImageView
    private lateinit var nameText: TextView
    private lateinit var nameBox: TextInputLayout
    private lateinit var addressBox: TextInputLayout
    private lateinit var iconSpinner: AutoCompleteTextView
    private lateinit var modeSpinner: AutoCompleteTextView
    private lateinit var roomSpinner: AutoCompleteTextView
    private lateinit var editRoomSpinner: AutoCompleteTextView
    private lateinit var specialDivider: View
    private lateinit var specialSection: LinearLayout
    private lateinit var usernameBox: TextInputLayout
    private lateinit var passwordBox: TextInputLayout
    private lateinit var configHide: CheckBox
    private lateinit var configDirectView: CheckBox
    private lateinit var configButton: Button
    private lateinit var infoButton: Button
    private var selectedRoomId: String = ""
    private var editSelectedRoomId: String = ""
    private var editing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_edit_device)
        } catch (e: Exception) {
            Log.e("EditDeviceActivity", "Error setting content view", e)
            Toast.makeText(this, "Error loading UI: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        applyBottomInsetPadding(findViewById(R.id.scrollView))
        applyBottomInsetMargin(fab)

        devices = Devices(this)
        rooms = Rooms(this)
        var deviceId = intent.getStringExtra("deviceId")
        editing =
            if (deviceId == null) {
                deviceId = devices.generateNewId()
                false
            } else {
                true
            }
        this.deviceId = deviceId

        try {
            deviceSecrets = DeviceSecrets(this, deviceId)
        } catch (e: Exception) {
            Log.e("EditDeviceActivity", "Error initializing DeviceSecrets", e)
            // Continue without encrypted secrets
        }

        deviceIcon = findViewById(R.id.deviceIcn)
        nameText = findViewById(R.id.nameTxt)
        nameBox = findViewById(R.id.nameBox)
        addressBox = findViewById(R.id.addressBox)
        iconSpinner = findViewById<AutoCompleteTextView>(R.id.iconSpinnerAutoComplete)
        modeSpinner = findViewById<AutoCompleteTextView>(R.id.modeSpinnerAutoComplete)
        specialDivider = findViewById(R.id.specialDivider)
        specialSection = findViewById(R.id.specialSection)
        usernameBox = findViewById(R.id.usernameBox)
        passwordBox = findViewById(R.id.passwordBox)
        configHide = findViewById(R.id.configHide)
        configDirectView = findViewById(R.id.configDirectView)
        configButton = findViewById(R.id.configBtn)
        infoButton = findViewById(R.id.infoBtn)

        findViewById<TextView>(R.id.idTxt).text = resources.getString(R.string.pref_add_id, deviceId)

        iconSpinner.addTextChangedListener(getIconTextWatcher())
        modeSpinner.addTextChangedListener(getModeTextWatcher(editing))
        nameBox.editText?.addTextChangedListener(getNameTextWatcher())

        iconSpinner.setAdapter(IconSpinnerAdapter(resources.getStringArray(R.array.pref_icons)))
        modeSpinner.setAdapter(
            ArrayAdapter(
                this,
                R.layout.dropdown_item,
                resources.getStringArray(R.array.pref_add_mode_array),
            ),
        )
        setupRoomSpinner()
        setupEditRoomSpinner()

        // Disable filtering for dropdown spinners to show all items
        iconSpinner.isFocusable = false
        iconSpinner.isFocusableInTouchMode = false
        iconSpinner.inputType = android.text.InputType.TYPE_NULL
        modeSpinner.isFocusable = false
        modeSpinner.isFocusableInTouchMode = false
        modeSpinner.inputType = android.text.InputType.TYPE_NULL

        if (editing) {
            // Hide main room spinner when editing - use the one in edit section instead
            findViewById<TextInputLayout>(R.id.roomSpinner).visibility = View.GONE
            onEditDevice()
        } else {
            onCreateDevice()
        }

        fab.setOnClickListener {
            onFloatingActionButtonClicked()
        }

        findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun getIconTextWatcher() =
        TextWatcher {
            deviceIcon.setImageResource(Global.getIcon(it))
        }

    private fun showExternalInfoBasedOnMode(mode: String) {
        configButton.visibility =
            if (HAS_CONFIG.contains(mode)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        infoButton.visibility =
            if (HAS_INFO.contains(mode)) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    @Suppress("ComplexCondition")
    private fun getModeTextWatcher(editing: Boolean) =
        TextWatcher {
            val specialVisibility =
                if (
                    it == Global.FRITZ_AUTO_LOGIN ||
                    it == Global.GRAFANA_AUTO_LOGIN ||
                    it == Global.PI_HOLE_AUTO_LOGIN ||
                    it == Global.SHELLY_GEN_1
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            val usernameVisibility =
                if (
                    it == Global.GRAFANA_AUTO_LOGIN ||
                    it == Global.SHELLY_GEN_1
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            specialDivider.visibility = specialVisibility
            specialSection.visibility = specialVisibility
            usernameBox.visibility = usernameVisibility

            if (SUPPORTS_DIRECT_VIEW.contains(it)) {
                configDirectView.isEnabled = true
            } else {
                configDirectView.isEnabled = false
                configDirectView.isChecked = false
            }

            if (editing) {
                showExternalInfoBasedOnMode(it)
            }
        }

    private fun getNameTextWatcher() =
        TextWatcher {
            if (it == "") {
                nameText.text = resources.getString(R.string.pref_add_name_empty)
            } else {
                nameText.text = it
            }
        }

    private fun onEditDevice() {
        val device = devices.getDeviceById(deviceId)
        nameBox.editText?.setText(device.name)
        addressBox.editText?.setText(device.address)
        iconSpinner.setText(device.iconName, false)
        modeSpinner.setText(device.mode, false)
        selectedRoomId = device.roomId
        updateRoomSpinnerSelection()
        // Set up edit room spinner
        editSelectedRoomId = device.roomId
        updateEditRoomSpinnerSelection()
        usernameBox.editText?.setText(deviceSecrets?.username ?: "")
        passwordBox.editText?.setText(deviceSecrets?.password ?: "")
        configHide.isChecked = device.hide
        configDirectView.isChecked = device.directView

        configButton.setOnClickListener {
            onConfigButtonClicked()
        }

        infoButton.setOnClickListener {
            startActivity(Intent(this, DeviceInfoActivity::class.java).putExtra(Devices.INTENT_EXTRA_DEVICE, deviceId))
        }

        findViewById<Button>(R.id.shortcutBtn).setOnClickListener {
            onShortcutButtonClicked(device)
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
    }

    private fun onConfigButtonClicked() {
        when (modeSpinner.text.toString()) {
            Global.ESP_EASY,
            Global.SHELLY_GEN_1,
            Global.SHELLY_GEN_2,
            Global.SHELLY_GEN_3,
            Global.SHELLY_GEN_4,
            Global.SIMPLE_HOME_API,
            -> {
                startActivity(
                    Intent(this, WebActivity::class.java)
                        .putExtra("URI", addressBox.editText?.text.toString()),
                )
            }
            Global.NODE_RED -> {
                startActivity(
                    Intent(this, WebActivity::class.java)
                        .putExtra("URI", formatNodeREDAddress(addressBox.editText?.text.toString())),
                )
            }
            Global.HUE_API -> {
                val huePackageName = "com.philips.lighting.hue2"
                val launchIntent = packageManager.getLaunchIntentForPackage(huePackageName)
                if (launchIntent == null) {
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "market://details?id=$huePackageName".toUri(),
                            ),
                        )
                    } catch (e: ActivityNotFoundException) {
                        Log.w(EditDeviceActivity::class.simpleName, e)
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://play.google.com/store/apps/details?id=$huePackageName".toUri(),
                            ),
                        )
                    }
                } else {
                    startActivity(launchIntent)
                }
            }
        }
    }

    private fun onShortcutButtonClicked(device: DeviceItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = this.getSystemService(ShortcutManager::class.java)
            if (shortcutManager != null) {
                if (shortcutManager.isRequestPinShortcutSupported) {
                    val shortcut =
                        ShortcutInfo.Builder(this, deviceId)
                            .setShortLabel(
                                device.name.ifEmpty {
                                    resources.getString(R.string.pref_add_name_empty)
                                },
                            )
                            .setLongLabel(
                                device.name.ifEmpty {
                                    resources.getString(R.string.pref_add_name_empty)
                                },
                            )
                            .setIcon(Icon.createWithResource(this, device.iconId))
                            .setIntent(
                                Intent(this, MainActivity::class.java)
                                    .putExtra(Devices.INTENT_EXTRA_DEVICE, deviceId)
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

    private fun onCreateDevice() {
        iconSpinner.setText(resources.getStringArray(R.array.pref_icons)[0], false)
        modeSpinner.setText(resources.getStringArray(R.array.pref_add_mode_array)[0], false)
        selectedRoomId = ""
        updateRoomSpinnerSelection()
        findViewById<View>(R.id.editDivider).visibility = View.GONE
        findViewById<LinearLayout>(R.id.editSection).visibility = View.GONE
    }

    private fun setupRoomSpinner() {
        roomSpinner = findViewById<AutoCompleteTextView>(R.id.roomSpinnerAutoComplete)
        // Disable filtering for dropdown spinners to show all items
        roomSpinner.isFocusable = false
        roomSpinner.isFocusableInTouchMode = false
        roomSpinner.inputType = android.text.InputType.TYPE_NULL
        updateRoomSpinnerAdapter()
    }

    private fun updateRoomSpinnerAdapter() {
        val roomNames = mutableListOf<String>()
        val roomIds = mutableListOf<String>()
        
        // Add "No room" option
        roomNames.add(getString(R.string.device_config_room_none))
        roomIds.add("")
        
        // Add all rooms
        for (i in 0 until rooms.length) {
            val room = rooms.getRoomByIndex(i)
            roomNames.add(room.name)
            roomIds.add(room.id)
        }
        
        val adapter = ArrayAdapter(
            this,
            R.layout.dropdown_item,
            roomNames,
        )
        roomSpinner.setAdapter(adapter)
        
        roomSpinner.setOnItemClickListener { _, _, position, _ ->
            if (position >= 0 && position < roomIds.size) {
                selectedRoomId = roomIds[position]
            }
        }
    }

    private fun updateRoomSpinnerSelection() {
        if (selectedRoomId.isEmpty()) {
            roomSpinner.setText(getString(R.string.device_config_room_none), false)
        } else {
            val room = rooms.getRoomById(selectedRoomId)
            roomSpinner.setText(room.name, false)
        }
    }

    private fun setupEditRoomSpinner() {
        editRoomSpinner = findViewById<AutoCompleteTextView>(R.id.editRoomSpinnerAutoComplete)
        // Disable filtering for dropdown spinners to show all items
        editRoomSpinner.isFocusable = false
        editRoomSpinner.isFocusableInTouchMode = false
        editRoomSpinner.inputType = android.text.InputType.TYPE_NULL
        updateEditRoomSpinnerAdapter()
    }

    private fun updateEditRoomSpinnerAdapter() {
        val roomNames = mutableListOf<String>()
        val roomIds = mutableListOf<String>()
        
        // Add "No room" option
        roomNames.add(getString(R.string.device_config_room_none))
        roomIds.add("")
        
        // Add all rooms
        for (i in 0 until rooms.length) {
            val room = rooms.getRoomByIndex(i)
            roomNames.add(room.name)
            roomIds.add(room.id)
        }
        
        val adapter = ArrayAdapter(
            this,
            R.layout.dropdown_item,
            roomNames,
        )
        editRoomSpinner.setAdapter(adapter)
        
        editRoomSpinner.setOnItemClickListener { _, _, position, _ ->
            if (position >= 0 && position < roomIds.size) {
                editSelectedRoomId = roomIds[position]
            }
        }
    }

    private fun updateEditRoomSpinnerSelection() {
        if (editSelectedRoomId.isEmpty()) {
            editRoomSpinner.setText(getString(R.string.device_config_room_none), false)
        } else {
            val room = rooms.getRoomById(editSelectedRoomId)
            editRoomSpinner.setText(room.name, false)
        }
    }

    private fun onFloatingActionButtonClicked() {
        try {
            val name = nameBox.editText?.text.toString()
            if (name == "") {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.err_missing_name)
                    .setMessage(R.string.err_missing_name_summary)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .show()
                return
            } else if (addressBox.editText?.text.toString() == "") {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.err_missing_address)
                    .setMessage(R.string.err_missing_address_summary)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .show()
                return
            }

            val modeText = modeSpinner.text?.toString() ?: "Website"
            val iconText = iconSpinner.text?.toString() ?: "Lamp"
            // Use editSelectedRoomId when editing, selectedRoomId when creating
            val roomId = if (editing) editSelectedRoomId else selectedRoomId
            
            val tempAddress =
                if (modeText == Global.NODE_RED) {
                    formatNodeREDAddress(addressBox.editText?.text.toString())
                } else {
                    addressBox.editText?.text.toString()
                }

            val newItem =
                DeviceItem(
                    deviceId,
                    name,
                    modeText,
                    iconText,
                    configHide.isChecked,
                    configDirectView.isChecked,
                    roomId,
                )
            newItem.address = tempAddress
            devices.addDevice(newItem)
            try {
                deviceSecrets?.let {
                    it.username = usernameBox.editText?.text.toString()
                    it.password = passwordBox.editText?.text.toString()
                    it.updateDeviceSecrets()
                }
            } catch (e: Exception) {
                Log.e("EditDeviceActivity", "Error saving device secrets", e)
            }
            finish()
        } catch (e: Exception) {
            Log.e(EditDeviceActivity::class.simpleName, "Error saving device", e)
            Toast.makeText(this, "Error saving device: ${e.message}", Toast.LENGTH_LONG).show()
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

    companion object {
        private val SUPPORTS_DIRECT_VIEW =
            arrayOf(
                Global.ESP_EASY,
                Global.HUE_API,
                Global.SHELLY_GEN_1,
                Global.SHELLY_GEN_2,
                Global.SHELLY_GEN_3,
                Global.SHELLY_GEN_4,
                Global.SIMPLE_HOME_API,
                Global.TASMOTA,
            )
        private val HAS_CONFIG =
            arrayOf(
                Global.HUE_API,
                Global.ESP_EASY,
                Global.NODE_RED,
                Global.SHELLY_GEN_1,
                Global.SHELLY_GEN_2,
                Global.SHELLY_GEN_3,
                Global.SHELLY_GEN_4,
                Global.SIMPLE_HOME_API,
            )
        private val HAS_INFO =
            arrayOf(
                Global.HUE_API,
                Global.SHELLY_GEN_2,
                Global.SHELLY_GEN_3,
                Global.SHELLY_GEN_4,
            )
    }
}
