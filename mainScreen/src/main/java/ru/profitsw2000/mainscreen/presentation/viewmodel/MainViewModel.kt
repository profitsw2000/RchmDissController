package ru.profitsw2000.mainscreen.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
import ru.profitsw2000.mainscreen.state.RchmDissUpdatingStatus

class MainViewModel(
    private val rchmDissStateRepository: RchmDissStateRepository,
    private val bluetoothRepository: BluetoothRepository
): ViewModel() {

    private val _rchmDissUpdatingStatus = MutableStateFlow<RchmDissUpdatingStatus>(
        RchmDissUpdatingStatus.Idle)
    val rchmDissUpdatingStatus: StateFlow<RchmDissUpdatingStatus> = _rchmDissUpdatingStatus

    fun updateTransmitter(byte: Byte) {

    }
}