package ru.profitsw2000.mainscreen.presentation.viewmodel.dialogs

import app.cash.turbine.test
import io.mockk.coEvery
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
import ru.profitsw2000.core.drawable.utils.CW_FREQUENCY_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.CW_FREQUENCY_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.HIGH_FREQUENCY_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.HIGH_FREQUENCY_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.LOW_FREQUENCY_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.LOW_FREQUENCY_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.LOW_FREQ_HIGHER_THAN_HIGH_FREQ_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.MODULATION_PERIOD_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.MODULATION_PERIOD_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.RESPONSE_PACKET_TIMEOUT_ERROR_CODE
import ru.profitsw2000.core.drawable.utils.UNKNOWN_ERROR_CODE
import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.pll.PLLRegisters1208PL1URepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
import ru.profitsw2000.data.model.bluetooth.state.rcd.RadiationMode
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissState
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel
import ru.profitsw2000.data.model.pll.LfmInputParametersModel
import ru.profitsw2000.data.model.rcd.RcdInputPacketType
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainDispatcherRule
import ru.profitsw2000.mainscreen.state.SynthesizerUpdatingStatus
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SynthesizerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val rchmDissStateRepository: RchmDissStateRepository = mockk(relaxed = true)
    private val bluetoothRepository: BluetoothRepository = mockk(relaxed = true)
    private val bluetoothPacketManager: BluetoothPacketManager = mockk(relaxed = true)
    private val pllRegisters1208PL1URepository: PLLRegisters1208PL1URepository = mockk(relaxed = true)

    @Test
    fun `ввод частоты НГ ниже порога`() = runTest {
        val synthesizerModuleState = SynthesizerModuleState(
            refRegister = listOf(0x10, 0x00),
            intRegister = listOf(0x200A72, 0x00),
            fracRegister = listOf(0x4001F4, 0x00),
            modRegister = listOf(0x6001F4, 0x00),
            ctr1Register = listOf(0x890688, 0x00),
            ctr2Register = listOf(0xA00005, 0x00),
            ctr3Register = listOf(0xC00001, 0x00),
            lfm1Register = listOf(0x00, 0x00),
            lfm2Register = listOf(0x00, 0x00),
            lfm3Register = listOf(0x00, 0x00),
            prwRegister = 0x700000,
            praRegister = 0x900000
        )
        val mockRchmDissState = RchmDissState(
            synthesizerModuleState = synthesizerModuleState
        )
        val mockSynthesizerPacket = byteArrayOf(0x53, 0x07, 0x02, 0x40, 0x01, 0xF4.toByte(), 0x77)
        val mockSynthesizerStateModel = SynthesizerModuleStateModel(
            radiationMode = RadiationMode.CW,
            cwFrequency = 13_320_000_000
        )
        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns mockSynthesizerStateModel
        coEvery { pllRegisters1208PL1URepository.getCwRegisters(any()) } returns listOf(
            0x10, 0x200A72, 0x4001F4, 0x6001F4, 0x890688, 0xA00005, 0xC00001, 0x00, 0x00, 0x00, 0x700000, 0x900000
        )
        every { bluetoothPacketManager.getWriteToSynthesizerPacket(any()) } returns mockSynthesizerPacket
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(mockRchmDissState)

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val synthesizerViewModel = SynthesizerViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager,
            pllRegisters1208PL1URepository,
            mainDispatcherRule.testDispatcher
        )

        synthesizerViewModel.synthesizerUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is SynthesizerUpdatingStatus.Idle)
            with(firstItem as SynthesizerUpdatingStatus.Idle) {
                assertEquals(firstItem.synthesizerModuleStateModel, mockSynthesizerStateModel)
            }

            synthesizerViewModel.updateSynthesizerCwMode(100L)
            val secondItem = awaitItem()
            assertTrue(secondItem is SynthesizerUpdatingStatus.Updating)
            advanceTimeBy(100.milliseconds)
            val thirdItem = awaitItem()
            assertTrue(thirdItem is SynthesizerUpdatingStatus.Error)
            with(thirdItem as SynthesizerUpdatingStatus.Error) {
                assertEquals(thirdItem.errorCode, CW_FREQUENCY_UNDER_INPUT_ERROR)
            }

            coVerify(exactly = 0) { pllRegisters1208PL1URepository.getCwRegisters(any()) }
            coVerify(exactly = 0) { bluetoothRepository.bluetoothDataRepository.writeData(any()) }
            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getLfmParameters(any()) }
        }
    }

    @Test
    fun `ввод частоты НГ выше порога`() = runTest {
        val synthesizerModuleState = SynthesizerModuleState(
            refRegister = listOf(0x10, 0x00),
            intRegister = listOf(0x200A72, 0x00),
            fracRegister = listOf(0x4001F4, 0x00),
            modRegister = listOf(0x6001F4, 0x00),
            ctr1Register = listOf(0x890688, 0x00),
            ctr2Register = listOf(0xA00005, 0x00),
            ctr3Register = listOf(0xC00001, 0x00),
            lfm1Register = listOf(0x00, 0x00),
            lfm2Register = listOf(0x00, 0x00),
            lfm3Register = listOf(0x00, 0x00),
            prwRegister = 0x700000,
            praRegister = 0x900000
        )
        val mockRchmDissState = RchmDissState(
            synthesizerModuleState = synthesizerModuleState
        )
        val mockSynthesizerPacket = byteArrayOf(0x53, 0x07, 0x02, 0x40, 0x01, 0xF4.toByte(), 0x77)
        val mockSynthesizerStateModel = SynthesizerModuleStateModel(
            radiationMode = RadiationMode.CW,
            cwFrequency = 13_320_000_000
        )
        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns mockSynthesizerStateModel
        coEvery { pllRegisters1208PL1URepository.getCwRegisters(any()) } returns listOf(
            0x10, 0x200A72, 0x4001F4, 0x6001F4, 0x890688, 0xA00005, 0xC00001, 0x00, 0x00, 0x00, 0x700000, 0x900000
        )
        every { bluetoothPacketManager.getWriteToSynthesizerPacket(any()) } returns mockSynthesizerPacket
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(mockRchmDissState)

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val synthesizerViewModel = SynthesizerViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager,
            pllRegisters1208PL1URepository,
            mainDispatcherRule.testDispatcher
        )

        synthesizerViewModel.synthesizerUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is SynthesizerUpdatingStatus.Idle)
            with(firstItem as SynthesizerUpdatingStatus.Idle) {
                assertEquals(firstItem.synthesizerModuleStateModel, mockSynthesizerStateModel)
            }

            synthesizerViewModel.updateSynthesizerCwMode(13_410)
            val secondItem = awaitItem()
            assertTrue(secondItem is SynthesizerUpdatingStatus.Updating)
            advanceTimeBy(100.milliseconds)
            val thirdItem = awaitItem()
            assertTrue(thirdItem is SynthesizerUpdatingStatus.Error)
            with(thirdItem as SynthesizerUpdatingStatus.Error) {
                assertEquals(thirdItem.errorCode, CW_FREQUENCY_ABOVE_INPUT_ERROR)
            }

            coVerify(exactly = 0) { pllRegisters1208PL1URepository.getCwRegisters(any()) }
            coVerify(exactly = 0) { bluetoothRepository.bluetoothDataRepository.writeData(any()) }
            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getLfmParameters(any()) }
        }
    }

    @Test
    fun `настройка НГ без ответа на первый пакет`() = runTest {
        val synthesizerModuleState = SynthesizerModuleState(
            refRegister = listOf(0x10, 0x00),
            intRegister = listOf(0x200A72, 0x00),
            fracRegister = listOf(0x4001F4, 0x00),
            modRegister = listOf(0x6001F4, 0x00),
            ctr1Register = listOf(0x890688, 0x00),
            ctr2Register = listOf(0xA00005, 0x00),
            ctr3Register = listOf(0xC00001, 0x00),
            lfm1Register = listOf(0x00, 0x00),
            lfm2Register = listOf(0x00, 0x00),
            lfm3Register = listOf(0x00, 0x00),
            prwRegister = 0x700000,
            praRegister = 0x900000
        )
        val mockRchmDissState = RchmDissState(
            synthesizerModuleState = synthesizerModuleState
        )
        val mockSynthesizerPacket = byteArrayOf(0x53, 0x07, 0x02, 0x40, 0x01, 0xF4.toByte(), 0x77)
        val mockSynthesizerStateModel = SynthesizerModuleStateModel(
            radiationMode = RadiationMode.CW,
            cwFrequency = 13_320_000_000
        )
        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns mockSynthesizerStateModel
        coEvery { pllRegisters1208PL1URepository.getCwRegisters(any()) } returns listOf(
            0x10, 0x200A72, 0x4001F4, 0x6001F4, 0x890688, 0xA00005, 0xC00001, 0x00, 0x00, 0x00, 0x700000, 0x900000
        )
        every { bluetoothPacketManager.getWriteToSynthesizerPacket(any()) } returns mockSynthesizerPacket
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(mockRchmDissState)

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val synthesizerViewModel = SynthesizerViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager,
            pllRegisters1208PL1URepository,
            mainDispatcherRule.testDispatcher
        )

        synthesizerViewModel.synthesizerUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is SynthesizerUpdatingStatus.Idle)
            with(firstItem as SynthesizerUpdatingStatus.Idle) {
                assertEquals(firstItem.synthesizerModuleStateModel, mockSynthesizerStateModel)
            }

            synthesizerViewModel.updateSynthesizerCwMode(13_320)
            val secondItem = awaitItem()
            assertTrue(secondItem is SynthesizerUpdatingStatus.Updating)
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()
            advanceTimeBy(4001.milliseconds)
            val thirdItem = awaitItem()
            assertTrue(thirdItem is SynthesizerUpdatingStatus.Error)
            with(thirdItem as SynthesizerUpdatingStatus.Error) {
                assertEquals(thirdItem.errorCode, RESPONSE_PACKET_TIMEOUT_ERROR_CODE)
            }

            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getCwRegisters(any()) }
            coVerify(exactly = 1) { bluetoothRepository.bluetoothDataRepository.writeData(any()) }
            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getLfmParameters(any()) }
        }
    }

    @Test
    fun `настройка НГ нет ответа после 6 пакетов`() = runTest {
        val synthesizerModuleState = SynthesizerModuleState(
            refRegister = listOf(0x10, 0x00),
            intRegister = listOf(0x200A72, 0x00),
            fracRegister = listOf(0x4001F4, 0x00),
            modRegister = listOf(0x6001F4, 0x00),
            ctr1Register = listOf(0x890688, 0x00),
            ctr2Register = listOf(0xA00005, 0x00),
            ctr3Register = listOf(0xC00001, 0x00),
            lfm1Register = listOf(0x00, 0x00),
            lfm2Register = listOf(0x00, 0x00),
            lfm3Register = listOf(0x00, 0x00),
            prwRegister = 0x700000,
            praRegister = 0x900000
        )
        val mockRchmDissState = RchmDissState(
            synthesizerModuleState = synthesizerModuleState
        )
        val mockSynthesizerPacket = byteArrayOf(0x53, 0x07, 0x02, 0x40, 0x01, 0xF4.toByte(), 0x77)
        val mockSynthesizerStateModel = SynthesizerModuleStateModel(
            radiationMode = RadiationMode.CW,
            cwFrequency = 13_320_000_000
        )
        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns mockSynthesizerStateModel
        coEvery { pllRegisters1208PL1URepository.getCwRegisters(any()) } returns listOf(
            0x700000, 0x10, 0x200A72, 0x4001F4, 0x6001F4, 0x890688, 0xA00005, 0xC00001, 0x00, 0x00, 0x00, 0x900000
        )
        every { bluetoothPacketManager.getWriteToSynthesizerPacket(any()) } returns mockSynthesizerPacket
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(mockRchmDissState)

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val synthesizerViewModel = SynthesizerViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager,
            pllRegisters1208PL1URepository,
            mainDispatcherRule.testDispatcher
        )

        synthesizerViewModel.synthesizerUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is SynthesizerUpdatingStatus.Idle)
            with(firstItem as SynthesizerUpdatingStatus.Idle) {
                assertEquals(firstItem.synthesizerModuleStateModel, mockSynthesizerStateModel)
            }

            synthesizerViewModel.updateSynthesizerCwMode(13_320)
            val secondItem = awaitItem()
            assertTrue(secondItem is SynthesizerUpdatingStatus.Updating)
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            advanceTimeBy(5001.milliseconds)
            val thirdItem = awaitItem()
            assertTrue(thirdItem is SynthesizerUpdatingStatus.Error)
            with(thirdItem as SynthesizerUpdatingStatus.Error) {
                assertEquals(thirdItem.errorCode, RESPONSE_PACKET_TIMEOUT_ERROR_CODE)
            }

            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getCwRegisters(any()) }
            coVerify(exactly = 7) { bluetoothRepository.bluetoothDataRepository.writeData(any()) }
            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getLfmParameters(any()) }
        }
    }

    @Test
    fun `настройка НГ нет ответа после 11 пакетов`() = runTest {
        val synthesizerModuleState = SynthesizerModuleState(
            refRegister = listOf(0x10, 0x00),
            intRegister = listOf(0x200A72, 0x00),
            fracRegister = listOf(0x4001F4, 0x00),
            modRegister = listOf(0x6001F4, 0x00),
            ctr1Register = listOf(0x890688, 0x00),
            ctr2Register = listOf(0xA00005, 0x00),
            ctr3Register = listOf(0xC00001, 0x00),
            lfm1Register = listOf(0x00, 0x00),
            lfm2Register = listOf(0x00, 0x00),
            lfm3Register = listOf(0x00, 0x00),
            prwRegister = 0x700000,
            praRegister = 0x900000
        )
        val mockRchmDissState = RchmDissState(
            synthesizerModuleState = synthesizerModuleState
        )
        val mockSynthesizerPacket = byteArrayOf(0x53, 0x07, 0x02, 0x40, 0x01, 0xF4.toByte(), 0x77)
        val mockSynthesizerStateModel = SynthesizerModuleStateModel(
            radiationMode = RadiationMode.CW,
            cwFrequency = 13_320_000_000
        )
        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns mockSynthesizerStateModel
        coEvery { pllRegisters1208PL1URepository.getCwRegisters(any()) } returns listOf(
            0x700000, 0x10, 0x200A72, 0x4001F4, 0x6001F4, 0x890688, 0xA00005, 0xC00001, 0x00, 0x00, 0x00, 0x900000
        )
        every { bluetoothPacketManager.getWriteToSynthesizerPacket(any()) } returns mockSynthesizerPacket
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(mockRchmDissState)

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val synthesizerViewModel = SynthesizerViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager,
            pllRegisters1208PL1URepository,
            mainDispatcherRule.testDispatcher
        )

        synthesizerViewModel.synthesizerUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is SynthesizerUpdatingStatus.Idle)
            with(firstItem as SynthesizerUpdatingStatus.Idle) {
                assertEquals(firstItem.synthesizerModuleStateModel, mockSynthesizerStateModel)
            }

            synthesizerViewModel.updateSynthesizerCwMode(13_320)
            val secondItem = awaitItem()
            assertTrue(secondItem is SynthesizerUpdatingStatus.Updating)
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            advanceTimeBy(1000.milliseconds)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            advanceTimeBy(5001.milliseconds)


            val thirdItem = awaitItem()
            assertTrue(thirdItem is SynthesizerUpdatingStatus.Error)
            with(thirdItem as SynthesizerUpdatingStatus.Error) {
                assertEquals(thirdItem.errorCode, RESPONSE_PACKET_TIMEOUT_ERROR_CODE)
            }

            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getCwRegisters(any()) }
            coVerify(exactly = 12) { bluetoothRepository.bluetoothDataRepository.writeData(any()) }
            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getLfmParameters(any()) }
        }
    }

    @Test
    fun `настройка НГ успешная отправка всех пакетов`() = runTest {
        val synthesizerModuleState = SynthesizerModuleState(
            refRegister = listOf(0x10, 0x00),
            intRegister = listOf(0x200A72, 0x00),
            fracRegister = listOf(0x4001F4, 0x00),
            modRegister = listOf(0x6001F4, 0x00),
            ctr1Register = listOf(0x890688, 0x00),
            ctr2Register = listOf(0xA00005, 0x00),
            ctr3Register = listOf(0xC00001, 0x00),
            lfm1Register = listOf(0x00, 0x00),
            lfm2Register = listOf(0x00, 0x00),
            lfm3Register = listOf(0x00, 0x00),
            prwRegister = 0x700000,
            praRegister = 0x900000
        )
        val mockRchmDissState = RchmDissState(
            synthesizerModuleState = synthesizerModuleState
        )
        val mockSynthesizerPacket = byteArrayOf(0x53, 0x07, 0x02, 0x40, 0x01, 0xF4.toByte(), 0x77)
        val mockSynthesizerStateModel = SynthesizerModuleStateModel(
            radiationMode = RadiationMode.CW,
            cwFrequency = 13_320_000_000
        )
        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns mockSynthesizerStateModel
        coEvery { pllRegisters1208PL1URepository.getCwRegisters(any()) } returns listOf(
            0x700000, 0x10, 0x200A72, 0x4001F4, 0x6001F4, 0x890688, 0xA00005, 0xC00001, 0x00, 0x00, 0x00, 0x900000
        )
        every { bluetoothPacketManager.getWriteToSynthesizerPacket(any()) } returns mockSynthesizerPacket
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(mockRchmDissState)

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val synthesizerViewModel = SynthesizerViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager,
            pllRegisters1208PL1URepository,
            mainDispatcherRule.testDispatcher
        )

        synthesizerViewModel.synthesizerUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is SynthesizerUpdatingStatus.Idle)
            with(firstItem as SynthesizerUpdatingStatus.Idle) {
                assertEquals(firstItem.synthesizerModuleStateModel, mockSynthesizerStateModel)
            }

            synthesizerViewModel.updateSynthesizerCwMode(13_320)
            val secondItem = awaitItem()
            assertTrue(secondItem is SynthesizerUpdatingStatus.Updating)
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            advanceTimeBy(1000.milliseconds)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)

            val thirdItem = awaitItem()
            assertTrue(thirdItem is SynthesizerUpdatingStatus.Success)
            advanceTimeBy(1000.milliseconds)

            val fourthItem = awaitItem()
            assertTrue(fourthItem is SynthesizerUpdatingStatus.Idle)
            with(fourthItem as SynthesizerUpdatingStatus.Idle) {
                assertEquals(fourthItem.synthesizerModuleStateModel, mockSynthesizerStateModel)
            }

            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getCwRegisters(any()) }
            coVerify(exactly = 12) { bluetoothRepository.bluetoothDataRepository.writeData(any()) }
            coVerify(exactly = 2) { pllRegisters1208PL1URepository.getLfmParameters(any()) }
        }
    }

    @Test
    fun `ЛЧМ нижняя частота ниже порога, верхняя выше порога, период ниже нижнего`() = runTest {
        val synthesizerModuleState = SynthesizerModuleState(
            refRegister = listOf(0x10, 0x00),
            intRegister = listOf(0x200A72, 0x00),
            fracRegister = listOf(0x4001F4, 0x00),
            modRegister = listOf(0x6001F4, 0x00),
            ctr1Register = listOf(0x890688, 0x00),
            ctr2Register = listOf(0xA00005, 0x00),
            ctr3Register = listOf(0xC00001, 0x00),
            lfm1Register = listOf(0x00, 0x00),
            lfm2Register = listOf(0x00, 0x00),
            lfm3Register = listOf(0x00, 0x00),
            prwRegister = 0x700000,
            praRegister = 0x900000
        )
        val mockRchmDissState = RchmDissState(
            synthesizerModuleState = synthesizerModuleState
        )
        val mockSynthesizerPacket = byteArrayOf(0x53, 0x07, 0x02, 0x40, 0x1F, 0x40, 0x77)
        val mockOutputSetPacket = byteArrayOf(0x53, 0x06, 0x08, 0x55, 0x1F, 0xA8.toByte())
        val mockSynthesizerStateModel = SynthesizerModuleStateModel(
            radiationMode = RadiationMode.LFM,
            lowestLfmFrequency = 13_280_000_000,
            highestLfmFrequency = 13_380_000_000,
            lfmPeriod = 0.001,
            isSymmetricLfm = true
        )
        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns mockSynthesizerStateModel
        coEvery { pllRegisters1208PL1URepository.getLfmRegisters(any()) } returns listOf(
            0x700000, 0x1, 0x2000A6, 0x400000, 0x607D00, 0x840608, 0xA00002, 0xC00001, 0x1000A0, 0x3FA018, 0x500204, 0x900000,
            0x704000, 0x1, 0x2000A7, 0x401F40, 0x607D00, 0x840608, 0xA00002, 0xC00001, 0x1000A0, 0x3FA018, 0x500006, 0x900000
        )
        every { bluetoothPacketManager.getWriteToSynthesizerPacket(any()) } returns mockSynthesizerPacket
        every { bluetoothPacketManager.getRchmDissOutputSetPacket(any()) } returns mockOutputSetPacket
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(mockRchmDissState)

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val synthesizerViewModel = SynthesizerViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager,
            pllRegisters1208PL1URepository,
            mainDispatcherRule.testDispatcher
        )

        synthesizerViewModel.synthesizerUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is SynthesizerUpdatingStatus.Idle)
            with(firstItem as SynthesizerUpdatingStatus.Idle) {
                assertEquals(firstItem.synthesizerModuleStateModel, mockSynthesizerStateModel)
            }

            synthesizerViewModel.updateSynthesizerLfmMode(
                startFrequency = 13_240,
                stopFrequency = 13_410,
                lfmPeriod = 0.1,
                isSymmetricLfm = true,
                isExtTriggerLfm = false
            )
            val secondItem = awaitItem()
            assertTrue(secondItem is SynthesizerUpdatingStatus.Updating)
            advanceTimeBy(100.milliseconds)
            val thirdItem = awaitItem()
            assertTrue(thirdItem is SynthesizerUpdatingStatus.Error)
            with(thirdItem as SynthesizerUpdatingStatus.Error) {
                assertEquals(
                    thirdItem.errorCode,
                    LOW_FREQUENCY_UNDER_INPUT_ERROR or HIGH_FREQUENCY_ABOVE_INPUT_ERROR or MODULATION_PERIOD_UNDER_INPUT_ERROR
                )
            }

            coVerify(exactly = 0) { pllRegisters1208PL1URepository.getLfmRegisters(any()) }
            coVerify(exactly = 0) { bluetoothRepository.bluetoothDataRepository.writeData(any()) }
            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getLfmParameters(any()) }
        }
    }

    @Test
    fun `ЛЧМ нижняя частота выше порога, верхняя ниже порога, период выше верхнего`() = runTest {
        val synthesizerModuleState = SynthesizerModuleState(
            refRegister = listOf(0x10, 0x00),
            intRegister = listOf(0x200A72, 0x00),
            fracRegister = listOf(0x4001F4, 0x00),
            modRegister = listOf(0x6001F4, 0x00),
            ctr1Register = listOf(0x890688, 0x00),
            ctr2Register = listOf(0xA00005, 0x00),
            ctr3Register = listOf(0xC00001, 0x00),
            lfm1Register = listOf(0x00, 0x00),
            lfm2Register = listOf(0x00, 0x00),
            lfm3Register = listOf(0x00, 0x00),
            prwRegister = 0x700000,
            praRegister = 0x900000
        )
        val mockRchmDissState = RchmDissState(
            synthesizerModuleState = synthesizerModuleState
        )
        val mockSynthesizerPacket = byteArrayOf(0x53, 0x07, 0x02, 0x40, 0x1F, 0x40, 0x77)
        val mockOutputSetPacket = byteArrayOf(0x53, 0x06, 0x08, 0x55, 0x1F, 0xA8.toByte())
        val mockSynthesizerStateModel = SynthesizerModuleStateModel(
            radiationMode = RadiationMode.LFM,
            lowestLfmFrequency = 13_280_000_000,
            highestLfmFrequency = 13_380_000_000,
            lfmPeriod = 0.001,
            isSymmetricLfm = true
        )
        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns mockSynthesizerStateModel
        coEvery { pllRegisters1208PL1URepository.getLfmRegisters(any()) } returns listOf(
            0x700000, 0x1, 0x2000A6, 0x400000, 0x607D00, 0x840608, 0xA00002, 0xC00001, 0x1000A0, 0x3FA018, 0x500204, 0x900000,
            0x704000, 0x1, 0x2000A7, 0x401F40, 0x607D00, 0x840608, 0xA00002, 0xC00001, 0x1000A0, 0x3FA018, 0x500006, 0x900000
        )
        every { bluetoothPacketManager.getWriteToSynthesizerPacket(any()) } returns mockSynthesizerPacket
        every { bluetoothPacketManager.getRchmDissOutputSetPacket(any()) } returns mockOutputSetPacket
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(mockRchmDissState)

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val synthesizerViewModel = SynthesizerViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager,
            pllRegisters1208PL1URepository,
            mainDispatcherRule.testDispatcher
        )

        synthesizerViewModel.synthesizerUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is SynthesizerUpdatingStatus.Idle)
            with(firstItem as SynthesizerUpdatingStatus.Idle) {
                assertEquals(firstItem.synthesizerModuleStateModel, mockSynthesizerStateModel)
            }

            synthesizerViewModel.updateSynthesizerLfmMode(
                startFrequency = 13_410,
                stopFrequency = 13_240,
                lfmPeriod = 200.0,
                isSymmetricLfm = true,
                isExtTriggerLfm = true
            )
            val secondItem = awaitItem()
            assertTrue(secondItem is SynthesizerUpdatingStatus.Updating)
            advanceTimeBy(100.milliseconds)
            val thirdItem = awaitItem()
            assertTrue(thirdItem is SynthesizerUpdatingStatus.Error)
            with(thirdItem as SynthesizerUpdatingStatus.Error) {
                assertEquals(
                    thirdItem.errorCode,
                    LOW_FREQUENCY_ABOVE_INPUT_ERROR or
                            HIGH_FREQUENCY_UNDER_INPUT_ERROR or
                            MODULATION_PERIOD_ABOVE_INPUT_ERROR or
                            LOW_FREQ_HIGHER_THAN_HIGH_FREQ_INPUT_ERROR
                )
            }

            coVerify(exactly = 0) { pllRegisters1208PL1URepository.getLfmRegisters(any()) }
            coVerify(exactly = 0) { bluetoothRepository.bluetoothDataRepository.writeData(any()) }
            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getLfmParameters(any()) }
            coVerify(exactly = 0) { bluetoothPacketManager.getRchmDissOutputSetPacket(any()) }
        }
    }

    @Test
    fun `ввод верных данных, нет ответа на первый пакет`() = runTest {
        val synthesizerModuleState = SynthesizerModuleState(
            refRegister = listOf(0x1, 0x1),
            intRegister = listOf(0x2000A6, 0x2000A7),
            fracRegister = listOf(0x400000, 0x401F40),
            modRegister = listOf(0x607D00, 0x607D00),
            ctr1Register = listOf(0x840609, 0x840609),
            ctr2Register = listOf(0xA00002, 0xA00002),
            ctr3Register = listOf(0xC00001, 0xC00001),
            lfm1Register = listOf(0x1000A0, 0x1000A0),
            lfm2Register = listOf(0x3FA018, 0x3FA018),
            lfm3Register = listOf(0x500204, 0x500006),
            prwRegister = 0x700000,
            praRegister = 0x900000
        )
        val mockRchmDissState = RchmDissState(
            synthesizerModuleState = synthesizerModuleState
        )
        val mockSynthesizerPacket = byteArrayOf(0x53, 0x07, 0x02, 0x40, 0x1F, 0x40, 0x77)
        val mockOutputSetPacket = byteArrayOf(0x53, 0x06, 0x08, 0x55, 0x1F, 0xA8.toByte())
        val mockSynthesizerStateModel = SynthesizerModuleStateModel(
            radiationMode = RadiationMode.LFM,
            lowestLfmFrequency = 13_280_000_000,
            highestLfmFrequency = 13_380_000_000,
            lfmPeriod = 0.01,
            isSymmetricLfm = true
        )
        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns mockSynthesizerStateModel
        coEvery { pllRegisters1208PL1URepository.getLfmRegisters(any()) } returns listOf(
            0x700000, 0x1, 0x2000A6, 0x400000, 0x607D00, 0x840608, 0xA00002, 0xC00001, 0x1000A0, 0x3FA018, 0x500204, 0x900000,
            0x704000, 0x1, 0x2000A7, 0x401F40, 0x607D00, 0x840608, 0xA00002, 0xC00001, 0x1000A0, 0x3FA018, 0x500006, 0x900000
        )
        every { bluetoothPacketManager.getWriteToSynthesizerPacket(any()) } returns mockSynthesizerPacket
        every { bluetoothPacketManager.getRchmDissOutputSetPacket(any()) } returns mockOutputSetPacket
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(mockRchmDissState)

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val synthesizerViewModel = SynthesizerViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager,
            pllRegisters1208PL1URepository,
            mainDispatcherRule.testDispatcher
        )

        synthesizerViewModel.synthesizerUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is SynthesizerUpdatingStatus.Idle)
            with(firstItem as SynthesizerUpdatingStatus.Idle) {
                assertEquals(firstItem.synthesizerModuleStateModel, mockSynthesizerStateModel)
            }

            synthesizerViewModel.updateSynthesizerLfmMode(
                startFrequency = 13_280,
                stopFrequency = 13_380,
                lfmPeriod = 10.0,
                isSymmetricLfm = true,
                isExtTriggerLfm = true
            )
            val secondItem = awaitItem()
            assertTrue(secondItem is SynthesizerUpdatingStatus.Updating)
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()
            advanceTimeBy(4001.milliseconds)
            val thirdItem = awaitItem()
            assertTrue(thirdItem is SynthesizerUpdatingStatus.Error)
            with(thirdItem as SynthesizerUpdatingStatus.Error) {
                assertEquals(thirdItem.errorCode,RESPONSE_PACKET_TIMEOUT_ERROR_CODE)
            }

            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getLfmRegisters(any()) }
            coVerify(exactly = 1) { bluetoothRepository.bluetoothDataRepository.writeData(any()) }
            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getLfmParameters(any()) }
            coVerify(exactly = 0) { bluetoothPacketManager.getRchmDissOutputSetPacket(any()) }
        }
    }

    @Test
    fun `ввод верных данных, нет ответа после отправки половины пакетов`() = runTest {
        val synthesizerModuleState = SynthesizerModuleState(
            refRegister = listOf(0x1, 0x1),
            intRegister = listOf(0x2000A6, 0x2000A7),
            fracRegister = listOf(0x400000, 0x401F40),
            modRegister = listOf(0x607D00, 0x607D00),
            ctr1Register = listOf(0x840609, 0x840609),
            ctr2Register = listOf(0xA00002, 0xA00002),
            ctr3Register = listOf(0xC00001, 0xC00001),
            lfm1Register = listOf(0x1000A0, 0x1000A0),
            lfm2Register = listOf(0x3FA018, 0x3FA018),
            lfm3Register = listOf(0x500204, 0x500006),
            prwRegister = 0x700000,
            praRegister = 0x900000
        )
        val mockRchmDissState = RchmDissState(
            synthesizerModuleState = synthesizerModuleState
        )
        val mockSynthesizerPacket = byteArrayOf(0x53, 0x07, 0x02, 0x40, 0x1F, 0x40, 0x77)
        val mockOutputSetPacket = byteArrayOf(0x53, 0x06, 0x08, 0x55, 0x1F, 0xA8.toByte())
        val mockSynthesizerStateModel = SynthesizerModuleStateModel(
            radiationMode = RadiationMode.LFM,
            lowestLfmFrequency = 13_280_000_000,
            highestLfmFrequency = 13_380_000_000,
            lfmPeriod = 0.01,
            isSymmetricLfm = true
        )
        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns mockSynthesizerStateModel
        coEvery { pllRegisters1208PL1URepository.getLfmRegisters(any()) } returns listOf(
            0x700000, 0x1, 0x2000A6, 0x400000, 0x607D00, 0x840608, 0xA00002, 0xC00001, 0x1000A0, 0x3FA018, 0x500204, 0x900000,
            0x704000, 0x1, 0x2000A7, 0x401F40, 0x607D00, 0x840608, 0xA00002, 0xC00001, 0x1000A0, 0x3FA018, 0x500006, 0x900000
        )
        every { bluetoothPacketManager.getWriteToSynthesizerPacket(any()) } returns mockSynthesizerPacket
        every { bluetoothPacketManager.getRchmDissOutputSetPacket(any()) } returns mockOutputSetPacket
        every { rchmDissStateRepository.rchmDissState } returns MutableStateFlow(mockRchmDissState)

        val lastPacketFlow = MutableSharedFlow<RcdInputPacketType>()
        every { rchmDissStateRepository.lastPacket } returns lastPacketFlow

        val synthesizerViewModel = SynthesizerViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            bluetoothPacketManager,
            pllRegisters1208PL1URepository,
            mainDispatcherRule.testDispatcher
        )

        synthesizerViewModel.synthesizerUpdatingStatusFlow.test {
            val firstItem = awaitItem()
            assertTrue(firstItem is SynthesizerUpdatingStatus.Idle)
            with(firstItem as SynthesizerUpdatingStatus.Idle) {
                assertEquals(firstItem.synthesizerModuleStateModel, mockSynthesizerStateModel)
            }

            synthesizerViewModel.updateSynthesizerLfmMode(
                startFrequency = 13_280,
                stopFrequency = 13_380,
                lfmPeriod = 10.0,
                isSymmetricLfm = true,
                isExtTriggerLfm = true
            )
            val secondItem = awaitItem()
            assertTrue(secondItem is SynthesizerUpdatingStatus.Updating)
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(500.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(500.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(500.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(500.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.RcdOutputControlInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(500.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(500.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(1000.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(500.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()
            advanceTimeBy(500.milliseconds)
            lastPacketFlow.emit(RcdInputPacketType.SynthesizerStateInputPacket)
            expectNoEvents()

            advanceTimeBy(5001.milliseconds)
            val thirdItem = awaitItem()
            assertTrue(thirdItem is SynthesizerUpdatingStatus.Error)
            with(thirdItem as SynthesizerUpdatingStatus.Error) {
                assertEquals(thirdItem.errorCode,RESPONSE_PACKET_TIMEOUT_ERROR_CODE)
            }

            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getLfmRegisters(any()) }
            coVerify(exactly = 13) { bluetoothRepository.bluetoothDataRepository.writeData(any()) }
            coVerify(exactly = 1) { pllRegisters1208PL1URepository.getLfmParameters(any()) }
            coVerify(exactly = 0) { bluetoothPacketManager.getRchmDissOutputSetPacket(any()) }
        }
    }

}