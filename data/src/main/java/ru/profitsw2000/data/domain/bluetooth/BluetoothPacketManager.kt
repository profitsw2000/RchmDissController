package ru.profitsw2000.data.domain.bluetooth

interface BluetoothPacketManager {

    val BUFFER_SIZE: Int
    val packetBuffer: MutableList<Byte>

    var packetState: Int
    var packetNumber: Int
    var packetSize: Int
    var packetCheckSum: Int

    fun observeBluetoothDataBytesFlow()

    fun parseIncomingFlow(byteArray: ByteArray)

    fun checkStartByte(byte: Byte)

    fun checkPacketSize(byte: Byte)

    fun checkPacketId(byte: Byte)

    fun getPacketData(byte: Byte)

    fun decodePacket(bytesList: List<Byte>, command: Int, packetSize: Int)

    fun getWriteToTransmitterPacket(dataByte: Byte): ByteArray

    fun getWriteToReceiverPacket(dataByteArray: ByteArray): ByteArray

    fun getWriteToSynthesizerPacket(dataByteArray: ByteArray): ByteArray

    fun getRchmDissOutputSetPacket(dataByteArray: ByteArray): ByteArray
}