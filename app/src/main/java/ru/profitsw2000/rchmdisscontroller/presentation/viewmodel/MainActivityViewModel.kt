package ru.profitsw2000.rchmdisscontroller.presentation.viewmodel

import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.asSharedFlow
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository

class MainActivityViewModel(
    private val bluetoothRepository: BluetoothRepository
) : ViewModel() {

    val bluetoothIsEnabled = bluetoothRepository.bluetoothIsEnabled
    val shouldShowRationale = bluetoothRepository.bluetoothStateRepository.shouldShowRationale

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
}