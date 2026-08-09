package com.unmark.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** A region as fractions of image width/height, so it applies at any resolution. */
data class NormalizedRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** Builds a mask bitmap with just this region painted in, the shape Inpainter expects. */
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
