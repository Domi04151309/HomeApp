package io.github.domi04151309.home.interfaces

import io.github.domi04151309.home.helpers.HueLampData
import org.json.JSONArray

interface HueRoomInterface: HueLampInterface {
    var isRoom: Boolean
    var lights: JSONArray?
    var lampData: HueLampData
}