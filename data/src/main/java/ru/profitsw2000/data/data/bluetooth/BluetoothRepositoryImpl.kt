package ru.profitsw2000.data.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository

class BluetoothRepositoryImpl(
    private val context: Context
) : BluetoothRepository {

    private val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    override val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    override var bluetoothSocket: BluetoothSocket? = null
    override val bluetoothStateRepository = BluetoothStateRepositoryImpl(context, bluetoothAdapter)
    override val bluetoothConnectionRepository = BluetoothConnectionRepositoryImpl(context, bluetoothSocket, bluetoothAdapter)
    override val bluetoothIsEnabled = bluetoothStateRepository.bluetoothIsEnabled

}