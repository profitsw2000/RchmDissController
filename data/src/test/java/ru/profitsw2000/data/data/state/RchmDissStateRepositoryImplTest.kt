package ru.profitsw2000.data.data.state

import com.google.common.truth.Truth.assertThat
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.profitsw2000.data.model.bluetooth.state.rcd.OutputModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissState
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.data.model.rcd.RcdInputPacketType

@OptIn(ExperimentalCoroutinesApi::class)
class RchmDissStateRepositoryImplTest {

    @Test
    fun `обновление состояния передатчика - включён канал 1`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateTransmitterModuleState(0x20.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.TransmitterStateInputPacket)
            assertThat(repository.rchmDissState.value.transmitterModuleState).isEqualTo(TransmitterModuleState(enabledChannelNumber = 1))
        }
    }

    @Test
    fun `обновление состояния передатчика - включён канал 2`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateTransmitterModuleState(0x10.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.TransmitterStateInputPacket)
            assertThat(repository.rchmDissState.value.transmitterModuleState).isEqualTo(TransmitterModuleState(enabledChannelNumber = 2))
        }
    }

    @Test
    fun `обновление состояния передатчика - выключены все каналы`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateTransmitterModuleState(0x00.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.TransmitterStateInputPacket)
            assertThat(repository.rchmDissState.value.transmitterModuleState).isEqualTo(TransmitterModuleState(enabledChannelNumber = 0))
        }
    }

    @Test
    fun `обновление состояния передатчика - нестандартный байт`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateTransmitterModuleState(0x58.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.TransmitterStateInputPacket)
            assertThat(repository.rchmDissState.value.transmitterModuleState).isEqualTo(TransmitterModuleState(enabledChannelNumber = 0))
        }
    }

    //6
    @Test
    fun `приёмник включён канал 2, заперт канал 2, включены секции 8 и 16 дБ, включён пилот-сигнал`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateReceiverModuleState(0x90.toByte(), 0xDD.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.ReceiverStateInputPacket)
            assertThat(repository.rchmDissState.value.receiverModuleState.toString())
                .isEqualTo(
                    ReceiverModuleState(
                        enabledChannelNumber = 2,
                        testSignalIsEnabled = true,
                        lockedInputChannels = booleanArrayOf(false, true, false, false, false),
                        inputAttenuationValue = 24,
                        inputAttenuatorsCode = 0x180
                    ).toString()
                )
        }
    }

    //7
    @Test
    fun `тест приёмника включён канал 3, заперт канал 4, 5, включены секции 2 и 32 дБ, включён пилот-сигнал`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateReceiverModuleState(0x46.toByte(), 0xEE.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.ReceiverStateInputPacket)
            assertThat(repository.rchmDissState.value.receiverModuleState.toString())
                .isEqualTo(
                    ReceiverModuleState(
                        enabledChannelNumber = 3,
                        testSignalIsEnabled = true,
                        lockedInputChannels = booleanArrayOf(false, false, false, true, true),
                        inputAttenuationValue = 34,
                        inputAttenuatorsCode = 0x240
                    ).toString()
                )
        }
    }

    //8
    @Test
    fun `тест приёмника включён канал 4, заперт канал 1,5, включены секции 32,8,16 дБ, выключен пилот-сигнал`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateReceiverModuleState(0xA2.toByte(), 0x77)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.ReceiverStateInputPacket)
            assertThat(repository.rchmDissState.value.receiverModuleState.toString())
                .isEqualTo(
                    ReceiverModuleState(
                        enabledChannelNumber = 4,
                        testSignalIsEnabled = false,
                        lockedInputChannels = booleanArrayOf(true, false, false, false, true),
                        inputAttenuationValue = 56,
                        inputAttenuatorsCode = 0x380
                    ).toString()
                )
        }
    }

    //9
    @Test
    fun `тест приёмника все каналы выключены, все заперты, выключены все секции аттенюатора, выключен пилот-сигнал`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateReceiverModuleState(0x3E.toByte(), 0x7C.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.ReceiverStateInputPacket)
            assertThat(repository.rchmDissState.value.receiverModuleState.toString())
                .isEqualTo(
                    ReceiverModuleState(
                        enabledChannelNumber = 0,
                        testSignalIsEnabled = false,
                        lockedInputChannels = booleanArrayOf(true, true, true, true, true),
                        inputAttenuationValue = 0,
                        inputAttenuatorsCode = 0
                    ).toString()
                )
        }
    }

    @Test
    fun `тест синтезатора несимметричная ЛЧМ`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateSynthesizerModuleState(0x00, 0x00, 0x70)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x01, 0x00, 0x00)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0xA5.toByte(), 0x00, 0x20)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x20, 0x4E.toByte(), 0x40)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x00, 0x7D, 0x60)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x08, 0x06, 0x84.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x02, 0x00, 0xA0.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x01, 0x00, 0xC0.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0xF0.toByte(), 0x00, 0x10)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0xF9.toByte(), 0xA0.toByte(), 0x3F.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x04.toByte(), 0x00, 0x50)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)

            assertThat(repository.rchmDissState.value.synthesizerModuleState)
                .isEqualTo(
                    SynthesizerModuleState().copy(
                        refRegister = listOf(0x01, 0x01),
                        intRegister = listOf(0x2000A5, 0x20000C),
                        fracRegister = listOf(0x404E20, 0x400000),
                        modRegister = listOf(0x607D00, 0x600000),
                        ctr1Register = listOf(0x840608, 0x800000),
                        ctr2Register = listOf(0xA00002, 0xA0003F),
                        ctr3Register = listOf(0xC00001, 0xC00015),
                        lfm1Register = listOf(0x1000F0, 0x100000),
                        lfm2Register = listOf(0x3FA0F9, 0x300000),
                        lfm3Register = listOf(0x500004, 0x500000),
                        prwRegister = 0x700000,
                        praRegister = 0x900000
                    )
                )
        }
    }

    @Test
    fun `тест синтезатора симметричная ЛЧМ`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateSynthesizerModuleState(0x00, 0x00, 0x70)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x01, 0x00, 0x00)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0xA5.toByte(), 0x00, 0x20)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x20, 0x4E.toByte(), 0x40)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x00, 0x7D, 0x60)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x08, 0x06, 0x84.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x02, 0x00, 0xA0.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x01, 0x00, 0xC0.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0xF0.toByte(), 0x00, 0x10)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x7C.toByte(), 0xA0.toByte(), 0x3F.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x04.toByte(), 0x02, 0x50)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)

            repository.updateSynthesizerModuleState(0x00, 0x40, 0x70)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x01, 0x00, 0x00)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0xA7.toByte(), 0x00, 0x20)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x80.toByte(), 0x3E.toByte(), 0x40)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x00, 0x7D, 0x60)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x08, 0x06, 0x84.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x02, 0x00, 0xA0.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x01, 0x00, 0xC0.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0xF0.toByte(), 0x00, 0x10)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x7C.toByte(), 0xA0.toByte(), 0x3F.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x06.toByte(), 0x00, 0x50)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)
            repository.updateSynthesizerModuleState(0x00, 0x00, 0x90.toByte())
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.SynthesizerStateInputPacket)

            assertThat(repository.rchmDissState.value.synthesizerModuleState)
                .isEqualTo(
                    SynthesizerModuleState().copy(
                        refRegister = listOf(0x01, 0x01),
                        intRegister = listOf(0x2000A5, 0x2000A7),
                        fracRegister = listOf(0x404E20, 0x403E80),
                        modRegister = listOf(0x607D00, 0x607D00),
                        ctr1Register = listOf(0x840608, 0x840608),
                        ctr2Register = listOf(0xA00002, 0xA00002),
                        ctr3Register = listOf(0xC00001, 0xC00001),
                        lfm1Register = listOf(0x1000F0, 0x1000F0),
                        lfm2Register = listOf(0x3FA07C, 0x3FA07C),
                        lfm3Register = listOf(0x500204, 0x500006),
                        prwRegister = 0x704000,
                        praRegister = 0x900000
                    )
                )
        }
    }

    @Test
    fun `контроль выводов - внешний запуск ЛЧМ отключён, включ передатчик, нет захвата ФАПЧ, напр детектора 2,44В, напр ВИП 7,32В`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateOutputModuleState(
                byteArrayOf(0x02, 0x01, 0xF4.toByte(), 0x05, 0xDC.toByte())
            )
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.RcdOutputControlInputPacket)
            assertThat(repository.rchmDissState.value.outputModuleState)
                .isEqualTo(
                    OutputModuleState(
                        lfmExtTriggerIsOn = false,
                        transmitterIsOn = true,
                        pllIsLocked = false,
                        transmitterDetectorVoltage = 2.44,
                        secondaryPowerSourceVoltage = 7.32
                    )
                )
        }
    }

    @Test
    fun `контроль выводов - внешний запуск ЛЧМ включён, включ передатчик, захват ФАПЧ, напр детектора 1,22В, напр ВИП 7,32В`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateOutputModuleState(
                byteArrayOf(0x02, 0x00, 0xFA.toByte(), 0x05, 0xDC.toByte())
            )
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.RcdOutputControlInputPacket)
            assertThat(repository.rchmDissState.value.outputModuleState)
                .isEqualTo(
                    OutputModuleState(
                        lfmExtTriggerIsOn = false,
                        transmitterIsOn = true,
                        pllIsLocked = false,
                        transmitterDetectorVoltage = 1.22,
                        secondaryPowerSourceVoltage = 7.32
                    )
                )
        }
    }

    @Test
    fun `контроль выводов - внешний запуск ЛЧМ отключён, выключ передатчик, нет захвата ФАПЧ, напр детектора 7,32В, напр ВИП 1,44В`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateOutputModuleState(
                byteArrayOf(0x00, 0x05, 0xDC.toByte(), 0x01, 0xF4.toByte())
            )
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.RcdOutputControlInputPacket)
            assertThat(repository.rchmDissState.value.outputModuleState)
                .isEqualTo(
                    OutputModuleState(
                        lfmExtTriggerIsOn = false,
                        transmitterIsOn = false,
                        pllIsLocked = false,
                        transmitterDetectorVoltage = 7.32,
                        secondaryPowerSourceVoltage = 2.44
                    )
                )
        }
    }

    @Test
    fun `контроль выводов - внешний запуск ЛЧМ включён, выключ передатчик, захват ФАПЧ, напр детектора 4,88В, напр ВИП 4,88В`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateOutputModuleState(
                byteArrayOf(0x05, 0x03, 0xE8.toByte(), 0x03, 0xE8.toByte())
            )
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.RcdOutputControlInputPacket)
            assertThat(repository.rchmDissState.value.outputModuleState)
                .isEqualTo(
                    OutputModuleState(
                        lfmExtTriggerIsOn = true,
                        transmitterIsOn = false,
                        pllIsLocked = true,
                        transmitterDetectorVoltage = 4.88,
                        secondaryPowerSourceVoltage = 4.88
                    )
                )
        }
    }

    @Test
    fun `тест обновления температуры - +85 градусов`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)
        val lowByte: Byte = 0x50.toByte()
        val highByte: Byte = 0x05.toByte()
        val expectedResult = 85.0

        repository.lastPacket.test {
            repository.writeModuleTemperature(lowByte, highByte)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.RcdTemperatureInputPacket)
            assertThat(repository.rchmDissState.value.innerModuleTemperature).isEqualTo(expectedResult)
        }
    }

    @Test
    fun `тест обновления температуры - -10,125 градусов`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)
        val lowByte: Byte = 0x5E.toByte()
        val highByte: Byte = 0xFF.toByte()
        val expectedResult = -10.125

        repository.lastPacket.test {
            repository.writeModuleTemperature(lowByte, highByte)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.RcdTemperatureInputPacket)
            assertThat(repository.rchmDissState.value.innerModuleTemperature).isEqualTo(expectedResult)
        }
    }

    @Test
    fun `тест обновления температуры - -0,5 градусов`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)
        val lowByte: Byte = 0xF8.toByte()
        val highByte: Byte = 0xFF.toByte()
        val expectedResult = -0.5

        repository.lastPacket.test {
            repository.writeModuleTemperature(lowByte, highByte)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.RcdTemperatureInputPacket)
            assertThat(repository.rchmDissState.value.innerModuleTemperature).isEqualTo(expectedResult)
        }
    }

    @Test
    fun `тест обновления байта данных из памяти модуля - 0xAA`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)
        val dataByte: Byte = 0xAA.toByte()
        val expectedByte = 0xAA.toByte()

        repository.lastPacket.test {
            repository.writeModuleMemoryByte(dataByte)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.RcdMemoryReadInputPacket)
            assertThat(repository.rchmDissState.value.readMemoryValue).isEqualTo(expectedByte)
        }
    }

    @Test
    fun `тест обновления байта данных из памяти модуля - 0x00`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)
        val dataByte: Byte = 0x00.toByte()
        val expectedByte = 0x00.toByte()

        repository.lastPacket.test {
            repository.writeModuleMemoryByte(dataByte)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.RcdMemoryReadInputPacket)
            assertThat(repository.rchmDissState.value.readMemoryValue).isEqualTo(expectedByte)
        }
    }

    @Test
    fun `тест обновления байта данных из памяти модуля - 0xFF`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)
        val dataByte: Byte = 0xFF.toByte()
        val expectedByte = 0xFF.toByte()

        repository.lastPacket.test {
            repository.writeModuleMemoryByte(dataByte)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.RcdMemoryReadInputPacket)
            assertThat(repository.rchmDissState.value.readMemoryValue).isEqualTo(expectedByte)
        }
    }
}