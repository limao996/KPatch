package org.limao996.kpatch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.withClip
import kotlin.collections.List
import kotlin.math.ceil
import kotlin.ranges.IntRange

class KPatch(val bitmap: Bitmap, val chunks: KPatchChunks) {

    constructor(bitmap: Bitmap) : this(bitmap, loadChunks(bitmap))

    private fun repeatChunk(
        canvas: Canvas,
        bounds: Rect, // 填充边界
        src: Rect, // 源区域
        scale: Double, // 缩放比例
        type: Int, // 块类型
        flags: Int, // 填充模式
        paint: Paint? = null,
    ) {
        canvas.withClip(bounds) {
            var repeatX = flags and type != 0
            var repeatY = repeatX
            if (type == REPEAT_INNER) {
                repeatX = flags and REPEAT_INNER_X != 0
                repeatY = flags and REPEAT_INNER_Y != 0
            }
            val dst = Rect(
                0,
                0,
                if (!repeatX) bounds.width() else (src.width() * scale).toInt(),
                if (!repeatY) bounds.height() else (src.height() * scale).toInt()
            )
            val dstSize = Size(dst.width(), dst.height())
            val countX = ceil(bounds.width() / dstSize.width.toFloat()).toInt()
            val countY = ceil(bounds.height() / dstSize.height.toFloat()).toInt()

            dst.offsetTo(bounds.left, bounds.top)
            repeat(countY) { y ->
                repeat(countX) { x ->
                    canvas.drawBitmap(
                        bitmap, src, dst, paint
                    )
                    dst.offset(dstSize.width, 0)
                }
                dst.offset(-(countX * dstSize.width), dstSize.height)
            }
        }
    }

    private fun fillChunk(
        canvas: Canvas,
        dst: Rect, // 填充边界
        src: Rect, // 源区域
        paint: Paint? = null,
    ) = canvas.drawBitmap(
        bitmap, src, dst, paint
    )


    fun draw(
        canvas: Canvas,
        bounds: Rect,
        scale: Double = 1.0,
        flags: Int = 0,
        debug: Boolean = false,
        paint: Paint? = null,
    ) {
        canvas.withClip(bounds) {
            val chunks = chunks.fill(bounds, scale)
            for (chunk in chunks) {
                when (chunk.type) {
                    REPEAT_INNER, REPEAT_OUTER_X, REPEAT_OUTER_Y -> repeatChunk(
                        canvas, chunk.dst!!, chunk.src, scale, chunk.type, flags, paint
                    )

                    REPEAT_FIXED -> fillChunk(
                        canvas, chunk.dst!!, chunk.src, paint
                    )
                }
                if (debug) canvas.drawRect(chunk.dst!!, Paint().apply {
                    color = when (chunk.type) {
                        REPEAT_INNER -> Color.BLUE
                        REPEAT_OUTER_X -> Color.GREEN
                        REPEAT_OUTER_Y -> Color.YELLOW
                        else -> Color.TRANSPARENT
                    }
                    alpha = 80
                    style = Paint.Style.FILL
                })
            }
        }
        if (debug) canvas.drawRect(bounds, Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 5f
        })
    }

    companion object {

        fun loadChunks(bitmap: Bitmap): KPatchChunks {
            val width = bitmap.width
            val height = bitmap.height

            val bounds = Rect(1, 1, width - 2, height - 2)
            val splitX = ArrayList<IntRange>(1)
            val splitY = ArrayList<IntRange>(1)
            val padding = Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)

            // top
            var pixels = IntArray(width)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, 1)
            var index = -1
            var first: Int? = null
            for (pixel in pixels) {
                index++
                when (pixel) {
                    Color.TRANSPARENT -> {
                        if (first != null) {
                            splitX.add(first..index - 1)
                            first = null
                        }
                    }

                    Color.BLACK -> {
                        if (first == null) {
                            first = index
                        }
                    }

                    else -> break
                }
            }

            // bottom
            pixels = IntArray(width)
            bitmap.getPixels(pixels, 0, width, 0, height - 1, width, 1)
            index = -1
            for (pixel in pixels) {
                index++
                when (pixel) {
                    Color.TRANSPARENT -> continue
                    Color.RED -> bounds.left = index
                    Color.BLACK -> {
                        padding.left = index
                        break
                    }

                    else -> break
                }
            }
            index = width - 1
            while (index >= 0) {
                val pos = index--
                when (pixels[pos]) {
                    Color.TRANSPARENT -> continue
                    Color.RED -> bounds.right = pos
                    Color.BLACK -> {
                        padding.right = pos
                        break
                    }

                    else -> break
                }
            }

            // left
            pixels = IntArray(height)
            bitmap.getPixels(pixels, 0, 1, 0, 0, 1, height)
            index = -1
            first = null
            for (pixel in pixels) {
                index++
                when (pixel) {
                    Color.TRANSPARENT -> {
                        if (first != null) {
                            splitY.add(first..index - 1)
                            first = null
                        }
                    }

                    Color.BLACK -> {
                        if (first == null) {
                            first = index
                        }
                    }

                    else -> break
                }
            }

            // right
            pixels = IntArray(height)
            bitmap.getPixels(pixels, 0, 1, width - 1, 0, 1, height)
            index = -1
            for (pixel in pixels) {
                index++
                when (pixel) {
                    Color.TRANSPARENT -> continue
                    Color.RED -> bounds.top = index
                    Color.BLACK -> {
                        padding.top = index
                        break
                    }

                    else -> break
                }
            }
            index = height - 1
            while (index >= 0) {
                val pos = index--
                when (pixels[pos]) {
                    Color.TRANSPARENT -> continue
                    Color.RED -> bounds.bottom = pos
                    Color.BLACK -> {
                        padding.bottom = pos
                        break
                    }

                    else -> break
                }
            }

            if (splitX.isEmpty()) splitX.add(bounds.left..bounds.right)
            if (splitY.isEmpty()) splitY.add(bounds.top..bounds.bottom)

            return KPatchChunks(
                bounds = bounds,
                splitX = splitX,
                splitY = splitY,
                padding = padding,
            )
        }

        const val REPEAT_INNER_X = 1
        const val REPEAT_INNER_Y = 1 shl 1
        const val REPEAT_INNER_BOTH = REPEAT_INNER_X or REPEAT_INNER_Y
        const val REPEAT_INNER = REPEAT_INNER_BOTH
        const val REPEAT_INNER_NONE = 0
        const val REPEAT_OUTER_X = 1 shl 2
        const val REPEAT_OUTER_Y = 1 shl 3
        const val REPEAT_OUTER_ALL = REPEAT_OUTER_X or REPEAT_OUTER_Y
        const val REPEAT_OUTER_NONE = 0
        const val REPEAT_FIXED = 0
    }
}


data class KPatchChunks(
    val bounds: Rect, // 素材源区域
    val splitX: List<IntRange>, // X轴切割线 范围内部为可拉伸区域
    val splitY: List<IntRange>, // Y轴切割线 范围内部为可拉伸区域
    val padding: Rect, // 内容边距 (这里直接忽略)
) {
    data class Chunk(
        val src: Rect, // 块源区域
        val type: Int, //块类型
        var dst: Rect? = null, // 填充区域
    )

    fun split(): Triple<List<Chunk>, List<Pair<IntRange, Boolean>>, List<Pair<IntRange, Boolean>>> {
        val chunks = ArrayList<Chunk>()
        val lineXList = ArrayList<Pair<IntRange, Boolean>>()
        val lineYList = ArrayList<Pair<IntRange, Boolean>>()

        var last = bounds.left
        for (line in splitX) {
            if (last < line.first) {
                lineXList.add(last..line.first to false)
            }
            lineXList.add(line to true)
            last = line.last //+ 1
        }
        if (last < bounds.right) {
            lineXList.add(last..bounds.right to false)
        }

        last = bounds.top
        for (line in splitY) {
            if (last < line.first) {
                lineYList.add(last..line.first to false)
            }
            lineYList.add(line to true)
            last = line.last //+ 1
        }
        if (last < bounds.bottom) {
            lineYList.add(last..bounds.bottom to false)
        }
        for (lineX in lineXList) {
            for (lineY in lineYList) {
                chunks.add(
                    Chunk(
                        src = Rect(
                            lineX.first.first, lineY.first.first, lineX.first.last, lineY.first.last
                        ), type = when {
                            lineX.second && lineY.second -> KPatch.REPEAT_INNER
                            lineX.second -> KPatch.REPEAT_OUTER_X
                            lineY.second -> KPatch.REPEAT_OUTER_Y
                            else -> KPatch.REPEAT_FIXED
                        }
                    )
                )
            }
        }

        return Triple(chunks, lineXList, lineYList)
    }

    fun fill(
        bounds: Rect, // 填充区域
        scale: Double, // 块缩放比例
    ): List<Chunk> {
        val (chunks, lineX, lineY) = split()
        val dstX = HashMap<IntRange, IntRange>()
        val dstY = HashMap<IntRange, IntRange>()
        var srcFixedXSize = 0
        var dstFixedXSize = 0
        val dstSizeX = HashMap<IntRange, Int>()
        for (pair in lineX) {
            val (line, type) = pair
            if (!type) {
                val size = line.last - line.first
                val dstSize = (size * scale).toInt()
                srcFixedXSize += size
                dstFixedXSize += dstSize
                dstSizeX[line] = dstSize
            }
        }
        val srcRepeatXSize = this.bounds.width() - srcFixedXSize
        val dstRepeatXSize = bounds.width() - dstFixedXSize
        for (pair in lineX) {
            val (line, type) = pair
            if (type) {
                val size = line.last - line.first
                val weight = size.toDouble() / srcRepeatXSize
                val dstSize = (dstRepeatXSize * weight).toInt()
                dstSizeX[line] = dstSize
            }
        }
        var lastX = 0
        for (entry in dstSizeX.entries.sortedBy { it.key.first }) {
            val size = entry.value
            dstX[entry.key] = lastX..(lastX + size)
            lastX += size //+ 1
        }
        var srcFixedYSize = 0
        var dstFixedYSize = 0
        val dstSizeY = HashMap<IntRange, Int>()
        for (pair in lineY) {
            val (line, type) = pair
            if (!type) {
                val size = line.last - line.first
                val dstSize = (size * scale).toInt()
                srcFixedYSize += size
                dstFixedYSize += dstSize
                dstSizeY[line] = dstSize
            }
        }
        val srcRepeatYSize = this.bounds.height() - srcFixedYSize
        val dstRepeatYSize = bounds.height() - dstFixedYSize
        for (pair in lineY) {
            val (line, type) = pair
            if (type) {
                val size = line.last - line.first
                val weight = size.toDouble() / srcRepeatYSize
                val dstSize = (dstRepeatYSize * weight).toInt()
                dstSizeY[line] = dstSize
            }
        }
        var lastY = 0
        for (entry in dstSizeY.entries.sortedBy { it.key.first }) {
            val size = entry.value
            dstY[entry.key] = lastY..(lastY + size)
            lastY += size //+ 1
        }

        for (chunk in chunks) {
            val lineX = dstX[chunk.src.left..chunk.src.right]!!
            val lineY = dstY[chunk.src.top..chunk.src.bottom]!!
            chunk.dst = Rect(
                lineX.first, lineY.first, lineX.last, lineY.last
            ).apply {
                offset(bounds.left, bounds.top)
            }
        }

        return chunks
    }

}

fun Canvas.drawKPatch(
    kPatch: KPatch,
    bounds: Rect,
    scale: Double = 1.0,
    flags: Int = 0,
    debug: Boolean = false,
    paint: Paint? = null,
) {
    kPatch.draw(
        this,
        bounds,
        scale,
        flags,
        debug,
        paint,
    )
}