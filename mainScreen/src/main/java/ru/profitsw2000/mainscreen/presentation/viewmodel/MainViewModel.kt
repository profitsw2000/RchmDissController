package ru.profitsw2000.mainscreen.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.profitsw2000.core.drawable.utils.MODULE_OUTPUT_CONTROL_PACKET_TIMEOUT
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.pll.PLLRegisters1208PL1URepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissStateModel
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel
import ru.profitsw2000.data.model.bluetooth.status.BluetoothConnectionStatus
import ru.profitsw2000.data.model.rcd.RcdInputPacketType

class MainViewModel(
    private val rchmDissStateRepository: RchmDissStateRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val pllRegisters1208PL1URepository: PLLRegisters1208PL1URepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
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
        viewModelScope.launch(defaultDispatcher) {
            val outputControlPacket = rchmDissStateRepository.lastPacket
                .filterIsInstance<RcdInputPacketType.RcdOutputControlInputPacket>()
                .onEach {
                    _isReceivedOutputControlPacket.value = true
                }

            outputControlPacket
                .flowOn(defaultDispatcher)
                .debounce(MODULE_OUTPUT_CONTROL_PACKET_TIMEOUT)
                .collect {
                    _isReceivedOutputControlPacket.value = false
                }
        }
    }

    private suspend fun getSynthesizerParametersModel(synthesizerModuleState: SynthesizerModuleState): SynthesizerModuleStateModel {
        return withContext(defaultDispatcher) {
            pllRegisters1208PL1URepository.getLfmParameters(
                synthesizerModuleState
            )
        }
    }
}
