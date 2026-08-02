package ru.profitsw2000.data.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
import java.nio.Buffer
import java.util.UUID

class BluetoothConnectionRepositoryImpl(
    private val context: Context,
    private var bluetoothSocket: BluetoothSocket?,
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothConnectionRepository, OnBluetoothConnectionListener, DefaultLifecycleObserver {

    private val BLE_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val BLE_CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    // UUID для Classic SPP (Старый HC-05)
    private val CLASSIC_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val filter = IntentFilter().apply {
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    }
    override var bluetoothGatt: BluetoothGatt? = null
    override var bluetoothGattCharacteristic: BluetoothGattCharacteristic? = null

    private val _bluetoothLowEnergyDataFlow = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val bluetoothLowEnergyDataFlow: Flow<ByteArray> = _bluetoothLowEnergyDataFlow

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
            val deviceType = device.type
            when(deviceType) {
                BluetoothDevice.DEVICE_TYPE_LE -> lowEnergyBluetoothConnect(device)
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> classicBluetoothConnect(device)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun disconnectBluetoothDevice() {
        withContext(Dispatchers.IO) {
            try {
                bluetoothGattDisconnectAndClose()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.registerReceiver(
                bluetoothConnectionBroadcastReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(bluetoothConnectionBroadcastReceiver, filter)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        context.unregisterReceiver(bluetoothConnectionBroadcastReceiver)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        bluetoothGattDisconnectAndClose()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun lowEnergyBluetoothConnect(device: BluetoothDevice) {
        val gattCallback = object : BluetoothGattCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    bluetoothGattDisconnectAndClose()
                    _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Failed
                    bluetoothDeviceDisconnected()
                    return
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    bluetoothGattDisconnectAndClose()
                    _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Disconnected
                    bluetoothDeviceDisconnected()
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(BLE_SERVICE_UUID)
                    bluetoothGattCharacteristic = service?.getCharacteristic(BLE_CHARACTERISTIC_UUID)

                    if (bluetoothGattCharacteristic != null) {
                        enableNotifications(gatt = gatt, bluetoothGattCharacteristic!!)
                        _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Connected
                    } else {
                        bluetoothGattDisconnectAndClose()
                        _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Failed
                    }
                } else {
                    bluetoothGattDisconnectAndClose()
                    _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Failed
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                _bluetoothLowEnergyDataFlow.tryEmit(value)
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic
            ) {
                _bluetoothLowEnergyDataFlow.tryEmit(characteristic.value)
            }
        }

        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun bluetoothGattDisconnectAndClose() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun classicBluetoothConnect(device: BluetoothDevice) {
        return try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(CLASSIC_UUID)
            bluetoothSocket?.connect()
            _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Connected
        } catch (ioException: IOException) {
            bluetoothSocket?.close()
            _bluetoothConnectionStatusFlow.value = BluetoothConnectionStatus.Failed
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enableNotifications(gatt: BluetoothGatt,
                                    characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)

        val configUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val descriptor = characteristic.getDescriptor(configUuid)
        if (descriptor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
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