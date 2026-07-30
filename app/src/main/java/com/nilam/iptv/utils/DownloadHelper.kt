package com.nilam.iptv.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.nilam.iptv.R
import java.net.URLConnection

object DownloadHelper {

    fun download(context: Context, url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            val cookies = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("Cookie", cookies)
            request.addRequestHeader("User-Agent", userAgent)

            val fileName = guessFileName(url, contentDisposition, mimeType)
            request.setMimeType(mimeType ?: guessMimeType(fileName))
            request.setTitle(fileName)
            request.setDescription(context.getString(R.string.app_name))
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            request.allowScanningByMediaScanner()

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, e.message ?: "Download failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guessFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        return android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
    }

    private fun guessMimeType(fileName: String): String {
        val ext = MimeTypeMap.getFileExtensionFromUrl(fileName)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: URLConnection.guessContentTypeFromName(fileName)
            ?: "application/octet-stream"
    }
}
