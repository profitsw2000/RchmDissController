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
import ru.profitsw2000.core.drawable.utils.RCHM_DISS_OUTPUT_CONTROL_PACKET_ID
import ru.profitsw2000.core.drawable.utils.READ_FROM_RECEIVER_PACKET_ID
import ru.profitsw2000.core.drawable.utils.READ_FROM_SYNTHESIZER_PACKET_ID
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
    fun `получаем мусор со стартовым байтом посередине, а потом нормальный пакет - проверяем запуск функций`() = runTest(testDispatcher) {
        val fakeBytesFlow = MutableSharedFlow<ByteArray>()
        io.mockk.every { bluetoothRepository.bluetoothBytesDataFlow } returns fakeBytesFlow
        manager.observeBluetoothDataBytesFlow()

        val garbageTransmitter = byteArrayOf(
            0xDF.toByte(), 0xAD.toByte(), 0x00, 0x15, TRANSMITTER_CONTROL_BIT_MASK,
            WRITE_TO_TRANSMITTER_PACKET_ID.toByte(), 0x78, 0x23,
            PACKET_START_BYTE.toByte(), 0x33, 0xFF.toByte(), 0x54, 0x18,
            0xAD.toByte(), 0x99.toByte(), 0x89.toByte(),
            PACKET_START_BYTE.toByte(), 0x05,
            READ_FROM_TRANSMITTER_PACKET_ID.toByte(), 0x08, 0x11
        )

        val garbageReceiver = byteArrayOf(
            0xDF.toByte(), 0xAD.toByte(), 0x00, 0x15, TRANSMITTER_CONTROL_BIT_MASK,
            WRITE_TO_TRANSMITTER_PACKET_ID.toByte(), 0x78, 0x23, 0x5F,
            PACKET_START_BYTE.toByte(), 0x33, 0xFF.toByte(), 0x54, 0x18,
            0xAD.toByte(), 0x99.toByte(), 0x89.toByte(),
            PACKET_START_BYTE.toByte(), 0x06,
            READ_FROM_RECEIVER_PACKET_ID.toByte(), 0xBC.toByte(), 0x3D, 0x05
        )

        val garbageSynthesizer = byteArrayOf(
            0xDF.toByte(), 0xAD.toByte(), 0x00, 0x15, TRANSMITTER_CONTROL_BIT_MASK,
            WRITE_TO_TRANSMITTER_PACKET_ID.toByte(), 0x78, 0x23,
            PACKET_START_BYTE.toByte(), 0x33, 0xFF.toByte(), 0x54, 0x18,
            0xAD.toByte(), 0x99.toByte(), 0x89.toByte(),
            PACKET_START_BYTE.toByte(), 0x07,
            READ_FROM_SYNTHESIZER_PACKET_ID.toByte(), 0x20, 0x00, 0xA5.toByte(), 0xD1.toByte()
        )

        val garbageOutput = byteArrayOf(
            0xDF.toByte(), 0xAD.toByte(), 0x00, 0x15, TRANSMITTER_CONTROL_BIT_MASK,
            WRITE_TO_TRANSMITTER_PACKET_ID.toByte(), 0x78, 0x23,
            PACKET_START_BYTE.toByte(), 0x33, 0xFF.toByte(), 0x54, 0x18,
            0xAD.toByte(), 0x99.toByte(), 0x89.toByte(),
            PACKET_START_BYTE.toByte(), 0x09,
            RCHM_DISS_OUTPUT_CONTROL_PACKET_ID.toByte(),
            0x07, 0x04, 0x17, 0x5, 0xBA.toByte(), 0xF1.toByte()
        )

        fakeBytesFlow.emit(garbageTransmitter)
        verify(exactly = 1) {
            rchmDissStateRepository.updateTransmitterModuleState(0x08)
        }

        fakeBytesFlow.emit(garbageReceiver)
        verify(exactly = 1) {
            rchmDissStateRepository.updateReceiverModuleState( 0x3D, 0xBC.toByte(),)
        }

        fakeBytesFlow.emit(garbageSynthesizer)
        verify(exactly = 1) {
            rchmDissStateRepository.updateSynthesizerModuleState(0xA5.toByte(), 0x00, 0x20)
        }

        fakeBytesFlow.emit(garbageOutput)
        verify(exactly = 1) {
            rchmDissStateRepository.updateOutputModuleState(
                byteArrayOf(0x07, 0x04, 0x17, 0x5, 0xBA.toByte())
            )
        }
    }

    @Test
    fun `получаем пакет с неверной контрольной суммой - проверяем отсутствие запуска функций`() = runTest(testDispatcher) {
        val fakeBytesFlow = MutableSharedFlow<ByteArray>()
        io.mockk.every { bluetoothRepository.bluetoothBytesDataFlow } returns fakeBytesFlow
        manager.observeBluetoothDataBytesFlow()

        val transmitter = byteArrayOf(
            PACKET_START_BYTE.toByte(), 0x05,
            READ_FROM_TRANSMITTER_PACKET_ID.toByte(), 0x08, 0x12
        )

        val receiver = byteArrayOf(
            PACKET_START_BYTE.toByte(), 0x06,
            READ_FROM_RECEIVER_PACKET_ID.toByte(), 0xBC.toByte(), 0x3D, 0x55
        )

        val synthesizer = byteArrayOf(
            PACKET_START_BYTE.toByte(), 0x07,
            READ_FROM_SYNTHESIZER_PACKET_ID.toByte(), 0x20, 0x00, 0xA5.toByte(), 0xD2.toByte()
        )

        val output = byteArrayOf(
            PACKET_START_BYTE.toByte(), 0x09,
            RCHM_DISS_OUTPUT_CONTROL_PACKET_ID.toByte(),
            0x07, 0x04, 0x17, 0x5, 0xBA.toByte(), 0x44
        )

        fakeBytesFlow.emit(transmitter)
        verify{ rchmDissStateRepository wasNot Called }

        fakeBytesFlow.emit(receiver)
        verify{ rchmDissStateRepository wasNot Called }

        fakeBytesFlow.emit(synthesizer)
        verify{ rchmDissStateRepository wasNot Called }

        fakeBytesFlow.emit(output)
        verify{ rchmDissStateRepository wasNot Called }
    }

    @Test
    fun `получаем пакет с неверным количеством данных - проверяем отсутствие запуска функций`() = runTest(testDispatcher) {
        val fakeBytesFlow = MutableSharedFlow<ByteArray>()
        io.mockk.every { bluetoothRepository.bluetoothBytesDataFlow } returns fakeBytesFlow
        manager.observeBluetoothDataBytesFlow()

        val transmitter = byteArrayOf(
            PACKET_START_BYTE.toByte(), 0x05,
            READ_FROM_TRANSMITTER_PACKET_ID.toByte(), 0x08, 0xD5.toByte(), 0xE6.toByte()
        )

        val receiver = byteArrayOf(
            PACKET_START_BYTE.toByte(), 0x06,
            READ_FROM_RECEIVER_PACKET_ID.toByte(), 0xBC.toByte(), 0x23, 0x3D, 0x28
        )

        val synthesizer = byteArrayOf(
            PACKET_START_BYTE.toByte(), 0x07,
            READ_FROM_SYNTHESIZER_PACKET_ID.toByte(), 0x20, 0xA5.toByte(), 0xD1.toByte()
        )

        val output = byteArrayOf(
            PACKET_START_BYTE.toByte(), 0x09,
            RCHM_DISS_OUTPUT_CONTROL_PACKET_ID.toByte(),
            0x07, 0x5, 0xBA.toByte(), 0xD6.toByte()
        )

        fakeBytesFlow.emit(transmitter)
        verify{ rchmDissStateRepository wasNot Called }

        fakeBytesFlow.emit(receiver)
        verify{ rchmDissStateRepository wasNot Called }

        fakeBytesFlow.emit(synthesizer)
        verify{ rchmDissStateRepository wasNot Called }

        fakeBytesFlow.emit(output)
        verify{ rchmDissStateRepository wasNot Called }
    }

    @Test
    fun `получаем мусор и нормальный пакет передатчика, приходящие по частям - проверяем запуск функций`() = runTest(testDispatcher) {
        val fakeBytesFlow = MutableSharedFlow<ByteArray>()
        io.mockk.every { bluetoothRepository.bluetoothBytesDataFlow } returns fakeBytesFlow
        manager.observeBluetoothDataBytesFlow()

        val garbageTransmitter1 = byteArrayOf(
            0xDF.toByte(), 0xAD.toByte(), 0x00, 0x15, TRANSMITTER_CONTROL_BIT_MASK,
            WRITE_TO_TRANSMITTER_PACKET_ID.toByte(), 0x78, 0x23,
            PACKET_START_BYTE.toByte(), 0x33, 0xFF.toByte(), 0x54, 0x18
        )

        fakeBytesFlow.emit(garbageTransmitter1)
        verify { rchmDissStateRepository wasNot Called }

        val garbageTransmitter2 = byteArrayOf(
            0xAD.toByte(), 0x99.toByte(), 0x89.toByte(),
            PACKET_START_BYTE.toByte(), 0x05
        )

        fakeBytesFlow.emit(garbageTransmitter2)
        verify { rchmDissStateRepository wasNot Called }

        val garbageTransmitter3 = byteArrayOf(
            READ_FROM_TRANSMITTER_PACKET_ID.toByte(), 0x08, 0x11
        )

        fakeBytesFlow.emit(garbageTransmitter3)
        verify(exactly = 1) {
            rchmDissStateRepository.updateTransmitterModuleState(0x08)
        }

    }

    @Test
    fun `получаем мусор и нормальный пакет приёмника, приходящие по частям - проверяем запуск функций`() = runTest(testDispatcher) {
        val fakeBytesFlow = MutableSharedFlow<ByteArray>()
        io.mockk.every { bluetoothRepository.bluetoothBytesDataFlow } returns fakeBytesFlow
        manager.observeBluetoothDataBytesFlow()

        val receiver1 = byteArrayOf(
            0xDF.toByte(), 0xAD.toByte(), 0x00, 0x15, TRANSMITTER_CONTROL_BIT_MASK,
            WRITE_TO_TRANSMITTER_PACKET_ID.toByte(), 0x78, 0x23, 0x5F
        )

        fakeBytesFlow.emit(receiver1)
        verify { rchmDissStateRepository wasNot Called }

        val receiver2 = byteArrayOf(
            PACKET_START_BYTE.toByte(), 0x33, 0xFF.toByte(), 0x54, 0x18,
            0xAD.toByte(), 0x99.toByte(), 0x89.toByte(),
            PACKET_START_BYTE.toByte()
        )

        fakeBytesFlow.emit(receiver2)
        verify { rchmDissStateRepository wasNot Called }

        val receiver3 = byteArrayOf(
            0x06, READ_FROM_RECEIVER_PACKET_ID.toByte(), 0xBC.toByte(), 0x3D, 0x05
        )

        fakeBytesFlow.emit(receiver3)
        verify(exactly = 1) {
            rchmDissStateRepository.updateReceiverModuleState(0x3D, 0xBC.toByte())
        }
    }

    @Test
    fun `получаем мусор и нормальный пакет синтезатора, приходящие по частям - проверяем запуск функций`() = runTest(testDispatcher) {
        val fakeBytesFlow = MutableSharedFlow<ByteArray>()
        io.mockk.every { bluetoothRepository.bluetoothBytesDataFlow } returns fakeBytesFlow
        manager.observeBluetoothDataBytesFlow()

        val garbageSynthesizer1 = byteArrayOf(
            0xDF.toByte(), 0xAD.toByte(), 0x00, 0x15, TRANSMITTER_CONTROL_BIT_MASK
        )

        fakeBytesFlow.emit(garbageSynthesizer1)
        verify { rchmDissStateRepository wasNot Called }

        val garbageSynthesizer2 = byteArrayOf(
            WRITE_TO_TRANSMITTER_PACKET_ID.toByte(), 0x78, 0x23,
            PACKET_START_BYTE.toByte(), 0x33, 0xFF.toByte(), 0x54, 0x18
        )

        fakeBytesFlow.emit(garbageSynthesizer2)
        verify { rchmDissStateRepository wasNot Called }

        val garbageSynthesizer3 = byteArrayOf(
            0xAD.toByte(), 0x99.toByte(), 0x89.toByte(),
            PACKET_START_BYTE.toByte(), 0x07
        )

        fakeBytesFlow.emit(garbageSynthesizer3)
        verify { rchmDissStateRepository wasNot Called }

        val garbageSynthesizer4 = byteArrayOf(
            READ_FROM_SYNTHESIZER_PACKET_ID.toByte(), 0x20, 0x00, 0xA5.toByte(), 0xD1.toByte()
        )

        fakeBytesFlow.emit(garbageSynthesizer4)
        verify(exactly = 1) {
            rchmDissStateRepository.updateSynthesizerModuleState(0xA5.toByte(), 0x00, 0x20)
        }
    }

    @Test
    fun `получаем мусор и нормальный пакет контроля выводов, приходящие по частям - проверяем запуск функций`() = runTest(testDispatcher) {
        val fakeBytesFlow = MutableSharedFlow<ByteArray>()
        io.mockk.every { bluetoothRepository.bluetoothBytesDataFlow } returns fakeBytesFlow
        manager.observeBluetoothDataBytesFlow()

        val garbageOutput1 = byteArrayOf(
            0xDF.toByte(), 0xAD.toByte(), 0x00, 0x15, TRANSMITTER_CONTROL_BIT_MASK,
            WRITE_TO_TRANSMITTER_PACKET_ID.toByte(), 0x78, 0x23,
            PACKET_START_BYTE.toByte(), 0x33, 0xFF.toByte(), 0x54, 0x18
        )

        fakeBytesFlow.emit(garbageOutput1)
        verify { rchmDissStateRepository wasNot Called }

        val garbageOutput2 = byteArrayOf(
            0xAD.toByte(), 0x99.toByte(), 0x89.toByte(),
            PACKET_START_BYTE.toByte(), 0x09,
            RCHM_DISS_OUTPUT_CONTROL_PACKET_ID.toByte(),
            0x07, 0x04, 0x17, 0x5, 0xBA.toByte(), 0xF1.toByte()
        )

        fakeBytesFlow.emit(garbageOutput2)
        verify(exactly = 1) {
            rchmDissStateRepository.updateOutputModuleState(
                byteArrayOf(0x07, 0x04, 0x17, 0x5, 0xBA.toByte())
            )
        }
    }
}