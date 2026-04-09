package io.github.domi04151309.home.activities

import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.IconSpinnerAdapter
import io.github.domi04151309.home.custom.TextWatcher
import io.github.domi04151309.home.data.RoomItem
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.Rooms

class EditRoomActivity : BaseActivity() {
    private lateinit var rooms: Rooms
    private lateinit var devices: Devices
    private lateinit var roomId: String
    private lateinit var roomIcon: ImageView
    private lateinit var nameText: TextView
    private lateinit var nameBox: TextInputLayout
    private lateinit var iconSpinner: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_room)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        applyBottomInsetPadding(findViewById(R.id.scrollView))
        applyBottomInsetMargin(fab)

        rooms = Rooms(this)
        devices = Devices(this)
        var roomId = intent.getStringExtra("roomId")
        val editing =
            if (roomId == null) {
                roomId = rooms.generateNewId()
                false
            } else {
                true
            }
        this.roomId = roomId

        roomIcon = findViewById(R.id.roomIcn)
        nameText = findViewById(R.id.nameTxt)
        nameBox = findViewById(R.id.nameBox)
        iconSpinner = findViewById<TextInputLayout>(R.id.iconSpinner).editText as AutoCompleteTextView

        findViewById<TextView>(R.id.idTxt).text = resources.getString(R.string.pref_add_id, roomId)

        iconSpinner.addTextChangedListener(
            TextWatcher {
                roomIcon.setImageResource(Global.getIcon(it))
            }
        )
        nameBox.editText?.addTextChangedListener(
            TextWatcher {
                if (it == "") {
                    nameText.text = resources.getString(R.string.rooms_new_room)
                } else {
                    nameText.text = it
                }
            }
        )

        if (editing) {
            onEditRoom()
        } else {
            onCreateRoom()
        }

        iconSpinner.setAdapter(IconSpinnerAdapter(resources.getStringArray(R.array.pref_icons)))

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

    private fun onEditRoom() {
        val room = rooms.getRoomById(roomId)
        nameBox.editText?.setText(room.name)
        iconSpinner.setText(room.iconName)

        findViewById<Button>(R.id.deleteBtn).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.str_delete)
                .setMessage(R.string.rooms_delete_question)
                .setPositiveButton(R.string.str_delete) { _, _ ->
                    // Remove room assignment from all devices in this room
                    val devicesInRoom = devices.getDevicesByRoom(roomId)
                    for (device in devicesInRoom) {
                        devices.moveDeviceToRoom(device.id, "")
                    }
                    rooms.deleteRoom(roomId)
                    finish()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
        }
    }

    private fun onCreateRoom() {
        iconSpinner.setText(resources.getStringArray(R.array.pref_icons)[0])
        findViewById<View>(R.id.editDivider).visibility = View.GONE
        findViewById<LinearLayout>(R.id.editSection).visibility = View.GONE
    }

    private fun onFloatingActionButtonClicked() {
        val name = nameBox.editText?.text.toString()
        if (name == "") {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.err_missing_name)
                .setMessage(R.string.err_missing_name_summary)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .show()
            return
        }

        val newItem =
            RoomItem(
                roomId,
                name,
                iconSpinner.text.toString(),
            )
        rooms.addRoom(newItem)
        finish()
    }
}
