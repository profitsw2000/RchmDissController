package ru.profitsw2000.mainscreen.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
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
                bluetoothRepository.bluetoothDataRepository.writeData(byte)
            } catch (exc: TimeoutCancellationException) {
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error("Timeout error")
            } catch (exc: Exception) {
                _rchmDissUpdatingStatus.value = RchmDissUpdatingStatus.Error(exc.message ?: "Unknown error")
            }
        }
    }
}