package io.github.domi04151309.home.helpers

class HueLampData {

    private var listeners = arrayListOf<(data: HueLampData) -> Unit>()

    var on: Boolean = false
    var ct: Int = -1
    var hue: Int = -1
    var sat: Int = -1

    fun notifyDataChanged() {
        listeners.forEach {
            it(this)
        }
    }

    fun addOnDataChangedListener(listener: (data: HueLampData) -> Unit) {
        listeners.add(listener)
    }
}