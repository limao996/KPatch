package org.limao996.kpatch.test

import android.graphics.BitmapFactory
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.AnimationVector
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toRectF
import androidx.core.graphics.withClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.limao996.kpatch.KPatch
import org.limao996.kpatch.drawKPatch
import org.limao996.kpatch.editor.KPatchEditor
import org.limao996.kpatch.log
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 加载.9图片
        val bitmap = BitmapFactory.decodeStream(assets.open("a.9.png"))
        val kPatch = KPatch(bitmap) // 自动解析属性
        kPatch.chunks.delX = listOf(30..50)
        kPatch.chunks.delY = listOf(30..50)
        kPatch.chunks.bounds = Rect(10, 15, 480, 740)

        /*
        // 加载普通图片
        val bitmap = BitmapFactory.decodeStream(assets.open("a.png"))
        val kPatch = KPatch(
            bitmap, KPatchChunks(
                bounds = Rect(0, 0, 499, 749),
                splitX = listOf(58..113, 387..438),
                splitY = listOf(86..679),
                delX = listOf(0..30),
                delY = listOf(0..30),
                padding = Rect(41, 77, 457, 704)
            )
        ) // 手动标注属性
        */

        // val patchBitmap = kPatch.export() // 导出.9图片

        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(Modifier.padding(innerPadding)) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                        ) {
                            val debug = remember { mutableStateOf(true) }
                            val inner = remember { mutableIntStateOf(KPatch.REPEAT_INNER_BOTH) }
                            val outer = remember { mutableIntStateOf(KPatch.REPEAT_OUTER_ALL) }
                            val scale = remember { mutableFloatStateOf(1f) }
                            val width = remember { mutableIntStateOf(800) }
                            val height = remember { mutableIntStateOf(1000) }

                            val showEditor = remember { mutableStateOf(true) }
                            if (showEditor.value) AlertDialog({ showEditor.value = false },
                                title = { Text("编辑（开发中）") },
                                text = {
                                    Editor(remember { KPatchEditor(kPatch.export(false)) })
                                },
                                confirmButton = {
                                    Button({ showEditor.value = false }) {
                                        Text("确定")
                                    }
                                })

                            Button({ showEditor.value = true }) {
                                Text("编辑")
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("调试：")
                                Checkbox(debug.value, { debug.value = it })
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("内侧：")
                                Checkbox(inner.intValue and KPatch.REPEAT_INNER_X != 0, {
                                    if (it) inner.intValue = inner.intValue or KPatch.REPEAT_INNER_X
                                    else inner.intValue = inner.intValue xor KPatch.REPEAT_INNER_X
                                })
                                Text("X轴")
                                Checkbox(inner.intValue and KPatch.REPEAT_INNER_Y != 0, {
                                    if (it) inner.intValue = inner.intValue or KPatch.REPEAT_INNER_Y
                                    else inner.intValue = inner.intValue xor KPatch.REPEAT_INNER_Y
                                })
                                Text("Y轴")
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("外侧：")
                                Checkbox(outer.intValue and KPatch.REPEAT_OUTER_X != 0, {
                                    if (it) outer.intValue = outer.intValue or KPatch.REPEAT_OUTER_X
                                    else outer.intValue = outer.intValue xor KPatch.REPEAT_OUTER_X
                                })
                                Text("X轴")
                                Checkbox(outer.intValue and KPatch.REPEAT_OUTER_Y != 0, {
                                    if (it) outer.intValue = outer.intValue or KPatch.REPEAT_OUTER_Y
                                    else outer.intValue = outer.intValue xor KPatch.REPEAT_OUTER_Y
                                })
                                Text("Y轴")
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("缩放：" + (scale.floatValue * 100).toInt() / 100f)
                                Slider(scale.floatValue, {
                                    scale.floatValue = it
                                }, valueRange = 0.3f..3f)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("宽度：" + width.intValue)
                                Slider(width.intValue.toFloat(), {
                                    width.intValue = it.toInt()
                                }, valueRange = 1f..2000f)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("高度：" + height.intValue)
                                Slider(height.intValue.toFloat(), {
                                    height.intValue = it.toInt()
                                }, valueRange = 1f..2000f)
                            }

                            Canvas(
                                Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                            ) {
                                drawIntoCanvas {
                                    it.nativeCanvas.drawKPatch(
                                        kPatch = kPatch,
                                        bounds = Rect(
                                            4, 4, width.intValue - 4, height.intValue - 4
                                        ),
                                        scale = scale.floatValue,
                                        flags = inner.intValue or outer.intValue,
                                        debug = debug.value
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Editor(editor: KPatchEditor) {
    val updater = remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clipToBounds()
            .pointerInput(Unit) {
                withContext(Dispatchers.Default) {
                    while (isActive) {
                        awaitPointerEventScope {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                if (change.type == PointerType.Mouse) {
                                    val delta = change.scrollDelta.y
                                    if (delta != 0f) {
                                        val centroid = change.position
                                        val value = 1 - (0.1f * delta)
                                        editor.scale(centroid.x, centroid.y, value)
                                        updater.intValue++
                                        break
                                    }
                                }
                            }
                            val pan = event.calculatePan()
                            val zoom = event.calculateZoom()
                            if (pan != Offset.Zero) {
                                editor.offset(pan.x, pan.y)
                                updater.intValue++
                            }
                            if (zoom != 1f) {
                                val centroid = event.calculateCentroid()
                                editor.scale(centroid.x, centroid.y, zoom)
                                updater.intValue++
                            }
                        }
                    }
                }
            }) {
            drawIntoCanvas {
                updater.intValue
                val canvas = it.nativeCanvas
                val bounds = canvas.clipBounds
                val clipPath = Path()
                clipPath.addRoundRect(bounds.toRectF(), 24.dp.value, 24.dp.value, Path.Direction.CW)
                canvas.withClip(clipPath) {
                    editor.draw(this, bounds)
                }
            }
        }
    }
}







