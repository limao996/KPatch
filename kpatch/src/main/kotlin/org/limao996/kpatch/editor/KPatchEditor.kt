package org.limao996.kpatch.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import org.limao996.kpatch.KPatch
import org.limao996.kpatch.KPatch.Companion.TYPE_DEL
import org.limao996.kpatch.KPatch.Companion.TYPE_FIXED
import org.limao996.kpatch.KPatch.Companion.TYPE_INNER
import org.limao996.kpatch.KPatch.Companion.TYPE_OUTER_X
import org.limao996.kpatch.KPatch.Companion.TYPE_OUTER_Y
import org.limao996.kpatch.KPatchChunks
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class KPatchEditor(val kPatch: KPatch) {
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

    open fun drawBackground(canvas: Canvas, bounds: Rect) {
        canvas.drawColor(0x10000000.toInt())
    }

    open val paints = mapOf(
        TYPE_INNER to Paint().apply {
            color = 0x570000ff
            style = Paint.Style.FILL
        },
        TYPE_OUTER_X to Paint().apply {
            color = 0x5700ff00
            style = Paint.Style.FILL
        },
        TYPE_OUTER_Y to Paint().apply {
            color = 0x57ffff00
            style = Paint.Style.FILL
        },
        TYPE_DEL to Paint().apply {
            color = 0x57ff0000
            style = Paint.Style.FILL
        },
        TYPE_FIXED to Paint().apply {
            color = 0x57000000
            style = Paint.Style.FILL
        },
        "bounds" to Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        },
    )

    open fun drawBody(canvas: Canvas, bounds: Rect) {
        val bitmap = kPatch.bitmap
        val chunks = kPatch.chunks
        val srcBounds = Rect(0, 0, bitmap.width - 1, bitmap.height - 1)

        val scale = scale!!
        val offsetX = offsetX!!
        val offsetY = offsetY!!
        canvas.withTranslation(offsetX, offsetY) {
            canvas.withScale(scale, scale) {
                //kPatch.draw(canvas, bitmapBounds)
                canvas.drawBitmap(bitmap, null, srcBounds, null)
                val chunkList = chunks.fill(chunks.bounds, 1f, true)
                for (chunk in chunkList) {
                    canvas.drawRect(chunk.dst!!, paints[chunk.type]!!)
                }
            }
            val boundsRect = RectF(
                chunks.bounds.left * scale,
                chunks.bounds.top * scale,
                chunks.bounds.right * scale,
                chunks.bounds.bottom * scale
            )
            canvas.drawRect(boundsRect, paints["bounds"]!!)
        }
    }

    open fun draw(
        canvas: Canvas, bounds: Rect = canvas.clipBounds
    ) {
        val bitmap = kPatch.bitmap

        scale = scale ?: (min(bounds.width(), bounds.height()) / max(
            bitmap.width, bitmap.height
        ).toFloat() * 0.8f)

        offsetX = offsetX ?: (bounds.centerX() - (bitmap.width * scale!! / 2f))
        offsetY = offsetY ?: (bounds.centerY() - (bitmap.height * scale!! / 2f))

        drawBackground(canvas, bounds)
        drawBody(canvas, bounds)
    }
}