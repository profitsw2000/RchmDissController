package ru.profitsw2000.rchmdisscontroller.presentation.view.adapter

import ru.profitsw2000.data.model.bluetooth.BluetoothDeviceModel

interface OnBluetoothDeviceClickListener {
    fun onClick(bluetoothDevice: BluetoothDeviceModel)
}