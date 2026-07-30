package com.nilam.iptv.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import com.nilam.iptv.R

class AppWebViewClient(
    private val onPageStarted: () -> Unit,
    private val onPageFinished: () -> Unit,
    private val onError: (Int, String) -> Unit,
    private val isConnected: () -> Boolean
) : WebViewClient() {

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted()
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished()
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        return UrlSchemeHandler.handle(view.context, url)
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        super.onReceivedError(view, request, error)
        if (request.isForMainFrame) {
            onError(error.errorCode, error.description?.toString() ?: "Unknown error")
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        AlertDialog.Builder(view.context)
            .setTitle(R.string.ssl_error_title)
            .setMessage(R.string.ssl_error_msg)
            .setCancelable(false)
            .setPositiveButton(R.string.cancel) { _, _ -> handler.cancel() }
            .setNegativeButton(R.string.continue_anyway) { _, _ -> handler.proceed() }
            .show()
    }

    override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
        view.destroy()
        return true
    }
}
