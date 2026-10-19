package com.mapaurbano.mobile.core.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import com.mapaurbano.mobile.core.model.PhotoAttachment
import com.mapaurbano.mobile.core.model.PhotoIssue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoReadException(val issue: PhotoIssue) : Exception(issue.message)

class PhotoRepository(private val context: Context) {
    suspend fun read(uri: String): PhotoAttachment = withContext(Dispatchers.IO) {
        try {
            val declaredMime = context.contentResolver.getType(Uri.parse(uri))?.lowercase()?.replace("image/jpg", "image/jpeg")
            if (declaredMime != null && declaredMime !in listOf("image/jpeg", "image/png", "image/webp")) throw PhotoReadException(PhotoIssue.InvalidType)
            val bytes = context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= 5 * 1024 * 1024) {
                    val count = input.read(buffer, 0, minOf(buffer.size, 5 * 1024 * 1024 + 1 - output.size()))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.toByteArray() }
                ?: throw PhotoReadException(PhotoIssue.PermissionDenied)
            if (bytes.size > 5 * 1024 * 1024) throw PhotoReadException(PhotoIssue.TooLarge)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val mime = bounds.outMimeType
            if (mime !in listOf("image/jpeg", "image/png", "image/webp")) throw PhotoReadException(PhotoIssue.InvalidType)
            if (declaredMime != null && declaredMime != mime) throw PhotoReadException(PhotoIssue.InvalidType)
            if (bounds.outWidth !in 1..12000 || bounds.outHeight !in 1..12000) throw PhotoReadException(PhotoIssue.InvalidDimensions)
            PhotoAttachment(uri, requireNotNull(mime), bytes)
        } finally {
            // Camera files are temporary; gallery content belongs to its provider.
            val parsed = Uri.parse(uri)
            if (parsed.authority == "${context.packageName}.fileprovider") {
                context.cacheDir.resolve("camera").listFiles()?.filter { it.name == parsed.lastPathSegment }?.forEach { it.delete() }
            }
        }
    }
}
