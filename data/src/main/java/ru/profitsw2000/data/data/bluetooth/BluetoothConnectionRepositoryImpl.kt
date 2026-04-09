package ru.profitsw2000.data.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import ru.profitsw2000.data.domain.bluetooth.BluetoothConnectionRepository
import ru.profitsw2000.data.model.bluetooth.BluetoothDeviceModel
import ru.profitsw2000.data.model.bluetooth.status.BluetoothConnectionStatus
import java.io.IOException
import java.util.UUID

class BluetoothConnectionRepositoryImpl(
    private var bluetoothSocket: BluetoothSocket?,
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothConnectionRepository {

    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val _bluetoothConnectionStatusFlow =
            MutableStateFlow<BluetoothConnectionStatus>(BluetoothConnectionStatus.Disconnected)
    override val bluetoothConnectionStatusFlow: StateFlow<BluetoothConnectionStatus>
        get() = _bluetoothConnectionStatusFlow

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun initConnection(bluetoothIsEnabled: Boolean) {
        if (bluetoothIsEnabled) {
            defineBluetoothConnectionAction()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun defineBluetoothConnectionAction() {
        when(bluetoothConnectionStatusFlow.value) {
            BluetoothConnectionStatus.Connected -> disconnectBluetoothDevice()
            BluetoothConnectionStatus.Connecting -> disconnectBluetoothDevice()
            is BluetoothConnectionStatus.DeviceSelection -> {}
            BluetoothConnectionStatus.Disconnected ->
                _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.DeviceSelection(getBondedDevicesList())
            BluetoothConnectionStatus.Failed ->
                _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.DeviceSelection(getBondedDevicesList())
        }
    }

    override suspend fun connectBluetoothDevice(address: String) {
        withContext(Dispatchers.IO) {
            try {
                bluetoothSocket = bluetoothAdapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Connected
            } catch (ioException: IOException) {
                bluetoothSocket?.close()
                _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Failed
            }
        }
    }

    override suspend fun disconnectBluetoothDevice() {
        withContext(Dispatchers.IO) {
            try {
                bluetoothSocket?.close()
                _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Disconnected
            } catch (ioException: IOException) {
                ioException.printStackTrace()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun getBondedDevicesList(): List<BluetoothDeviceModel> {
        return bluetoothAdapter.bondedDevices.map { device ->
            BluetoothDeviceModel(
                name = device.name ?: "Unknown device",
                address = device.address
            )
        }
    }
}