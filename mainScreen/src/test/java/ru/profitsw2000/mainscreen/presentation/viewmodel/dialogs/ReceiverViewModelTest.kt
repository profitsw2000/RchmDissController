package ru.profitsw2000.mainscreen.presentation.viewmodel.dialogs

import app.cash.turbine.test
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.profitsw2000.core.drawable.utils.RESPONSE_PACKET_TIMEOUT_ERROR_CODE
import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.pll.PLLRegisters1208PL1URepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissState
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.rcd.RcdInputPacketType
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainDispatcherRule
import ru.profitsw2000.mainscreen.state.ReceiverUpdatingStatus
import ru.profitsw2000.mainscreen.state.TransmitterUpdatingStatus
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val rchmDissStateRepository: RchmDissStateRepository = mockk(relaxed = true)
    private val bluetoothRepository: BluetoothRepository = mockk(relaxed = true)
    private val bluetoothPacketManager: BluetoothPacketManager = mockk(relaxed = true)

    @Test
    fun `отправка пакета и получение ответного`() = runTest {
        val mockRchmDissState = RchmDissState(
            receiverModuleState =
                ReceiverModuleState(
                    enabledChannelNumber = 3,
                    testSignalIsEnabled = true,
                    lockedInputChannels = booleanArrayOf(true, false, true, false, false),
                    inputAttenuationValue = 6,
                    inputAttenuatorsCode = 0x41
                )
        )
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(mockRchmDissState)

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val mockReceiverPacket = byteArrayOf(0x53, 0x06, 0x03, 0x92.toByte(), 0x41, 0xBD.toByte())

        every { bluetoothPacketManager.getWriteToReceiverPacket(any()) } returns mockReceiverPacket

        val receiverViewModel = ReceiverViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager
        )

        receiverViewModel.receiverUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is ReceiverUpdatingStatus.Idle)
            with(firstItem as ReceiverUpdatingStatus.Idle) {
                assertEquals(this.receiverModuleState, mockRchmDissState.receiverModuleState)
            }

            receiverViewModel.updateReceiver(byteArrayOf(0x92.toByte(), 0x41))
            val secondItem = awaitItem()
            assertTrue(secondItem is ReceiverUpdatingStatus.Updating)

            advanceTimeBy(1000.milliseconds)

            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()

            advanceTimeBy(3999.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.ReceiverStateInputPacket)

            val thirdItem = awaitItem()
            assertTrue(thirdItem is ReceiverUpdatingStatus.Success)

            advanceTimeBy(100.milliseconds)

            lastPacketFlow.emit(RcdInputPacketType.TransmitterStateInputPacket)
            expectNoEvents()

            advanceTimeBy(401.milliseconds)
            val fourthItem = awaitItem()
            assertTrue(fourthItem is ReceiverUpdatingStatus.Idle)

            with(fourthItem as ReceiverUpdatingStatus.Idle) {
                assertEquals(this.receiverModuleState, mockRchmDissState.receiverModuleState)
            }

            coVerify(exactly = 1) { bluetoothRepository.bluetoothDataRepository.writeData(any()) }
            ensureAllEventsConsumed()
        }
    }



    @Test
    fun `отправка пакета и не получение ответного`() = runTest {
        val mockRchmDissState = RchmDissState(
            receiverModuleState =
                ReceiverModuleState(
                    enabledChannelNumber = 0,
                    testSignalIsEnabled = false,
                    lockedInputChannels = booleanArrayOf(true, true, true, false, false),
                    inputAttenuationValue = 32,
                    inputAttenuatorsCode = 0x100
                )
        )
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(
            mockRchmDissState
        )

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val mockReceiverPacket =
            byteArrayOf(0x53, 0x06, 0x03, 0x93.toByte(), 0x00, 0xF5.toByte())

        every { bluetoothPacketManager.getWriteToReceiverPacket(any()) } returns mockReceiverPacket

        val receiverViewModel = ReceiverViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager
        )

        receiverViewModel.receiverUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is ReceiverUpdatingStatus.Idle)
            with(firstItem as ReceiverUpdatingStatus.Idle) {
                assertEquals(this.receiverModuleState, mockRchmDissState.receiverModuleState)
            }

            receiverViewModel.updateReceiver(byteArrayOf(0x92.toByte(), 0x41))
            val secondItem = awaitItem()
            assertTrue(secondItem is ReceiverUpdatingStatus.Updating)

            advanceTimeBy(1000.milliseconds)

            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()

            advanceTimeBy(4001.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.ReceiverStateInputPacket)

            val thirdItem = awaitItem()
            assertTrue(thirdItem is ReceiverUpdatingStatus.Error)
            with(thirdItem as ReceiverUpdatingStatus.Error) {
                assertTrue(errorCode == RESPONSE_PACKET_TIMEOUT_ERROR_CODE)
            }
            expectNoEvents()

            coVerify(exactly = 1) { bluetoothRepository.bluetoothDataRepository.writeData(any()) }
            ensureAllEventsConsumed()
        }
    }
}