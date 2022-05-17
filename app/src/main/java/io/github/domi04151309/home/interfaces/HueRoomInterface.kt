package io.github.domi04151309.home.interfaces

import org.json.JSONArray

interface HueRoomInterface: HueLampInterface {
    var isRoom: Boolean
    var lights: JSONArray?
}