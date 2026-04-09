package ru.profitsw2000.data.data.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.profitsw2000.core.drawable.utils.bluetooth.BluetoothStateBroadcastReceiver
import ru.profitsw2000.core.drawable.utils.bluetooth.OnBluetoothStateListener
import ru.profitsw2000.data.domain.bluetooth.BluetoothConnectionRepository
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.bluetooth.BluetoothStateRepository

class BluetoothRepositoryImpl(
    private val context: Context
) : BluetoothRepository {

    private val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    override val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    override var bluetoothSocket: BluetoothSocket? = null
    override val bluetoothStateRepository = BluetoothStateRepositoryImpl(context, bluetoothAdapter)
    override val bluetoothConnectionRepository = BluetoothConnectionRepositoryImpl(bluetoothSocket, bluetoothAdapter)
    override val bluetoothIsEnabled = bluetoothStateRepository.bluetoothIsEnabled

}