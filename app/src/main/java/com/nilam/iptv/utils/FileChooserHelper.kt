package com.nilam.iptv.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

object FileChooserHelper {

    fun createImageCaptureIntent(context: Context): Pair<Intent, Uri>? {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            Pair(intent, uri)
        } catch (e: Exception) {
            null
        }
    }

    fun createFileChooserIntent(acceptTypes: Array<String>?, allowMultiple: Boolean): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = if (!acceptTypes.isNullOrEmpty() && acceptTypes[0].isNotBlank()) acceptTypes[0] else "*/*"
        if (allowMultiple) intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        return intent
    }
}
