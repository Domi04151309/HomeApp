package io.github.domi04151309.home.interfaces

import io.github.domi04151309.home.helpers.HueLightListener
import org.json.JSONArray

interface HueRoomInterface : HueLampInterface {
    var lights: JSONArray?
    var lampData: HueLightListener
}
