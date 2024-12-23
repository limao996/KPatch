package org.limao996.kpatch.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import org.limao996.kpatch.KPatch
import org.limao996.kpatch.KPatchChunks
import org.limao996.kpatch.log
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

class KPatchEditor(val kPatch: KPatch) {
    constructor(
        bitmap: Bitmap, chunks: KPatchChunks, isPatch: Boolean = false
    ) : this(KPatch(bitmap, chunks, isPatch))

    constructor(bitmap: Bitmap) : this(KPatch(bitmap))

    var offsetX: Float? = null
    var offsetY: Float? = null

    @JvmField
    var scale: Float? = null

    fun offset(dx: Float = 0f, dy: Float = 0f) {
        offsetX = offsetX!! + dx
        offsetY = offsetY!! + dy
    }

    fun scale(value: Float) {
        scale = scale!! * value
    }

    fun scale(centroidX: Float, centroidY: Float, value: Float) {
        val oldScale = scale!!
        scale(value)
        // 1. 计算触摸点在视图坐标系中的相对位置
        val relativeX = centroidX - offsetX!!
        val relativeY = centroidY - offsetY!!

        // 2. 计算缩放前和缩放后的视图坐标
        val newRelativeX = relativeX * scale!! / oldScale
        val newRelativeY = relativeY * scale!! / oldScale

        // 3. 计算新的偏移量，保持触摸点在视图中的位置不变
        offsetX = centroidX - newRelativeX
        offsetY = centroidY - newRelativeY
    }

    fun draw(
        canvas: Canvas, bounds: Rect = canvas.clipBounds, paint: Paint? = null
    ) {
        val bitmap = kPatch.bitmap
        val chunks = kPatch.chunks
        val bitmapBounds = Rect(0, 0, bitmap.width, bitmap.height)

        scale = scale ?: (min(bounds.width(), bounds.height()) / max(
            bitmap.width, bitmap.height
        ).toFloat() * 0.8f)

        offsetX = offsetX ?: (bounds.centerX() - (bitmap.width * scale!! / 2f))
        offsetY = offsetY ?: (bounds.centerY() - (bitmap.height * scale!! / 2f))

        val scale = scale!!
        val offsetX = offsetX!!
        val offsetY = offsetY!!

        canvas.withClip(bounds) {
            save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)
            drawBitmap(bitmap, null, bitmapBounds, paint)
            restore()
        }
    }
}