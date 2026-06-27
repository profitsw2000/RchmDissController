package ru.profitsw2000.mainscreen.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
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
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.data.model.pll.LfmInputParametersModel
import ru.profitsw2000.data.model.rcd.RcdInputPacketType
import ru.profitsw2000.mainscreen.state.ReceiverUpdatingStatus
import ru.profitsw2000.mainscreen.state.SynthesizerUpdatingStatus
import ru.profitsw2000.mainscreen.state.TransmitterUpdatingStatus
import kotlin.time.Duration.Companion.milliseconds

class MainViewModel(
    private val rchmDissStateRepository: RchmDissStateRepository,
    private val pllRegisters1208PL1URepository: PLLRegisters1208PL1URepository
): ViewModel() {
    private val _rchmDissStateModelFlow: MutableStateFlow<RchmDissStateModel> = MutableStateFlow(
        RchmDissStateModel()
    )
    val rchmDissStateModelFlow: StateFlow<RchmDissStateModel> = _rchmDissStateModelFlow

    init {
        loadRchmDissInitialParameters()
    }

    private fun loadRchmDissInitialParameters() {
        viewModelScope.launch {
            try {
                val lfmParams = withContext(Dispatchers.IO) {
                    pllRegisters1208PL1URepository.getLfmParameters(
                        rchmDissStateRepository.rchmDissState.value.synthesizerModuleState
                    )
                }
                _rchmDissStateModelFlow.value = RchmDissStateModel(
                    synthesizerModuleState = lfmParams
                )
            } catch (exc: Exception) {
                _rchmDissStateModelFlow.value = RchmDissStateModel()
            }
        }
    }
}
