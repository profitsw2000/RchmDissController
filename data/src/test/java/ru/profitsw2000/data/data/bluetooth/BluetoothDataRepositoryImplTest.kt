package ru.profitsw2000.data.data.bluetooth

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Rule
import org.junit.Test
import ru.profitsw2000.data.MainDispatcherRule
import ru.profitsw2000.data.domain.bluetooth.BluetoothDataRepository
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothDataRepositoryImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `тест, что функция чтения забирает нужные данные и закрывает поток при исключении`() = runTest {
        val mockInputStream: InputStream = mockk()

        every { mockInputStream.read(any()) } answers {
            val destinationBuffer = firstArg<ByteArray>()
            destinationBuffer[0] = 0x23.toByte()
            destinationBuffer[1] = 0x45.toByte()
            destinationBuffer[2] = 0xAA.toByte()
            destinationBuffer[3] = 0x48.toByte()
            destinationBuffer[4] = 0xB8.toByte()
            destinationBuffer[5] = 0x00.toByte()
            destinationBuffer[6] = 0x00.toByte()
            4
        } andThenThrows IOException("Connection reset by peer")

        val repository = BluetoothDataRepositoryImpl(
            socket = mockk(relaxed = true),
            ioDispatcher = mainDispatcherRule.testDispatcher
        )

        repository.readData(mockInputStream).test {
            val actualPacket = awaitItem()

            assertArrayEquals(byteArrayOf(0x23, 0x45, 0xAA.toByte(), 0x48), actualPacket)

            awaitComplete()
        }
    }

    @Test
    fun `тест, что данные не отправляются, если сокет не подключён`() = runTest {
        val mockSocket: android.bluetooth.BluetoothSocket = mockk()
        val mockOutputStream: OutputStream = mockk(relaxed = true)

        every { mockSocket.isConnected } returns false
        every { mockSocket.outputStream } returns mockOutputStream

        val repository = BluetoothDataRepositoryImpl(
            socket = mockSocket,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )

        repository.writeData(byteArrayOf(0x01, 0x02, 0x9E.toByte()))

        verify(exactly = 0) { mockOutputStream.write(any<ByteArray>()) }
    }
}