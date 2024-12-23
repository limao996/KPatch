package org.limao996.kpatch.editor

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF

const val DefaultDepth = 0f

data class KPointF(
    var rawPointF: PointF = PointF(), val depth: Float = DefaultDepth
) {
    constructor(x: Float, y: Float, depth: Float = DefaultDepth) : this(PointF(x, y), depth)

    var scale = 1f

    fun translate(dx: Float = 0f, dy: Float = 0f) = rawPointF.offset(dx, dy)


    fun translateTo(x: Float = rawPointF.x, y: Float = rawPointF.y) = rawPointF.set(x, y)

    fun scale(dv: Float) {
        scale += dv
    }

    fun scaleTo(value: Float) {
        scale = value
    }

    val x: Float
        get() = rawPointF.x * scale * (1 - depth)

    val y: Float
        get() = rawPointF.y * scale * (1 - depth)

    fun toPoint() = Point(x.toInt(), y.toInt())
    fun toPointF() = PointF(x, y)
}

data class KRectF(val rawRectF: RectF = RectF(), val depth: Float = DefaultDepth) {
    constructor(
        left: Float, top: Float, right: Float, bottom: Float, depth: Float = DefaultDepth
    ) : this(RectF(left, top, right, bottom), depth)

    constructor(
        width: Float, height: Float, depth: Float = DefaultDepth
    ) : this(
        0f, 0f, width, height, depth
    )

    var scale = 1f

    fun translate(dx: Float = 0f, dy: Float = 0f) = rawRectF.offset(dx, dy)
    fun translateTo(x: Float = rawRectF.left, y: Float = rawRectF.top) = rawRectF.offsetTo(x, y)

    fun scale(dv: Float) {
        scale += dv
    }

    fun scaleTo(value: Float) {
        scale = value
    }

    val factory: Float
        get() = scale * (1 - depth)

    val left: Float
        get() = (rawRectF.left * factory)
    val top: Float
        get() = (rawRectF.top * factory)
    val right: Float
        get() = (rawRectF.right * factory)
    val bottom: Float
        get() = (rawRectF.bottom * factory)

    val width: Float
        get() = right - left
    val height: Float
        get() = bottom - top

    fun toRect() = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    fun toRectF() = RectF(left, top, right, bottom)
}