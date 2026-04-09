package ru.profitsw2000.data.model.bluetooth.status

sealed class BluetoothConnectionStatus {
    data object Disconnected: BluetoothConnectionStatus()
    data class DeviceSelection(val devicesNameList: List<String>): BluetoothConnectionStatus()
    data object Connecting: BluetoothConnectionStatus()
    data object Connected: BluetoothConnectionStatus()
    data object Failed: BluetoothConnectionStatus()
}