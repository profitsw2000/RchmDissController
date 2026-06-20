package ru.profitsw2000.data.data.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import ru.profitsw2000.data.mapper.PacketBytesConverter
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

/*    fun testReceiverBytes() {
    }

    fun testSynthesizerBytes() {
    }

    fun testRcdOutputBytes() {
    }

    fun testRcdTemperatureBytes() {
    }*/

}