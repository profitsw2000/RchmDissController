package ru.profitsw2000.data.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import ru.profitsw2000.data.mapper.PacketBytesConverter
import ru.profitsw2000.data.model.bluetooth.state.rcd.OutputModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState

class PacketBytesConverterTest {

    private val packetBytesConverter = PacketBytesConverter()

    @Test
    fun `тест состояния включённого канала 1 передатчика`() {
        val channel1Byte: Byte = 0x20
        val expectedChannel1State = TransmitterModuleState(enabledChannelNumber = 1)
        val result = packetBytesConverter.transmitterByte(channel1Byte)

        assertThat(result).isEqualTo(expectedChannel1State)
    }

    @Test
    fun `тест состояния включённого канала 2 передатчика`() {
        val channel2Byte: Byte = 0x10
        val expectedChannel2State = TransmitterModuleState(enabledChannelNumber = 2)
        val result = packetBytesConverter.transmitterByte(channel2Byte)

        assertThat(result).isEqualTo(expectedChannel2State)
    }

    @Test
    fun `тест состояния включённого канала 3 передатчика`() {
        val channelByte: Byte = 0x08
        val expectedChannelState = TransmitterModuleState(enabledChannelNumber = 3)
        val result = packetBytesConverter.transmitterByte(channelByte)

        assertThat(result).isEqualTo(expectedChannelState)
    }

    @Test
    fun `тест состояния включённого канала 4 передатчика`() {
        val channelByte: Byte = 0x04
        val expectedChannelState = TransmitterModuleState(enabledChannelNumber = 4)
        val result = packetBytesConverter.transmitterByte(channelByte)

        assertThat(result).isEqualTo(expectedChannelState)
    }

    @Test
    fun `тест состояния включённого канала 5 передатчика`() {
        val channelByte: Byte = 0x02
        val expectedChannelState = TransmitterModuleState(enabledChannelNumber = 5)
        val result = packetBytesConverter.transmitterByte(channelByte)

        assertThat(result).isEqualTo(expectedChannelState)
    }

    @Test
    fun `тест состояния всех выключенных каналов передатчика`() {
        val channelByte: Byte = 0x00
        val expectedChannelState = TransmitterModuleState(enabledChannelNumber = 0)
        val result = packetBytesConverter.transmitterByte(channelByte)

        assertThat(result).isEqualTo(expectedChannelState)
    }

    @Test
    fun `тест состояния всех выключенных каналов передатчика с произвольным значением байта`() {
        val channelByte: Byte = 0x53
        val expectedChannelState = TransmitterModuleState(enabledChannelNumber = 0)
        val result = packetBytesConverter.transmitterByte(channelByte)

        assertThat(result).isEqualTo(expectedChannelState)
    }

    //1
    @Test
    fun `тест приёмника включён канал 1, заперт канал 1, включены секции 8 и 16 дБ, включён пилот-сигнал`() {
        val lowByte: Byte = 0xA0.toByte()
        val highByte: Byte = 0xBD.toByte()
        val expectedState =
            ReceiverModuleState(
                enabledChannelNumber = 1,
                testSignalIsEnabled = true,
                lockedInputChannels = booleanArrayOf(true, false, false, false, false),
                inputAttenuationValue = 24,
                inputAttenuatorsCode = 0x180
            )
        val result = packetBytesConverter.receiverBytes(lowByte, highByte)

        assertThat(result.toString()).isEqualTo(expectedState.toString())
    }

    //2
    @Test
    fun `тест приёмника включён канал 3, заперт канал 1,2,3, включены секции 2 и 32 дБ, включён пилот-сигнал`() {
        val lowByte: Byte = 0x78
        val highByte: Byte = 0xEE.toByte()
        val expectedState =
            ReceiverModuleState(
                enabledChannelNumber = 3,
                testSignalIsEnabled = true,
                lockedInputChannels = booleanArrayOf(true, true, true, false, false),
                inputAttenuationValue = 34,
                inputAttenuatorsCode = 0x240
            )
        val result = packetBytesConverter.receiverBytes(lowByte, highByte)

        assertThat(result.toString()).isEqualTo(expectedState.toString())
    }

    //3
    @Test
    fun `тест приёмника включён канал 2, заперт канал 3,5, включена секция 4 дБ, выключен пилот-сигнал`() {
        val lowByte: Byte = 0x0B
        val highByte: Byte = 0x5C.toByte()
        val expectedState =
            ReceiverModuleState(
                enabledChannelNumber = 2,
                testSignalIsEnabled = false,
                lockedInputChannels = booleanArrayOf(false, false, true, false, true),
                inputAttenuationValue = 4,
                inputAttenuatorsCode = 0x1
            )
        val result = packetBytesConverter.receiverBytes(lowByte, highByte)

        assertThat(result.toString()).isEqualTo(expectedState.toString())
    }

    //4
    @Test
    fun `тест приёмника включён канал 5, заперт канал 4,5, включены секции 2,8,16 дБ, выключен пилот-сигнал`() {
        val lowByte: Byte = 0xC6.toByte()
        val highByte: Byte = 0x79.toByte()
        val expectedState =
            ReceiverModuleState(
                enabledChannelNumber = 5,
                testSignalIsEnabled = false,
                lockedInputChannels = booleanArrayOf(false, false, false, true, true),
                inputAttenuationValue = 26,
                inputAttenuatorsCode = 0x1C0
            )
        val result = packetBytesConverter.receiverBytes(lowByte, highByte)

        assertThat(result.toString()).isEqualTo(expectedState.toString())
    }

    //5
    @Test
    fun `тест приёмника все каналы выключены, все заперты, включены все секции аттенюатора, выключен пилот-сигнал`() {
        val lowByte: Byte = 0xFF.toByte()
        val highByte: Byte = 0x7F.toByte()
        val expectedState =
            ReceiverModuleState(
                enabledChannelNumber = 0,
                testSignalIsEnabled = false,
                lockedInputChannels = booleanArrayOf(true, true, true, true, true),
                inputAttenuationValue = 62,
                inputAttenuatorsCode = 0x3C1
            )
        val result = packetBytesConverter.receiverBytes(lowByte, highByte)

        assertThat(result.toString()).isEqualTo(expectedState.toString())
    }

    @Test
    fun `тест синтезатора - 0x404E20`() {
        val lowByte: Byte = 0x20
        val middleByte: Byte = 0x4E.toByte()
        val highByte: Byte = 0x40.toByte()
        val expectedState = 0x404E20
        val result = packetBytesConverter.synthesizerBytes(lowByte, middleByte, highByte)

        assertThat(result).isEqualTo(expectedState)
    }

    @Test
    fun `тест синтезатора - неверный 0x840608`() {
        val lowByte: Byte = 0x84.toByte()
        val middleByte: Byte = 0x06.toByte()
        val highByte: Byte = 0x08.toByte()
        val expectedState = 0x840608
        val result = packetBytesConverter.synthesizerBytes(lowByte, middleByte, highByte)

        assertThat(result).isNotEqualTo(expectedState)
    }

    @Test
    fun `тест синтезатора - 0x840608`() {
        val lowByte: Byte = 0x08.toByte()
        val middleByte: Byte = 0x06.toByte()
        val highByte: Byte = 0x84.toByte()
        val expectedState = 0x840608
        val result = packetBytesConverter.synthesizerBytes(lowByte, middleByte, highByte)

        assertThat(result).isEqualTo(expectedState)
    }

    @Test
    fun `тест контроля выводов - выключен ПРД, выключен внешний запуск ЛЧМ, OUT_ФАПЧ - 0, напряжение детектора 3,66 В, напряжение ВИП - 7,32 В`() {
        val incomingBytes: ByteArray = byteArrayOf(0x00, 0x02, 0xEE.toByte(), 0x05, 0xDC.toByte())
        val expectedState = OutputModuleState(
            lfmExtTriggerIsOn = false,
            transmitterIsOn = false,
            pllIsLocked = false,
            transmitterDetectorVoltage = 3.66,
            secondaryPowerSourceVoltage = 7.32
        )
        val result = packetBytesConverter.rcdOutputBytes(incomingBytes)

        assertThat(result).isEqualTo(expectedState)
    }

    @Test
    fun `тест контроля выводов - включен ПРД, выключен внешний запуск ЛЧМ, OUT_ФАПЧ - 0, напряжение детектора 1,22 В, напряжение ВИП - 4,88 В`() {
        val incomingBytes: ByteArray = byteArrayOf(0x02, 0x00, 0xFA.toByte(), 0x03, 0xE8.toByte())
        val expectedState = OutputModuleState(
            lfmExtTriggerIsOn = false,
            transmitterIsOn = true,
            pllIsLocked = false,
            transmitterDetectorVoltage = 1.22,
            secondaryPowerSourceVoltage = 4.88
        )
        val result = packetBytesConverter.rcdOutputBytes(incomingBytes)

        assertThat(result).isEqualTo(expectedState)
    }

    @Test
    fun `тест контроля выводов - включен ПРД, выключен внешний запуск ЛЧМ, OUT_ФАПЧ - 1, напряжение детектора 0 В, напряжение ВИП - 0 В`() {
        val incomingBytes: ByteArray = byteArrayOf(0x06, 0x00, 0x00, 0x00, 0x00)
        val expectedState = OutputModuleState(
            lfmExtTriggerIsOn = false,
            transmitterIsOn = true,
            pllIsLocked = true,
            transmitterDetectorVoltage = 0.00,
            secondaryPowerSourceVoltage = 0.00
        )
        val result = packetBytesConverter.rcdOutputBytes(incomingBytes)

        assertThat(result).isEqualTo(expectedState)
    }

    @Test
    fun `тест контроля выводов - включен ПРД, включен внешний запуск ЛЧМ, OUT_ФАПЧ - 1, напряжение детектора 9,76 В, напряжение ВИП - 9,76 В`() {
        val incomingBytes: ByteArray = byteArrayOf(0x07, 0x07, 0xD0.toByte(), 0x07, 0xD0.toByte())
        val expectedState = OutputModuleState(
            lfmExtTriggerIsOn = true,
            transmitterIsOn = true,
            pllIsLocked = true,
            transmitterDetectorVoltage = 9.76,
            secondaryPowerSourceVoltage = 9.76
        )
        val result = packetBytesConverter.rcdOutputBytes(incomingBytes)

        assertThat(result).isEqualTo(expectedState)
    }

    @Test
    fun `тест функции получения температуры - +125 градусов`() {
        val lowByte: Byte = 0xD0.toByte()
        val highByte: Byte = 0x07.toByte()
        val expectedResult = 125.0
        val result = packetBytesConverter.rcdTemperatureBytes(lowByte, highByte)

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `тест функции получения температуры - +85 градусов`() {
        val lowByte: Byte = 0x50.toByte()
        val highByte: Byte = 0x05.toByte()
        val expectedResult = 85.0
        val result = packetBytesConverter.rcdTemperatureBytes(lowByte, highByte)

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `тест функции получения температуры - +0,5 градусов`() {
        val lowByte: Byte = 0x08.toByte()
        val highByte: Byte = 0x00.toByte()
        val expectedResult = 0.5
        val result = packetBytesConverter.rcdTemperatureBytes(lowByte, highByte)

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `тест функции получения температуры - +10,125 градусов`() {
        val lowByte: Byte = 0xA2.toByte()
        val highByte: Byte = 0x00.toByte()
        val expectedResult = 10.125
        val result = packetBytesConverter.rcdTemperatureBytes(lowByte, highByte)

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `тест функции получения температуры - 0 градусов`() {
        val lowByte: Byte = 0x00.toByte()
        val highByte: Byte = 0x00.toByte()
        val expectedResult = 0.0
        val result = packetBytesConverter.rcdTemperatureBytes(lowByte, highByte)

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `тест функции получения температуры - -0,5 градусов`() {
        val lowByte: Byte = 0xF8.toByte()
        val highByte: Byte = 0xFF.toByte()
        val expectedResult = -0.5
        val result = packetBytesConverter.rcdTemperatureBytes(lowByte, highByte)

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `тест функции получения температуры - -10,125 градусов`() {
        val lowByte: Byte = 0x5E.toByte()
        val highByte: Byte = 0xFF.toByte()
        val expectedResult = -10.125
        val result = packetBytesConverter.rcdTemperatureBytes(lowByte, highByte)

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `тест функции получения температуры - -25,0625 градусов`() {
        val lowByte: Byte = 0x6F.toByte()
        val highByte: Byte = 0xFE.toByte()
        val expectedResult = -25.0625
        val result = packetBytesConverter.rcdTemperatureBytes(lowByte, highByte)

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `тест функции получения температуры - -55 градусов`() {
        val lowByte: Byte = 0x90.toByte()
        val highByte: Byte = 0xFC.toByte()
        val expectedResult = -55.0
        val result = packetBytesConverter.rcdTemperatureBytes(lowByte, highByte)

        assertThat(result).isEqualTo(expectedResult)
    }
}