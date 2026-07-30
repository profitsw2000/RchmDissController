package ru.profitsw2000.data.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import ru.profitsw2000.data.domain.bluetooth.BluetoothDataRepository
import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.model.bluetooth.status.BluetoothConnectionStatus

class BluetoothRepositoryImpl(
    private val context: Context
) : BluetoothRepository {

    private val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    override val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    override var bluetoothSocket: BluetoothSocket? = null
    override var bluetoothGatt: BluetoothGatt? = null
    override var bluetoothGattCharacteristic: BluetoothGattCharacteristic? = null
    override val bluetoothStateRepository = BluetoothStateRepositoryImpl(context, bluetoothAdapter)
    override val bluetoothConnectionRepository = BluetoothConnectionRepositoryImpl(
        context,
        bluetoothSocket,
        bluetoothAdapter
    )
    override val bluetoothDataRepository = BluetoothDataRepositoryImpl(bluetoothSocket, bluetoothConnectionRepository)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val bluetoothClassicBytesDataFlow: Flow<ByteArray> = bluetoothConnectionRepository.bluetoothConnectionStatusFlow
        .flatMapLatest { status ->
            if (status is BluetoothConnectionStatus.Connected) {
                bluetoothSocket?.inputStream?.let {bluetoothDataRepository.readData(it)} ?: emptyFlow()
            } else emptyFlow()
        }
    private val bluetoothLowEnergyBytesDataFlow: Flow<ByteArray> = bluetoothConnectionRepository._bluetoothLowEnergyDataFlow
    override val bluetoothBytesDataFlow: Flow<ByteArray> = bluetoothLowEnergyBytesDataFlow
    override val bluetoothIsEnabled = bluetoothStateRepository.bluetoothIsEnabled

    init {
        collectData()
    }

    fun collectData() {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            bluetoothBytesDataFlow.collect { bytes ->
                Log.d("VVV", "collectData: $bytes")
            }
        }
    }
}