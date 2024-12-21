package org.limao996.kpatch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
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
            if (type == TYPE_INNER) {
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
                    TYPE_INNER, REPEAT_OUTER_X, REPEAT_OUTER_Y -> repeatChunk(
                        canvas, chunk.dst!!, chunk.src, scale, chunk.type, flags, paint
                    )

                    TYPE_FIXED -> fillChunk(
                        canvas, chunk.dst!!, chunk.src, paint
                    )
                }
                if (debug) canvas.drawRect(chunk.dst!!, Paint().apply {
                    color = when (chunk.type) {
                        TYPE_INNER -> Color.BLUE
                        REPEAT_OUTER_X -> Color.GREEN
                        REPEAT_OUTER_Y -> Color.YELLOW
                        TYPE_FIXED -> Color.TRANSPARENT
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
            val delX = ArrayList<IntRange>(1)
            val delY = ArrayList<IntRange>(1)
            val padding = Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)

            // top
            var pixels = IntArray(width)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, 1)
            var index = -1
            var delFirst: Int? = null
            var splitFirst: Int? = null
            for (pixel in pixels) {
                index++
                when (pixel) {
                    Color.TRANSPARENT -> {
                        if (splitFirst != null) {
                            splitX.add(splitFirst..index - 1)
                            splitFirst = null
                        }
                        if (delFirst != null) {
                            delX.add(delFirst..index - 1)
                            delFirst = null
                        }
                    }

                    Color.RED -> {
                        if (splitFirst != null) {
                            splitX.add(splitFirst..index - 1)
                            splitFirst = null
                        }
                        if (delFirst == null) {
                            delFirst = index
                        }
                    }

                    Color.BLACK -> {
                        if (splitFirst == null) {
                            splitFirst = index
                        }
                        if (delFirst != null) {
                            delX.add(delFirst..index - 1)
                            delFirst = null
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
            splitFirst = null
            delFirst = null
            for (pixel in pixels) {
                index++
                when (pixel) {
                    Color.TRANSPARENT -> {
                        if (splitFirst != null) {
                            splitY.add(splitFirst..index - 1)
                            splitFirst = null
                        }
                        if (delFirst != null) {
                            delY.add(delFirst..index - 1)
                            delFirst = null
                        }
                    }

                    Color.RED -> {
                        if (splitFirst != null) {
                            splitY.add(splitFirst..index - 1)
                            splitFirst = null
                        }
                        if (delFirst == null) {
                            delFirst = index
                        }
                    }

                    Color.BLACK -> {
                        if (splitFirst == null) {
                            splitFirst = index
                        }
                        if (delFirst != null) {
                            delY.add(delFirst..index - 1)
                            delFirst = null
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
                delX = delX,
                delY = delY,
                padding = padding,
            )
        }

        const val REPEAT_INNER_X = 1
        const val REPEAT_INNER_Y = 1 shl 1
        const val REPEAT_INNER_BOTH = REPEAT_INNER_X or REPEAT_INNER_Y
        const val TYPE_INNER = REPEAT_INNER_BOTH
        const val REPEAT_INNER_NONE = 0
        const val REPEAT_OUTER_X = 1 shl 2
        const val REPEAT_OUTER_Y = 1 shl 3
        const val TYPE_OUTER_X = REPEAT_OUTER_X
        const val TYPE_OUTER_Y = REPEAT_OUTER_Y
        const val REPEAT_OUTER_ALL = REPEAT_OUTER_X or REPEAT_OUTER_Y
        const val REPEAT_OUTER_NONE = 0
        const val TYPE_FIXED = 1 shl 4
        const val TYPE_DEL = 1 shl 5
    }
}


data class KPatchChunks(
    val bounds: Rect, // 素材源区域
    val splitX: List<IntRange>, // X轴切割范围 范围内部为可拉伸区域
    val splitY: List<IntRange>, // Y轴切割范围 范围内部为可拉伸区域
    val delX: List<IntRange> = emptyList(), // X轴删除范围 范围内部为删除区域
    val delY: List<IntRange> = emptyList(), // Y轴删除范围 范围内部为删除区域
    val padding: Rect, // 内容边距 (这里直接忽略)
) {
    data class Chunk(
        val src: Rect, // 块源区域
        val type: Int, //块类型
        var dst: Rect? = null, // 填充区域
    )

    fun split(): Triple<List<Chunk>, List<Pair<IntRange, Int>>, List<Pair<IntRange, Int>>> {
        val chunks = ArrayList<Chunk>()
        val lineXList = ArrayList<Pair<IntRange, Int>>()
        val lineYList = ArrayList<Pair<IntRange, Int>>()
        val lineX = ArrayList<Pair<IntRange, Int>>()
        val lineY = ArrayList<Pair<IntRange, Int>>()

        for (line in splitX) lineX.add(line to 1)
        for (line in splitY) lineY.add(line to 1)
        for (line in delX) lineX.add(line to -1)
        for (line in delY) lineY.add(line to -1)
        lineX.sortBy { it.first.first }
        lineY.sortBy { it.first.first }

        var last = bounds.left
        for (pair in lineX) {
            val (line, type) = pair
            if (last < line.first) {
                lineXList.add(last..line.first to 0)
            }
            lineXList.add(line to type)
            last = line.last //+ 1
        }
        if (last < bounds.right) {
            lineXList.add(last..bounds.right to 0)
        }

        last = bounds.top
        for (pair in lineY) {
            val (line, type) = pair
            if (last < line.first) {
                lineYList.add(last..line.first to 0)
            }
            lineYList.add(line to type)
            last = line.last //+ 1
        }
        if (last < bounds.bottom) {
            lineYList.add(last..bounds.bottom to 0)
        }

        for (lineX in lineXList) {
            for (lineY in lineYList) {
                if (lineX.second == -1 || lineY.second == -1) continue
                chunks.add(
                    Chunk(
                        src = Rect(
                            lineX.first.first, lineY.first.first, lineX.first.last, lineY.first.last
                        ), type = when {
                            lineX.second == 1 && lineY.second == 1 -> KPatch.TYPE_INNER
                            lineX.second == 1 -> KPatch.REPEAT_OUTER_X
                            lineY.second == 1 -> KPatch.REPEAT_OUTER_Y
                            lineY.second == 0 -> KPatch.TYPE_FIXED
                            else -> KPatch.TYPE_DEL
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
            if (type != 1) {
                val size = line.last - line.first
                val dstSize = (size * scale).toInt()
                srcFixedXSize += size
                if (type == 0) {
                    dstFixedXSize += dstSize
                    dstSizeX[line] = dstSize
                }
            }
        }
        val srcRepeatXSize = this.bounds.width() - srcFixedXSize
        val dstRepeatXSize = bounds.width() - dstFixedXSize
        for (pair in lineX) {
            val (line, type) = pair
            if (type == 1) {
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
            if (type != 1) {
                val size = line.last - line.first
                val dstSize = (size * scale).toInt()
                srcFixedYSize += size
                if (type == 0) {
                    dstFixedYSize += dstSize
                    dstSizeY[line] = dstSize
                }
            }
        }
        val srcRepeatYSize = this.bounds.height() - srcFixedYSize
        val dstRepeatYSize = bounds.height() - dstFixedYSize
        for (pair in lineY) {
            val (line, type) = pair
            if (type == 1) {
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


private fun log(vararg msg: Any?) {
    Log.i("KPatch-Log", msg.joinToString("\t"))
}