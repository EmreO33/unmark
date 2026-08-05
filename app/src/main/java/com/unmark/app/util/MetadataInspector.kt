package com.unmark.app.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

data class MetadataFindings(
    val exifTags: List<String>,
    val hasXmp: Boolean,
    val hasC2pa: Boolean
) {
    val isClean: Boolean get() = exifTags.isEmpty() && !hasXmp && !hasC2pa
}

/**
 * Reads metadata from the file the user picked, purely to show them what's there. Unmark's
 * save pipeline never copies original file bytes: it decodes to pixels and re-encodes a fresh
 * PNG (see ImageUtils/ImageStore), so EXIF, XMP, IPTC, and C2PA data never make it into the
 * saved output regardless of what this finds. This exists so people can see (and trust) that,
 * not to drive a separate "strip" step.
 */
object MetadataInspector {

    /** Tags worth surfacing: anything that could carry AI-generation provenance or personal info. */
    private val RELEVANT_EXIF_TAGS = listOf(
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_DATETIME
    )

    private const val SCAN_LIMIT_BYTES = 5 * 1024 * 1024

    fun inspect(context: Context, uri: Uri): MetadataFindings {
        val exifTags = context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            RELEVANT_EXIF_TAGS.filter { exif.getAttribute(it) != null }
        } ?: emptyList()

        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes(SCAN_LIMIT_BYTES)
        } ?: ByteArray(0)
        val text = String(bytes, Charsets.ISO_8859_1)

        val hasC2pa = text.contains("c2pa", ignoreCase = true) || text.contains("jumb")
        val hasXmp = text.contains("<x:xmpmeta") || text.contains("http://ns.adobe.com/xap/")

        return MetadataFindings(exifTags, hasXmp, hasC2pa)
    }

    private fun java.io.InputStream.readBytes(limit: Int): ByteArray {
        val buffer = ByteArray(limit)
        var total = 0
        while (total < limit) {
            val read = read(buffer, total, limit - total)
            if (read == -1) break
            total += read
        }
        return buffer.copyOf(total)
    }
}
