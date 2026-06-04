package ru.profitsw2000.data.data.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.profitsw2000.core.drawable.utils.PACKET_SIZE_MAXIMUM
import ru.profitsw2000.core.drawable.utils.PACKET_START_BYTE
import ru.profitsw2000.core.drawable.utils.RCHM_DISS_OUTPUT_SET_PACKET_ID
import ru.profitsw2000.core.drawable.utils.RCHM_DISS_OUTPUT_SET_PACKET_SIZE
import ru.profitsw2000.core.drawable.utils.READ_FROM_RECEIVER_PACKET_ID
import ru.profitsw2000.core.drawable.utils.READ_FROM_SYNTHESIZER_PACKET_ID
import ru.profitsw2000.core.drawable.utils.READ_FROM_TRANSMITTER_PACKET_ID
import ru.profitsw2000.core.drawable.utils.WRITE_TO_RECEIVER_PACKET_ID
import ru.profitsw2000.core.drawable.utils.WRITE_TO_RECEIVER_PACKET_SIZE
import ru.profitsw2000.core.drawable.utils.WRITE_TO_SYNTHESIZER_PACKET_ID
import ru.profitsw2000.core.drawable.utils.WRITE_TO_SYNTHESIZER_PACKET_SIZE
import ru.profitsw2000.core.drawable.utils.WRITE_TO_TRANSMITTER_PACKET_ID
import ru.profitsw2000.core.drawable.utils.WRITE_TO_TRANSMITTER_PACKET_SIZE
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
        if (byte.toUnsignedInteger() == PACKET_START_BYTE) {
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
            if ((packetCheckSum and 0xFF) == byte.toUnsignedInteger()) decodePacket(packetBuffer, packetNumber, (packetSize - 4))
            packetState = 0
        }
    }

    override fun decodePacket(
        bytesList: List<Byte>,
        command: Int,
        packetSize: Int
    ) {
        when(command) {
            READ_FROM_TRANSMITTER_PACKET_ID -> rchmDissStateRepository.updateTransmitterModuleState(bytesList[0])
            READ_FROM_RECEIVER_PACKET_ID -> receiverPacket(bytesList)
            READ_FROM_SYNTHESIZER_PACKET_ID -> synthesizerPacket(bytesList)
            else -> {}
        }
    }

    override fun getWriteToTransmitterPacket(dataByte: Byte): ByteArray {
        val checkSum = ((PACKET_START_BYTE +
                WRITE_TO_TRANSMITTER_PACKET_SIZE +
                WRITE_TO_TRANSMITTER_PACKET_ID +
                dataByte.toUnsignedInteger()) and 0xFF).toByte()

        return byteArrayOf(
            PACKET_START_BYTE.toByte(),
            WRITE_TO_TRANSMITTER_PACKET_SIZE.toByte(),
            WRITE_TO_TRANSMITTER_PACKET_ID.toByte(),
            dataByte,
            checkSum
        )
    }

    override fun getWriteToReceiverPacket(dataByteArray: ByteArray): ByteArray {
        return getWriteByteArrayPacket(
            WRITE_TO_RECEIVER_PACKET_SIZE,
            WRITE_TO_RECEIVER_PACKET_ID,
            dataByteArray
        )
    }

    override fun getWriteToSynthesizerPacket(dataByteArray: ByteArray): ByteArray {
        return getWriteByteArrayPacket(
            WRITE_TO_SYNTHESIZER_PACKET_SIZE,
            WRITE_TO_SYNTHESIZER_PACKET_ID,
            dataByteArray
        )
    }

    override fun getRchmDissOutputSetPacket(dataByteArray: ByteArray): ByteArray {
        return getWriteByteArrayPacket(
            RCHM_DISS_OUTPUT_SET_PACKET_SIZE,
            RCHM_DISS_OUTPUT_SET_PACKET_ID,
            dataByteArray
        )
    }

    private fun receiverPacket(bytesList: List<Byte>) {
        rchmDissStateRepository.updateReceiverModuleState(
            bytesList[1],
            bytesList[0]
        )
    }

    private fun synthesizerPacket(bytesList: List<Byte>) {
        rchmDissStateRepository.updateSynthesizerModuleState(
            bytesList[2],
            bytesList[1],
            bytesList[0]
        )
    }

    private fun getWriteByteArrayPacket(packetSize: Int, packetId: Int, byteArray: ByteArray): ByteArray {
        val checkSum = ((PACKET_START_BYTE +
                packetSize +
                packetId +
                getByteArrayCheckSum(byteArray)) and 0xFF).toByte()

        return byteArrayOf(
                PACKET_START_BYTE.toByte(),
                packetSize.toByte(),
                packetId.toByte()
            ) +
            byteArray +
            byteArrayOf(checkSum)
    }

    private fun getByteArrayCheckSum(byteArray: ByteArray): Int {
        var sum = 0
        for (byte in byteArray) {
            sum += byte.toUnsignedInteger()
        }
        return sum
    }
}