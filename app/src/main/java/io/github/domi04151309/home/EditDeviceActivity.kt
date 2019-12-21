package io.github.domi04151309.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.widget.*
import io.github.domi04151309.home.data.DeviceItem
import android.content.Intent
import android.widget.Toast
import android.content.pm.ShortcutInfo
import android.os.Build
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.text.TextWatcher
import android.text.Editable
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import com.google.android.material.floatingactionbutton.FloatingActionButton

class EditDeviceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_device)

        val devices = Devices(this)
        var deviceId = intent.getStringExtra("deviceId")
        val editing =
                if (deviceId == null){
                    deviceId = devices.generateNewId()
                    false
                } else true

        val deviceIcn = findViewById<ImageView>(R.id.deviceIcn)
        val nameTxt = findViewById<TextView>(R.id.nameTxt)
        val nameBox = findViewById<EditText>(R.id.nameBox)
        val addressBox = findViewById<EditText>(R.id.addressBox)
        val iconSpinner = findViewById<Spinner>(R.id.iconSpinner)
        val modeSpinner = findViewById<Spinner>(R.id.modeSpinner)

        val iconSpinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.pref_icons))
        val modeSpinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.pref_add_mode_array))

        findViewById<TextView>(R.id.idTxt).text = (resources.getString(R.string.pref_add_id, deviceId))

        iconSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View, position: Int, id: Long) {
                deviceIcn.setImageResource(Global.getIcon(parentView.selectedItem.toString()))
            }
            override fun onNothingSelected(parentView: AdapterView<*>) {
                deviceIcn.setImageResource(R.drawable.ic_device_lamp)
            }
        }
        nameBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val string = s.toString()
                if (string == "") nameTxt.text = resources.getString(R.string.pref_add_name_example)
                else nameTxt.text = string
            }
        })

        if (editing){
            title = resources.getString(R.string.pref_edit_device)
            val deviceObj = devices.getDeviceById(deviceId)
            nameBox.setText(deviceObj.name)
            addressBox.setText(deviceObj.address)
            iconSpinner.setSelection(iconSpinnerArrayAdapter.getPosition(deviceObj.iconName))
            modeSpinner.setSelection(modeSpinnerArrayAdapter.getPosition(deviceObj.mode))

            findViewById<Button>(R.id.shortcutBtn).setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val shortcutManager = this.getSystemService(ShortcutManager::class.java)
                    if (shortcutManager != null) {
                        if (shortcutManager.isRequestPinShortcutSupported) {
                            val shortcut = ShortcutInfo.Builder(this, deviceId)
                                    .setShortLabel(deviceObj.name)
                                    .setLongLabel(deviceObj.name)
                                    .setIcon(Icon.createWithResource(this, deviceObj.iconId))
                                    .setIntent(
                                            Intent(this, MainActivity::class.java)
                                                    .putExtra("device", deviceId)
                                                    .setAction(Intent.ACTION_MAIN)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    )
                                    .build()
                            shortcutManager.requestPinShortcut(shortcut, null)
                        } else
                            Toast.makeText(this, R.string.pref_add_shortcut_failed, Toast.LENGTH_LONG).show()
                    }
                } else
                    Toast.makeText(this, R.string.pref_add_shortcut_failed, Toast.LENGTH_LONG).show()
            }

            findViewById<Button>(R.id.deleteBtn).setOnClickListener {
                AlertDialog.Builder(this)
                        .setTitle(R.string.str_delete)
                        .setMessage(R.string.pref_delete_device_question)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            devices.deleteDevice(deviceId)
                            finish()
                        }
                        .setNegativeButton(resources.getString(android.R.string.cancel)) { _, _ -> }
                        .show()
            }
        } else {
            findViewById<View>(R.id.editDivider).visibility = View.GONE
            findViewById<LinearLayout>(R.id.editSection).visibility = View.GONE
        }

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            val name = nameBox.text.toString()
            if (name == "") {
                AlertDialog.Builder(this)
                        .setTitle(R.string.err_missing_name)
                        .setMessage(R.string.err_missing_name_summary)
                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                        .show()
                return@setOnClickListener
            } else if (addressBox.text.toString() == "") {
                AlertDialog.Builder(this)
                        .setTitle(R.string.err_missing_address)
                        .setMessage(R.string.err_missing_address_summary)
                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                        .show()
                return@setOnClickListener
            }
            if (editing) devices.deleteDevice(deviceId)
            val newItem = DeviceItem(deviceId)
            newItem.name = name
            newItem.address = addressBox.text.toString()
            newItem.mode = modeSpinner.selectedItem.toString()
            newItem.iconName = iconSpinner.selectedItem.toString()
            devices.addDevice(newItem)
            finish()
        }
    }
}
