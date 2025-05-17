package io.github.domi04151309.home.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.http.SslError
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.domi04151309.home.R
import io.github.domi04151309.home.helpers.DeviceSecrets

class WebActivityWebViewClient(
    private val context: Context,
    private val intent: Intent,
    private val progressView: View,
    private val errorView: View,
) :
    WebViewClient() {
    var hasError: Boolean = false
        private set

    private var isFirstLoad: Boolean = true
    private val nullParent: ViewGroup? = null

    override fun onPageFinished(
        view: WebView,
        url: String,
    ) {
        if (url == ABOUT_BLANK) {
            view.visibility = View.GONE
            return
        }
        if (isFirstLoad) {
            if (intent.hasExtra("fritz_auto_login")) {
                injectFritzLogin(
                    view,
                    DeviceSecrets(
                        context,
                        intent.getStringExtra("fritz_auto_login") ?: "",
                    ).password,
                )
            } else if (intent.hasExtra("grafana_auto_login")) {
                val secrets =
                    DeviceSecrets(
                        context,
                        intent.getStringExtra("grafana_auto_login") ?: "",
                    )
                injectGrafanaLogin(
                    view,
                    secrets.username,
                    secrets.password,
                )
            } else if (intent.hasExtra("pi_hole_auto_login")) {
                injectPiHoleLogin(
                    view,
                    DeviceSecrets(
                        context,
                        intent.getStringExtra("pi_hole_auto_login") ?: "",
                    ).password,
                )
            }
            isFirstLoad = false
        }

        progressView.visibility = View.GONE
        view.visibility = View.VISIBLE
        super.onPageFinished(view, url)
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView,
        handler: HttpAuthHandler,
        host: String,
        realm: String,
    ) {
        val dialogView =
            LayoutInflater.from(context)
                .inflate(R.layout.dialog_web_authentication, nullParent, false)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.webView_authentication)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                handler.proceed(
                    dialogView.findViewById<EditText>(R.id.username).text.toString(),
                    dialogView.findViewById<EditText>(R.id.password).text.toString(),
                )
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError,
    ) {
        Toast.makeText(context, R.string.webView_ssl_error, Toast.LENGTH_LONG)
            .show()
        handler.proceed()
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        view.loadUrl(ABOUT_BLANK)
        hasError = true
        progressView.visibility = View.GONE
        errorView.visibility = View.VISIBLE
    }

    private fun injectFritzLogin(
        webView: WebView,
        password: String,
    ) {
        injectJavaScript(
            webView,
            """
            document.getElementById('uiPass').value = '$password';
            document.getElementById('submitLoginBtn').click();
            """,
        )
    }

    private fun injectGrafanaLogin(
        webView: WebView,
        username: String,
        password: String,
    ) {
        // Submission does not work because the form validation does not update correctly.
        injectJavaScript(
            webView,
            """
            const check = setInterval(() => {
                const username = document.querySelector('input[name=user]');
                const password = document.querySelector('input[name=password]');
                if (username && password) {
                    clearInterval(check);
                    
                    username.value = '$username';
                    password.value = '$password';
                }
            }, 100);
            """,
        )
    }

    private fun injectPiHoleLogin(
        webView: WebView,
        password: String,
    ) {
        injectJavaScript(
            webView,
            """
            document.getElementById('current-password').value = '$password';
            document.querySelector('button[type=submit]').click();
            """,
        )
    }

    private fun injectJavaScript(
        webView: WebView,
        javaScript: String,
    ) {
        webView.loadUrl("javascript:(() => {$javaScript})()")
    }

    companion object {
        private const val ABOUT_BLANK = "about:blank"
    }
}
