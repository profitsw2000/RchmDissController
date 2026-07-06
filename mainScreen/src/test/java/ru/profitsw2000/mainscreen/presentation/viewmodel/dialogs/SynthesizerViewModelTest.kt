package ru.profitsw2000.mainscreen.presentation.viewmodel.dialogs

import app.cash.turbine.test
import io.mockk.coEvery
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
import ru.profitsw2000.core.drawable.utils.CW_FREQUENCY_UNDER_INPUT_ERROR
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
        }
    }

}