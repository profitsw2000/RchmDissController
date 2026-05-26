package ru.profitsw2000.mainscreen.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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
import ru.profitsw2000.data.model.pll.LfmInputParametersModel
import ru.profitsw2000.data.model.rcd.RcdInputPacketType
import ru.profitsw2000.mainscreen.state.RchmDissUpdatingStatus

class MainViewModel(
    private val rchmDissStateRepository: RchmDissStateRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val bluetoothPacketManager: BluetoothPacketManager,
    private val pllRegisters1208PL1URepository: PLLRegisters1208PL1URepository
): ViewModel() {

    private val _rchmDissUpdatingStatus = MutableStateFlow<RchmDissUpdatingStatus>(
        RchmDissUpdatingStatus.Idle)
    val rchmDissUpdatingStatus: StateFlow<RchmDissUpdatingStatus> = _rchmDissUpdatingStatus

    fun updateTransmitter(byte: Byte) {
        viewModelScope.launch {
            _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Updating

            try {
                bluetoothRepository.bluetoothDataRepository.writeData(
                    bluetoothPacketManager.getWriteToTransmitterPacket(byte)
                )
                withTimeout(5000L) {
                    rchmDissStateRepository.lastPacket.first {
                        it == RcdInputPacketType.TransmitterStateInputPacket
                    }
                }

                _rchmDissUpdatingStatus.value =
                    RchmDissUpdatingStatus.Success(rchmDissStateRepository.rchmDissState.value)
            } catch (exc: TimeoutCancellationException) {
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error("Timeout error. ${exc.message}",
                    RESPONSE_PACKET_TIMEOUT_ERROR_CODE
                )
            } catch (exc: Exception) {
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error(exc.message ?: "Unknown error",
                    UNKNOWN_ERROR_CODE
                )
            }
        }
    }

    fun updateReceiver(byteArray: ByteArray) {
        viewModelScope.launch {
            _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Updating

            try {
                bluetoothRepository.bluetoothDataRepository.writeData(
                    bluetoothPacketManager.getWriteToReceiverPacket(byteArray)
                )
                withTimeout(5000L) {
                    rchmDissStateRepository.lastPacket.first {
                        it == RcdInputPacketType.ReceiverStateInputPacket
                    }
                }

                _rchmDissUpdatingStatus.value =
                    RchmDissUpdatingStatus.Success(rchmDissStateRepository.rchmDissState.value)
            } catch (exc: TimeoutCancellationException) {
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error("Timeout error. ${exc.message}", RESPONSE_PACKET_TIMEOUT_ERROR_CODE)
            } catch (exc: Exception) {
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error(exc.message ?: "Unknown error", UNKNOWN_ERROR_CODE)
            }
        }
    }

    fun updateSynthesizerCwMode(frequency: Long) {
        val errorCode = checkCwInputValues(frequency * 1_000_000)
        if (errorCode == NO_ERROR) {
            viewModelScope.launch {
                try {
                    val registersList = pllRegisters1208PL1URepository.getCwRegisters(frequency)
                    updateSynthesizer(registersList)
                } catch (e: Exception) {
                    _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error(e.message ?: "Unknown error", REGISTERS_CALCULATION_ERROR_CODE)
                }
            }
        }
        else _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error("", errorCode)
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
                    _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error(e.message ?: "Unknown error", REGISTERS_CALCULATION_ERROR_CODE)
                }
            }
        }
    }

    private fun updateSynthesizer(synthesizerRegisters: List<Int>) {
        viewModelScope.launch {
            _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Updating

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

                _rchmDissUpdatingStatus.value =
                    RchmDissUpdatingStatus.Success(rchmDissStateRepository.rchmDissState.value)
            } catch (exc: TimeoutCancellationException) {
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error("Timeout error. ${exc.message}", RESPONSE_PACKET_TIMEOUT_ERROR_CODE)
            } catch (exc: Exception) {
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error(exc.message ?: "Unknown error", UNKNOWN_ERROR_CODE)
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
}
