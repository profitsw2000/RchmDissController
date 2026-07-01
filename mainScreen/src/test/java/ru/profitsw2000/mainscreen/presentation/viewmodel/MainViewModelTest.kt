package ru.profitsw2000.mainscreen.presentation.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.profitsw2000.core.drawable.utils.ATTENUATOR_BIT_MASK
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.pll.PLLRegisters1208PL1URepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
import ru.profitsw2000.data.model.bluetooth.state.rcd.OutputModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.RadiationMode
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissState
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissStateModel
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val bluetoothRepository: BluetoothRepository = mockk()
    private val rchmDissStateRepository: RchmDissStateRepository = mockk(relaxed = true)
    private val pllRegisters1208PL1URepository: PLLRegisters1208PL1URepository = mockk(relaxed = true)

    @Test
    fun `тест правильного состояния rcmDissStateModelFlow при состоянии 1 rchmDissState`() = runTest {
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
        val rchmDissState = RchmDissState(
            receiverModuleState = ReceiverModuleState(
                enabledChannelNumber = 2,
                testSignalIsEnabled = true,
                lockedInputChannels = booleanArrayOf(true, true, false, true, false),
                inputAttenuationValue = 62,
                inputAttenuatorsCode = ATTENUATOR_BIT_MASK
            ),
            transmitterModuleState = TransmitterModuleState(
                enabledChannelNumber = 4
            ),
            synthesizerModuleState = synthesizerModuleState,
            outputModuleState = OutputModuleState(
                lfmExtTriggerIsOn = false,
                transmitterIsOn = true,
                pllIsLocked = false,
                transmitterDetectorVoltage = 1.68,
                secondaryPowerSourceVoltage = 6.77
            ),
            innerModuleTemperature = 28.7,
            readMemoryValue = 0xA4.toByte()
        )
        val rchmDissStateFlow = MutableStateFlow(rchmDissState)
        val synthesizerParameters = SynthesizerModuleStateModel(
            radiationMode = RadiationMode.CW,
            cwFrequency = 13_370_000_000
        )

        every { rchmDissStateRepository.rchmDissState } returns rchmDissStateFlow
        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns synthesizerParameters

        val mainViewModel = MainViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            pllRegisters1208PL1URepository,
            defaultDispatcher = mainDispatcherRule.testDispatcher
        )

        mainViewModel.rchmDissStateModelFlow.test {

            val emittedItem = awaitItem()
            assertEquals(
                emittedItem.transmitterModuleState,
                TransmitterModuleState(
                    enabledChannelNumber = 4
                )
            )
            assertEquals(
                emittedItem.receiverModuleState.toString(),
                ReceiverModuleState(
                    enabledChannelNumber = 2,
                    testSignalIsEnabled = true,
                    lockedInputChannels = booleanArrayOf(true, true, false, true, false),
                    inputAttenuationValue = 62,
                    inputAttenuatorsCode = ATTENUATOR_BIT_MASK
                ).toString()
            )
            assertEquals(
                synthesizerParameters,
                emittedItem.synthesizerModuleState
            )
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `тест правильного состояния rcmDissStateModelFlow при состоянии 2 rchmDissState`() = runTest {
        val synthesizerModuleState = SynthesizerModuleState(
            refRegister = listOf(0x1, 0x00),
            intRegister = listOf(0x2000A5, 0x00),
            fracRegister = listOf(0x404E20, 0x00),
            modRegister = listOf(0x607D00, 0x00),
            ctr1Register = listOf(0x840608, 0x00),
            ctr2Register = listOf(0xA00002, 0x00),
            ctr3Register = listOf(0xC00001, 0x00),
            lfm1Register = listOf(0x1000F0, 0x00),
            lfm2Register = listOf(0x3FA031, 0x00),
            lfm3Register = listOf(0x500004, 0x00),
            prwRegister = 0x700000,
            praRegister = 0x900000,
        )
        val rchmDissState = RchmDissState(
            receiverModuleState = ReceiverModuleState(
                enabledChannelNumber = 5,
                testSignalIsEnabled = false,
                lockedInputChannels = booleanArrayOf(false, true, false, true, false),
                inputAttenuationValue = 18,
                inputAttenuatorsCode = 0x140
            ),
            transmitterModuleState = TransmitterModuleState(
                enabledChannelNumber = 0
            ),
            synthesizerModuleState = synthesizerModuleState,
            outputModuleState = OutputModuleState(
                lfmExtTriggerIsOn = false,
                transmitterIsOn = true,
                pllIsLocked = false,
                transmitterDetectorVoltage = 1.68,
                secondaryPowerSourceVoltage = 6.77
            ),
            innerModuleTemperature = 28.7,
            readMemoryValue = 0xA4.toByte()
        )
        val rchmDissStateFlow = MutableStateFlow(rchmDissState)
        val synthesizerParameters = SynthesizerModuleStateModel(
            radiationMode = RadiationMode.LFM,
            lowestLfmFrequency = 13_250_000_000,
            highestLfmFrequency = 13_400_000_000,
            lfmPeriod = 0.01,
            isSymmetricLfm = false
        )

        every { rchmDissStateRepository.rchmDissState } returns rchmDissStateFlow
        coEvery { pllRegisters1208PL1URepository.getLfmParameters(any()) } returns synthesizerParameters

        val mainViewModel = MainViewModel(
            rchmDissStateRepository,
            bluetoothRepository,
            pllRegisters1208PL1URepository,
            defaultDispatcher = mainDispatcherRule.testDispatcher
        )

        mainViewModel.rchmDissStateModelFlow.test {

            val emittedItem = awaitItem()
            assertEquals(
                emittedItem.transmitterModuleState,
                TransmitterModuleState(
                    enabledChannelNumber = 0
                )
            )
            assertEquals(
                emittedItem.receiverModuleState.toString(),
                ReceiverModuleState(
                    enabledChannelNumber = 5,
                    testSignalIsEnabled = false,
                    lockedInputChannels = booleanArrayOf(false, true, false, true, false),
                    inputAttenuationValue = 18,
                    inputAttenuatorsCode = 0x140
                ).toString()
            )
            assertEquals(
                synthesizerParameters,
                emittedItem.synthesizerModuleState
            )
            assertEquals(
                28.7,
                emittedItem.innerModuleTemperature,
                0.1
            )
            assertEquals(
                0xA4.toByte(),
                emittedItem.readMemoryValue
            )
            ensureAllEventsConsumed()
        }
    }
}