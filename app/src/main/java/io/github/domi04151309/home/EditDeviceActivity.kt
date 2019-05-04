package io.github.domi04151309.home

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.*

class EditDeviceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_device)

        val devices = Devices(PreferenceManager.getDefaultSharedPreferences(this))
        val device = intent.getStringExtra("Device")

        val nameTxt = findViewById<TextView>(R.id.nameTxt)
        val nameBox = findViewById<EditText>(R.id.nameBox)
        val addressBox = findViewById<EditText>(R.id.addressBox)
        val iconSpinner = findViewById<Spinner>(R.id.iconSpinner)
        val modeSpinner = findViewById<Spinner>(R.id.modeSpinner)
        val deleteBtn = findViewById<Button>(R.id.deleteBtn)

        val iconSpinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.pref_icons))
        val modeSpinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.pref_add_mode_array))

        if (device != null){
            title = resources.getString(R.string.pref_edit_device)
            findViewById<ImageView>(R.id.deviceIcn).setImageDrawable(resources.getDrawable(Global.getIconId(devices.getIcon(device))))
            nameTxt.text = device

            nameBox.setText(device)
            addressBox.setText(devices.getAddress(device))
            iconSpinner.setSelection(iconSpinnerArrayAdapter.getPosition(devices.getIcon(device)))
            modeSpinner.setSelection(modeSpinnerArrayAdapter.getPosition(devices.getMode(device)))

            deleteBtn.setOnClickListener {
                AlertDialog.Builder(this)
                        .setTitle(resources.getString(R.string.pref_delete_device))
                        .setMessage(resources.getString(R.string.pref_delete_device_question))
                        .setPositiveButton(resources.getString(android.R.string.ok)) { _, _ ->
                            devices.deleteDevice(device)
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
            if (device != null) devices.deleteDevice(device)
            devices.addDevice(
                    name,
                    addressBox.text.toString(),
                    iconSpinner.selectedItem.toString(),
                    modeSpinner.selectedItem.toString()
            )
            finish()
        }

        findViewById<Button>(R.id.cancelBtn).setOnClickListener {
            finish()
        }
    }
}
