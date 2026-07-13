package ru.profitsw2000.mainscreen.presentation.view.dialogs

import androidx.fragment.app.testing.launchFragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import ru.profitsw2000.core.R
import ru.profitsw2000.data.model.bluetooth.state.rcd.OutputModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.mainscreen.presentation.view.bottomsheet.TransmitterBottomSheetDialogFragment
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

    @Test
    fun установившееся_состояние_включён_3_канал_выкл_прд(): Unit = runBlocking {
        val idleState = TransmitterUpdatingStatus.Idle(
            TransmitterModuleState(
                enabledChannelNumber = 3
            ),
            OutputModuleState(
                transmitterIsOn = false
            )
        )
        fakeStatusFlow.emit(idleState)

        launchFragment<TransmitterBottomSheetDialogFragment>(themeResId = ru.profitsw2000.core.R.style.Theme_RchmDissController)

        onView(withId(ru.profitsw2000.mainscreen.R.id.first_channel_selection_chip))
            .check(matches(not(isChecked())))
        onView(withId(ru.profitsw2000.mainscreen.R.id.second_channel_selection_chip))
            .check(matches(not(isChecked())))
        onView(withId(ru.profitsw2000.mainscreen.R.id.third_channel_selection_chip))
            .check(matches(isChecked()))
        onView(withId(ru.profitsw2000.mainscreen.R.id.fourth_channel_selection_chip))
            .check(matches(not(isChecked())))
        onView(withId(ru.profitsw2000.mainscreen.R.id.fifth_channel_selection_chip))
            .check(matches(not(isChecked())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.switch_transmitter_on_check_box))
            .check(matches(not(isChecked())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.updating_status_result_text_view))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun установившееся_состояние_все_каналы_выкл_вкл_прд(): Unit = runBlocking {
        val idleState = TransmitterUpdatingStatus.Idle(
            TransmitterModuleState(
                enabledChannelNumber = 0
            ),
            OutputModuleState(
                transmitterIsOn = true
            )
        )
        fakeStatusFlow.emit(idleState)

        launchFragment<TransmitterBottomSheetDialogFragment>(themeResId = ru.profitsw2000.core.R.style.Theme_RchmDissController)

        onView(withId(ru.profitsw2000.mainscreen.R.id.first_channel_selection_chip))
            .check(matches(not(isChecked())))
        onView(withId(ru.profitsw2000.mainscreen.R.id.second_channel_selection_chip))
            .check(matches(not(isChecked())))
        onView(withId(ru.profitsw2000.mainscreen.R.id.third_channel_selection_chip))
            .check(matches(not(isChecked())))
        onView(withId(ru.profitsw2000.mainscreen.R.id.fourth_channel_selection_chip))
            .check(matches(not(isChecked())))
        onView(withId(ru.profitsw2000.mainscreen.R.id.fifth_channel_selection_chip))
            .check(matches(not(isChecked())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.switch_transmitter_on_check_box))
            .check(matches(isChecked()))

        onView(withId(ru.profitsw2000.mainscreen.R.id.updating_status_result_text_view))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun вызов_функции_обновить_вью_модели_при_нажатии_кнопки_отправить_кан_1_вкл_прд() = runBlocking() {
        launchFragment<TransmitterBottomSheetDialogFragment>(themeResId = R.style.Theme_RchmDissController)

        Thread.sleep(400)
        onView(withId(ru.profitsw2000.mainscreen.R.id.first_channel_selection_chip)).perform(click())
        onView(withId(ru.profitsw2000.mainscreen.R.id.switch_transmitter_on_check_box)).perform(click())
        onView(withId(ru.profitsw2000.mainscreen.R.id.transmitter_params_send_button)).perform(click())

        verify(exactly = 1) {
            mockViewModel.updateTransmitter(0x20.toByte(), turnTransmitterOn = true)
        }

    }

    @Test
    fun вызов_функции_обновить_вью_модели_при_нажатии_кнопки_отправить_кан_5_выкл_прд() = runBlocking() {
        launchFragment<TransmitterBottomSheetDialogFragment>(themeResId = R.style.Theme_RchmDissController)

        Thread.sleep(400)
        onView(withId(ru.profitsw2000.mainscreen.R.id.fifth_channel_selection_chip)).perform(click())
        onView(withId(ru.profitsw2000.mainscreen.R.id.transmitter_params_send_button)).perform(click())

        verify(exactly = 1) {
            mockViewModel.updateTransmitter(0x02.toByte(), turnTransmitterOn = false)
        }

    }
}