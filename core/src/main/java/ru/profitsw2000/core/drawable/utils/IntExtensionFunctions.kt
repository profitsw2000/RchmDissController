package ru.profitsw2000.core.drawable.utils

fun Int.toRegisterByteArray(): ByteArray = byteArrayOf(
        this.shr(16).toByte(),
        this.shr(8).toByte(),
        this.toByte()
    )

fun Int.dpToPx(): Int {
    val density = android.content.res.Resources.getSystem().displayMetrics.density
    return (this * density).toInt()
}