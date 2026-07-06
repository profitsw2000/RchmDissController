package ru.profitsw2000.mainscreen.presentation.viewmodel.dialogs

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class SynthesizerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val rchmDissStateRepository: RchmDissStateRepository = mockk(relaxed = true)
    private val bluetoothRepository: BluetoothRepository = mockk(relaxed = true)
    private val bluetoothPacketManager: BluetoothPacketManager = mockk(relaxed = true)


}