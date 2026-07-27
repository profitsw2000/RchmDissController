package ru.profitsw2000.core.drawable.utils.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.profitsw2000.core.drawable.utils.listeners.OnBluetoothStateListener

class BluetoothStateBroadcastReceiver(
    private val onBluetoothStateListener: OnBluetoothStateListener
): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            onBluetoothStateListener.onBluetoothStateChanged(state == BluetoothAdapter.STATE_ON)
        }
    }
}
