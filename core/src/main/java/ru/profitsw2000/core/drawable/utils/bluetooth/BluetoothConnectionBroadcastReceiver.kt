package ru.profitsw2000.core.drawable.utils.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.profitsw2000.core.drawable.utils.listeners.OnBluetoothConnectionListener

class BluetoothConnectionBroadcastReceiver(
    private val onBluetoothConnectionListener: OnBluetoothConnectionListener
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) onBluetoothConnectionListener.bluetoothDeviceDisconnected()
    }
}