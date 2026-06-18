package ru.profitsw2000.data.data.bluetooth

import com.google.common.truth.Truth.assertThat
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.profitsw2000.core.drawable.utils.PACKET_START_BYTE
import ru.profitsw2000.core.drawable.utils.READ_FROM_TRANSMITTER_PACKET_ID
import ru.profitsw2000.core.drawable.utils.TRANSMITTER_CONTROL_BIT_MASK
import ru.profitsw2000.core.drawable.utils.WRITE_TO_TRANSMITTER_PACKET_ID
import ru.profitsw2000.core.drawable.utils.WRITE_TO_TRANSMITTER_PACKET_SIZE
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository

class BluetoothPacketManagerImplTest {

    private val bluetoothRepository: BluetoothRepository = mockk(relaxed = true)
    private val rchmDissStateRepository: RchmDissStateRepository = mockk(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var manager: BluetoothPacketManagerImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        manager = BluetoothPacketManagerImpl(bluetoothRepository, rchmDissStateRepository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearMocks(bluetoothRepository, rchmDissStateRepository)
    }

    @Test
    fun `получаем мусор и проверяем, что ни одна функция записи не запустилась`() = runTest(testDispatcher) {
        val fakeBytesFlow = MutableSharedFlow<ByteArray>()
        io.mockk.every { bluetoothRepository.bluetoothBytesDataFlow } returns fakeBytesFlow
        manager.observeBluetoothDataBytesFlow()

        val garbage = byteArrayOf(
            0xDF.toByte(), 0xAD.toByte(), 0x00, 0x15, TRANSMITTER_CONTROL_BIT_MASK,
            WRITE_TO_TRANSMITTER_PACKET_ID.toByte(), 0x78, 0x23
        )

        fakeBytesFlow.emit(garbage)
        testDispatcher.scheduler.advanceUntilIdle()

        verify{ rchmDissStateRepository wasNot Called }
    }

    @Test
    fun `получаем мусор со стартовым байтом посередине и проверяем, что ни одна функция записи не запустилась`() = runTest(testDispatcher) {
        val fakeBytesFlow = MutableSharedFlow<ByteArray>()
        io.mockk.every { bluetoothRepository.bluetoothBytesDataFlow } returns fakeBytesFlow
        manager.observeBluetoothDataBytesFlow()

        val garbage = byteArrayOf(
            0xDF.toByte(), 0xAD.toByte(), 0x00, 0x15, TRANSMITTER_CONTROL_BIT_MASK,
            WRITE_TO_TRANSMITTER_PACKET_ID.toByte(), 0x78, 0x23,
            PACKET_START_BYTE.toByte(), 0x33, 0xFF.toByte(), 0x54, 0x18,
            0xAD.toByte(), 0x99.toByte(), 0x89.toByte()
        )

        fakeBytesFlow.emit(garbage)
        testDispatcher.scheduler.advanceUntilIdle()

        verify{ rchmDissStateRepository wasNot Called }
    }

    @Test
    fun `получаем мусор со стартовым байтом посередине, а потом пакет передатчика - проверяем запуск функции`() = runTest(testDispatcher) {
        val fakeBytesFlow = MutableSharedFlow<ByteArray>()
        io.mockk.every { bluetoothRepository.bluetoothBytesDataFlow } returns fakeBytesFlow
        manager.observeBluetoothDataBytesFlow()

        val garbage = byteArrayOf(
            0xDF.toByte(), 0xAD.toByte(), 0x00, 0x15, TRANSMITTER_CONTROL_BIT_MASK,
            WRITE_TO_TRANSMITTER_PACKET_ID.toByte(), 0x78, 0x23,
            PACKET_START_BYTE.toByte(), 0x33, 0xFF.toByte(), 0x54, 0x18,
            0xAD.toByte(), 0x99.toByte(), 0x89.toByte(),
            PACKET_START_BYTE.toByte(), 0x05,
            READ_FROM_TRANSMITTER_PACKET_ID.toByte(), 0x08, 0x11
        )

        fakeBytesFlow.emit(garbage)
        verify(exactly = 1) {
            rchmDissStateRepository.updateTransmitterModuleState(0x08)
        }
        assertThat(manager.packetCheckSum).isEqualTo(0x11)
    }

}