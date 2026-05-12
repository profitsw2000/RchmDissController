package ru.profitsw2000.data.data.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository

class BluetoothPacketManagerImpl(
    private val bluetoothRepository: BluetoothRepository
) : BluetoothPacketManager {

    private val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)

    override val BUFFER_SIZE: Int = 16
    override val packetBuffer: MutableList<Byte> = arrayListOf()
    override var packetState = 0
    override var packetNumber = 0
    override var packetSize = 0
    override var packetCheckSum = 0

    override fun observeBluetoothDataBytesFlow() {
        coroutineScope.launch {
            bluetoothRepository.bluetoothBytesDataFlow.collect { bytes ->
                parseIncomingFlow(bytes)
            }
        }
    }

    override fun parseIncomingFlow(byteArray: ByteArray) {
        byteArray.forEach { byte ->
            when(packetState) {
                0 -> checkStartByte(byte)
                1 -> checkPacketSize(byte)
                2 -> checkPacketId(byte)
                else -> getPacketData(byte)
            }
        }
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