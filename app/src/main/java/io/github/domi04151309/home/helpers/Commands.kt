package io.github.domi04151309.home.helpers

import org.json.JSONObject

class Commands(source: JSONObject) {

    private val _source = source
    private var selectedCommandName: String = ""
    private var selectedCommand: JSONObject = JSONObject()

    fun length(): Int {
        return _source.length()
    }

    fun selectCommand(index: Int) {
        selectedCommandName = _source.names()?.getString(index) ?: ""
        selectedCommand = _source.getJSONObject(selectedCommandName)
    }

    fun getSelected(): String?{
        return selectedCommandName
    }

    fun getSelectedTitle(): String{
        return selectedCommand.getString("title")
    }

    fun getSelectedSummary(): String{
        return selectedCommand.getString("summary")
    }
}