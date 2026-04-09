package ru.profitsw2000.rchmdisscontroller.presentation.viewmodel

import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository

class MainActivityViewModel(
    private val bluetoothRepository: BluetoothRepository
) : ViewModel() {

    val bluetoothIsEnabled = bluetoothRepository.bluetoothIsEnabled
    val shouldShowRationale = bluetoothRepository.bluetoothStateRepository.shouldShowRationale
    val bluetoothConnectionStatus = bluetoothRepository.bluetoothConnectionRepository.bluetoothConnectionStatusFlow
    fun checkBluetoothState() = bluetoothRepository.bluetoothStateRepository.checkBluetoothState()

    fun setupRegistry(registry: ActivityResultRegistry, owner: LifecycleOwner) {
        bluetoothRepository.bluetoothStateRepository.setupRegistry(registry, owner)
    }

    fun switchBluetooth(shouldShowRationale: Boolean) {
        bluetoothRepository.bluetoothStateRepository.switchBluetooth(shouldShowRationale)
    }

    fun rationaleIsShowed() {
        bluetoothRepository.bluetoothStateRepository.clearShouldShowRationale()
    }

    fun initBluetoothConnection() {
        viewModelScope.launch {
            bluetoothRepository.bluetoothConnectionRepository.initConnection(bluetoothRepository.bluetoothIsEnabled.value)
        }
    }

    fun connectBluetoothDevice(address: String) {
        viewModelScope.launch {
            bluetoothRepository.bluetoothConnectionRepository.connectBluetoothDevice(address)
        }
    }
}