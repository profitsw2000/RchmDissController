package ru.profitsw2000.mainscreen.presentation.view.dialogs

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import ru.profitsw2000.data.model.bluetooth.state.rcd.OutputModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.mainscreen.presentation.viewmodel.dialogs.TransmitterViewModel
import ru.profitsw2000.mainscreen.state.TransmitterUpdatingStatus

class TransmitterBottomSheetDialogFragmentTest : KoinTest {

    private val mockViewModel: TransmitterViewModel = mockk(relaxed = true)

    private val fakeInitialState = TransmitterUpdatingStatus.Idle(
        transmitterModuleState = TransmitterModuleState(enabledChannelNumber = 0),
        outputModuleState = OutputModuleState(transmitterIsOn = false)
    )
    private val fakeStatusFlow = MutableStateFlow<TransmitterUpdatingStatus>(fakeInitialState)

    @Before
    fun setUp() {
        // Связываем мок ViewModel с нашим фейковым потоком
        every { mockViewModel.transmitterUpdatingStatusFlow } returns fakeStatusFlow

        // Регистрируем мок в Koin
        startKoin {
            modules(module {
                viewModel { mockViewModel }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }
}