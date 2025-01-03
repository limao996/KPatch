package org.limao996.kpatch

import android.util.Log

inline fun CodeBlock(key: Any? = null, block: () -> Unit) = block()


inline fun CodeBlock(key: Any? = null, enable: Boolean, block: () -> Unit) {
    if (enable) block()
}

fun log(vararg msg: Any?) {
    Log.i("KPatch-Log", msg.joinToString("\t"))
}