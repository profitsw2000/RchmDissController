package ru.profitsw2000.mainscreen.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
import ru.profitsw2000.data.model.rcd.RcdInputPacketType
import ru.profitsw2000.mainscreen.state.RchmDissUpdatingStatus

class MainViewModel(
    private val rchmDissStateRepository: RchmDissStateRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val bluetoothPacketManager: BluetoothPacketManager
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
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error("Timeout error")
            } catch (exc: Exception) {
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error(exc.message ?: "Unknown error")
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
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error("Timeout error")
            } catch (exc: Exception) {
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error(exc.message ?: "Unknown error")
            }
        }
    }

    fun updateSynthesizerCwMode(frequency: Long) {

    }

    fun updateSynthesizerLfmMode(
        startFrequency: Long,
        stopFrequency: Long,
        lfmPeriod: Float
    ) {

    }

    private fun updateSynthesizer(byteArray: ByteArray, expectedInputPacketType: RcdInputPacketType) {
        viewModelScope.launch {
            _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Updating

            try {
                bluetoothRepository.bluetoothDataRepository.writeData(
                    bluetoothPacketManager.getWriteToSynthesizerPacket(byteArray)
                )
                withTimeout(5000L) {
                    rchmDissStateRepository.lastPacket.first {
                        it == expectedInputPacketType
                    }
                }

                _rchmDissUpdatingStatus.value =
                    RchmDissUpdatingStatus.Success(rchmDissStateRepository.rchmDissState.value)
            } catch (exc: TimeoutCancellationException) {
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error("Timeout error")
            } catch (exc: Exception) {
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error(exc.message ?: "Unknown error")
            }
        }
    }
}
