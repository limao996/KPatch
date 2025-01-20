package org.limao996.kpatch.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import androidx.core.graphics.toRectF
import androidx.core.graphics.withClip
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

    open var backgroundColor = 0x10000000.toInt()
    open fun drawBackground(canvas: Canvas, bounds: Rect) {
        // 填充灰色
        canvas.drawColor(backgroundColor)
    }

    open var paints = mapOf(
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
            color = 0x30FF00FF
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
        "chunk_padding" to Paint().apply {
            color = Color.argb(0x30, 0, 179, 255)
            style = Paint.Style.FILL
        },
        "ruler_container" to Paint().apply {
            color = 0xffcccccc.toInt()
            setShadowLayer(12f, 0f, 0f, 0x50000000.toInt())
        },
        "ruler_body" to Paint().apply {
            color = Color.WHITE
        },
        "ruler_slider" to Paint().apply {
            setShadowLayer(4f, 0f, 0f, 0x50000000.toInt())
        },
    )

    open var rulerWidth = 36f

    open fun drawRulerSlider(
        canvas: Canvas, containerRect: Rect, range: IntRange, rectPaint: Paint, sliderColor: Int
    ) {
        val isHorizontal = containerRect.width() > containerRect.height()
        val rect = if (isHorizontal) RectF(
            offsetX!! + (range.first * scale!!),
            containerRect.top.toFloat(),
            offsetX!! + (range.last * scale!!),
            containerRect.bottom.toFloat(),
        )
        else RectF(
            containerRect.left.toFloat(),
            offsetY!! + (range.first * scale!!),
            containerRect.right.toFloat(),
            offsetY!! + (range.last * scale!!),
        )
        canvas.drawRect(rect, rectPaint)

        CodeBlock("滑块") {
            val paint = paints["ruler_slider"]!!
            paint.color = sliderColor

            val sliderWidth = rulerWidth / 3
            val sliderRound = sliderWidth / 2
            val sliderStartRect = if (isHorizontal) RectF(
                rect.left - (sliderWidth / 2),
                rect.top,
                rect.left + (sliderWidth / 2),
                rect.bottom,
            )
            else RectF(
                rect.left,
                rect.top - (sliderWidth / 2),
                rect.right,
                rect.top + (sliderWidth / 2),
            )
            val sliderEndRect = if (isHorizontal) RectF(
                rect.right - (sliderWidth / 2),
                rect.top,
                rect.right + (sliderWidth / 2),
                rect.bottom,
            )
            else RectF(
                rect.left,
                rect.bottom - (sliderWidth / 2),
                rect.right,
                rect.bottom + (sliderWidth / 2),
            )
            canvas.drawRoundRect(
                sliderStartRect, sliderRound, sliderRound, paint
            )
            canvas.drawRoundRect(
                sliderEndRect, sliderRound, sliderRound, paint
            )
        }
    }

    open fun drawRuler(canvas: Canvas, bounds: Rect) {
        val bitmap = kPatch.bitmap
        val chunks = kPatch.chunks

        val containerRects = object {
            val left = Rect(
                bounds.left, bounds.top, bounds.left + rulerWidth.toInt(), bounds.bottom
            )
            val top = Rect(
                bounds.left, bounds.top, bounds.right, bounds.top + rulerWidth.toInt()
            )
            val right = Rect(
                bounds.right, bounds.top, bounds.right - rulerWidth.toInt(), bounds.bottom
            )
            val bottom = Rect(
                bounds.left, bounds.bottom, bounds.right, bounds.bottom - rulerWidth.toInt()
            )
        }
        Path().apply {
            addRect(containerRects.left.toRectF(), Path.Direction.CW)
            addRect(containerRects.top.toRectF(), Path.Direction.CW)
            addRect(containerRects.right.toRectF(), Path.Direction.CCW)
            addRect(containerRects.bottom.toRectF(), Path.Direction.CCW)
            canvas.drawPath(this, paints["ruler_container"]!!)
        }


        CodeBlock("左侧") {
            val containerRect = containerRects.left

            val bodyRect = RectF(
                containerRect.left.toFloat(),
                offsetY!!,
                containerRect.right.toFloat(),
                (offsetY!! + ((bitmap.height - 1) * scale!!)),
            )
            // 主体
            val bodyRound = rulerWidth / 6
            canvas.drawRoundRect(bodyRect, bodyRound, bodyRound, paints["ruler_body"]!!)

            CodeBlock("标注") {
                for (line in chunks.delY) {
                    drawRulerSlider(
                        canvas,
                        containerRect,
                        line,
                        paints["chunk_del"]!!,
                        paints["split_remove"]!!.color
                    )
                }
                for (line in chunks.splitY) {
                    drawRulerSlider(
                        canvas,
                        containerRect,
                        line,
                        paints["chunk_inner"]!!,
                        paints["split_expand"]!!.color
                    )
                }
            }
        }
        CodeBlock("上侧") {
            val containerRect = containerRects.top

            val bodyRect = RectF(
                offsetX!!,
                containerRect.top.toFloat(),
                (offsetX!! + ((bitmap.width - 1) * scale!!)),
                containerRect.bottom.toFloat(),
            )
            // 主体
            val bodyRound = rulerWidth / 6
            canvas.drawRoundRect(bodyRect, bodyRound, bodyRound, paints["ruler_body"]!!)

            CodeBlock("标注") {

                for (line in chunks.delX) {
                    drawRulerSlider(
                        canvas,
                        containerRect,
                        line,
                        paints["chunk_del"]!!,
                        paints["split_remove"]!!.color
                    )
                }
                for (line in chunks.splitX) {
                    drawRulerSlider(
                        canvas,
                        containerRect,
                        line,
                        paints["chunk_inner"]!!,
                        paints["split_expand"]!!.color
                    )
                }
            }
        }
        CodeBlock("右侧") {
            val containerRect = containerRects.right

            val bodyRect = RectF(
                containerRect.left.toFloat(),
                offsetY!!,
                containerRect.right.toFloat(),
                (offsetY!! + ((bitmap.height - 1) * scale!!)),
            )
            // 主体
            val bodyRound = rulerWidth / 6
            canvas.drawRoundRect(bodyRect, bodyRound, bodyRound, paints["ruler_body"]!!)

            CodeBlock("标注") {
                // 边界
                drawRulerSlider(
                    canvas,
                    containerRect,
                    chunks.bounds.top..chunks.bounds.bottom,
                    paints["chunk_fixed"]!!,
                    paints["bounds"]!!.color
                )

                // 边距
                drawRulerSlider(
                    canvas,
                    containerRect,
                    chunks.padding.top..chunks.padding.bottom,
                    paints["chunk_padding"]!!,
                    paints["padding"]!!.color
                )
            }
        }
        CodeBlock("下侧") {
            val containerRect = containerRects.bottom

            val bodyRect = RectF(
                offsetX!!,
                containerRect.top.toFloat(),
                (offsetX!! + ((bitmap.width - 1) * scale!!)),
                containerRect.bottom.toFloat(),
            )
            // 主体
            val bodyRound = rulerWidth / 6
            canvas.drawRoundRect(bodyRect, bodyRound, bodyRound, paints["ruler_body"]!!)


            CodeBlock("标注") {
                // 边界
                drawRulerSlider(
                    canvas,
                    containerRect,
                    chunks.bounds.left..chunks.bounds.right,
                    paints["chunk_fixed"]!!,
                    paints["bounds"]!!.color
                )

                // 边距
                drawRulerSlider(
                    canvas,
                    containerRect,
                    chunks.padding.left..chunks.padding.right,
                    paints["chunk_padding"]!!,
                    paints["padding"]!!.color
                )
            }
        }

    }


    open fun drawUi(canvas: Canvas, bounds: Rect) {
        // 绘制标尺
        drawRuler(canvas, bounds)
    }

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

        // 绘制背景
        drawBackground(canvas, bounds)
        // 绘制主体
        drawBody(canvas, bounds)
        // 绘制UI
        drawUi(canvas, bounds)
    }

}