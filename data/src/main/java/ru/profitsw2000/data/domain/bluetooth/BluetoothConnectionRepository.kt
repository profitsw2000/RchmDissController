package ru.profitsw2000.data.domain.bluetooth

import kotlinx.coroutines.flow.StateFlow
import ru.profitsw2000.data.model.bluetooth.status.BluetoothConnectionStatus

interface BluetoothConnectionRepository {

    val bluetoothConnectionStatusFlow: StateFlow<BluetoothConnectionStatus>

    suspend fun initConnection(bluetoothIsEnabled: Boolean)

    suspend fun connectBluetoothDevice(address: String)

    suspend fun disconnectBluetoothDevice()
}