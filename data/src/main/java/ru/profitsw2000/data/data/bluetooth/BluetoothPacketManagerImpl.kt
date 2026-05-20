package ru.profitsw2000.data.data.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.profitsw2000.core.drawable.utils.PACKET_SIZE_MAXIMUM
import ru.profitsw2000.core.drawable.utils.READ_FROM_RECEIVER_PACKET_ID
import ru.profitsw2000.core.drawable.utils.READ_FROM_SYNTHESIZER_PACKET_ID
import ru.profitsw2000.core.drawable.utils.READ_FROM_TRANSMITTER_PACKET_ID
import ru.profitsw2000.core.drawable.utils.toUnsignedInteger
import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository

class BluetoothPacketManagerImpl(
    private val bluetoothRepository: BluetoothRepository,
    private val rchmDissStateRepository: RchmDissStateRepository
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
        if (byte.toUnsignedInteger() == 0x53) {
            packetState = 1
            packetCheckSum = 0
            packetBuffer.clear()
        }
    }

    override fun checkPacketSize(byte: Byte) {
        packetSize = byte.toUnsignedInteger()
        packetCheckSum += packetSize
        packetState = if (packetSize < PACKET_SIZE_MAXIMUM) 2
        else 0
    }

    override fun checkPacketId(byte: Byte) {
        packetNumber = byte.toUnsignedInteger()
        packetCheckSum += packetNumber
        packetState = if (packetNumber < 0x10) 3
        else 0
    }

    override fun getPacketData(byte: Byte) {
        if (packetState < packetSize) {
            packetBuffer.add(byte)
            packetCheckSum += byte.toUnsignedInteger()
            packetState++
        } else {
            if ((packetCheckSum and 0xFF) == byte.toUnsignedInteger()) decodePacket(packetBuffer, packetNumber, packetSize)
            packetState = 0
        }
    }

    override fun decodePacket(
        bytesList: List<Byte>,
        command: Int,
        packetSize: Int
    ) {
        when(command) {
            READ_FROM_TRANSMITTER_PACKET_ID -> TODO()
            READ_FROM_RECEIVER_PACKET_ID -> TODO()
            READ_FROM_SYNTHESIZER_PACKET_ID -> TODO()
            else -> {}
        }
    }
}