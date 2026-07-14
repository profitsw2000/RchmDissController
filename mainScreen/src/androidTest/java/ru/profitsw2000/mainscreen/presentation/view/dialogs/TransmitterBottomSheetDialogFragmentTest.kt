package ru.profitsw2000.mainscreen.presentation.view.dialogs

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.testing.launchFragment
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.material.button.MaterialButton
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Description
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
    private val scarletColor by lazy {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ContextCompat.getColor(context, R.color.scarlet)
    }
    private val eucaliptusColor by lazy {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ContextCompat.getColor(context, R.color.eucaliptus)
    }

    fun hasButtonIcon(): BoundedMatcher<View, MaterialButton> {
        return object : BoundedMatcher<View, MaterialButton>(MaterialButton::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has a non-null icon drawable")
            }

            override fun matchesSafely(button: MaterialButton): Boolean {
                return button.icon != null
            }
        }
    }
    fun withTextColor(expectedColor: Int): BoundedMatcher<View, TextView> {
        return object : BoundedMatcher<View, TextView>(TextView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with text color: $expectedColor")
            }

            override fun matchesSafely(textView: TextView): Boolean {
                return textView.currentTextColor == expectedColor
            }
        }
    }

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

    @Test
    fun кнопка_заблокирована_текст_статуса_очищен_в_состоянии_обновления(): Unit = runBlocking {
        launchFragment<TransmitterBottomSheetDialogFragment>(themeResId = R.style.Theme_RchmDissController)

        fakeStatusFlow.emit(TransmitterUpdatingStatus.Updating)

        onView(withId(ru.profitsw2000.mainscreen.R.id.transmitter_params_send_button))
            .check(matches(not(isEnabled())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.transmitter_params_send_button))
            .check(matches(withText("")))

        onView(withId(ru.profitsw2000.mainscreen.R.id.transmitter_params_send_button))
            .check(matches(hasButtonIcon()))
    }

    @Test
    fun кнопка_разблокирована_текст_статуса_заполнен_в_состоянии_успешного_обновления(): Unit = runBlocking {
        launchFragment<TransmitterBottomSheetDialogFragment>(themeResId = R.style.Theme_RchmDissController)

        fakeStatusFlow.emit(TransmitterUpdatingStatus.Success)

        onView(withId(ru.profitsw2000.mainscreen.R.id.transmitter_params_send_button))
            .check(matches(isEnabled()))

        onView(withId(ru.profitsw2000.mainscreen.R.id.transmitter_params_send_button))
            .check(matches(withText("ОТПРАВИТЬ")))

        onView(withId(ru.profitsw2000.mainscreen.R.id.updating_status_result_text_view))
            .check(matches(isDisplayed()))

        onView(withId(ru.profitsw2000.mainscreen.R.id.updating_status_result_text_view))
            .check(matches(withTextColor(expectedColor = eucaliptusColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.updating_status_result_text_view))
            .check(matches(withText("Успешная отправка")))
    }
}