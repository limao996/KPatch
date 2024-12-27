package org.limao996.kpatch

import android.util.Log

inline fun CodeBlock(key: Any? = null, block: () -> Unit) = block()
inline fun <R> CodeBlock(key: Any? = null, block: () -> R) = block()


inline fun CodeBlock(key: Any? = null, enable: Boolean, block: () -> Unit) {
    if (enable) block()
}

inline fun <R> CodeBlock(key: Any? = null, enable: Boolean, block: () -> R): R? {
    return if (enable) block() else null
}

fun log(vararg msg: Any?) {
    Log.i("KPatch-Log", msg.joinToString("\t"))
}