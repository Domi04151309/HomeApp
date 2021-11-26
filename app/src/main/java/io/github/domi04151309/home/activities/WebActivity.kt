package io.github.domi04151309.home.activities

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Base64
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.HttpAuthHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import io.github.domi04151309.home.R
import io.github.domi04151309.home.helpers.DeviceSecrets
import io.github.domi04151309.home.helpers.Theme

class WebActivity : AppCompatActivity() {

    internal var errorOccurred = false
    internal var c: Context = this
    internal val nullParent: ViewGroup? = null
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        val progress = findViewById<ProgressBar>(R.id.progressBar)
        val errorView = findViewById<RelativeLayout>(R.id.error)
        var isFirstLoad = true
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView, url: String) {
                if (url == "about:blank") {
                    view.visibility = View.GONE
                    return
                }
                if (url.contains("github.com")) injectCSS(view)
                if (isFirstLoad && intent.hasExtra("fritz_auto_login")) {
                    injectFritzLogin(view, DeviceSecrets(
                        this@WebActivity,
                        intent.getStringExtra("fritz_auto_login") ?: ""
                    ).password)
                    isFirstLoad = false
                }
                progress.visibility = View.GONE
                view.visibility = View.VISIBLE
                super.onPageFinished(view, url)
            }

            override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String, realm: String) {
                val dialogView = LayoutInflater.from(c).inflate(R.layout.dialog_web_authentication, nullParent, false)
                AlertDialog.Builder(c)
                        .setTitle(R.string.webView_authentication)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            handler.proceed(
                                    dialogView.findViewById<EditText>(R.id.username).text.toString(),
                                    dialogView.findViewById<EditText>(R.id.password).text.toString()
                            )
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                        .show()
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                view.loadUrl("about:blank")
                errorOccurred = true
                progress.visibility = View.GONE
                errorView.visibility = View.VISIBLE
            }
        }
        webView.loadUrl(intent.getStringExtra("URI") ?: "about:blank")
        title = intent.getStringExtra("title")
    }

    internal fun injectCSS(webView: WebView) {
        try {
            val inputStream = assets.open("github_style.css")
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            webView.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "style.innerHTML = window.atob('" + Base64.encodeToString(buffer, Base64.NO_WRAP) + "');" +
                    "parent.appendChild(style)" +
                    "})()")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun injectFritzLogin(webView: WebView, uiPass: String) {
        webView.loadUrl("javascript:(function() {" +
                "document.getElementById('uiPass').value = '" + uiPass + "';" +
                "document.getElementById('submitLoginBtn').click()" +
                "})()")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack() && !errorOccurred) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
