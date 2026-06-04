package ru.profitsw2000.mainscreen.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import ru.profitsw2000.core.drawable.utils.HIGH_FREQUENCY_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.HIGH_FREQUENCY_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.LOW_FREQUENCY_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.LOW_FREQUENCY_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.LOW_FREQ_HIGHER_THAN_HIGH_FREQ_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.MAX_LFM_FREQ
import ru.profitsw2000.core.drawable.utils.MAX_LFM_PERIOD_MS
import ru.profitsw2000.core.drawable.utils.MIN_LFM_FREQ
import ru.profitsw2000.core.drawable.utils.MIN_LFM_PERIOD_MS
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
import kotlin.experimental.and
import kotlin.experimental.or

class MainViewModel(
    private val rchmDissStateRepository: RchmDissStateRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val bluetoothPacketManager: BluetoothPacketManager,
    private val pllRegisters1208PL1URepository: PLLRegisters1208PL1URepository
): ViewModel() {
    private val _transmitterUpdatingStatusFlow = MutableStateFlow<TransmitterUpdatingStatus>(
        TransmitterUpdatingStatus.Idle(
            rchmDissStateRepository.rchmDissState.value.transmitterModuleState,
            rchmDissStateRepository.rchmDissState.value.outputModuleState
        )
    )
    val transmitterUpdatingStatusFlow: StateFlow<TransmitterUpdatingStatus> = _transmitterUpdatingStatusFlow
    private val _receiverUpdatingStatusFlow = MutableStateFlow<ReceiverUpdatingStatus>(
        ReceiverUpdatingStatus.Idle
    )
    val receiverUpdatingStatusFlow: StateFlow<ReceiverUpdatingStatus> = _receiverUpdatingStatusFlow
    private val _synthesizerUpdatingStatusFlow = MutableStateFlow<SynthesizerUpdatingStatus>(
        SynthesizerUpdatingStatus.Idle
    )
    val synthesizerUpdatingStatusFlow: StateFlow<SynthesizerUpdatingStatus> = _synthesizerUpdatingStatusFlow

    val rchmDissState: StateFlow<RchmDissStateModel> = combine(
        transmitterUpdatingStatusFlow,
        receiverUpdatingStatusFlow,
        synthesizerUpdatingStatusFlow
    ) { transmitter, receiver, synthesizer ->

        getRchmDissStateModelFromSubModulesState(
            transmitter, receiver, synthesizer
        )
    }
        .scan(RchmDissStateModel()){ oldState, updatedState ->
            RchmDissStateModel(
                receiverModuleState = if (updatedState.isActualReceiverData) updatedState.receiverModuleState
                else oldState.receiverModuleState,
                isActualReceiverData = updatedState.isActualReceiverData,
                transmitterModuleState = if (updatedState.isActualTransmitterData) updatedState.transmitterModuleState
                else oldState.transmitterModuleState,
                isActualTransmitterData = updatedState.isActualTransmitterData,
                synthesizerModuleState = if (updatedState.isActualSynthesizerData) updatedState.synthesizerModuleState
                else oldState.synthesizerModuleState,
                isActualSynthesizerData = updatedState.isActualSynthesizerData

            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RchmDissStateModel()
        )

    init {
        _transmitterUpdatingStatusFlow.onEach { state ->
            if (state is TransmitterUpdatingStatus.Success) {
                delay(3000)
                _transmitterUpdatingStatusFlow.value =
                    TransmitterUpdatingStatus.Idle(
                        rchmDissStateRepository.rchmDissState.value.transmitterModuleState,
                        rchmDissStateRepository.rchmDissState.value.outputModuleState
                    )
            }
        }.launchIn(viewModelScope)
    }

    fun updateTransmitter(channelByte: Byte, turnTransmitterOn: Boolean) {
        viewModelScope.launch {
            _transmitterUpdatingStatusFlow.value = TransmitterUpdatingStatus.Updating

            try {
                bluetoothRepository.bluetoothDataRepository.writeData(
                    bluetoothPacketManager.getWriteToTransmitterPacket(channelByte)
                )
                withTimeout(5000L) {
                    rchmDissStateRepository.lastPacket.first {
                        it == RcdInputPacketType.TransmitterStateInputPacket
                    }
                    //Здесь отправляем пакет для установки сигнала Упр_ПРД
                    launch {
                        bluetoothRepository.bluetoothDataRepository.writeData(
                            bluetoothPacketManager.getRchmDissOutputSetPacket(
                                getOutputModuleStateByteArray(turnTransmitterOn)
                            )
                        )
                    }
                }

                _transmitterUpdatingStatusFlow.value =
                    TransmitterUpdatingStatus.Success(rchmDissStateRepository.rchmDissState.value.transmitterModuleState)
            } catch (exc: TimeoutCancellationException) {
                _transmitterUpdatingStatusFlow.value = TransmitterUpdatingStatus.Error(RESPONSE_PACKET_TIMEOUT_ERROR_CODE)
            } catch (exc: Exception) {
                _transmitterUpdatingStatusFlow.value = TransmitterUpdatingStatus.Error(UNKNOWN_ERROR_CODE)
            }
        }
    }

    fun updateReceiver(byteArray: ByteArray) {
        viewModelScope.launch {
            _receiverUpdatingStatusFlow.value = ReceiverUpdatingStatus.Updating

            try {
                bluetoothRepository.bluetoothDataRepository.writeData(
                    bluetoothPacketManager.getWriteToReceiverPacket(byteArray)
                )
                withTimeout(5000L) {
                    rchmDissStateRepository.lastPacket.first {
                        it == RcdInputPacketType.ReceiverStateInputPacket
                    }
                }

                _receiverUpdatingStatusFlow.value =
                    ReceiverUpdatingStatus.Success(rchmDissStateRepository.rchmDissState.value.receiverModuleState)
            } catch (exc: TimeoutCancellationException) {
                _receiverUpdatingStatusFlow.value = ReceiverUpdatingStatus.Error(RESPONSE_PACKET_TIMEOUT_ERROR_CODE)
            } catch (exc: Exception) {
                _receiverUpdatingStatusFlow.value = ReceiverUpdatingStatus.Error(UNKNOWN_ERROR_CODE)
            }
        }
    }

    fun updateSynthesizerCwMode(frequency: Long) {
        val errorCode = checkCwInputValues(frequency * 1_000_000)
        if (errorCode == NO_ERROR) {
            _synthesizerUpdatingStatusFlow.value = SynthesizerUpdatingStatus.Updating
            viewModelScope.launch {
                try {
                    val registersList = pllRegisters1208PL1URepository.getCwRegisters(frequency * 1_000_000)
                    updateSynthesizer(registersList)
                } catch (e: Exception) {
                    _synthesizerUpdatingStatusFlow.value = SynthesizerUpdatingStatus.Error(REGISTERS_CALCULATION_ERROR_CODE)
                }
            }
        }
        else _synthesizerUpdatingStatusFlow.value = SynthesizerUpdatingStatus.Error(errorCode)
    }

    fun updateSynthesizerLfmMode(
        startFrequency: Long,
        stopFrequency: Long,
        lfmPeriod: Double,
        isSymmetricLfm: Boolean
    ) {
        val errorCode = checkLfmInputValues(
            getLfmParametersModel(
                startFrequency = startFrequency,
                stopFrequency = stopFrequency,
                lfmPeriod = lfmPeriod,
                isSymmetricLfm = isSymmetricLfm
            )
        )
        if (errorCode == NO_ERROR) {
            _synthesizerUpdatingStatusFlow.value = SynthesizerUpdatingStatus.Updating
            viewModelScope.launch {
                try {
                    val registersList = pllRegisters1208PL1URepository.getLfmRegisters(
                        getLfmParametersModel(
                            startFrequency = startFrequency * 1_000_000,
                            stopFrequency = stopFrequency * 1_000_000,
                            lfmPeriod = lfmPeriod * 0.001,
                            isSymmetricLfm = isSymmetricLfm
                        )
                    )
                    updateSynthesizer(registersList)
                } catch (e: Exception) {
                    _synthesizerUpdatingStatusFlow.value = SynthesizerUpdatingStatus.Error(REGISTERS_CALCULATION_ERROR_CODE)
                }
            }
        }
        else _synthesizerUpdatingStatusFlow.value = SynthesizerUpdatingStatus.Error(errorCode)
    }

    private fun updateSynthesizer(synthesizerRegisters: List<Int>) {
        viewModelScope.launch {
            try {
                for (register in synthesizerRegisters) {
                    bluetoothRepository.bluetoothDataRepository.writeData(
                        bluetoothPacketManager.getWriteToSynthesizerPacket(
                            register.toRegisterByteArray()
                        )
                    )
                    withTimeout(5000L) {
                        rchmDissStateRepository.lastPacket.first {
                            it == RcdInputPacketType.SynthesizerStateInputPacket
                        }
                    }
                }

                _synthesizerUpdatingStatusFlow.value =
                    SynthesizerUpdatingStatus.Success(
                        pllRegisters1208PL1URepository.getLfmParameters(
                            rchmDissStateRepository.rchmDissState.value.synthesizerModuleState
                        )
                    )
            } catch (exc: TimeoutCancellationException) {
                _synthesizerUpdatingStatusFlow.value = SynthesizerUpdatingStatus.Error(RESPONSE_PACKET_TIMEOUT_ERROR_CODE)
            } catch (exc: Exception) {
                _synthesizerUpdatingStatusFlow.value = SynthesizerUpdatingStatus.Error(UNKNOWN_ERROR_CODE)
            }
        }
    }

    private fun getLfmParametersModel(
        startFrequency: Long,
        stopFrequency: Long,
        lfmPeriod: Double,
        isSymmetricLfm: Boolean
    ): LfmInputParametersModel {
        return LfmInputParametersModel(
            lowestLfmFrequency = startFrequency,
            highestLfmFrequency = stopFrequency,
            lfmDeviationPeriod = lfmPeriod,
            isSymmetricLfm = isSymmetricLfm
        )
    }

    private fun checkLfmInputValues(lfmInputParametersModel: LfmInputParametersModel): Int {
        var errorCode = NO_ERROR

        lfmInputParametersModel.apply {
            if (lowestLfmFrequency < MIN_LFM_FREQ) errorCode = errorCode or LOW_FREQUENCY_UNDER_INPUT_ERROR
            if (lowestLfmFrequency > MAX_LFM_FREQ) errorCode = errorCode or LOW_FREQUENCY_ABOVE_INPUT_ERROR
            if (highestLfmFrequency - lowestLfmFrequency < 10_000_000) errorCode = errorCode or LOW_FREQ_HIGHER_THAN_HIGH_FREQ_INPUT_ERROR
            if (highestLfmFrequency < MIN_LFM_FREQ) errorCode = errorCode or HIGH_FREQUENCY_UNDER_INPUT_ERROR
            if (highestLfmFrequency > MAX_LFM_FREQ) errorCode = errorCode or HIGH_FREQUENCY_ABOVE_INPUT_ERROR
            if (lfmDeviationPeriod > MAX_LFM_PERIOD_MS) errorCode = errorCode or MODULATION_PERIOD_ABOVE_INPUT_ERROR
            if (lfmDeviationPeriod < MIN_LFM_PERIOD_MS) errorCode = errorCode or MODULATION_PERIOD_UNDER_INPUT_ERROR
        }
        return errorCode
    }

    private fun checkCwInputValues(frequency: Long): Int {
        var errorCode = NO_ERROR
        if (frequency < MIN_LFM_FREQ) errorCode = errorCode or LOW_FREQUENCY_UNDER_INPUT_ERROR
        if (frequency > MAX_LFM_FREQ) errorCode = errorCode or LOW_FREQUENCY_ABOVE_INPUT_ERROR

        return errorCode
    }

    private fun getRchmDissStateModelFromSubModulesState(
        transmitter: TransmitterUpdatingStatus,
        receiver: ReceiverUpdatingStatus,
        synthesizer: SynthesizerUpdatingStatus
    ): RchmDissStateModel {
        val transmitterData = when(transmitter) {
            is TransmitterUpdatingStatus.Success ->
                Pair(transmitter.transmitterModuleState, true)
            else -> Pair(TransmitterModuleState(), false)
        }
        val receiverData = when(receiver) {
            is ReceiverUpdatingStatus.Success ->
                Pair(receiver.receiverModuleState, true)
            else -> Pair(ReceiverModuleState(), false)
        }
        val synthesizerData = when(synthesizer) {
            is SynthesizerUpdatingStatus.Success ->
                Pair(synthesizer.synthesizerModuleStateModel, true)
            else -> Pair(SynthesizerModuleStateModel(), false)
        }

        return RchmDissStateModel(
            receiverModuleState = receiverData.first,
            isActualReceiverData = receiverData.second,
            transmitterModuleState = transmitterData.first,
            isActualTransmitterData = transmitterData.second,
            synthesizerModuleState = synthesizerData.first,
            isActualSynthesizerData = synthesizerData.second
        )
    }

    private fun getOutputModuleStateByteArray(turnTransmitterOn: Boolean): ByteArray {
        val currentOutput = rchmDissStateRepository.rchmDissState.value.outputModuleState.rchmDissDigitalOutput
        val mask: UShort = 0xFFFDu
        val transmitterOutput: UShort = if (turnTransmitterOn) 0x2u
        else 0u
        val newOutput = (currentOutput and mask) or transmitterOutput

        return byteArrayOf(newOutput.toByte(), (newOutput.toUInt().shr(8)).toByte())
    }
}
