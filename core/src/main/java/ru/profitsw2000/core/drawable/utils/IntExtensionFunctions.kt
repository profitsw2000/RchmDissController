package ru.profitsw2000.core.drawable.utils

fun Int.toRegisterByteArray(): ByteArray = byteArrayOf(
        this.toByte(),
        this.shr(8).toByte(),
        this.shr(16).toByte()
    )