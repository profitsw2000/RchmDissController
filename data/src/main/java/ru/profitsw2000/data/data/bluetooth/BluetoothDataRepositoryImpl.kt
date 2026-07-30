package ru.profitsw2000.data.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothSocket
import android.os.Build
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import ru.profitsw2000.data.domain.bluetooth.BluetoothDataRepository
import java.io.InputStream
import java.io.OutputStream

class BluetoothDataRepositoryImpl(
    private val socket: BluetoothSocket?,
    private var bluetoothConnectionRepositoryImpl: BluetoothConnectionRepositoryImpl,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BluetoothDataRepository {

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun writeData(byteArray: ByteArray) {
        withContext(ioDispatcher) {

            val bluetoothGattCharacteristic = bluetoothConnectionRepositoryImpl.getActiveCharacteristic()
            val bluetoothGatt = bluetoothConnectionRepositoryImpl.getActiveGatt()
            if (bluetoothGattCharacteristic != null && bluetoothGatt != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic, byteArray, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                } else {
                    @Suppress("DEPRECATION")
                    bluetoothGattCharacteristic.value = byteArray
                    @Suppress("DEPRECATION")
                    bluetoothGattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)
                }
                return@withContext
            }

            socket?.let {
                if (it.isConnected) {
                    writeByteArray(socket.outputStream, byteArray)
                }
            }
        }
    }

    private suspend fun writeByteArray(outputStream: OutputStream, byteArray: ByteArray): Boolean {
        return try {
            outputStream.write(byteArray)
            outputStream.flush()
            true
        } catch (exception: Exception) {
            false
        }
    }

    override fun readData(inputStream: InputStream): Flow<ByteArray> = flow<ByteArray> {
        val buffer = ByteArray(256)

        while (true) {
            try {
                val bytesNumber = inputStream.read(buffer)
                if (bytesNumber > 0) {
                    emit(buffer.copyOfRange(0, bytesNumber))
                }
            } catch (exception: Exception) {
                break
            }
        }
    }.flowOn(ioDispatcher)
}