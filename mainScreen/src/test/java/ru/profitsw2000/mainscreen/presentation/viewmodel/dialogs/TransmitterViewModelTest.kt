package ru.profitsw2000.mainscreen.presentation.viewmodel.dialogs

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import ru.profitsw2000.data.model.bluetooth.state.rcd.OutputModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.data.model.rcd.RcdInputPacketType
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainDispatcherRule
import ru.profitsw2000.mainscreen.state.TransmitterUpdatingStatus
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class TransmitterViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val rchmDissStateRepository: RchmDissStateRepository = mockk(relaxed = true)
    private val bluetoothRepository: BluetoothRepository = mockk(relaxed = true)
    private val bluetoothPacketManager: BluetoothPacketManager = mockk(relaxed = true)
    private val pllRegisters1208PL1URepository: PLLRegisters1208PL1URepository = mockk(relaxed = true)

    @Test
    fun `проверка удачной отправки и получения ответа`() = runTest {
        val mockRchmDissState = RchmDissState(
            transmitterModuleState = TransmitterModuleState(
                enabledChannelNumber = 4
            ),
            outputModuleState = OutputModuleState(
                lfmExtTriggerIsOn = true,
                transmitterDetectorVoltage = 20.0,
                secondaryPowerSourceVoltage = 12.3
            )
        )
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(mockRchmDissState)

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val mockTransmitterPacket = byteArrayOf(0x53, 0x05, 0x01, 0x22, 0x3A)
        val mockOutputPacket = byteArrayOf(0x53, 0x05, 0x08, 0xA7.toByte(), 0x8B.toByte(), 0x98.toByte())
        every { bluetoothPacketManager.getWriteToTransmitterPacket(any()) } returns mockTransmitterPacket
        every { bluetoothPacketManager.getRchmDissOutputSetPacket(any()) } returns mockOutputPacket

        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns SynthesizerModuleStateModel(lfmPeriod = 0.01)

        val transmitterViewModel = TransmitterViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager,
            pllRegisters1208PL1URepository
        )

        transmitterViewModel.transmitterUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is TransmitterUpdatingStatus.Idle)
            with(firstItem as TransmitterUpdatingStatus.Idle) {
                assertEquals(mockRchmDissState.transmitterModuleState, transmitterModuleState)
                assertEquals(mockRchmDissState.outputModuleState, outputModuleState)
            }
            //запускаем отправку по блютуз
            transmitterViewModel.updateTransmitter(0x22, true)

            val secondItem = awaitItem()
            assertTrue(secondItem is TransmitterUpdatingStatus.Updating)
            advanceTimeBy(500.milliseconds)

            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)

            expectNoEvents()

            advanceTimeBy(500.milliseconds)

            lastPacketFlow.emit(RcdInputPacketType.TransmitterStateInputPacket)

            val fourthItem = awaitItem()
            assertTrue(fourthItem is TransmitterUpdatingStatus.Success)

            advanceTimeBy(501.milliseconds)

            val fifthItem = awaitItem()
            assertTrue(fifthItem is TransmitterUpdatingStatus.Idle)
            with(fifthItem as TransmitterUpdatingStatus.Idle) {
                assertEquals(mockRchmDissState.transmitterModuleState, transmitterModuleState)
                assertEquals(mockRchmDissState.outputModuleState, outputModuleState)
            }
            coVerify(exactly = 2) { bluetoothRepository.bluetoothDataRepository.writeData(any()) }
            coVerify { bluetoothRepository.bluetoothDataRepository.writeData(mockTransmitterPacket) }
            coVerify { bluetoothRepository.bluetoothDataRepository.writeData(mockOutputPacket) }

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `проверка удачной отправки и не получения ответа`() = runTest {
        val mockRchmDissState = RchmDissState(
            transmitterModuleState = TransmitterModuleState(
                enabledChannelNumber = 1
            ),
            outputModuleState = OutputModuleState(
                lfmExtTriggerIsOn = false,
                transmitterDetectorVoltage = 7.0,
                secondaryPowerSourceVoltage = 5.3
            )
        )
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(mockRchmDissState)

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val mockTransmitterPacket = byteArrayOf(0x53, 0x05, 0x01, 0x22, 0x3A)
        val mockOutputPacket = byteArrayOf(0x53, 0x05, 0x08, 0xA7.toByte(), 0x8B.toByte(), 0x98.toByte())
        every { bluetoothPacketManager.getWriteToTransmitterPacket(any()) } returns mockTransmitterPacket
        every { bluetoothPacketManager.getRchmDissOutputSetPacket(any()) } returns mockOutputPacket

        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns SynthesizerModuleStateModel(lfmPeriod = 0.05)

        val transmitterViewModel = TransmitterViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager,
            pllRegisters1208PL1URepository
        )

        transmitterViewModel.transmitterUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is TransmitterUpdatingStatus.Idle)
            with(firstItem as TransmitterUpdatingStatus.Idle) {
                assertEquals(mockRchmDissState.transmitterModuleState, transmitterModuleState)
                assertEquals(mockRchmDissState.outputModuleState, outputModuleState)
            }
            //запускаем отправку по блютуз
            transmitterViewModel.updateTransmitter(0x22, true)

            val secondItem = awaitItem()
            assertTrue(secondItem is TransmitterUpdatingStatus.Updating)
            advanceTimeBy(2500.milliseconds)

            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)

            expectNoEvents()

            advanceTimeBy(2000.milliseconds)

            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)

            expectNoEvents()

            advanceTimeBy(501.milliseconds)

            val fifthItem = awaitItem()
            assertTrue(fifthItem is TransmitterUpdatingStatus.Error)
            with(fifthItem as TransmitterUpdatingStatus.Error) {
                assertEquals(RESPONSE_PACKET_TIMEOUT_ERROR_CODE, errorCode)
            }
/*            coVerify { bluetoothRepository.bluetoothDataRepository.writeData(mockTransmitterPacket) }
            coVerify(exactly = 0) { bluetoothPacketManager.getRchmDissOutputSetPacket(any()) }*/
            coVerifySequence {
                // Тест подтвердит, что за всё время выполнился только ОДИН этот вызов
                bluetoothRepository.bluetoothDataRepository.writeData(mockTransmitterPacket)
            }

            cancelAndIgnoreRemainingEvents()
        }
    }
}