package ru.profitsw2000.data.domain.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import ru.profitsw2000.core.drawable.utils.bluetooth.BluetoothStateBroadcastReceiver

interface BluetoothRepository {

    val bluetoothIsEnabled: StateFlow<Boolean>

    val bluetoothAdapter: BluetoothAdapter

    var bluetoothSocket: BluetoothSocket

    val bluetoothStateRepository: BluetoothStateRepository

}