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

class KPatch(val bitmap: Bitmap, val chunks: KPatchChunks, val isPatch: Boolean = false) {

    constructor(bitmap: Bitmap) : this(bitmap, loadChunks(bitmap), true)

    private fun repeatChunk(
        canvas: Canvas,
        bounds: Rect, // 填充边界
        src: Rect, // 源区域
        scale: Float, // 缩放比例
        type: Int, // 块类型
        flags: Int, // 填充模式
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
                        bitmap, src, dst, null
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
    ) = canvas.drawBitmap(
        bitmap, src, dst, null
    )


    fun draw(
        canvas: Canvas,
        bounds: Rect = canvas.clipBounds,
        scale: Float = 1f,
        flags: Int = 0,
        debug: Boolean = false,
        demo: Boolean = false,
    ) {
        canvas.withClip(bounds) {
            val chunks = chunks.fill(bounds, scale, demo)
            for (chunk in chunks) {
                when (chunk.type) {
                    TYPE_INNER, TYPE_OUTER_X, TYPE_OUTER_Y -> repeatChunk(
                        canvas, chunk.dst!!, chunk.src, scale, chunk.type, flags
                    )

                    TYPE_FIXED -> fillChunk(
                        canvas, chunk.dst!!, chunk.src
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

    fun export(tidy: Boolean = false): Bitmap {
        if (tidy) return exportWithTidy()
        val dstWidth = bitmap.width - if (isPatch) 2 else 0
        val dstHeight = bitmap.height - if (isPatch) 2 else 0
        val newBitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)

        val dst = Rect(1, 1, dstWidth - 1, dstHeight - 1)

        CodeBlock("填充") {
            val src = if (isPatch) Rect(1, 1, bitmap.width - 2, bitmap.height - 2)
            else null
            canvas.drawBitmap(bitmap, src, dst, null)
        }

        CodeBlock("边界") {
            val rect = Rect(
                chunks.bounds.left - if (isPatch) 1 else 0,
                chunks.bounds.top - if (isPatch) 1 else 0,
                dstWidth - (bitmap.width - chunks.bounds.right) - 1,
                dstHeight - (bitmap.height - chunks.bounds.bottom) - 1
            )
            rect.offset(1, 1)
            newBitmap.setPixel(rect.left, dstHeight - 1, Color.RED)
            newBitmap.setPixel(rect.right, dstHeight - 1, Color.RED)
            newBitmap.setPixel(dstWidth - 1, rect.top, Color.RED)
            newBitmap.setPixel(dstWidth - 1, rect.bottom, Color.RED)
        }

        CodeBlock("边距") {
            val rect = Rect(
                chunks.padding.left - if (isPatch) 1 else 0,
                chunks.padding.top - if (isPatch) 1 else 0,
                dstWidth - (bitmap.width - chunks.padding.right) - 1,
                dstHeight - (bitmap.height - chunks.padding.bottom) - 1
            )
            rect.offset(1, 1)
            for (x in rect.left..rect.right) {
                newBitmap.setPixel(x, dstHeight - 1, Color.BLACK)
            }
            for (y in rect.top..rect.bottom) {
                newBitmap.setPixel(dstWidth - 1, y, Color.BLACK)
            }
        }

        CodeBlock("切割") {
            for (line in chunks.splitX) {
                val first = line.first - if (isPatch) 1 else 0
                val last = line.last - if (isPatch) 1 else 0
                for (x in first..last) {
                    newBitmap.setPixel(x, 0, Color.BLACK)
                }
            }

            for (line in chunks.splitY) {
                val first = line.first - if (isPatch) 1 else 0
                val last = line.last - if (isPatch) 1 else 0
                for (y in first..last) {
                    newBitmap.setPixel(0, y, Color.BLACK)
                }
            }

            for (line in chunks.delX) {
                val first = line.first - if (isPatch) 1 else 0
                val last = line.last - if (isPatch) 1 else 0
                for (x in first..last) {
                    newBitmap.setPixel(x, 0, Color.RED)
                }
            }

            for (line in chunks.delY) {
                val first = line.first - if (isPatch) 1 else 0
                val last = line.last - if (isPatch) 1 else 0
                for (x in first..last) {
                    newBitmap.setPixel(0, x, Color.RED)
                }
            }
        }

        return newBitmap
    }

    fun exportWithTidy(): Bitmap {
        var width = chunks.bounds.width()
        var height = chunks.bounds.height()

        val delX = chunks.delX
        val delY = chunks.delY
        val splitX = ArrayList(chunks.splitX)
        val splitY = ArrayList(chunks.splitY)
        val padding = Rect(chunks.padding)
        val bounds = Rect(chunks.bounds)

        val lineX = ArrayList<Triple<IntRange, Int, Int>>()
        val lineY = ArrayList<Triple<IntRange, Int, Int>>()

        CodeBlock("计算") {
            var offsetX = bounds.left - 1
            var offsetY = bounds.top - 1

            CodeBlock("合并排序") {
                delX.forEachIndexed { index, line ->
                    lineX.add(Triple(line, -1, index))
                }
                delY.forEachIndexed { index, line ->
                    lineY.add(Triple(line, -1, index))
                }
                splitX.forEachIndexed { index, line ->
                    lineX.add(Triple(line, 1, index))
                }
                splitY.forEachIndexed { index, line ->
                    lineY.add(Triple(line, 1, index))
                }

                lineX.sortBy { it.first.first }
                lineY.sortBy { it.first.first }
            }

            CodeBlock("重采样") {
                for (triple in lineX) {
                    val (line, type, index) = triple
                    val first = line.first
                    val last = line.last
                    if (type == -1) {
                        val size = last - first
                        width -= size
                        offsetX += size
                        continue
                    }
                    splitX[index] = first - offsetX..last - offsetX
                }

                for (triple in lineY) {
                    val (line, type, index) = triple
                    val first = line.first
                    val last = line.last
                    if (type == -1) {
                        val size = last - first
                        height -= size
                        offsetY += size
                        continue
                    }
                    splitY[index] = first - offsetY..last - offsetY
                }
            }

            width += 2
            height += 2
            bounds.set(1, 1, width - 2, height - 2)

            CodeBlock("重载边距") {
                padding.set(
                    bounds.left + (bounds.width() * ((padding.left - chunks.bounds.left) / chunks.bounds.width()
                        .toFloat())).toInt(),
                    bounds.top + (bounds.height() * ((padding.top - chunks.bounds.top) / chunks.bounds.height()
                        .toFloat())).toInt(),
                    bounds.right - (bounds.width() * ((chunks.bounds.right - padding.right) / chunks.bounds.width()
                        .toFloat())).toInt(),
                    bounds.bottom - (bounds.height() * ((chunks.bounds.bottom - padding.bottom) / chunks.bounds.height()
                        .toFloat())).toInt()
                )
            }

        }
        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)

        CodeBlock("填充") {
            draw(canvas, bounds)
        }


        CodeBlock("边界") {
            newBitmap.setPixel(bounds.left, height - 1, Color.RED)
            newBitmap.setPixel(bounds.right, height - 1, Color.RED)
            newBitmap.setPixel(width - 1, bounds.top, Color.RED)
            newBitmap.setPixel(width - 1, bounds.bottom, Color.RED)
        }

        CodeBlock("边距") {
            log("padding: $padding")
            for (x in padding.left..padding.right) {
                newBitmap.setPixel(x, height - 1, Color.BLACK)
            }
            for (y in padding.top..padding.bottom) {
                newBitmap.setPixel(width - 1, y, Color.BLACK)
            }
        }

        CodeBlock("切割") {
            for (line in splitX) {
                val first = line.first
                val last = line.last

                for (x in first..last) {
                    newBitmap.setPixel(x, 0, Color.BLACK)
                }
            }

            for (line in splitY) {
                val first = line.first
                val last = line.last
                for (y in first..last) {
                    newBitmap.setPixel(0, y, Color.BLACK)
                }
            }
        }

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
                            splitX.add(splitFirst..<index)
                            splitFirst = null
                        }
                        if (delFirst != null) {
                            delX.add(delFirst..<index)
                            delFirst = null
                        }
                    }

                    Color.RED -> {
                        if (splitFirst != null) {
                            splitX.add(splitFirst..<index)
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
                            delX.add(delFirst..<index)
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
            var boundLock = false
            for (pixel in pixels) {
                index++
                when (pixel) {
                    Color.TRANSPARENT -> continue
                    Color.RED -> if (!boundLock) {
                        bounds.left = index
                        boundLock = true
                    }

                    Color.BLACK -> {
                        padding.left = index
                        break
                    }

                    else -> break
                }
            }
            index = width - 1
            boundLock = false
            while (index >= 0) {
                val pos = index--
                when (pixels[pos]) {
                    Color.TRANSPARENT -> continue
                    Color.RED -> if (!boundLock) {
                        bounds.right = pos
                        boundLock = true
                    }

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
                            splitY.add(splitFirst..<index)
                            splitFirst = null
                        }
                        if (delFirst != null) {
                            delY.add(delFirst..<index)
                            delFirst = null
                        }
                    }

                    Color.RED -> {
                        if (splitFirst != null) {
                            splitY.add(splitFirst..<index)
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
                            delY.add(delFirst..<index)
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
            boundLock = false
            for (pixel in pixels) {
                index++
                when (pixel) {
                    Color.TRANSPARENT -> continue
                    Color.RED -> if (!boundLock) {
                        bounds.top = index
                        boundLock = true
                    }

                    Color.BLACK -> {
                        padding.top = index
                        break
                    }

                    else -> break
                }
            }
            index = height - 1
            boundLock = false
            while (index >= 0) {
                val pos = index--
                when (pixels[pos]) {
                    Color.TRANSPARENT -> continue
                    Color.RED -> if (!boundLock) {
                        bounds.bottom = pos
                        boundLock = true
                    }

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
    ) = fill(split(isDemoMode), bounds, scale, isDemoMode)

    fun fill(
        data: Triple<List<Chunk>, List<Pair<IntRange, Int>>, List<Pair<IntRange, Int>>>,// 分割数据
        bounds: Rect, // 填充区域
        scale: Float, // 块缩放比例
        isDemoMode: Boolean = false, // 是否为演示模式
    ): List<Chunk> {
        val (chunks, lineX, lineY) = data
        val dstX = HashMap<IntRange, IntRange>()
        val dstY = HashMap<IntRange, IntRange>()
        var srcFixedXSize = 0.0
        var dstFixedXSize = 0.0
        val dstSizeX = HashMap<IntRange, Int>()
        for (pair in lineX) {
            val (line, type) = pair
            if (type != 1) {
                val size = line.last - line.first
                val dstSize = (size * scale)
                srcFixedXSize += size
                if (isDemoMode || type == 0) {
                    dstFixedXSize += dstSize
                    dstSizeX[line] = dstSize.toInt()
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
                val dstSize = (dstRepeatXSize * weight)
                dstSizeX[line] = dstSize.toInt()
            }
        }
        var lastX = 0
        for (entry in dstSizeX.entries.sortedBy { it.key.first }) {
            val size = entry.value
            dstX[entry.key] = lastX..(lastX + size)
            lastX += size
        }
        var srcFixedYSize = 0.0
        var dstFixedYSize = 0.0
        val dstSizeY = HashMap<IntRange, Int>()
        for (pair in lineY) {
            val (line, type) = pair
            if (type != 1) {
                val size = line.last - line.first
                val dstSize = size * scale
                srcFixedYSize += size
                if (isDemoMode || type == 0) {
                    dstFixedYSize += dstSize
                    dstSizeY[line] = dstSize.toInt()
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
                val dstSize = dstRepeatYSize * weight
                dstSizeY[line] = dstSize.toInt()
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
) {
    kPatch.draw(
        this,
        bounds,
        scale,
        flags,
        debug,
        demo,
    )
}
