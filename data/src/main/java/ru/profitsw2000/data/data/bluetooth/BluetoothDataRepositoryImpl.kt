package ru.profitsw2000.data.data.bluetooth

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import ru.profitsw2000.data.domain.bluetooth.BluetoothDataRepository
import java.io.InputStream
import java.io.OutputStream

class BluetoothDataRepositoryImpl(
    private val socket: BluetoothSocket?
) : BluetoothDataRepository {

    override suspend fun writeData(byteArray: ByteArray) {
        socket?.let {
            if (it.isConnected) {
                writeByteArray(socket.outputStream, byteArray)
            }
        }
    }

    private suspend fun writeByteArray(outputStream: OutputStream, byteArray: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                outputStream.write(byteArray)
                outputStream.flush()
                true
            } catch (exception: Exception) {
                false
            }
        }
    }

    override fun readData(inputStream: InputStream): Flow<ByteArray> = flow<ByteArray> {
        val buffer = ByteArray(1024)

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
    }.flowOn(Dispatchers.IO)
}