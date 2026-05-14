package ru.profitsw2000.core.drawable.utils

fun Byte.toUnsignedInteger(): Int = this.toInt() and 0xFF

fun Byte.toInteger(): Int = this.toInt()