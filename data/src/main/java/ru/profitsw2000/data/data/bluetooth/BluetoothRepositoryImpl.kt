package ru.profitsw2000.data.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultRegistry
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.profitsw2000.core.drawable.utils.bluetooth.BluetoothStateBroadcastReceiver
import ru.profitsw2000.core.drawable.utils.bluetooth.OnBluetoothStateListener
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository

class BluetoothRepositoryImpl(
    private val context: Context
) : BluetoothRepository, DefaultLifecycleObserver, OnBluetoothStateListener {

    private val _bluetoothIsEnabled = MutableStateFlow(false)
    override val bluetoothIsEnabled: StateFlow<Boolean>
        get() = _bluetoothIsEnabled.asStateFlow()
    override val bluetoothStateBroadcastReceiver: BluetoothStateBroadcastReceiver
        get() = BluetoothStateBroadcastReceiver(this)

    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        bluetoothManager.adapter
    }

    override fun checkBluetoothState() {
        if (permissionIsGranted())
            _bluetoothIsEnabled.value = bluetoothAdapter.isEnabled
    }

    override fun setupRegistry(registry: ActivityResultRegistry, owner: LifecycleOwner) {
        owner.lifecycle.addObserver(this)
    }

    private fun permissionIsGranted(): Boolean {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    override fun onBluetoothStateChanged(bluetoothIsEnabled: Boolean) {
        _bluetoothIsEnabled.value = bluetoothIsEnabled
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        context.registerReceiver(bluetoothStateBroadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        context.unregisterReceiver(bluetoothStateBroadcastReceiver)
    }
}