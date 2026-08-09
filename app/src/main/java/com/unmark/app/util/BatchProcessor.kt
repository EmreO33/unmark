package com.unmark.app.util

import android.content.Context
import android.net.Uri
import com.unmark.app.inpaint.Inpainter

enum class BatchOutcome { ERASED, FAILED_TO_LOAD }

data class BatchItemResult(val uri: Uri, val outcome: BatchOutcome)

/**
 * Applies one watermark region, drawn once by the user on a preview photo, to many photos in
 * sequence: load, mask that same region, inpaint, save to gallery. Meant for a batch of photos
 * that share the same watermark placement, not a substitute for per-photo manual brushing.
 */
object BatchProcessor {

    suspend fun processAll(
        context: Context,
        uris: List<Uri>,
        region: NormalizedRect,
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

            val mask = rectMask(bitmap.width, bitmap.height, region)
            val erased = Inpainter.inpaint(bitmap, mask)
            ImageStore.saveToGallery(context, erased)
            results.add(BatchItemResult(uri, BatchOutcome.ERASED))
            onProgress(index + 1, uris.size)
        }

        return results
    }
}
