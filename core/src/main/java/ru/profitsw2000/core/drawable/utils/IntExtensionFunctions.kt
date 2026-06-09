package ru.profitsw2000.core.drawable.utils

fun Int.toRegisterByteArray(): ByteArray = byteArrayOf(
        this.toByte(),
        this.shr(8).toByte(),
        this.shr(16).toByte()
    )

fun Int.dpToPx(): Int {
    val density = android.content.res.Resources.getSystem().displayMetrics.density
    return (this * density).toInt()
}