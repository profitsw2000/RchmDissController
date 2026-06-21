package ru.profitsw2000.data.data.state

import com.google.common.truth.Truth.assertThat
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissState
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
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

    @Test
    fun `приёмник включён канал 2, заперт канал 2, включены секции 8 и 16 дБ, включён пилот-сигнал`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateReceiverModuleState(0x58.toByte(), 0x34)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.ReceiverStateInputPacket)
            assertThat(repository.rchmDissState.value.receiverModuleState)
                .isEqualTo(
                    ReceiverModuleState(
                        enabledChannelNumber = 2,
                        testSignalIsEnabled = true,
                        lockedInputChannels = booleanArrayOf(false, true, false, false, false),
                        inputAttenuationValue = 24,
                        inputAttenuatorsCode = 0x35
                    )
                )
        }
    }

    @Test
    fun `тест приёмника включён канал 3, заперт канал 4, 5, включены секции 2 и 32 дБ, включён пилот-сигнал`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateReceiverModuleState(0x58.toByte(), 0x34)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.ReceiverStateInputPacket)
            assertThat(repository.rchmDissState.value.receiverModuleState)
                .isEqualTo(
                    ReceiverModuleState(
                        enabledChannelNumber = 3,
                        testSignalIsEnabled = true,
                        lockedInputChannels = booleanArrayOf(false, false, false, true, true),
                        inputAttenuationValue = 34,
                        inputAttenuatorsCode = 0x35
                    )
                )
        }
    }

    @Test
    fun `тест приёмника включён канал 4, заперт канал 1,5, включены секции 32,8,16 дБ, выключен пилот-сигнал`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateReceiverModuleState(0x58.toByte(), 0x34)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.ReceiverStateInputPacket)
            assertThat(repository.rchmDissState.value.receiverModuleState)
                .isEqualTo(
                    ReceiverModuleState(
                        enabledChannelNumber = 4,
                        testSignalIsEnabled = false,
                        lockedInputChannels = booleanArrayOf(true, false, false, false, true),
                        inputAttenuationValue = 56,
                        inputAttenuatorsCode = 0x35
                    )
                )
        }
    }

    @Test
    fun `тест приёмника все каналы выключены, все заперты, выключены все секции аттенюатора, выключен пилот-сигнал`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = RchmDissStateRepositoryImpl(defaultDispatcher = testDispatcher)

        repository.lastPacket.test {
            repository.updateReceiverModuleState(0x58.toByte(), 0x34)
            assertThat(awaitItem()).isEqualTo(RcdInputPacketType.ReceiverStateInputPacket)
            assertThat(repository.rchmDissState.value.receiverModuleState)
                .isEqualTo(
                    ReceiverModuleState(
                        enabledChannelNumber = 0,
                        testSignalIsEnabled = false,
                        lockedInputChannels = booleanArrayOf(true, true, true, true, true),
                        inputAttenuationValue = 0,
                        inputAttenuatorsCode = 0x35
                    )
                )
        }
    }
}