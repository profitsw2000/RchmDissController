package ru.profitsw2000.data.data.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.profitsw2000.core.drawable.utils.bluetooth.BluetoothStateBroadcastReceiver
import ru.profitsw2000.core.drawable.utils.bluetooth.OnBluetoothStateListener
import ru.profitsw2000.data.domain.bluetooth.BluetoothStateRepository

class BluetoothStateRepositoryImpl(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothStateRepository, DefaultLifecycleObserver, OnBluetoothStateListener {
    private val _bluetoothIsEnabled = MutableStateFlow(false)
    override val bluetoothIsEnabled: StateFlow<Boolean>
        get() = _bluetoothIsEnabled
    override val bluetoothStateBroadcastReceiver: BluetoothStateBroadcastReceiver
        get() = BluetoothStateBroadcastReceiver(this)

    private val _shouldShowRationale = MutableStateFlow(false)
    override val shouldShowRationale = _shouldShowRationale.asStateFlow()

    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var bluetoothEnableLauncher: ActivityResultLauncher<Intent>? = null

    override fun checkBluetoothState() {
        if (permissionIsGranted())
            _bluetoothIsEnabled.value = bluetoothAdapter.isEnabled
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun setupRegistry(
        registry: ActivityResultRegistry,
        owner: LifecycleOwner
    ) {
        setupPermissionLauncher(registry, owner)
        setupBluetoothEnableLauncher(registry, owner)
        owner.lifecycle.addObserver(this)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun switchBluetooth(shouldShowPermissionRationale: Boolean) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkBluetoothPermissions(shouldShowPermissionRationale)
        } else switchBluetoothState()
    }

    override fun clearShouldShowRationale() {
        _shouldShowRationale.value = false
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

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        permissionLauncher = null
        bluetoothEnableLauncher = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun setupPermissionLauncher(registry: ActivityResultRegistry, owner: LifecycleOwner) {
        permissionLauncher = registry.register(
            "bt_permission_key",
            owner,
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted)
                switchBluetoothState()
            else
                _shouldShowRationale.value = true
        }
    }

    private fun setupBluetoothEnableLauncher(registry: ActivityResultRegistry, owner: LifecycleOwner) {
        bluetoothEnableLauncher = registry.register(
            "bt_enable_key",
            owner,
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            _bluetoothIsEnabled.value = result.resultCode == Activity.RESULT_OK
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun switchBluetoothState() {
        bluetoothAdapter?.let {
            if (it.isEnabled){
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) it.disable()
            }
            else
                launchBluetoothEnableIntent()
        }
    }

    private fun permissionIsGranted(): Boolean {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun checkBluetoothPermissions(shouldShowPermissionRationale: Boolean) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED -> switchBluetoothState()
            shouldShowPermissionRationale -> _shouldShowRationale.value = true
            else -> permissionLauncher?.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    private fun launchBluetoothEnableIntent() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        bluetoothEnableLauncher?.launch(intent)
    }
}