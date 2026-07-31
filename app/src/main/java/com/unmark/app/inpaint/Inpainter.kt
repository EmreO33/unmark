package com.unmark.app.inpaint

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

/**
 * Lightweight, fully on-device inpainting. No native libraries, no bundled ML weights —
 * keeps the app small and avoids anything F-Droid would need to vet as a binary blob.
 *
 * Approach: multi-source BFS fills each masked pixel with the color of the nearest
 * unmasked pixel (cheap, O(n)), then a few averaging passes over just the masked
 * region smooth out the hard BFS boundaries so the fill doesn't look blocky.
 * This is not a Telea/Navier-Stokes-quality result, but it's fast enough to run on
 * a mid-range phone with zero dependencies and works well for small-to-medium marks
 * (logos, corner watermarks) on reasonably textured backgrounds.
 */
object Inpainter {

    /** Downscale target for the processing pass; results are upscaled back into the original. */
    private const val MAX_PROCESSING_DIMENSION = 1024
    private const val SMOOTHING_PASSES = 6

    suspend fun inpaint(source: Bitmap, mask: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        require(mask.width == source.width && mask.height == source.height) {
            "Mask dimensions must match the source image"
        }

        val scale = MAX_PROCESSING_DIMENSION.toFloat() / maxOf(source.width, source.height)
        val workingSource = if (scale < 1f) scaleBitmap(source, scale) else source
        val workingMask = if (scale < 1f) scaleMask(mask, workingSource.width, workingSource.height) else mask

        val result = inpaintAtScale(workingSource, workingMask)

        if (scale < 1f) {
            // Upscale the processed region and only blend it back where the (full-res) mask is set,
            // so untouched areas keep their original full-resolution detail.
            val upscaled = Bitmap.createScaledBitmap(result, source.width, source.height, true)
            blendMasked(base = source, overlay = upscaled, mask = mask)
        } else {
            result
        }
    }

    private fun inpaintAtScale(source: Bitmap, mask: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixelCount = width * height

        val pixels = IntArray(pixelCount)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val maskPixels = IntArray(pixelCount)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)
        val masked = BooleanArray(pixelCount) { i -> (maskPixels[i] ushr 24) and 0xFF > 32 }

        nearestFill(pixels, masked, width, height)
        smooth(pixels, masked, width, height, SMOOTHING_PASSES)

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    /** Multi-source BFS: every masked pixel takes the color of the closest unmasked pixel. */
    private fun nearestFill(pixels: IntArray, masked: BooleanArray, width: Int, height: Int) {
        val queue = ArrayDeque<Int>()
        val visited = BooleanArray(pixels.size)

        for (i in pixels.indices) {
            if (!masked[i]) {
                visited[i] = true
                queue.add(i)
            }
        }
        if (queue.isEmpty()) return // nothing unmasked to seed from

        while (queue.isNotEmpty()) {
            val idx = queue.poll()
            val x = idx % width
            val y = idx / width
            val color = pixels[idx]

            forEachNeighbor(x, y, width, height) { nx, ny ->
                val nIdx = ny * width + nx
                if (!visited[nIdx]) {
                    visited[nIdx] = true
                    if (masked[nIdx]) pixels[nIdx] = color
                    queue.add(nIdx)
                }
            }
        }
    }

    /** Averages each masked pixel with its neighbors for a few passes to soften BFS block edges. */
    private fun smooth(pixels: IntArray, masked: BooleanArray, width: Int, height: Int, passes: Int) {
        var current = pixels
        repeat(passes) {
            val next = current.copyOf()
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val idx = y * width + x
                    if (!masked[idx]) continue

                    var a = 0; var r = 0; var g = 0; var b = 0; var count = 0
                    forEachNeighbor(x, y, width, height) { nx, ny ->
                        val c = current[ny * width + nx]
                        a += (c ushr 24) and 0xFF
                        r += (c ushr 16) and 0xFF
                        g += (c ushr 8) and 0xFF
                        b += c and 0xFF
                        count++
                    }
                    if (count > 0) {
                        next[idx] = ((a / count) shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
                    }
                }
            }
            current = next
        }
        if (current !== pixels) System.arraycopy(current, 0, pixels, 0, pixels.size)
    }

    private inline fun forEachNeighbor(x: Int, y: Int, width: Int, height: Int, action: (Int, Int) -> Unit) {
        for (dy in -1..1) {
            for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) action(nx, ny)
            }
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private fun scaleMask(mask: Bitmap, width: Int, height: Int): Bitmap =
        Bitmap.createScaledBitmap(mask, width, height, true)

    private fun blendMasked(base: Bitmap, overlay: Bitmap, mask: Bitmap): Bitmap {
        val width = base.width
        val height = base.height
        val pixelCount = width * height

        val basePixels = IntArray(pixelCount)
        base.getPixels(basePixels, 0, width, 0, 0, width, height)
        val overlayPixels = IntArray(pixelCount)
        overlay.getPixels(overlayPixels, 0, width, 0, 0, width, height)
        val maskPixels = IntArray(pixelCount)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        for (i in 0 until pixelCount) {
            val alpha = (maskPixels[i] ushr 24) and 0xFF
            if (alpha > 32) basePixels[i] = overlayPixels[i]
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(basePixels, 0, width, 0, 0, width, height)
        return out
    }
}
