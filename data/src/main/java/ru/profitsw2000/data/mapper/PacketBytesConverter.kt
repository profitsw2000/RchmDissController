package ru.profitsw2000.data.mapper

import ru.profitsw2000.core.drawable.utils.ADC_NEGATIVE_BIT_MASK
import ru.profitsw2000.core.drawable.utils.ADC_RESULT_MASK
import ru.profitsw2000.core.drawable.utils.ADC_VOLTAGE_RESOLUTION_VALUE
import ru.profitsw2000.core.drawable.utils.ATTENUATOR_16_DECIBELS_BIT
import ru.profitsw2000.core.drawable.utils.ATTENUATOR_2_DECIBELS_BIT
import ru.profitsw2000.core.drawable.utils.ATTENUATOR_32_DECIBELS_BIT
import ru.profitsw2000.core.drawable.utils.ATTENUATOR_4_DECIBELS_BIT
import ru.profitsw2000.core.drawable.utils.ATTENUATOR_8_DECIBELS_BIT
import ru.profitsw2000.core.drawable.utils.ATTENUATOR_BIT_MASK
import ru.profitsw2000.core.drawable.utils.LFM_EXT_TRIGGER_BIT_MASK
import ru.profitsw2000.core.drawable.utils.PLL_LOCK_STATE_BIT_MASK
import ru.profitsw2000.core.drawable.utils.RX_CHANNEL_1
import ru.profitsw2000.core.drawable.utils.RX_CHANNEL_2
import ru.profitsw2000.core.drawable.utils.RX_CHANNEL_3
import ru.profitsw2000.core.drawable.utils.RX_CHANNEL_4
import ru.profitsw2000.core.drawable.utils.RX_CHANNEL_5
import ru.profitsw2000.core.drawable.utils.RX_CHANNEL_MASK
import ru.profitsw2000.core.drawable.utils.TEST_SIGNAL_BIT
import ru.profitsw2000.core.drawable.utils.TRANSMITTER_CONTROL_BIT_MASK
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_1
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_2
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_3
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_4
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_5
import ru.profitsw2000.core.drawable.utils.toUnsignedInteger
import ru.profitsw2000.data.model.bluetooth.state.rcd.OutputModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import kotlin.experimental.and

class PacketBytesConverter {

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

        return ReceiverModuleState(
            enabledChannelNumber = getEnabledRxChannelNumber(receiverCommand),
            testSignalIsEnabled = getTestSignalState(receiverCommand),
            lockedInputChannels = getLockedRxChannelNumber(receiverCommand),
            inputAttenuationValue = getAttenuationValue(receiverCommand),
            inputAttenuatorsCode = getAttenuatorCode(receiverCommand)
        )
    }

    fun synthesizerBytes(lowByte: Byte, middleByte: Byte, highByte: Byte): Int {
        return (highByte.toUnsignedInteger().shl(16)) or
                (middleByte.toUnsignedInteger().shl(8)) or
                lowByte.toUnsignedInteger()
    }

    fun rcdOutputBytes(byteArray: ByteArray): OutputModuleState {
        return OutputModuleState(
            lfmExtTriggerIsOn = isLfmExtTrigger(byteArray[0]),
            transmitterIsOn = transmitterIsOn(byteArray[0]),
            pllIsLocked = pllIsLocked(byteArray[0]),
            transmitterDetectorVoltage = getVoltageValue(byteArray[1], byteArray[2]),
            secondaryPowerSourceVoltage = getVoltageValue(byteArray[3], byteArray[4])
        )
    }

    fun rcdTemperatureBytes(lowByte: Byte, highByte: Byte): Double {
        val temperatureCode = (highByte.toInt() shl 8) or lowByte.toUnsignedInteger()

        return temperatureCode*0.0625
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

    private fun getAttenuationValue(command: Int): Int {
        return (command.shr(ATTENUATOR_8_DECIBELS_BIT) and 1)*8 +
                (command.shr(ATTENUATOR_32_DECIBELS_BIT) and 1)*32 +
                (command.shr(ATTENUATOR_2_DECIBELS_BIT) and 1)*2 +
                (command.shr(ATTENUATOR_4_DECIBELS_BIT) and 1)*4 +
                (command.shr(ATTENUATOR_16_DECIBELS_BIT) and 1)*16
    }

    private fun getTestSignalState(command: Int): Boolean {
        return (command.shr(TEST_SIGNAL_BIT) and 1) == 1
    }

    private fun getAttenuatorCode(command: Int): Int {
        return command and ATTENUATOR_BIT_MASK
    }

    private fun isLfmExtTrigger(byte: Byte): Boolean {
        return (byte and LFM_EXT_TRIGGER_BIT_MASK).toInt() != 0
    }

    private fun transmitterIsOn(byte: Byte): Boolean {
        return (byte and TRANSMITTER_CONTROL_BIT_MASK).toInt() != 0
    }

    private fun pllIsLocked(byte: Byte): Boolean {
        return (byte and PLL_LOCK_STATE_BIT_MASK).toInt() != 0
    }

    private fun getVoltageValue(highByte: Byte, lowByte: Byte): Double {
        val adcVoltageCode = (highByte.toInt().shl(8) and
                lowByte.toInt()) and ADC_RESULT_MASK

        return if ((adcVoltageCode and ADC_NEGATIVE_BIT_MASK) == 0)
            adcVoltageCode*ADC_VOLTAGE_RESOLUTION_VALUE
        else (adcVoltageCode.inv())*ADC_VOLTAGE_RESOLUTION_VALUE
    }
}