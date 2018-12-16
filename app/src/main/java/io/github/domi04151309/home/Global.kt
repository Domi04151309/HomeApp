package io.github.domi04151309.home

import android.widget.ListView
import java.util.*

object Global {

    const val LOG_TAG = "HomeApp"

    fun formatURL(url: String): String {
        var _url = url
        if (!(_url.startsWith("https://") || _url.startsWith("http://")))
            _url = "http://$_url"
        if (!_url.endsWith("/"))
            _url += "/"
        return _url
    }

    private val listViewItemHeights = Hashtable<Int, Int>()

    fun getScroll(listView: ListView): Int {
        val c = listView.getChildAt(0)
        var scrollY = -c.top
        listViewItemHeights[listView.firstVisiblePosition] = c.height
        for (i in 0 until listView.firstVisiblePosition) {
            if (listViewItemHeights[i] != null)
                scrollY += listViewItemHeights[i]!!
        }
        return scrollY
    }

}
