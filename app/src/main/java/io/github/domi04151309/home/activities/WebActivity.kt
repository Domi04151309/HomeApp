package io.github.domi04151309.home.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.domi04151309.home.R

class WebActivity : BaseActivity() {
    private var valueCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, windowInsets ->
            val systemBars = WindowInsetsCompat.Type.systemBars()
            WindowInsetsCompat.Builder(windowInsets)
                .setInsets(
                    systemBars,
                    Insets.of(
                        0,
                        0,
                        0,
                        windowInsets.getInsets(systemBars).bottom,
                    ),
                )
                .build()
        }

        val url = intent.getStringExtra("URI") ?: ABOUT_BLANK

        val webView = findViewById<WebView>(R.id.webView)
        val errorButton = findViewById<Button>(R.id.openBtn)
        val webViewClient =
            WebActivityWebViewClient(
                this,
                intent,
                findViewById<ProgressBar>(R.id.progressBar),
                findViewById<ProgressBar>(R.id.error),
            )

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = webViewClient
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

        webView.loadUrl(url)

        errorButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
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

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack() && !webViewClient.hasError) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )
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

    companion object {
        private const val ABOUT_BLANK = "about:blank"
    }
}
