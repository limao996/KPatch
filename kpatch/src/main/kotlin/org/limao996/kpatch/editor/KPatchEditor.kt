package org.limao996.kpatch.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import androidx.core.graphics.toRectF
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import org.limao996.kpatch.CodeBlock
import org.limao996.kpatch.KPatch
import org.limao996.kpatch.KPatch.Companion.TYPE_DEL
import org.limao996.kpatch.KPatch.Companion.TYPE_FIXED
import org.limao996.kpatch.KPatch.Companion.TYPE_INNER
import org.limao996.kpatch.KPatch.Companion.TYPE_OUTER_X
import org.limao996.kpatch.KPatch.Companion.TYPE_OUTER_Y
import org.limao996.kpatch.KPatchChunks
import org.limao996.kpatch.log
import kotlin.math.max
import kotlin.math.min


private val CHUNK_TYPE_MAP = mapOf(
    TYPE_INNER to "chunk_inner",
    TYPE_OUTER_X to "chunk_outerX",
    TYPE_OUTER_Y to "chunk_outerY",
    TYPE_DEL to "chunk_del",
    TYPE_FIXED to "chunk_fixed",
)

private val SPLIT_TYPE_MAP = mapOf(
    1 to "split_expand",
    -1 to "split_remove",
)

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
        val relativeX = centroidX - offsetX!!
        val relativeY = centroidY - offsetY!!

        val newRelativeX = relativeX * scale!! / oldScale
        val newRelativeY = relativeY * scale!! / oldScale

        offsetX = centroidX - newRelativeX
        offsetY = centroidY - newRelativeY
    }


    open fun drawBackground(canvas: Canvas, bounds: Rect) {
        canvas.drawColor(0x10000000.toInt())
    }

    open val paints = mapOf(
        "chunk_inner" to Paint().apply {
            color = 0x300000ff
            style = Paint.Style.FILL
        },
        "chunk_outerX" to Paint().apply {
            color = 0x300000ff
            style = Paint.Style.FILL
        },
        "chunk_outerY" to Paint().apply {
            color = 0x300000ff
            style = Paint.Style.FILL
        },
        "chunk_del" to Paint().apply {
            color = 0x30ff0000
            style = Paint.Style.FILL
        },
        "chunk_fixed" to Paint().apply {
            color = 0x30ff7f00
            style = Paint.Style.FILL
        },
        "split_expand" to TextPaint().apply {
            color = Color.BLUE
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            textSize = 4f
        },
        "split_remove" to TextPaint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            textSize = 4f
        },
        "bounds" to TextPaint().apply {
            color = Color.MAGENTA
            style = Paint.Style.STROKE
            textAlign = Paint.Align.CENTER
            textSize = 4f
            pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
        },
        "padding" to TextPaint().apply {
            color = Color.rgb(0, 179, 255)
            style = Paint.Style.STROKE
            textAlign = Paint.Align.CENTER
            textSize = 4f
            pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
        },
    )

    open fun drawBody(canvas: Canvas, bounds: Rect) {
        val bitmap = kPatch.bitmap
        val chunks = kPatch.chunks
        val srcBounds = Rect(0, 0, bitmap.width - 1, bitmap.height - 1)

        val scale = scale!!
        val offsetX = offsetX!!
        val offsetY = offsetY!!

        val data = chunks.split(true)
        val chunkList = chunks.fill(data, chunks.bounds, 1f, true)
        val (_, lineX, lineY) = data

        canvas.withTranslation(offsetX, offsetY) {
            CodeBlock("素材与块") {
                canvas.withScale(scale, scale) {
                    canvas.drawBitmap(bitmap, null, srcBounds, null)

                    for (chunk in chunkList) {
                        val dst = chunk.dst!!.toRectF()
                        val paint = paints[CHUNK_TYPE_MAP[chunk.type]]!!
                        canvas.drawRect(dst, paint)
                    }

                }
            }

            CodeBlock("分割线") {
                CodeBlock("X轴") {
                    for (pair in lineX) {
                        val (line, type) = pair
                        if (type == 0) continue
                        val paint = paints[SPLIT_TYPE_MAP[type]]!! as TextPaint
                        CodeBlock("线") {
                            val left = line.first * scale
                            val right = line.last * scale
                            val top = bounds.top - offsetY
                            val bottom = bounds.bottom - offsetY
                            canvas.drawLine(
                                left, top, left, bottom, paint
                            )
                            canvas.drawLine(
                                right, top, right, bottom, paint
                            )
                        }

                        val size = line.last - line.first
                        val text = "$size px"
                        val textBounds = Rect()
                        paint.getTextBounds(text, 0, text.length - 1, textBounds)

                        CodeBlock("标签") {
                            val textX = line.first + (size / 2f)
                            val textY1 = srcBounds.top - paint.descent() - 1f
                            val textY2 = srcBounds.bottom - paint.ascent() + 1f

                            canvas.withScale(scale, scale) {
                                canvas.drawText(text, textX, textY1, paint)
                                canvas.drawText(text, textX, textY2, paint)
                            }
                        }

                        CodeBlock("标签线") {
                            val first = line.first.toFloat()
                            val last = line.last.toFloat()
                            val textWidth = textBounds.width()
                            val textHeight = textBounds.height()
                            val center = (line.first + line.last) / 2f
                            val top = srcBounds.top - (textHeight / 2) - 1f
                            val bottom = srcBounds.bottom + (textHeight / 2) + 1f

                            canvas.withScale(scale, scale) {
                                CodeBlock("上") {
                                    canvas.drawLine(
                                        first, top, center - (textWidth / 2) - 1, top, paint
                                    )
                                    canvas.drawLine(
                                        center + (textWidth / 2) + 1, top, last, top, paint
                                    )
                                }

                                CodeBlock("下") {
                                    canvas.drawLine(
                                        first, bottom, center - (textWidth / 2) - 1, bottom, paint
                                    )
                                    canvas.drawLine(
                                        center + (textWidth / 2) + 1, bottom, last, bottom, paint
                                    )
                                }
                            }
                        }
                    }
                }

                CodeBlock("Y轴") {
                    for (pair in lineY) {
                        val (line, type) = pair
                        if (type == 0) continue
                        val paint = paints[SPLIT_TYPE_MAP[type]]!! as TextPaint

                        CodeBlock("线") {
                            val left = bounds.left - offsetX
                            val right = bounds.right - offsetX
                            val top = line.first * scale
                            val bottom = line.last * scale
                            canvas.drawLine(
                                left, top, right, top, paint
                            )
                            canvas.drawLine(
                                left, bottom, right, bottom, paint
                            )
                        }

                        val size = line.last - line.first
                        val text = "$size px"
                        val textBounds = Rect()
                        paint.getTextBounds(text, 0, text.length - 1, textBounds)

                        CodeBlock("标签") {
                            val textX1 = srcBounds.left - (textBounds.width() / 2f) - 1f
                            val textX2 = srcBounds.right + (textBounds.width() / 2f) + 1f
                            val textY =
                                line.first + (size / 2f) + (((-paint.ascent()) - paint.descent()) / 2f)

                            canvas.withScale(scale, scale) {
                                canvas.drawText(text, textX1, textY, paint)
                                canvas.drawText(text, textX2, textY, paint)
                            }
                        }

                        CodeBlock("标签线") {
                            val first = line.first.toFloat()
                            val last = line.last.toFloat()
                            val textWidth = textBounds.width()
                            val textHeight = textBounds.height()
                            val center = (line.first + line.last) / 2f
                            val left = srcBounds.left - (textWidth / 2) - 1f
                            val right = srcBounds.right + (textWidth / 2) + 1f

                            canvas.withScale(scale, scale) {
                                CodeBlock("左") {
                                    canvas.drawLine(
                                        left, first, left, center - (textHeight / 2) - 1, paint
                                    )
                                    canvas.drawLine(
                                        left, center + (textHeight / 2) + 1, left, last, paint
                                    )
                                }

                                CodeBlock("右") {
                                    canvas.drawLine(
                                        right, first, right, center - (textHeight / 2) - 1, paint
                                    )
                                    canvas.drawLine(
                                        right, center + (textHeight / 2) + 1, right, last, paint
                                    )
                                }
                            }
                        }
                    }
                }
            }

            CodeBlock("边界") {
                val paint = paints["bounds"]!!
                val chunksBounds = chunks.bounds.toRectF()

                chunksBounds.set(
                    chunksBounds.left * scale,
                    chunksBounds.top * scale,
                    chunksBounds.right * scale,
                    chunksBounds.bottom * scale
                )

                val left = bounds.left - offsetX
                val top = bounds.top - offsetY
                val right = bounds.right - offsetX
                val bottom = bounds.bottom - offsetY

                canvas.drawLine(chunksBounds.left, top, chunksBounds.left, bottom, paint)
                canvas.drawLine(chunksBounds.right, top, chunksBounds.right, bottom, paint)
                canvas.drawLine(left, chunksBounds.top, right, chunksBounds.top, paint)
                canvas.drawLine(left, chunksBounds.bottom, right, chunksBounds.bottom, paint)
            }

            CodeBlock("边距") {
                val paint = paints["padding"]!!
                val chunksBounds = chunks.padding.toRectF()

                chunksBounds.set(
                    chunksBounds.left * scale,
                    chunksBounds.top * scale,
                    chunksBounds.right * scale,
                    chunksBounds.bottom * scale
                )

                val left = bounds.left - offsetX
                val top = bounds.top - offsetY
                val right = bounds.right - offsetX
                val bottom = bounds.bottom - offsetY

                canvas.drawLine(chunksBounds.left, top, chunksBounds.left, bottom, paint)
                canvas.drawLine(chunksBounds.right, top, chunksBounds.right, bottom, paint)
                canvas.drawLine(left, chunksBounds.top, right, chunksBounds.top, paint)
                canvas.drawLine(left, chunksBounds.bottom, right, chunksBounds.bottom, paint)
            }
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