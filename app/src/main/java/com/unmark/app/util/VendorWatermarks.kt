package com.unmark.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/** A watermark region as fractions of image width/height, so it applies at any resolution. */
data class NormalizedRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class VendorWatermark(
    val id: String,
    val displayName: String,
    val region: NormalizedRect,
    val signatures: List<String>
)

/**
 * Known corner/edge positions generators commonly stamp a visible watermark into, keyed by
 * text signatures those tools tend to leave in EXIF or embedded XMP/C2PA blocks. This is a
 * heuristic, not a lookup against any vendor's spec: positions are approximate and meant as a
 * starting selection the user can still adjust with the brush before erasing, not a guarantee.
 * No network calls, no model, just fixed rectangles plus a substring search.
 */
object VendorWatermarks {

    val KNOWN = listOf(
        VendorWatermark(
            id = "bing_image_creator",
            displayName = "Bing Image Creator",
            region = NormalizedRect(0.70f, 0.85f, 1.00f, 1.00f),
            signatures = listOf("bing image creator", "microsoft designer")
        ),
        VendorWatermark(
            id = "meta_ai",
            displayName = "Meta AI",
            region = NormalizedRect(0.00f, 0.90f, 1.00f, 1.00f),
            signatures = listOf("meta ai", "imagined with ai")
        ),
        VendorWatermark(
            id = "google_imagefx",
            displayName = "Google (ImageFX / Gemini)",
            region = NormalizedRect(0.85f, 0.90f, 1.00f, 1.00f),
            signatures = listOf("google ai", "imagefx", "gemini", "made with google ai")
        ),
        VendorWatermark(
            id = "adobe_firefly",
            displayName = "Adobe Firefly",
            region = NormalizedRect(0.00f, 0.92f, 0.28f, 1.00f),
            signatures = listOf("adobe firefly", "firefly")
        ),
        VendorWatermark(
            id = "playground_ai",
            displayName = "Playground AI",
            region = NormalizedRect(0.78f, 0.90f, 1.00f, 1.00f),
            signatures = listOf("playground ai", "playgroundai")
        ),
        VendorWatermark(
            id = "leonardo_ai",
            displayName = "Leonardo.Ai",
            region = NormalizedRect(0.78f, 0.90f, 1.00f, 1.00f),
            signatures = listOf("leonardo.ai", "leonardo ai")
        ),
        VendorWatermark(
            id = "craiyon",
            displayName = "Craiyon",
            region = NormalizedRect(0.00f, 0.93f, 1.00f, 1.00f),
            signatures = listOf("craiyon")
        )
    )

    private const val SCAN_LIMIT_BYTES = 5 * 1024 * 1024

    private val exifTagsToCheck = listOf(
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT
    )

    /** Scans EXIF tags and a raw byte prefix (catches XMP/C2PA generator claims too) for a known signature. */
    fun detect(context: Context, uri: Uri): VendorWatermark? {
        val resolver = context.contentResolver

        val exifText = resolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            exifTagsToCheck.mapNotNull { exif.getAttribute(it) }.joinToString(" ")
        } ?: ""

        val rawText = resolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArray(SCAN_LIMIT_BYTES)
            var total = 0
            while (total < buffer.size) {
                val read = stream.read(buffer, total, buffer.size - total)
                if (read == -1) break
                total += read
            }
            String(buffer, 0, total, Charsets.ISO_8859_1)
        } ?: ""

        val haystack = (exifText + " " + rawText).lowercase()
        return KNOWN.firstOrNull { vendor -> vendor.signatures.any { haystack.contains(it) } }
    }

    /** Builds a mask bitmap with just this vendor's region painted in, same shape Inpainter expects. */
    fun rectMask(width: Int, height: Int, region: NormalizedRect): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        val paint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(
            region.left * width,
            region.top * height,
            region.right * width,
            region.bottom * height,
            paint
        )
        return mask
    }
}
