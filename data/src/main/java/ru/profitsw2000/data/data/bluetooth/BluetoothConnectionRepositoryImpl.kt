package ru.profitsw2000.data.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import androidx.annotation.RequiresPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import ru.profitsw2000.core.drawable.utils.bluetooth.BluetoothConnectionBroadcastReceiver
import ru.profitsw2000.core.drawable.utils.listeners.OnBluetoothConnectionListener
import ru.profitsw2000.data.domain.bluetooth.BluetoothConnectionRepository
import ru.profitsw2000.data.model.bluetooth.BluetoothDeviceModel
import ru.profitsw2000.data.model.bluetooth.status.BluetoothConnectionStatus
import java.io.IOException
import java.util.UUID

class BluetoothConnectionRepositoryImpl(
    private val context: Context,
    private var bluetoothSocket: BluetoothSocket?,
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothConnectionRepository, OnBluetoothConnectionListener, DefaultLifecycleObserver {

    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val filter = IntentFilter().apply {
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    }

    private val _bluetoothConnectionStatusFlow =
            MutableStateFlow<BluetoothConnectionStatus>(BluetoothConnectionStatus.Disconnected)
    override val bluetoothConnectionStatusFlow: StateFlow<BluetoothConnectionStatus>
        get() = _bluetoothConnectionStatusFlow
    override val bluetoothConnectionBroadcastReceiver = BluetoothConnectionBroadcastReceiver(this)

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun initConnection(bluetoothIsEnabled: Boolean) {
        if (bluetoothIsEnabled) {
            defineBluetoothConnectionAction()
        } else
            disconnectBluetoothDevice()
    }

    override suspend fun connectBluetoothDevice(address: String) {
        _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Connecting
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

    override fun setupLifecycleOwner(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun bluetoothDeviceDisconnected() {
        _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Disconnected
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        context.registerReceiver(bluetoothConnectionBroadcastReceiver, filter)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        context.unregisterReceiver(bluetoothConnectionBroadcastReceiver)
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun defineBluetoothConnectionAction() {
        when(bluetoothConnectionStatusFlow.value) {
            BluetoothConnectionStatus.Connected -> disconnectBluetoothDevice()
            BluetoothConnectionStatus.Connecting -> disconnectBluetoothDevice()
            is BluetoothConnectionStatus.DeviceSelection -> _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Disconnected
            BluetoothConnectionStatus.Disconnected ->
                _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.DeviceSelection(getBondedDevicesList())
            BluetoothConnectionStatus.Failed ->
                _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.DeviceSelection(getBondedDevicesList())
        }
    }
}