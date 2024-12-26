package org.limao996.kpatch

inline fun CodeBlock(key: Any? = null, block: () -> Unit) = block()
inline fun <R> CodeBlock(key: Any? = null, block: () -> R) = block()


inline fun CodeBlock(key: Any? = null, enable: Boolean, block: () -> Unit) {
    if (enable) block()
}

inline fun <R> CodeBlock(key: Any? = null, enable: Boolean, block: () -> R): R? {
    return if (enable) block() else null
}