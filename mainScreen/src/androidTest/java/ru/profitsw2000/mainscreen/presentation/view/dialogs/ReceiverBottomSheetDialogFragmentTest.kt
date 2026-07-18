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
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import ru.profitsw2000.core.R
import ru.profitsw2000.core.drawable.utils.RESPONSE_PACKET_TIMEOUT_ERROR_CODE
import ru.profitsw2000.core.drawable.utils.UNKNOWN_ERROR_CODE
import ru.profitsw2000.data.model.bluetooth.state.rcd.OutputModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.mainscreen.databinding.FragmentReceiverBottomSheetDialogBinding
import ru.profitsw2000.mainscreen.presentation.view.bottomsheet.ReceiverBottomSheetDialogFragment
import ru.profitsw2000.mainscreen.presentation.view.bottomsheet.TransmitterBottomSheetDialogFragment
import ru.profitsw2000.mainscreen.presentation.viewmodel.dialogs.ReceiverViewModel
import ru.profitsw2000.mainscreen.presentation.viewmodel.dialogs.TransmitterViewModel
import ru.profitsw2000.mainscreen.state.ReceiverUpdatingStatus
import ru.profitsw2000.mainscreen.state.TransmitterUpdatingStatus

class ReceiverBottomSheetDialogFragmentTest() : KoinTest {


    private val mockViewModel: ReceiverViewModel = mockk(relaxed = true)
    private val fakeInitialState = ReceiverUpdatingStatus.Idle(
        receiverModuleState = ReceiverModuleState()
    )
    private val fakeStatusFlow = MutableStateFlow<ReceiverUpdatingStatus>(fakeInitialState)
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
        every { mockViewModel.receiverUpdatingStatusFlow } returns fakeStatusFlow

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
    fun устоявшееся_состояние_вкл_канал_4_атт_4_8_32_дБ_зап_кан_2_4_пс_откл() : Unit = runBlocking {
        val idleState = ReceiverUpdatingStatus.Idle(
            ReceiverModuleState(
                enabledChannelNumber = 4,
                testSignalIsEnabled = false,
                lockedInputChannels = booleanArrayOf(false, true, false, true, false),
                inputAttenuationValue = 44,
                inputAttenuatorsCode = 0x281
            )
        )
        fakeStatusFlow.emit(idleState)

        val scenario = launchFragment<ReceiverBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )
        var firstChannelSelectionChipId = 0
        var secondChannelSelectionChipId = 0
        var thirdChannelSelectionChipId = 0
        var fourthChannelSelectionChipId = 0
        var fifthChannelSelectionChipId = 0

        var twoDecibelSelectionChipId = 0
        var fourDecibelSelectionChipId = 0
        var eightDecibelSelectionChipId = 0
        var sixteenDecibelSelectionChipId = 0
        var thirtyTwoDecibelSelectionChipId = 0

        var channel1LockSelectionChipId = 0
        var channel2LockSelectionChipId = 0
        var channel3LockSelectionChipId = 0
        var channel4LockSelectionChipId = 0
        var channel5LockSelectionChipId = 0

        var receiverTestSignalSwitchCheckBoxId = 0
        var transmitterParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentReceiverBottomSheetDialogBinding.bind(fragment.requireView())

            with(binding) {
                firstChannelSelectionChipId = firstChannelSelectionChip.id
                secondChannelSelectionChipId = secondChannelSelectionChip.id
                thirdChannelSelectionChipId = thirdChannelSelectionChip.id
                fourthChannelSelectionChipId = fourthChannelSelectionChip.id
                fifthChannelSelectionChipId = fifthChannelSelectionChip.id

                twoDecibelSelectionChipId = twoDecibelSelectionChip.id
                fourDecibelSelectionChipId = fourDecibelSelectionChip.id
                eightDecibelSelectionChipId = eightDecibelSelectionChip.id
                sixteenDecibelSelectionChipId = sixteenDecibelSelectionChip.id
                thirtyTwoDecibelSelectionChipId = thirtyTwoDecibelChip.id

                channel1LockSelectionChipId = channel1LockSelectionChip.id
                channel2LockSelectionChipId = channel2LockSelectionChip.id
                channel3LockSelectionChipId = channel3LockSelectionChip.id
                channel4LockSelectionChipId = channel4LockSelectionChip.id
                channel5LockSelectionChipId = channel5LockSelectionChip.id

                receiverTestSignalSwitchCheckBoxId = receiverTestSignalSwitchCheckBox.id
                transmitterParamsSendButtonId = transmitterParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }

        onView(withId(firstChannelSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(secondChannelSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(thirdChannelSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(fourthChannelSelectionChipId))
            .check(matches(isChecked()))
        onView(withId(fifthChannelSelectionChipId))
            .check(matches(not(isChecked())))

        onView(withId(twoDecibelSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(fourDecibelSelectionChipId))
            .check(matches(isChecked()))
        onView(withId(eightDecibelSelectionChipId))
            .check(matches(isChecked()))
        onView(withId(sixteenDecibelSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(thirtyTwoDecibelSelectionChipId))
            .check(matches(isChecked()))

        onView(withId(channel1LockSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(channel2LockSelectionChipId))
            .check(matches(isChecked()))
        onView(withId(channel3LockSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(channel4LockSelectionChipId))
            .check(matches(isChecked()))
        onView(withId(channel5LockSelectionChipId))
            .check(matches(not(isChecked())))

        onView(withId(receiverTestSignalSwitchCheckBoxId))
            .check(matches(not(isChecked())))

        onView(withId(updatingStatusResultTextViewId))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun устоявшееся_состояние_все_каналы_выкл_атт_2_4_8_дБ_зап_кан_1_пс_вкл() : Unit = runBlocking {
        val idleState = ReceiverUpdatingStatus.Idle(
            ReceiverModuleState(
                enabledChannelNumber = 0,
                testSignalIsEnabled = true,
                lockedInputChannels = booleanArrayOf(true, false, false, false, false),
                inputAttenuationValue = 14,
                inputAttenuatorsCode = 0xC1
            )
        )
        fakeStatusFlow.emit(idleState)

        val scenario = launchFragment<ReceiverBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )
        var firstChannelSelectionChipId = 0
        var secondChannelSelectionChipId = 0
        var thirdChannelSelectionChipId = 0
        var fourthChannelSelectionChipId = 0
        var fifthChannelSelectionChipId = 0

        var twoDecibelSelectionChipId = 0
        var fourDecibelSelectionChipId = 0
        var eightDecibelSelectionChipId = 0
        var sixteenDecibelSelectionChipId = 0
        var thirtyTwoDecibelSelectionChipId = 0

        var channel1LockSelectionChipId = 0
        var channel2LockSelectionChipId = 0
        var channel3LockSelectionChipId = 0
        var channel4LockSelectionChipId = 0
        var channel5LockSelectionChipId = 0

        var receiverTestSignalSwitchCheckBoxId = 0
        var transmitterParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentReceiverBottomSheetDialogBinding.bind(fragment.requireView())

            with(binding) {
                firstChannelSelectionChipId = firstChannelSelectionChip.id
                secondChannelSelectionChipId = secondChannelSelectionChip.id
                thirdChannelSelectionChipId = thirdChannelSelectionChip.id
                fourthChannelSelectionChipId = fourthChannelSelectionChip.id
                fifthChannelSelectionChipId = fifthChannelSelectionChip.id

                twoDecibelSelectionChipId = twoDecibelSelectionChip.id
                fourDecibelSelectionChipId = fourDecibelSelectionChip.id
                eightDecibelSelectionChipId = eightDecibelSelectionChip.id
                sixteenDecibelSelectionChipId = sixteenDecibelSelectionChip.id
                thirtyTwoDecibelSelectionChipId = thirtyTwoDecibelChip.id

                channel1LockSelectionChipId = channel1LockSelectionChip.id
                channel2LockSelectionChipId = channel2LockSelectionChip.id
                channel3LockSelectionChipId = channel3LockSelectionChip.id
                channel4LockSelectionChipId = channel4LockSelectionChip.id
                channel5LockSelectionChipId = channel5LockSelectionChip.id

                receiverTestSignalSwitchCheckBoxId = receiverTestSignalSwitchCheckBox.id
                transmitterParamsSendButtonId = transmitterParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }

        onView(withId(firstChannelSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(secondChannelSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(thirdChannelSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(fourthChannelSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(fifthChannelSelectionChipId))
            .check(matches(not(isChecked())))

        onView(withId(twoDecibelSelectionChipId))
            .check(matches(isChecked()))
        onView(withId(fourDecibelSelectionChipId))
            .check(matches(isChecked()))
        onView(withId(eightDecibelSelectionChipId))
            .check(matches(isChecked()))
        onView(withId(sixteenDecibelSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(thirtyTwoDecibelSelectionChipId))
            .check(matches(not(isChecked())))

        onView(withId(channel1LockSelectionChipId))
            .check(matches(isChecked()))
        onView(withId(channel2LockSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(channel3LockSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(channel4LockSelectionChipId))
            .check(matches(not(isChecked())))
        onView(withId(channel5LockSelectionChipId))
            .check(matches(not(isChecked())))

        onView(withId(receiverTestSignalSwitchCheckBoxId))
            .check(matches(isChecked()))

        onView(withId(updatingStatusResultTextViewId))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun нажата_кнопка_отправить_вкл_кан_4_прм_кан_3_4_5_заперты_вкл_атт_4_и_32_дБ_пс_выкл(): Unit = runBlocking {

        val scenario = launchFragment<ReceiverBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )
        var fourthChannelSelectionChipId = 0

        var twoDecibelSelectionChipId = 0
        var eightDecibelSelectionChipId = 0
        var sixteenDecibelSelectionChipId = 0

        var channel1LockSelectionChipId = 0
        var channel2LockSelectionChipId = 0

        var transmitterParamsSendButtonId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentReceiverBottomSheetDialogBinding.bind(fragment.requireView())

            with(binding) {
                fourthChannelSelectionChipId = fourthChannelSelectionChip.id

                twoDecibelSelectionChipId = twoDecibelSelectionChip.id
                eightDecibelSelectionChipId = eightDecibelSelectionChip.id
                sixteenDecibelSelectionChipId = sixteenDecibelSelectionChip.id

                channel1LockSelectionChipId = channel1LockSelectionChip.id
                channel2LockSelectionChipId = channel2LockSelectionChip.id
                transmitterParamsSendButtonId = transmitterParamsSendButton.id
            }
        }

        Thread.sleep(400)

        onView(withId(fourthChannelSelectionChipId))
            .perform(click())

        onView(withId(channel1LockSelectionChipId))
            .perform(click())
        onView(withId(channel2LockSelectionChipId))
            .perform(click())

        onView(withId(twoDecibelSelectionChipId))
            .perform(click())
        onView(withId(eightDecibelSelectionChipId))
            .perform(click())
        onView(withId(sixteenDecibelSelectionChipId))
            .perform(click())

        onView(withId(transmitterParamsSendButtonId))
            .perform(click())

        verify(exactly = 1) {
            mockViewModel.updateReceiver(
                byteArrayOf(0x76.toByte(), 0xF.toByte())
            )
        }
    }

    @Test
    fun нажата_кнопка_отправить_все_кан_выкл_прм_кан_2_заперт_вкл_атт_8_и_16_дБ_пс_вкл(): Unit = runBlocking {

        val scenario = launchFragment<ReceiverBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )
        var firstChannelSelectionChipId = 0
        var secondChannelSelectionChipId = 0
        var thirdChannelSelectionChipId = 0
        var fourthChannelSelectionChipId = 0
        var fifthChannelSelectionChipId = 0

        var twoDecibelSelectionChipId = 0
        var fourDecibelSelectionChipId = 0
        var eightDecibelSelectionChipId = 0
        var sixteenDecibelSelectionChipId = 0
        var thirtyTwoDecibelSelectionChipId = 0

        var channel1LockSelectionChipId = 0
        var channel2LockSelectionChipId = 0
        var channel3LockSelectionChipId = 0
        var channel4LockSelectionChipId = 0
        var channel5LockSelectionChipId = 0

        var receiverTestSignalSwitchCheckBoxId = 0
        var transmitterParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentReceiverBottomSheetDialogBinding.bind(fragment.requireView())

            with(binding) {
                firstChannelSelectionChipId = firstChannelSelectionChip.id
                secondChannelSelectionChipId = secondChannelSelectionChip.id
                thirdChannelSelectionChipId = thirdChannelSelectionChip.id
                fourthChannelSelectionChipId = fourthChannelSelectionChip.id
                fifthChannelSelectionChipId = fifthChannelSelectionChip.id

                twoDecibelSelectionChipId = twoDecibelSelectionChip.id
                fourDecibelSelectionChipId = fourDecibelSelectionChip.id
                eightDecibelSelectionChipId = eightDecibelSelectionChip.id
                sixteenDecibelSelectionChipId = sixteenDecibelSelectionChip.id
                thirtyTwoDecibelSelectionChipId = thirtyTwoDecibelChip.id

                channel1LockSelectionChipId = channel1LockSelectionChip.id
                channel2LockSelectionChipId = channel2LockSelectionChip.id
                channel3LockSelectionChipId = channel3LockSelectionChip.id
                channel4LockSelectionChipId = channel4LockSelectionChip.id
                channel5LockSelectionChipId = channel5LockSelectionChip.id

                receiverTestSignalSwitchCheckBoxId = receiverTestSignalSwitchCheckBox.id
                transmitterParamsSendButtonId = transmitterParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }

        Thread.sleep(400)

        onView(withId(channel1LockSelectionChipId))
            .perform(click())
        onView(withId(channel3LockSelectionChipId))
            .perform(click())
        onView(withId(channel4LockSelectionChipId))
            .perform(click())
        onView(withId(channel5LockSelectionChipId))
            .perform(click())

        onView(withId(twoDecibelSelectionChipId))
            .perform(click())
        onView(withId(fourDecibelSelectionChipId))
            .perform(click())
        onView(withId(thirtyTwoDecibelSelectionChipId))
            .perform(click())

        onView(withId(receiverTestSignalSwitchCheckBoxId))
            .perform(click())

        onView(withId(transmitterParamsSendButtonId))
            .perform(click())

        verify(exactly = 1) {
            mockViewModel.updateReceiver(
                byteArrayOf(0xFD.toByte(), 0x90.toByte())
            )
        }
    }

    @Test
    fun кнопка_заблокирована_текст_статуса_очищен_в_состоянии_обновления(): Unit = runBlocking {
        launchFragment<ReceiverBottomSheetDialogFragment>(themeResId = R.style.Theme_RchmDissController)

        fakeStatusFlow.emit(ReceiverUpdatingStatus.Updating)

        onView(withId(ru.profitsw2000.mainscreen.R.id.transmitter_params_send_button))
            .check(matches(CoreMatchers.not(isEnabled())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.transmitter_params_send_button))
            .check(matches(withText("")))

        onView(withId(ru.profitsw2000.mainscreen.R.id.transmitter_params_send_button))
            .check(matches(hasButtonIcon()))
    }

    @Test
    fun кнопка_заблокирована_текст_статуса_заполнен_в_состоянии_успешного_обновления(): Unit = runBlocking {

        val scenario = launchFragment<ReceiverBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )

        var transmitterParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentReceiverBottomSheetDialogBinding.bind(fragment.requireView())

            with(binding) {
                transmitterParamsSendButtonId = transmitterParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }
        fakeStatusFlow.emit(ReceiverUpdatingStatus.Updating)
        fakeStatusFlow.emit(ReceiverUpdatingStatus.Success)

        onView(withId(transmitterParamsSendButtonId))
            .check(matches(not(isEnabled())))

        onView(withId(transmitterParamsSendButtonId))
            .check(matches(withText("ОТПРАВИТЬ")))

        onView(withId(updatingStatusResultTextViewId))
            .check(matches(isDisplayed()))

        onView(withId(updatingStatusResultTextViewId))
            .check(matches(withTextColor(expectedColor = eucaliptusColor)))

        onView(withId(updatingStatusResultTextViewId))
            .check(matches(withText("Успешная отправка")))

    }

    @Test
    fun ошибка_по_таймауту_получения_ответного_пакета(): Unit = runBlocking {
        val scenario = launchFragment<ReceiverBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )

        var transmitterParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentReceiverBottomSheetDialogBinding.bind(fragment.requireView())

            with(binding) {
                transmitterParamsSendButtonId = transmitterParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }

        fakeStatusFlow.emit(ReceiverUpdatingStatus.Error(RESPONSE_PACKET_TIMEOUT_ERROR_CODE))

        onView(withId(transmitterParamsSendButtonId))
            .check(matches(isEnabled()))

        onView(withId(transmitterParamsSendButtonId))
            .check(matches(withText("ОТПРАВИТЬ")))

        onView(withId(updatingStatusResultTextViewId))
            .check(matches(isDisplayed()))

        onView(withId(updatingStatusResultTextViewId))
            .check(matches(withTextColor(expectedColor = scarletColor)))

        onView(withId(updatingStatusResultTextViewId))
            .check(matches(withText("Ошибка приёма ответного байта данных")))
    }

    @Test
    fun неизвестная_ошибка(): Unit = runBlocking {
        val scenario = launchFragment<ReceiverBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )

        var transmitterParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentReceiverBottomSheetDialogBinding.bind(fragment.requireView())

            with(binding) {
                transmitterParamsSendButtonId = transmitterParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }

        fakeStatusFlow.emit(ReceiverUpdatingStatus.Error(UNKNOWN_ERROR_CODE))

        onView(withId(transmitterParamsSendButtonId))
            .check(matches(isEnabled()))

        onView(withId(transmitterParamsSendButtonId))
            .check(matches(withText("ОТПРАВИТЬ")))

        onView(withId(updatingStatusResultTextViewId))
            .check(matches(isDisplayed()))

        onView(withId(updatingStatusResultTextViewId))
            .check(matches(withTextColor(expectedColor = scarletColor)))

        onView(withId(updatingStatusResultTextViewId))
            .check(matches(withText("Неизвестная ошибка")))

    }

}