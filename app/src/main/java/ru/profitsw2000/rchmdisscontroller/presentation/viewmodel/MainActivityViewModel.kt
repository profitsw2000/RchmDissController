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
    val permissionIsDenied = bluetoothRepository.permissionIsDenied

    fun checkBluetoothState() = bluetoothRepository.checkBluetoothState()

    fun setupRegistry(registry: ActivityResultRegistry, owner: LifecycleOwner) {
        bluetoothRepository.setupRegistry(registry, owner)
    }

    fun switchBluetooth() {
        bluetoothRepository.switchBluetooth()
    }
}