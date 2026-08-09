package com.unmark.app.util

import android.content.Context
import android.net.Uri
import com.unmark.app.inpaint.Inpainter

enum class BatchOutcome { ERASED, SKIPPED_NO_MATCH, FAILED_TO_LOAD }

data class BatchItemResult(val uri: Uri, val outcome: BatchOutcome)

/**
 * Applies one watermark region to many photos in sequence: load, mask that region, inpaint,
 * save to gallery. Meant for photos that share the same watermark placement (e.g. a batch of
 * screenshots from the same generator), not a substitute for per-photo manual brushing.
 */
object BatchProcessor {

    /** [fixedVendor] null means auto-detect a vendor per photo instead of using one fixed region. */
    suspend fun processAll(
        context: Context,
        uris: List<Uri>,
        fixedVendor: VendorWatermark?,
        onProgress: (done: Int, total: Int) -> Unit
    ): List<BatchItemResult> {
        val results = mutableListOf<BatchItemResult>()

        for ((index, uri) in uris.withIndex()) {
            val bitmap = ImageUtils.loadBitmap(context, uri)
            if (bitmap == null) {
                results.add(BatchItemResult(uri, BatchOutcome.FAILED_TO_LOAD))
                onProgress(index + 1, uris.size)
                continue
            }

            val vendor = fixedVendor ?: VendorWatermarks.detect(context, uri)
            if (vendor == null) {
                results.add(BatchItemResult(uri, BatchOutcome.SKIPPED_NO_MATCH))
                onProgress(index + 1, uris.size)
                continue
            }

            val mask = VendorWatermarks.rectMask(bitmap.width, bitmap.height, vendor.region)
            val erased = Inpainter.inpaint(bitmap, mask)
            ImageStore.saveToGallery(context, erased)
            results.add(BatchItemResult(uri, BatchOutcome.ERASED))
            onProgress(index + 1, uris.size)
        }

        return results
    }
}
