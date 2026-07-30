package com.nilam.iptv.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent

object UrlSchemeHandler {

    fun handle(context: Context, url: String): Boolean {
        return when {
            url.startsWith("tel:") -> openIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse(url)))
            url.startsWith("mailto:") -> openIntent(context, Intent(Intent.ACTION_SENDTO, Uri.parse(url)))
            url.startsWith("sms:") -> openIntent(context, Intent(Intent.ACTION_SENDTO, Uri.parse(url)))
            url.startsWith("whatsapp:") -> openIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            url.startsWith("geo:") -> openIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            url.contains("maps.google") || url.contains("goo.gl/maps") -> openIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            url.startsWith("intent://") -> openByIntentUri(context, url)
            url.startsWith("market://") -> openIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            url.startsWith("http://") || url.startsWith("https://") -> false
            else -> openExternal(context, url)
        }
    }

    private fun openIntent(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to handle this link", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun openByIntentUri(context: Context, url: String): Boolean {
        return try {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            true
        }
    }

    private fun openExternal(context: Context, url: String): Boolean {
        return try {
            val tabsIntent = CustomTabsIntent.Builder().build()
            tabsIntent.launchUrl(context, Uri.parse(url))
            true
        } catch (e: Exception) {
            false
        }
    }
}
