package io.github.domi04151309.home

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

}
