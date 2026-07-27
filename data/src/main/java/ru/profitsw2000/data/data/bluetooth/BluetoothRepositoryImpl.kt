package ru.profitsw2000.data.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
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
        bluetoothAdapter,
        bluetoothGatt,
        bluetoothGattCharacteristic
    )
    override val bluetoothDataRepository = BluetoothDataRepositoryImpl(bluetoothSocket)
    @OptIn(ExperimentalCoroutinesApi::class)
    override val bluetoothBytesDataFlow: Flow<ByteArray> = bluetoothConnectionRepository.bluetoothConnectionStatusFlow
        .flatMapLatest { status ->
            if (status is BluetoothConnectionStatus.Connected) {
                bluetoothSocket?.inputStream?.let {bluetoothDataRepository.readData(it)} ?: emptyFlow()
            } else emptyFlow()
        }
    override val bluetoothIsEnabled = bluetoothStateRepository.bluetoothIsEnabled
}