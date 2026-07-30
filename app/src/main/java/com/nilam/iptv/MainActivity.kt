package com.nilam.iptv

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.snackbar.Snackbar
import com.nilam.iptv.databinding.ActivityMainBinding
import com.nilam.iptv.utils.ConnectivityObserver
import com.nilam.iptv.utils.DownloadHelper
import com.nilam.iptv.utils.FileChooserHelper
import com.nilam.iptv.utils.PermissionHelper
import com.nilam.iptv.webview.AppWebChromeClient
import com.nilam.iptv.webview.AppWebViewClient
import com.nilam.iptv.webview.WebViewConfigurator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var connectivityObserver: ConnectivityObserver

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private var lastBackPressTime = 0L
    private var isPageLoaded = false

    companion object {
        const val SITE_URL = "https://nilam-app.netlify.app/"
    }

    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = filePathCallback
        filePathCallback = null
        if (callback == null) return@registerForActivityResult
        val results: Array<Uri>? = when {
            result.resultCode != RESULT_OK -> null
            result.data?.dataString != null -> arrayOf(Uri.parse(result.data!!.dataString))
            result.data?.clipData != null -> {
                val clip = result.data!!.clipData!!
                Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
            }
            cameraImageUri != null -> arrayOf(cameraImageUri!!)
            else -> null
        }
        callback.onReceiveValue(results)
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)

        connectivityObserver = ConnectivityObserver(this)
        setupWebView()
        setupSwipeRefresh()
        setupOfflineScreen()
        setupBackPress()

        connectivityObserver.start { connected ->
            runOnUiThread { if (connected) loadSiteIfNeeded() else showOffline() }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        WebViewConfigurator.configure(binding.webView)

        binding.webView.webViewClient = AppWebViewClient(
            onPageStarted = {
                binding.progressBar.visibility = View.VISIBLE
            },
            onPageFinished = {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                isPageLoaded = true
            },
            onError = { _, _ ->
                binding.swipeRefresh.isRefreshing = false
                if (!connectivityObserver.isCurrentlyConnected()) showOffline()
            },
            isConnected = { connectivityObserver.isCurrentlyConnected() }
        )

        binding.webView.webChromeClient = AppWebChromeClient(
            onProgressChanged = { progress ->
                binding.progressBar.progress = progress
            },
            onShowCustomView = { view, callback -> showCustomView(view, callback) },
            onHideCustomView = { hideCustomView() },
            onShowFileChooser = { callback, params -> showFileChooser(callback, params) },
            onGeolocationRequest = { origin, callback -> requestGeolocation(origin, callback) },
            onPermissionRequest = { request -> requestWebPermission(request) }
        )

        binding.webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            DownloadHelper.download(this, url, userAgent, contentDisposition, mimeType)
        }

        binding.webView.loadUrl(SITE_URL)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary, R.color.secondary)
        binding.swipeRefresh.setOnRefreshListener {
            if (connectivityObserver.isCurrentlyConnected()) {
                binding.webView.reload()
            } else {
                binding.swipeRefresh.isRefreshing = false
                showOffline()
            }
        }
    }

    private fun setupOfflineScreen() {
        binding.retryButton.setOnClickListener {
            if (connectivityObserver.isCurrentlyConnected()) {
                hideOffline()
                binding.webView.reload()
            } else {
                Snackbar.make(binding.root, R.string.no_internet, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this) {
            when {
                customView != null -> hideCustomView()
                binding.webView.canGoBack() -> binding.webView.goBack()
                System.currentTimeMillis() - lastBackPressTime < 2000 -> finish()
                else -> {
                    lastBackPressTime = System.currentTimeMillis()
                    Snackbar.make(binding.root, R.string.exit_hint, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadSiteIfNeeded() {
        hideOffline()
        if (!isPageLoaded) binding.webView.loadUrl(SITE_URL)
    }

    private fun showOffline() {
        binding.offlineLayout.visibility = View.VISIBLE
        binding.swipeRefresh.visibility = View.GONE
    }

    private fun hideOffline() {
        binding.offlineLayout.visibility = View.GONE
        binding.swipeRefresh.visibility = View.VISIBLE
    }

    // --- Fullscreen video ---
    private fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (customView != null) {
            callback.onCustomViewHidden()
            return
        }
        customView = view
        customViewCallback = callback
        binding.videoContainer.addView(view)
        binding.videoContainer.visibility = View.VISIBLE
        binding.swipeRefresh.visibility = View.GONE
        enterImmersiveMode()
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    private fun hideCustomView() {
        if (customView == null) return
        binding.videoContainer.removeView(customView)
        binding.videoContainer.visibility = View.GONE
        binding.swipeRefresh.visibility = View.VISIBLE
        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
        exitImmersiveMode()
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun enterImmersiveMode() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    private fun exitImmersiveMode() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    // --- File chooser (upload) ---
    private fun showFileChooser(callback: ValueCallback<Array<Uri>>, params: WebChromeClient.FileChooserParams): Boolean {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback

        if (!PermissionHelper.hasPermissions(this, PermissionHelper.cameraPermissions())) {
            permissionLauncher.launch(PermissionHelper.cameraPermissions())
        }

        val intents = mutableListOf<Intent>()
        val camera = FileChooserHelper.createImageCaptureIntent(this)
        if (camera != null) {
            cameraImageUri = camera.second
            intents.add(camera.first)
        }

        val contentIntent = FileChooserHelper.createFileChooserIntent(
            params.acceptTypes, params.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
        )

        val chooser = Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, contentIntent)
            putExtra(Intent.EXTRA_TITLE, "Select File")
            if (intents.isNotEmpty()) putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
        }

        fileChooserLauncher.launch(chooser)
        return true
    }

    private fun requestGeolocation(origin: String, callback: GeolocationPermissions.Callback) {
        if (PermissionHelper.hasPermissions(this, PermissionHelper.locationPermissions())) {
            callback.invoke(origin, true, false)
        } else {
            permissionLauncher.launch(PermissionHelper.locationPermissions())
            callback.invoke(origin, false, false)
        }
    }

    private fun requestWebPermission(request: PermissionRequest) {
        val needsAudio = request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        val needsCamera = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
        val perms = mutableListOf<String>()
        if (needsAudio) perms.addAll(PermissionHelper.audioPermission())
        if (needsCamera) perms.addAll(PermissionHelper.cameraPermissions())

        if (perms.isEmpty() || PermissionHelper.hasPermissions(this, perms.toTypedArray())) {
            request.grant(request.resources)
        } else {
            permissionLauncher.launch(perms.toTypedArray())
            request.grant(request.resources)
        }
    }

    // --- Picture in Picture ---
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (customView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPip()
        }
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            try { enterPictureInPictureMode(params) } catch (_: Exception) {}
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        binding.progressBar.visibility = if (isInPictureInPictureMode) View.GONE else binding.progressBar.visibility
    }

    fun shareCurrentUrl() {
        val url = binding.webView.url ?: SITE_URL
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_url)))
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
        CookieManager.getInstance().flush()
    }

    override fun onPause() {
        binding.webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        connectivityObserver.stop()
        binding.webView.apply {
            clearHistory()
            destroy()
        }
        super.onDestroy()
    }
}
