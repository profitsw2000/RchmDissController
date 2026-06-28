package ru.profitsw2000.mainscreen.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import ru.profitsw2000.core.drawable.utils.CW_FREQUENCY_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.CW_FREQUENCY_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.HIGH_FREQUENCY_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.HIGH_FREQUENCY_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.LOW_FREQUENCY_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.LOW_FREQUENCY_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.LOW_FREQ_HIGHER_THAN_HIGH_FREQ_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.MAX_LFM_FREQ
import ru.profitsw2000.core.drawable.utils.MAX_LFM_PERIOD_SEC
import ru.profitsw2000.core.drawable.utils.MIN_LFM_FREQ
import ru.profitsw2000.core.drawable.utils.MIN_LFM_PERIOD_SEC
import ru.profitsw2000.core.drawable.utils.MODULATION_PERIOD_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.MODULATION_PERIOD_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.MODULE_OUTPUT_CONTROL_PACKET_TIMEOUT
import ru.profitsw2000.core.drawable.utils.NO_ERROR
import ru.profitsw2000.core.drawable.utils.REGISTERS_CALCULATION_ERROR_CODE
import ru.profitsw2000.core.drawable.utils.RESPONSE_PACKET_TIMEOUT_ERROR_CODE
import ru.profitsw2000.core.drawable.utils.UNKNOWN_ERROR_CODE
import ru.profitsw2000.core.drawable.utils.toRegisterByteArray
import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.pll.PLLRegisters1208PL1URepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissStateModel
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.data.model.bluetooth.status.BluetoothConnectionStatus
import ru.profitsw2000.data.model.pll.LfmInputParametersModel
import ru.profitsw2000.data.model.rcd.RcdInputPacketType
import ru.profitsw2000.mainscreen.state.ReceiverUpdatingStatus
import ru.profitsw2000.mainscreen.state.SynthesizerUpdatingStatus
import ru.profitsw2000.mainscreen.state.TransmitterUpdatingStatus
import kotlin.time.Duration.Companion.milliseconds

class MainViewModel(
    private val rchmDissStateRepository: RchmDissStateRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val pllRegisters1208PL1URepository: PLLRegisters1208PL1URepository
): ViewModel() {

    val rchmDissStateModelFlow: StateFlow<RchmDissStateModel> =
        rchmDissStateRepository.rchmDissState
            .mapLatest { state ->
                RchmDissStateModel(
                    receiverModuleState = state.receiverModuleState,
                    transmitterModuleState = state.transmitterModuleState,
                    synthesizerModuleState = getSynthesizerParametersModel(state.synthesizerModuleState),
                    innerModuleTemperature = state.innerModuleTemperature,
                    readMemoryValue = state.readMemoryValue
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = RchmDissStateModel()
            )

    private val _isReceivedOutputControlPacket: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isReceivedOutputControlPacket: StateFlow<Boolean> = _isReceivedOutputControlPacket.asStateFlow()
    val clickActionSharedFlow: MutableSharedFlow<Int> = MutableSharedFlow<Int>(replay = 0)

    init {
        monitorOutputControlPackets()
    }

    fun layoutClicked(action: Int) {
        if (bluetoothRepository.bluetoothConnectionRepository.bluetoothConnectionStatusFlow.value is BluetoothConnectionStatus.Connected)
            viewModelScope.launch {
                clickActionSharedFlow.emit(action)
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorOutputControlPackets() {
        viewModelScope.launch {
            val outputControlPacket = rchmDissStateRepository.lastPacket
                .filterIsInstance<RcdInputPacketType.RcdOutputControlInputPacket>()
                .onEach {
                    _isReceivedOutputControlPacket.value = true
                }

            outputControlPacket
                .debounce(MODULE_OUTPUT_CONTROL_PACKET_TIMEOUT)
                .collect {
                    _isReceivedOutputControlPacket.value = false
                }
        }
    }

    private suspend fun getSynthesizerParametersModel(synthesizerModuleState: SynthesizerModuleState): SynthesizerModuleStateModel {
        return withContext(Dispatchers.Default) {
            pllRegisters1208PL1URepository.getLfmParameters(
                synthesizerModuleState
            )
        }
    }
}
