package io.github.domi04151309.home

object General {

    const val LOG_TAG = "Home"

    fun formatURL(url: String): String {
        var _url = url
        if (!(_url.startsWith("https://") || _url.startsWith("http://")))
            _url = "http://$_url"
        if (!_url.endsWith("/"))
            _url += "/"
        return _url
    }

}
