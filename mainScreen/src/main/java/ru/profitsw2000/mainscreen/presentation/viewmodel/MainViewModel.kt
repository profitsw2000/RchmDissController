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
        ReceiverUpdatingStatus.Idle(rchmDissStateRepository.rchmDissState.value.receiverModuleState)
    )
    val receiverUpdatingStatusFlow: StateFlow<ReceiverUpdatingStatus> = _receiverUpdatingStatusFlow
    private val _synthesizerUpdatingStatusFlow = MutableStateFlow<SynthesizerUpdatingStatus>(
        SynthesizerUpdatingStatus.Updating
    )
    val synthesizerUpdatingStatusFlow: StateFlow<SynthesizerUpdatingStatus> = _synthesizerUpdatingStatusFlow

    private val transmitterStatePairFlow: StateFlow<Pair<TransmitterModuleState, Boolean>> =
        transmitterUpdatingStatusFlow.scan(Pair(TransmitterModuleState(), false)) { prevState, newStatus ->
            when(newStatus) {
                is TransmitterUpdatingStatus.Success -> Pair(prevState.first, true)
                is TransmitterUpdatingStatus.Error -> Pair(prevState.first, false)
                is TransmitterUpdatingStatus.Idle -> Pair(newStatus.transmitterModuleState, prevState.second)
                TransmitterUpdatingStatus.Updating -> Pair(prevState.first, prevState.second)
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = Pair(TransmitterModuleState(), false)
            )
    private val receiverStatePairFlow: StateFlow<Pair<ReceiverModuleState, Boolean>> =
        receiverUpdatingStatusFlow.scan(Pair(ReceiverModuleState(), false)) { prevState, newStatus ->
            when(newStatus) {
                ReceiverUpdatingStatus.Success -> Pair(prevState.first, true)
                is ReceiverUpdatingStatus.Error -> Pair(prevState.first, false)
                is ReceiverUpdatingStatus.Idle -> Pair(newStatus.receiverModuleState, prevState.second)
                ReceiverUpdatingStatus.Updating -> Pair(prevState.first, prevState.second)
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = Pair(ReceiverModuleState(), false)
            )
    private val synthesizerStatePairFlow: StateFlow<Pair<SynthesizerModuleStateModel, Boolean>> =
        synthesizerUpdatingStatusFlow.scan(Pair(SynthesizerModuleStateModel(), false)) { prevState, newStatus ->
            when(newStatus) {
                SynthesizerUpdatingStatus.Success -> Pair(prevState.first, true)
                is SynthesizerUpdatingStatus.Error -> Pair(prevState.first, false)
                is SynthesizerUpdatingStatus.Idle -> Pair(newStatus.synthesizerModuleStateModel, prevState.second)
                SynthesizerUpdatingStatus.Updating -> Pair(prevState.first, prevState.second)
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = Pair(SynthesizerModuleStateModel(), false)
            )

    val rchmDissState: StateFlow<RchmDissStateModel> = combine(
        transmitterStatePairFlow,
        receiverStatePairFlow,
        synthesizerStatePairFlow
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
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RchmDissStateModel()
        )

    init {
        loadSynthesizerInitialParameters()
    }

    fun updateTransmitter(channelByte: Byte, turnTransmitterOn: Boolean) {
        viewModelScope.launch {
            _transmitterUpdatingStatusFlow.value = TransmitterUpdatingStatus.Updating

            try {
                bluetoothRepository.bluetoothDataRepository.writeData(
                    bluetoothPacketManager.getWriteToTransmitterPacket(channelByte)
                )
                withTimeout(5000L.milliseconds) {
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

                _transmitterUpdatingStatusFlow.value = TransmitterUpdatingStatus.Success

                delay(500.milliseconds)

                _transmitterUpdatingStatusFlow.value = TransmitterUpdatingStatus.Idle(
                    rchmDissStateRepository.rchmDissState.value.transmitterModuleState,
                    rchmDissStateRepository.rchmDissState.value.outputModuleState
                )
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
                withTimeout(5000L.milliseconds) {
                    rchmDissStateRepository.lastPacket.first {
                        it == RcdInputPacketType.ReceiverStateInputPacket
                    }
                }

                _receiverUpdatingStatusFlow.value =
                    ReceiverUpdatingStatus.Success

                delay(500.milliseconds)

                _receiverUpdatingStatusFlow.value =
                    ReceiverUpdatingStatus.Idle(
                        rchmDissStateRepository.rchmDissState.value.receiverModuleState
                    )

            } catch (exc: TimeoutCancellationException) {
                _receiverUpdatingStatusFlow.value = ReceiverUpdatingStatus.Error(RESPONSE_PACKET_TIMEOUT_ERROR_CODE)
            } catch (exc: Exception) {
                _receiverUpdatingStatusFlow.value = ReceiverUpdatingStatus.Error(UNKNOWN_ERROR_CODE)
            }
        }
    }

    fun updateSynthesizerCwMode(frequency: Long) {
        val errorCode = checkCwInputValues(frequency * 1_000_000)
        _synthesizerUpdatingStatusFlow.value = SynthesizerUpdatingStatus.Updating
        if (errorCode == NO_ERROR) {
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
        isSymmetricLfm: Boolean,
        isExtTriggerLfm: Boolean
    ) {
        val lfmParameters = getLfmParametersModel(
            startFrequency = startFrequency * 1_000_000,
            stopFrequency = stopFrequency * 1_000_000,
            lfmPeriod = lfmPeriod * 0.001,
            isSymmetricLfm = isSymmetricLfm
        )
        val errorCode = checkLfmInputValues(lfmParameters)

        _synthesizerUpdatingStatusFlow.value = SynthesizerUpdatingStatus.Updating
        if (errorCode == NO_ERROR) {
            viewModelScope.launch {
                try {
                    val registersList = pllRegisters1208PL1URepository.getLfmRegisters(lfmParameters)
                    updateSynthesizer(registersList)

                    //Здесь отправляем пакет для установки сигнала Вкл_ЛЧМ
                    launch {
                        bluetoothRepository.bluetoothDataRepository.writeData(
                            bluetoothPacketManager.getRchmDissOutputSetPacket(
                                getOutputModuleStateByteArray(isExtTriggerLfm, lfmPeriod)
                            )
                        )
                    }
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
                    withTimeout(5000L.milliseconds) {
                        rchmDissStateRepository.lastPacket.first {
                            it == RcdInputPacketType.SynthesizerStateInputPacket
                        }
                    }
                }
                _synthesizerUpdatingStatusFlow.value = SynthesizerUpdatingStatus.Success

                delay(500.milliseconds)

                _synthesizerUpdatingStatusFlow.value =
                    SynthesizerUpdatingStatus.Idle(
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

    private fun loadSynthesizerInitialParameters() {
        viewModelScope.launch {
            try {
                val lfmParams = withContext(Dispatchers.IO) {
                    pllRegisters1208PL1URepository.getLfmParameters(
                        rchmDissStateRepository.rchmDissState.value.synthesizerModuleState
                    )
                }
                _synthesizerUpdatingStatusFlow.value = SynthesizerUpdatingStatus.Idle(lfmParams)

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
            if (lfmDeviationPeriod > MAX_LFM_PERIOD_SEC) errorCode = errorCode or MODULATION_PERIOD_ABOVE_INPUT_ERROR
            if (lfmDeviationPeriod < MIN_LFM_PERIOD_SEC) errorCode = errorCode or MODULATION_PERIOD_UNDER_INPUT_ERROR
        }
        return errorCode
    }

    private fun checkCwInputValues(frequency: Long): Int {
        var errorCode = NO_ERROR
        if (frequency < MIN_LFM_FREQ) errorCode = errorCode or CW_FREQUENCY_UNDER_INPUT_ERROR
        if (frequency > MAX_LFM_FREQ) errorCode = errorCode or CW_FREQUENCY_ABOVE_INPUT_ERROR

        return errorCode
    }

    private fun getRchmDissStateModelFromSubModulesState(
        transmitter: Pair<TransmitterModuleState, Boolean>,
        receiver: Pair<ReceiverModuleState, Boolean>,
        synthesizer: Pair<SynthesizerModuleStateModel, Boolean>
    ): RchmDissStateModel {
        return RchmDissStateModel(
            receiverModuleState = receiver.first,
            isActualReceiverData = receiver.second,
            transmitterModuleState = transmitter.first,
            isActualTransmitterData = transmitter.second,
            synthesizerModuleState = synthesizer.first,
            isActualSynthesizerData = synthesizer.second
        )
    }

    private suspend fun getOutputModuleStateByteArray(transmitterIsOn: Boolean): ByteArray {
        val outputModuleState = rchmDissStateRepository.rchmDissState.value.outputModuleState
        val lfmPeriod = pllRegisters1208PL1URepository.getLfmParameters(
            rchmDissStateRepository.rchmDissState.value.synthesizerModuleState
        ).lfmPeriod
        val periodConventionalUnits = (lfmPeriod/0.000008).toInt()
        val transmitterIsOnBit = if (transmitterIsOn) 1 else 0
        val extTriggerLfmBit = if (outputModuleState.lfmExtTriggerIsOn) 1 else 0

        val newOutput = ((periodConventionalUnits shl 2) or (transmitterIsOnBit shl 1) or extTriggerLfmBit)

        return byteArrayOf((newOutput.toUInt().shr(8)).toByte(), newOutput.toByte())
    }

    private suspend fun getOutputModuleStateByteArray(extTriggerLfm: Boolean, lfmPeriod: Double): ByteArray {
        val outputModuleState = rchmDissStateRepository.rchmDissState.value.outputModuleState
        val periodConventionalUnits = (lfmPeriod/0.000008).toInt()
        val transmitterIsOnBit = if (outputModuleState.transmitterIsOn) 1 else 0
        val extTriggerLfmBit = if (extTriggerLfm) 1 else 0
        val newOutput = ((periodConventionalUnits shl 2) or (transmitterIsOnBit shl 1) or extTriggerLfmBit)

        return byteArrayOf((newOutput.toUInt().shr(8)).toByte(), newOutput.toByte())
    }
}
