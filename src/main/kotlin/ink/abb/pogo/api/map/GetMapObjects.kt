package ink.abb.pogo.api.map

import ink.abb.pogo.api.PoGoApi
import ink.abb.pogo.api.cache.Map
import ink.abb.pogo.api.request.GetMapObjects

class GetMapObjects(poGoApi: PoGoApi, width: Int = 3) : GetMapObjects() {
    init {
        Map.getCellIds(poGoApi.latitude, poGoApi.longitude, width).forEach {
            this.withCellId(it)
            this.withSinceTimestampMs(0)
        }
    }
}