package ru.profitsw2000.mainscreen.presentation.view.dialogs

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.testing.launchFragment
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.android.material.button.MaterialButton
import io.mockk.every
import io.mockk.mockk
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
import ru.profitsw2000.data.model.bluetooth.state.rcd.OutputModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.mainscreen.databinding.FragmentReceiverBottomSheetDialogBinding
import ru.profitsw2000.mainscreen.presentation.view.bottomsheet.ReceiverBottomSheetDialogFragment
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
}