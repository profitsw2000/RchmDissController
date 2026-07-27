package ru.profitsw2000.data.model.bluetooth.status

import ru.profitsw2000.data.model.bluetooth.BluetoothDeviceModel

sealed class BluetoothConnectionStatus {
    data object Disconnected: BluetoothConnectionStatus()
    data class DeviceSelection(val devicesNameList: List<BluetoothDeviceModel>): BluetoothConnectionStatus()
    data object Connecting: BluetoothConnectionStatus()
    data object Connected: BluetoothConnectionStatus()
    data object Failed: BluetoothConnectionStatus()
}