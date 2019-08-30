package io.github.domi04151309.home

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.RelativeLayout

class WebActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var errorOccurred = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        val progress = findViewById<ProgressBar>(R.id.progressBar)
        val errorView = findViewById<RelativeLayout>(R.id.error)
        val uri = intent.getStringExtra("URI") ?: "about:blank"
        val title = intent.getStringExtra("title")
        webView = findViewById(R.id.webView)
        val webSettings = webView!!.settings
        Log.d(Global.LOG_TAG,uri)
        webSettings.javaScriptEnabled = true
        webView!!.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView, url: String) {
                if (url == "about:blank") {
                    view.visibility = View.GONE
                    return
                }
                if (url.contains("github.com")) injectCSS(webView!!)
                progress.visibility = View.GONE
                view.visibility = View.VISIBLE
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                view.loadUrl("about:blank")
                errorOccurred = true
                progress.visibility = View.GONE
                errorView.visibility = View.VISIBLE
            }
        }
        webView!!.loadUrl(uri)
        if (title != null)
            setTitle(title)
    }

    private fun injectCSS(webView: WebView) {
        try {
            val inputStream = assets.open("github_style.css")
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
            webView.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style)" +
                    "})()")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView!!.canGoBack() && !errorOccurred) {
            webView!!.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
