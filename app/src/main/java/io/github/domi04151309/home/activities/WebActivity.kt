package io.github.domi04151309.home.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import io.github.domi04151309.home.R

class WebActivity : BaseActivity() {
    private var valueCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var webView: WebView
    private lateinit var webViewClient: WebActivityWebViewClient
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        webViewClient =
            WebActivityWebViewClient(
                this,
                intent,
                findViewById<ProgressBar>(R.id.progressBar),
                findViewById<ProgressBar>(R.id.error),
            )

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = webViewClient

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val path = result.data?.data
                    valueCallback?.onReceiveValue(
                        if (path == null) {
                            arrayOf()
                        } else {
                            arrayOf(path)
                        },
                    )
                }
            }

        webView.webChromeClient =
            object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?,
                ): Boolean = showFileChooser(filePathCallback)
            }

        webView.setDownloadListener { url, _, _, _, _ ->
            onDownload(url)
        }

        webView.loadUrl(intent.getStringExtra("URI") ?: ABOUT_BLANK)
        title = intent.getStringExtra("title")
    }

    internal fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>?): Boolean {
        valueCallback = filePathCallback
        resultLauncher.launch(
            Intent(Intent.ACTION_CHOOSER)
                .putExtra(
                    Intent.EXTRA_INTENT,
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    },
                )
                .putExtra(Intent.EXTRA_TITLE, "Image Chooser"),
        )
        return true
    }

    private fun onDownload(url: String) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ),
                1,
            )
        }

        val uri = url.toUri()
        (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(
            DownloadManager.Request(uri).apply {
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    uri.lastPathSegment,
                )
            },
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_web_actions, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_open) {
            startActivity(Intent(Intent.ACTION_VIEW, webView.url?.toUri()))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack() && !webViewClient.hasError) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val ABOUT_BLANK = "about:blank"
    }
}
