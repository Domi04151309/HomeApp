package io.github.domi04151309.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.widget.*
import io.github.domi04151309.home.data.DeviceItem

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

        val nameTxt = findViewById<TextView>(R.id.nameTxt)
        val nameBox = findViewById<EditText>(R.id.nameBox)
        val addressBox = findViewById<EditText>(R.id.addressBox)
        val iconSpinner = findViewById<Spinner>(R.id.iconSpinner)
        val modeSpinner = findViewById<Spinner>(R.id.modeSpinner)
        val deleteBtn = findViewById<Button>(R.id.deleteBtn)

        val iconSpinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.pref_icons))
        val modeSpinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.pref_add_mode_array))

        findViewById<TextView>(R.id.idTxt).text = (resources.getString(R.string.pref_add_id, deviceId))

        if (editing){
            title = resources.getString(R.string.pref_edit_device)
            val deviceObj = devices.getDeviceById(deviceId)
            findViewById<ImageView>(R.id.deviceIcn).setImageResource(deviceObj.iconId)
            nameTxt.text = deviceObj.name

            nameBox.setText(deviceObj.name)
            addressBox.setText(deviceObj.address)
            iconSpinner.setSelection(iconSpinnerArrayAdapter.getPosition(deviceObj.iconName))
            modeSpinner.setSelection(modeSpinnerArrayAdapter.getPosition(deviceObj.mode))

            deleteBtn.setOnClickListener {
                AlertDialog.Builder(this)
                        .setTitle(resources.getString(R.string.pref_delete_device))
                        .setMessage(resources.getString(R.string.pref_delete_device_question))
                        .setPositiveButton(resources.getString(android.R.string.ok)) { _, _ ->
                            devices.deleteDevice(deviceId)
                            finish()
                        }
                        .setNegativeButton(resources.getString(android.R.string.cancel)) { _, _ -> }
                        .show()
            }
        } else
            deleteBtn.visibility = View.GONE

        findViewById<Button>(R.id.okBtn).setOnClickListener {
            val name = nameBox.text.toString()
            if (name == "") {
                AlertDialog.Builder(this)
                        .setTitle(resources.getString(R.string.pref_info))
                        .setMessage(resources.getString(R.string.pref_add_unsuccessful))
                        .setPositiveButton(resources.getString(android.R.string.ok)) { _, _ -> }
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

        findViewById<Button>(R.id.cancelBtn).setOnClickListener {
            finish()
        }
    }
}
