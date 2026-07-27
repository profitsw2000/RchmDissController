package ru.profitsw2000.data.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import androidx.annotation.RequiresPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
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
    private val bluetoothAdapter: BluetoothAdapter,
    private var bluetoothGatt: BluetoothGatt?,
    private var bluetoothGattCharacteristic: BluetoothGattCharacteristic?
) : BluetoothConnectionRepository, OnBluetoothConnectionListener, DefaultLifecycleObserver {

    private val BLE_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val BLE_CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    // UUID для Classic SPP (Старый HC-05)
    private val CLASSIC_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun connectBluetoothDevice(address: String) {
        _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Connecting
        withContext(Dispatchers.IO) {
            val device = bluetoothAdapter.getRemoteDevice(address)
            val lowEnergyBluetoothConnectionSuccess = lowEnergyBluetoothConnect(device)
            if (lowEnergyBluetoothConnectionSuccess) {
                _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Connected
                return@withContext
            }

            val classicBluetoothConnection = classicBluetoothConnect(device)
            if (classicBluetoothConnection) {
                _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Connected
            } else {
                _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Failed
            }
/*            try {
                bluetoothSocket = bluetoothAdapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Connected
            } catch (ioException: IOException) {
                bluetoothSocket?.close()
                _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Failed
            }*/
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun disconnectBluetoothDevice() {
        withContext(Dispatchers.IO) {
            try {
                bluetoothGatt?.close()
                bluetoothGatt = null
                bluetoothGattCharacteristic = null
                bluetoothSocket?.close()
                bluetoothSocket = null
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        context.unregisterReceiver(bluetoothConnectionBroadcastReceiver)
        bluetoothGatt?.close()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun lowEnergyBluetoothConnect(device: BluetoothDevice): Boolean {
        if (device.type == BluetoothDevice.DEVICE_TYPE_CLASSIC) return false

        return suspendCancellableCoroutine { continuation ->
            val gattCallback = object : BluetoothGattCallback() {
                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        bluetoothDeviceDisconnected()
                        if (continuation.isActive) continuation.resume(false) { cause, _, _ -> }
                    }
                }

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val service = gatt.getService(BLE_SERVICE_UUID)
                        bluetoothGattCharacteristic = service?.getCharacteristic(BLE_CHARACTERISTIC_UUID)

                        if (bluetoothGattCharacteristic != null) {
                            if (continuation.isActive) continuation.resume(true) {
                                bluetoothGatt?.disconnect()
                                bluetoothGatt?.close()
                                bluetoothGatt = null
                            }
                        } else {
                            if (continuation.isActive) continuation.resume(false) {}
                        }
                    } else {
                        if (continuation.isActive) continuation.resume(false) {}
                    }
                }
            }

            bluetoothGatt = device.connectGatt(context, false, gattCallback)

            continuation.invokeOnCancellation {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun classicBluetoothConnect(device: BluetoothDevice): Boolean {
        return try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(CLASSIC_UUID)
            bluetoothSocket?.connect()
            true
        } catch (ioException: IOException) {
            bluetoothSocket?.close()
            false
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