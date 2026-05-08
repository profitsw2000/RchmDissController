package ru.profitsw2000.data.domain.bluetooth

interface BluetoothPacketManager {

    val RING_BUFFER_SIZE: Int
    val ringBuffer: MutableList<Byte>
    val packetBuffer: MutableList<Byte>
    var bufferTail: Int
    var bufferHead: Int

    var packetState: Int
    var packetNumber: Int
    var packetSize: Int
    var packetCheckSum: Int

    fun insertBytes(byteArray: ByteArray)
    fun parseBuffer()

}