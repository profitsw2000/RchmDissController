package ru.profitsw2000.data.mapper

import ru.profitsw2000.core.drawable.utils.toUnsignedInteger
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState

class PacketBytesConverter {
    val TX_CHANNEL_1 = 0x1
    val TX_CHANNEL_2 = 0x2
    val TX_CHANNEL_3 = 0x4
    val TX_CHANNEL_4 = 0x8
    val TX_CHANNEL_5 = 0x10
    val RX_CHANNEL_MASK = 0x1F
    val RX_CHANNEL_1 = 0x1E
    val RX_CHANNEL_2 = 0x1D
    val RX_CHANNEL_3 = 0x1B
    val RX_CHANNEL_4 = 0x17
    val RX_CHANNEL_5 = 0xF
    val ATTENUATOR_8_DECIBELS_BIT = 0
    val ATTENUATOR_32_DECIBELS_BIT = 6
    val ATTENUATOR_2_DECIBELS_BIT = 7
    val ATTENUATOR_4_DECIBELS_BIT = 8
    val ATTENUATOR_16_DECIBELS_BIT = 9

    fun transmitterByte(byte: Byte): TransmitterModuleState {
        return when(byte.toUnsignedInteger().shr(1)) {
            TX_CHANNEL_1 -> TransmitterModuleState(1)
            TX_CHANNEL_2 -> TransmitterModuleState(2)
            TX_CHANNEL_3 -> TransmitterModuleState(3)
            TX_CHANNEL_4 -> TransmitterModuleState(4)
            TX_CHANNEL_5 -> TransmitterModuleState(5)
            else -> TransmitterModuleState(0)
        }
    }

    fun receiverBytes(lowByte: Byte, highByte: Byte): ReceiverModuleState {
        val receiverCommand = (highByte.toUnsignedInteger().shl(8)) or lowByte.toUnsignedInteger()


    }

    private fun getEnabledRxChannelNumber(command: Int): Int {
        return when(command.shr(10) and RX_CHANNEL_MASK) {
            RX_CHANNEL_1 -> 1
            RX_CHANNEL_2 -> 2
            RX_CHANNEL_3 -> 3
            RX_CHANNEL_4 -> 4
            RX_CHANNEL_5 -> 5
            else -> 0
        }
    }

    private fun getLockedRxChannelNumber(command: Int): BooleanArray {
        val rxState = command.shr(1) and RX_CHANNEL_MASK
        return booleanArrayOf(
            rxState and 0x1 != 0,
            rxState and 0x2 != 0,
            rxState and 0x4 != 0,
            rxState and 0x8 != 0,
            rxState and 0x10 != 0
        )
    }
}