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

class KPatch(val bitmap: Bitmap, val chunks: KPatchChunks, val isPatch: Boolean = false) {

    constructor(bitmap: Bitmap) : this(bitmap, loadChunks(bitmap), true)

    private fun repeatChunk(
        canvas: Canvas,
        bounds: Rect, // 填充边界
        src: Rect, // 源区域
        scale: Float, // 缩放比例
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
        bounds: Rect = canvas.clipBounds,
        scale: Float = 1f,
        flags: Int = 0,
        debug: Boolean = false,
        demo: Boolean = false,
        paint: Paint? = null,
    ) {
        canvas.withClip(bounds) {
            val chunks = chunks.fill(bounds, scale, demo)
            for (chunk in chunks) {
                when (chunk.type) {
                    TYPE_INNER, TYPE_OUTER_X, TYPE_OUTER_Y -> repeatChunk(
                        canvas, chunk.dst!!, chunk.src, scale, chunk.type, flags, paint
                    )

                    TYPE_FIXED -> fillChunk(
                        canvas, chunk.dst!!, chunk.src, paint
                    )
                }
                if (debug) canvas.drawRect(chunk.dst!!, Paint().apply {
                    color = when (chunk.type) {
                        TYPE_INNER -> 0x570000ff
                        TYPE_OUTER_X -> 0x5700ff00
                        TYPE_OUTER_Y -> 0x57ffff00
                        TYPE_DEL -> 0x57ff0000
                        TYPE_FIXED -> 0x57000000
                        else -> Color.TRANSPARENT
                    }.toInt()
                    style = Paint.Style.FILL
                })
            }
        }
        if (debug) canvas.drawRect(Rect(
            bounds.left - 2, bounds.top - 2, bounds.right - 2, bounds.bottom - 2
        ), Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f
        })
    }

    fun export(paint: Paint? = null): Bitmap {
        val offset = if (isPatch) 0 else 1
        var width = bitmap.width + (offset * 2)
        var height = bitmap.height + (offset * 2)
        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        val src = Rect(offset, offset, width - 1 - offset, height - 1 - offset)
        val dst = Rect(offset, offset, width - 1 - offset, height - 1 - offset)
        canvas.drawBitmap(bitmap, src, dst, paint)

        // 边界
        val bounds = Rect(chunks.bounds)
        bounds.offset(offset, offset)
        newBitmap.setPixel(bounds.left, height - 1, Color.RED)
        newBitmap.setPixel(bounds.right, height - 1, Color.RED)
        newBitmap.setPixel(width - 1, bounds.top, Color.RED)
        newBitmap.setPixel(width - 1, bounds.bottom, Color.RED)

        // 切割
        for (line in chunks.splitX) {
            val start = line.first + offset
            val end = line.last + offset
            for (pos in start..end) newBitmap.setPixel(pos, 0, Color.BLACK)
        }
        for (line in chunks.splitY) {
            val start = line.first + offset
            val end = line.last + offset
            for (pos in start..end) newBitmap.setPixel(0, pos, Color.BLACK)
        }

        // 删除
        for (line in chunks.delX) {
            val start = line.first + offset
            val end = line.last + offset
            for (pos in start..end) newBitmap.setPixel(pos, 0, Color.RED)
        }
        for (line in chunks.delY) {
            val start = line.first + offset
            val end = line.last + offset
            for (pos in start..end) newBitmap.setPixel(0, pos, Color.RED)
        }

        // 边距
        val padding = Rect(chunks.padding)
        padding.offset(offset, offset)
        for (pos in padding.left..padding.right) newBitmap.setPixel(pos, height - 1, Color.BLACK)

        for (pos in padding.top..padding.bottom) newBitmap.setPixel(width - 1, pos, Color.BLACK)


        return newBitmap
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
    var bounds: Rect, // 素材源区域
    var splitX: List<IntRange>, // X轴切割范围 范围内部为可拉伸区域
    var splitY: List<IntRange>, // Y轴切割范围 范围内部为可拉伸区域
    var delX: List<IntRange> = emptyList(), // X轴删除范围 范围内部为删除区域
    var delY: List<IntRange> = emptyList(), // Y轴删除范围 范围内部为删除区域
    var padding: Rect, // 内容边距
) {
    data class Chunk(
        val src: Rect, // 块源区域
        val type: Int, //块类型
        var dst: Rect? = null, // 填充区域
    )

    fun split(
        isDemoMode: Boolean = false, // 是否为演示模式
    ): Triple<List<Chunk>, List<Pair<IntRange, Int>>, List<Pair<IntRange, Int>>> {
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
            last = line.last
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
            last = line.last
        }
        if (last < bounds.bottom) {
            lineYList.add(last..bounds.bottom to 0)
        }

        for (lineX in lineXList) {
            for (lineY in lineYList) {
                if (!isDemoMode && (lineX.second == -1 || lineY.second == -1)) continue
                chunks.add(
                    Chunk(
                        src = Rect(
                            lineX.first.first, lineY.first.first, lineX.first.last, lineY.first.last
                        ), type = when {
                            lineX.second == -1 || lineY.second == -1 -> KPatch.TYPE_DEL
                            lineX.second == 1 && lineY.second == 1 -> KPatch.TYPE_INNER
                            lineX.second == 1 -> KPatch.TYPE_OUTER_X
                            lineY.second == 1 -> KPatch.TYPE_OUTER_Y
                            lineY.second == 0 -> KPatch.TYPE_FIXED
                            else -> 0
                        }
                    )
                )
            }
        }

        return Triple(chunks, lineXList, lineYList)
    }

    fun fill(
        bounds: Rect, // 填充区域
        scale: Float, // 块缩放比例
        isDemoMode: Boolean = false, // 是否为演示模式
    ): List<Chunk> {
        val (chunks, lineX, lineY) = split(isDemoMode)
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
                if (isDemoMode || type == 0) {
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
            lastX += size
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
                if (isDemoMode || type == 0) {
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
            lastY += size
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
    bounds: Rect = clipBounds,
    scale: Float = 1f,
    flags: Int = 0,
    debug: Boolean = false,
    demo: Boolean = false,
    paint: Paint? = null,
) {
    kPatch.draw(
        this,
        bounds,
        scale,
        flags,
        debug,
        demo,
        paint,
    )
}

fun log(vararg msg: Any?) {
    Log.i("KPatch-Log", msg.joinToString("\t"))
}