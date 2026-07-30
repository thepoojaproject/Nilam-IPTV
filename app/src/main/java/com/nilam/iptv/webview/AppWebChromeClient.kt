package com.nilam.iptv.webview

import android.net.Uri
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class AppWebChromeClient(
    private val onProgressChanged: (Int) -> Unit,
    private val onShowCustomView: (View, WebChromeClient.CustomViewCallback) -> Unit,
    private val onHideCustomView: () -> Unit,
    private val onShowFileChooser: (ValueCallback<Array<Uri>>, android.webkit.WebChromeClient.FileChooserParams) -> Boolean,
    private val onGeolocationRequest: (String, GeolocationPermissions.Callback) -> Unit,
    private val onPermissionRequest: (PermissionRequest) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        onShowCustomView.invoke(view, callback)
    }

    override fun onHideCustomView() {
        onHideCustomView.invoke()
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        return onShowFileChooser(filePathCallback, fileChooserParams)
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        onGeolocationRequest(origin, callback)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        onPermissionRequest.invoke(request)
    }
}
