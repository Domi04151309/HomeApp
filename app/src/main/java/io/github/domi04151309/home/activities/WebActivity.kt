package io.github.domi04151309.home.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Base64
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import io.github.domi04151309.home.R
import io.github.domi04151309.home.helpers.DeviceSecrets
import io.github.domi04151309.home.helpers.Theme
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.webkit.*
import android.content.Intent
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts

class WebActivity : AppCompatActivity() {

    internal val nullParent: ViewGroup? = null
    internal var errorOccurred = false
    internal var c: Context = this
    internal var valueCallback: ValueCallback<Array<Uri>>? = null
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

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                view.loadUrl("about:blank")
                errorOccurred = true
                progress.visibility = View.GONE
                errorView.visibility = View.VISIBLE
            }
        }

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val path = result.data?.data
                if (path == null) valueCallback?.onReceiveValue(arrayOf())
                else valueCallback?.onReceiveValue(arrayOf(path))
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                valueCallback = filePathCallback

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "*/*"

                resultLauncher.launch(
                    Intent(Intent.ACTION_CHOOSER)
                        .putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                        .putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                )
                return true
            }
        }

        webView.setDownloadListener { url, _, _, _, _ ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    1
                )
            }

            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                uri.lastPathSegment
            )
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        }

        webView.loadUrl(intent.getStringExtra("URI") ?: "about:blank")
        title = intent.getStringExtra("title")
    }

    internal fun injectCSS(webView: WebView) {
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
    }

    internal fun injectFritzLogin(webView: WebView, uiPass: String) {
        webView.loadUrl("javascript:(function() {" +
                "document.getElementById('uiPass').value = '" + uiPass + "';" +
                "document.getElementById('submitLoginBtn').click()" +
                "})()")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_web_actions, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_open) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webView.url)))
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack() && !errorOccurred) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
