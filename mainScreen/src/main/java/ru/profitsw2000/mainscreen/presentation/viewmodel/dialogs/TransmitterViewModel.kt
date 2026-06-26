package ru.profitsw2000.mainscreen.presentation.viewmodel.dialogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import ru.profitsw2000.core.drawable.utils.RESPONSE_PACKET_TIMEOUT_ERROR_CODE
import ru.profitsw2000.core.drawable.utils.UNKNOWN_ERROR_CODE
import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.pll.PLLRegisters1208PL1URepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
import ru.profitsw2000.data.model.rcd.RcdInputPacketType
import ru.profitsw2000.mainscreen.state.TransmitterUpdatingStatus
import kotlin.time.Duration.Companion.milliseconds

class TransmitterViewModel(
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
}