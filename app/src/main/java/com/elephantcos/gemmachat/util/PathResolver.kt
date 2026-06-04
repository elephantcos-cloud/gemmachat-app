package com.elephantcos.gemmachat.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore

object PathResolver {
    fun resolve(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        if (uri.scheme != "content") return null

        // Try MediaStore DATA column
        try {
            context.contentResolver.query(
                uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val path = cursor.getString(0)
                    if (!path.isNullOrEmpty()) return path
                }
            }
        } catch (_: Exception) {}

        // DocumentsContract approach
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                when (uri.authority) {
                    "com.android.providers.downloads.documents" -> {
                        if (docId.startsWith("raw:")) return docId.removePrefix("raw:")
                        try {
                            val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), docId.toLong()
                            )
                            context.contentResolver.query(
                                contentUri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null
                            )?.use { c -> if (c.moveToFirst()) return c.getString(0) }
                        } catch (_: Exception) {}
                    }
                    "com.android.externalstorage.documents" -> {
                        val parts = docId.split(":")
                        if (parts.size >= 2 && parts[0].equals("primary", true)) {
                            return "${Environment.getExternalStorageDirectory()}/${parts[1]}"
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }
}
