package ru.profitsw2000.data.domain.bluetooth

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow
import ru.profitsw2000.core.drawable.utils.bluetooth.BluetoothConnectionBroadcastReceiver
import ru.profitsw2000.data.model.bluetooth.status.BluetoothConnectionStatus

interface BluetoothConnectionRepository {

    val bluetoothConnectionStatusFlow: StateFlow<BluetoothConnectionStatus>
    val bluetoothConnectionBroadcastReceiver: BluetoothConnectionBroadcastReceiver

    suspend fun initConnection(bluetoothIsEnabled: Boolean)

    suspend fun connectBluetoothDevice(address: String)

    suspend fun disconnectBluetoothDevice()

    fun setupLifecycleOwner(lifecycleOwner: LifecycleOwner)
}