package org.limao996.kpatch.test

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import org.limao996.kpatch.KPatch
import org.limao996.kpatch.KPatchChunks
import org.limao996.kpatch.drawKPatch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 加载.9图片
        // val bitmap = BitmapFactory.decodeStream(assets.open("a.9.png"))
        // val kPatch = KPatch(bitmap) // 自动解析属性

        //*
        // 加载普通图片
        val bitmap = BitmapFactory.decodeStream(assets.open("a.png"))
        val kPatch = KPatch(
            bitmap, KPatchChunks(
                bounds = Rect(0, 0, 499, 749),
                splitX = listOf(59..114, 388..439),
                splitY = listOf(87..680),
                padding = Rect(42, 78, 458, 705)
            )
        ) // 手动标注属性
        // */

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
                            val scale = remember { mutableDoubleStateOf(1.0) }
                            val width = remember { mutableIntStateOf(800) }
                            val height = remember { mutableIntStateOf(1200) }
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
                                Text("缩放：" + (scale.doubleValue * 100).toInt() / 100f)
                                Slider(scale.doubleValue.toFloat(), {
                                    scale.doubleValue = it.toDouble()
                                }, Modifier.width(300.dp), valueRange = 0.5f..15f, steps = 50)
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
                                Modifier.fillMaxSize()
                            ) {
                                drawIntoCanvas {
                                    it.nativeCanvas.drawKPatch(
                                        kPatch = kPatch,
                                        bounds = Rect(
                                            0, 0, width.intValue, height.intValue
                                        ),
                                        scale = scale.doubleValue,
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
