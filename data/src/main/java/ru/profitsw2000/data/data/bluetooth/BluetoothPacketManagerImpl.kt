package ru.profitsw2000.data.data.bluetooth

import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository

class BluetoothPacketManagerImpl(
    bluetoothRepository: BluetoothRepository
) : BluetoothPacketManager {
    override val BUFFER_SIZE: Int
        get() = TODO("Not yet implemented")
    override val packetBuffer: MutableList<Byte>
        get() = TODO("Not yet implemented")
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

    override fun observeBluetoothDataBytesFlow() {
        TODO("Not yet implemented")
    }

    override fun parseIncomingFlow(byteArray: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun checkStartByte(byte: Byte) {
        TODO("Not yet implemented")
    }

    override fun checkPacketSize(byte: Byte) {
        TODO("Not yet implemented")
    }

    override fun checkPacketId(byte: Byte) {
        TODO("Not yet implemented")
    }

    override fun getPacketData(byte: Byte) {
        TODO("Not yet implemented")
    }
}