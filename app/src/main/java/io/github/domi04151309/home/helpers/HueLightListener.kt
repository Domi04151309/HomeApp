package io.github.domi04151309.home.helpers

import io.github.domi04151309.home.data.LightStates

class HueLightListener {
    private var listeners = mutableListOf<(data: LightStates.Light) -> Unit>()
    var state: LightStates.Light = LightStates.Light()
        set(value) {
            if (value != field) {
                field = value
                listeners.forEach {
                    it(value)
                }
            }
        }

    fun addOnDataChangedListener(listener: (data: LightStates.Light) -> Unit) {
        listeners.add(listener)
    }
}
