package ru.profitsw2000.data.data.bluetooth

import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager

class BluetoothPacketManagerImpl() : BluetoothPacketManager {
    override val RING_BUFFER_SIZE: Int
        get() = TODO("Not yet implemented")
    override val ringBuffer: MutableList<Byte>
        get() = TODO("Not yet implemented")
    override val packetBuffer: MutableList<Byte>
        get() = TODO("Not yet implemented")
    override var bufferTail: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var bufferHead: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var packetState: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var packetNumber: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var packetSize: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var packetCheckSum: Int
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun insertBytes(byteArray: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun parseBuffer() {
        TODO("Not yet implemented")
    }
}