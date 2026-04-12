package ru.profitsw2000.data.domain.bluetooth

import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface BluetoothDataRepository {

    suspend fun writeData(byteArray: ByteArray)

    fun readData(inputStream: InputStream): Flow<ByteArray>
}