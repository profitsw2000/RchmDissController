package ru.profitsw2000.mainscreen.presentation.view.dialogs

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.testing.FragmentScenario
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
import com.google.android.material.textfield.TextInputLayout
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
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
import ru.profitsw2000.core.drawable.utils.CW_FREQUENCY_UNDER_INPUT_ERROR
import ru.profitsw2000.data.model.bluetooth.state.rcd.OutputModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.RadiationMode
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel
import ru.profitsw2000.mainscreen.databinding.FragmentSynthesizerBottomSheetDialogBinding
import ru.profitsw2000.mainscreen.presentation.view.bottomsheet.ReceiverBottomSheetDialogFragment
import ru.profitsw2000.mainscreen.presentation.view.bottomsheet.SynthesizerBottomSheetDialogFragment
import ru.profitsw2000.mainscreen.presentation.viewmodel.dialogs.ReceiverViewModel
import ru.profitsw2000.mainscreen.presentation.viewmodel.dialogs.SynthesizerViewModel
import ru.profitsw2000.mainscreen.state.ReceiverUpdatingStatus
import ru.profitsw2000.mainscreen.state.SynthesizerUpdatingStatus

class SynthesizerBottomSheetDialogFragmentTest : KoinTest {

    private val mockViewModel: SynthesizerViewModel = mockk(relaxed = true)
    private val fakeInitialState = SynthesizerUpdatingStatus.Idle(
        synthesizerModuleStateModel = SynthesizerModuleStateModel(),
        outputModuleState = OutputModuleState()
    )
    private val fakeStatusFlow = MutableStateFlow<SynthesizerUpdatingStatus>(fakeInitialState)
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

    fun hasTextInputLayoutError(expectedErrorText: String): BoundedMatcher<View, TextInputLayout> {
        return object : BoundedMatcher<View, TextInputLayout>(TextInputLayout::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with error text: $expectedErrorText")
            }

            override fun matchesSafely(textInputLayout: TextInputLayout): Boolean {
                val error = textInputLayout.error ?: return false
                return error.contains(expectedErrorText)//expectedErrorText == error.toString()
            }
        }
    }

    @Before
    fun setUp() {
        // Связываем мок ViewModel с нашим фейковым потоком
        every { mockViewModel.synthesizerUpdatingStatusFlow } returns fakeStatusFlow

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
    fun устоявшееся_значение_нет_генерации(): Unit = runBlocking {
        val idleState = SynthesizerUpdatingStatus.Idle(
            SynthesizerModuleStateModel(),
            OutputModuleState()
        )
        fakeStatusFlow.emit(idleState)

        val scenario = launchFragment<SynthesizerBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )

        var cwModeRadioButtonId = 0
        var cwFrequencyTextInputLayoutId = 0
        var cwFrequencyTextInputEditTextId = 0
        var lfmModeRadioButtonId = 0
        var lfmLowFrequencyTextInputLayoutId = 0
        var lfmLowFrequencyTextInputEditTextId = 0
        var lfmHighFrequencyTextInputLayoutId = 0
        var lfmHighFrequencyTextInputEditTextId = 0
        var lfmPeriodTextInputLayoutId = 0
        var lfmPeriodTextInputEditTextId = 0
        var symmetricLfmCheckBoxId = 0
        var lfmExtTriggerSwitchCheckBoxId = 0
        var synthesizerParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentSynthesizerBottomSheetDialogBinding.bind(fragment.requireView())
            with(binding) {
                cwModeRadioButtonId = cwModeRadioButton.id
                cwFrequencyTextInputLayoutId = cwFrequencyTextInputLayout.id
                cwFrequencyTextInputEditTextId = cwFrequencyTextInputEditText.id
                lfmModeRadioButtonId = lfmModeRadioButton.id
                lfmLowFrequencyTextInputLayoutId = lfmLowFrequencyTextInputLayout.id
                lfmLowFrequencyTextInputEditTextId = lfmLowFrequencyTextInputEditText.id
                lfmHighFrequencyTextInputLayoutId = lfmHighFrequencyTextInputLayout.id
                lfmHighFrequencyTextInputEditTextId = lfmHighFrequencyTextInputEditText.id
                lfmPeriodTextInputLayoutId = lfmPeriodTextInputLayout.id
                lfmPeriodTextInputEditTextId = lfmPeriodTextInputEditText.id
                symmetricLfmCheckBoxId = symmetricLfmCheckBox.id
                lfmExtTriggerSwitchCheckBoxId = lfmExtTriggerSwitchCheckBox.id
                synthesizerParamsSendButtonId = synthesizerParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }

        onView(withId(cwModeRadioButtonId))
            .check(matches(isChecked()))

        onView(withId(cwFrequencyTextInputLayoutId))
            .check(matches(isDisplayed()))

        onView(withId(cwFrequencyTextInputEditTextId))
            .check(matches(withText("13325")))

        onView(withId(lfmModeRadioButtonId))
            .check(matches(not(isChecked())))

        onView(withId(lfmLowFrequencyTextInputLayoutId))
            .check(matches(not(isDisplayed())))

        onView(withId(lfmHighFrequencyTextInputLayoutId))
            .check(matches(not(isDisplayed())))

        onView(withId(lfmPeriodTextInputLayoutId))
            .check(matches(not(isDisplayed())))

        onView(withId(symmetricLfmCheckBoxId))
            .check(matches(not(isDisplayed())))

        onView(withId(lfmExtTriggerSwitchCheckBoxId))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun устоявшееся_значение_НГ_13300_МГц(): Unit = runBlocking {
        val idleState = SynthesizerUpdatingStatus.Idle(
            SynthesizerModuleStateModel(
                radiationMode = RadiationMode.CW,
                cwFrequency = 13_300_000_000
            ),
            OutputModuleState()
        )
        fakeStatusFlow.emit(idleState)

        val scenario = launchFragment<SynthesizerBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )

        var cwModeRadioButtonId = 0
        var cwFrequencyTextInputLayoutId = 0
        var cwFrequencyTextInputEditTextId = 0
        var lfmModeRadioButtonId = 0
        var lfmLowFrequencyTextInputLayoutId = 0
        var lfmLowFrequencyTextInputEditTextId = 0
        var lfmHighFrequencyTextInputLayoutId = 0
        var lfmHighFrequencyTextInputEditTextId = 0
        var lfmPeriodTextInputLayoutId = 0
        var lfmPeriodTextInputEditTextId = 0
        var symmetricLfmCheckBoxId = 0
        var lfmExtTriggerSwitchCheckBoxId = 0
        var synthesizerParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentSynthesizerBottomSheetDialogBinding.bind(fragment.requireView())
            with(binding) {
                cwModeRadioButtonId = cwModeRadioButton.id
                cwFrequencyTextInputLayoutId = cwFrequencyTextInputLayout.id
                cwFrequencyTextInputEditTextId = cwFrequencyTextInputEditText.id
                lfmModeRadioButtonId = lfmModeRadioButton.id
                lfmLowFrequencyTextInputLayoutId = lfmLowFrequencyTextInputLayout.id
                lfmLowFrequencyTextInputEditTextId = lfmLowFrequencyTextInputEditText.id
                lfmHighFrequencyTextInputLayoutId = lfmHighFrequencyTextInputLayout.id
                lfmHighFrequencyTextInputEditTextId = lfmHighFrequencyTextInputEditText.id
                lfmPeriodTextInputLayoutId = lfmPeriodTextInputLayout.id
                lfmPeriodTextInputEditTextId = lfmPeriodTextInputEditText.id
                symmetricLfmCheckBoxId = symmetricLfmCheckBox.id
                lfmExtTriggerSwitchCheckBoxId = lfmExtTriggerSwitchCheckBox.id
                synthesizerParamsSendButtonId = synthesizerParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }

        onView(withId(cwModeRadioButtonId))
            .check(matches(isChecked()))

        onView(withId(cwFrequencyTextInputLayoutId))
            .check(matches(isDisplayed()))

        onView(withId(cwFrequencyTextInputEditTextId))
            .check(matches(withText("13300")))

        onView(withId(lfmModeRadioButtonId))
            .check(matches(not(isChecked())))

        onView(withId(lfmLowFrequencyTextInputLayoutId))
            .check(matches(not(isDisplayed())))

        onView(withId(lfmHighFrequencyTextInputLayoutId))
            .check(matches(not(isDisplayed())))

        onView(withId(lfmPeriodTextInputLayoutId))
            .check(matches(not(isDisplayed())))

        onView(withId(symmetricLfmCheckBoxId))
            .check(matches(not(isDisplayed())))

        onView(withId(lfmExtTriggerSwitchCheckBoxId))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun устоявшееся_значение_ЛЧМ_13280_13355_МГц_30_мс_НСМ_внутр_запуск(): Unit = runBlocking {
        val idleState = SynthesizerUpdatingStatus.Idle(
            SynthesizerModuleStateModel(
                radiationMode = RadiationMode.LFM,
                lowestLfmFrequency = 13_280_000_000,
                highestLfmFrequency = 13_355_000_000,
                lfmPeriod = 0.03,
                isSymmetricLfm = false
            ),
            OutputModuleState()
        )
        fakeStatusFlow.emit(idleState)

        val scenario = launchFragment<SynthesizerBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )

        var cwModeRadioButtonId = 0
        var cwFrequencyTextInputLayoutId = 0
        var cwFrequencyTextInputEditTextId = 0
        var lfmModeRadioButtonId = 0
        var lfmLowFrequencyTextInputLayoutId = 0
        var lfmLowFrequencyTextInputEditTextId = 0
        var lfmHighFrequencyTextInputLayoutId = 0
        var lfmHighFrequencyTextInputEditTextId = 0
        var lfmPeriodTextInputLayoutId = 0
        var lfmPeriodTextInputEditTextId = 0
        var symmetricLfmCheckBoxId = 0
        var lfmExtTriggerSwitchCheckBoxId = 0
        var synthesizerParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentSynthesizerBottomSheetDialogBinding.bind(fragment.requireView())
            with(binding) {
                cwModeRadioButtonId = cwModeRadioButton.id
                cwFrequencyTextInputLayoutId = cwFrequencyTextInputLayout.id
                cwFrequencyTextInputEditTextId = cwFrequencyTextInputEditText.id
                lfmModeRadioButtonId = lfmModeRadioButton.id
                lfmLowFrequencyTextInputLayoutId = lfmLowFrequencyTextInputLayout.id
                lfmLowFrequencyTextInputEditTextId = lfmLowFrequencyTextInputEditText.id
                lfmHighFrequencyTextInputLayoutId = lfmHighFrequencyTextInputLayout.id
                lfmHighFrequencyTextInputEditTextId = lfmHighFrequencyTextInputEditText.id
                lfmPeriodTextInputLayoutId = lfmPeriodTextInputLayout.id
                lfmPeriodTextInputEditTextId = lfmPeriodTextInputEditText.id
                symmetricLfmCheckBoxId = symmetricLfmCheckBox.id
                lfmExtTriggerSwitchCheckBoxId = lfmExtTriggerSwitchCheckBox.id
                synthesizerParamsSendButtonId = synthesizerParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }

        onView(withId(cwModeRadioButtonId))
            .check(matches(not(isChecked())))

        onView(withId(cwFrequencyTextInputLayoutId))
            .check(matches(not(isDisplayed())))

        onView(withId(lfmModeRadioButtonId))
            .check(matches(isChecked()))

        onView(withId(lfmLowFrequencyTextInputLayoutId))
            .check(matches(isDisplayed()))

        onView(withId(lfmLowFrequencyTextInputEditTextId))
            .check(matches(withText("13280")))

        onView(withId(lfmHighFrequencyTextInputLayoutId))
            .check(matches(isDisplayed()))

        onView(withId(lfmHighFrequencyTextInputEditTextId))
            .check(matches(withText("13355")))

        onView(withId(lfmPeriodTextInputLayoutId))
            .check(matches(isDisplayed()))

        onView(withId(lfmPeriodTextInputEditTextId))
            .check(matches(withText("30")))

        onView(withId(symmetricLfmCheckBoxId))
            .check(matches(isDisplayed()))

        onView(withId(symmetricLfmCheckBoxId))
            .check(matches(not(isChecked())))

        onView(withId(lfmExtTriggerSwitchCheckBoxId))
            .check(matches(isDisplayed()))

        onView(withId(lfmExtTriggerSwitchCheckBoxId))
            .check(matches(not(isChecked())))
    }

    @Test
    fun устоявшееся_значение_ЛЧМ_13250_13400_МГц_0_5_мс_СМ_внеш_запуск(): Unit = runBlocking {
        val idleState = SynthesizerUpdatingStatus.Idle(
            SynthesizerModuleStateModel(
                radiationMode = RadiationMode.LFM,
                lowestLfmFrequency = 13_275_000_000,
                highestLfmFrequency = 13_388_000_000,
                lfmPeriod = 0.0005,
                isSymmetricLfm = true
            ),
            OutputModuleState(
                lfmExtTriggerIsOn = true
            )
        )
        fakeStatusFlow.emit(idleState)

        val scenario = launchFragment<SynthesizerBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )

        var cwModeRadioButtonId = 0
        var cwFrequencyTextInputLayoutId = 0
        var cwFrequencyTextInputEditTextId = 0
        var lfmModeRadioButtonId = 0
        var lfmLowFrequencyTextInputLayoutId = 0
        var lfmLowFrequencyTextInputEditTextId = 0
        var lfmHighFrequencyTextInputLayoutId = 0
        var lfmHighFrequencyTextInputEditTextId = 0
        var lfmPeriodTextInputLayoutId = 0
        var lfmPeriodTextInputEditTextId = 0
        var symmetricLfmCheckBoxId = 0
        var lfmExtTriggerSwitchCheckBoxId = 0
        var synthesizerParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentSynthesizerBottomSheetDialogBinding.bind(fragment.requireView())
            with(binding) {
                cwModeRadioButtonId = cwModeRadioButton.id
                cwFrequencyTextInputLayoutId = cwFrequencyTextInputLayout.id
                cwFrequencyTextInputEditTextId = cwFrequencyTextInputEditText.id
                lfmModeRadioButtonId = lfmModeRadioButton.id
                lfmLowFrequencyTextInputLayoutId = lfmLowFrequencyTextInputLayout.id
                lfmLowFrequencyTextInputEditTextId = lfmLowFrequencyTextInputEditText.id
                lfmHighFrequencyTextInputLayoutId = lfmHighFrequencyTextInputLayout.id
                lfmHighFrequencyTextInputEditTextId = lfmHighFrequencyTextInputEditText.id
                lfmPeriodTextInputLayoutId = lfmPeriodTextInputLayout.id
                lfmPeriodTextInputEditTextId = lfmPeriodTextInputEditText.id
                symmetricLfmCheckBoxId = symmetricLfmCheckBox.id
                lfmExtTriggerSwitchCheckBoxId = lfmExtTriggerSwitchCheckBox.id
                synthesizerParamsSendButtonId = synthesizerParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }

        onView(withId(cwModeRadioButtonId))
            .check(matches(not(isChecked())))

        onView(withId(cwFrequencyTextInputLayoutId))
            .check(matches(not(isDisplayed())))

        onView(withId(lfmModeRadioButtonId))
            .check(matches(isChecked()))

        onView(withId(lfmLowFrequencyTextInputLayoutId))
            .check(matches(isDisplayed()))

        onView(withId(lfmLowFrequencyTextInputEditTextId))
            .check(matches(withText("13275")))

        onView(withId(lfmHighFrequencyTextInputLayoutId))
            .check(matches(isDisplayed()))

        onView(withId(lfmHighFrequencyTextInputEditTextId))
            .check(matches(withText("13388")))

        onView(withId(lfmPeriodTextInputLayoutId))
            .check(matches(isDisplayed()))

        onView(withId(lfmPeriodTextInputEditTextId))
            .check(matches(withText("0.5")))

        onView(withId(symmetricLfmCheckBoxId))
            .check(matches(isDisplayed()))

        onView(withId(symmetricLfmCheckBoxId))
            .check(matches(isChecked()))

        onView(withId(lfmExtTriggerSwitchCheckBoxId))
            .check(matches(isDisplayed()))

        onView(withId(lfmExtTriggerSwitchCheckBoxId))
            .check(matches(isChecked()))
    }

    @Test
    fun нажато_отправить_НГ_13285_МГц(): Unit = runBlocking {
        val idleState = SynthesizerUpdatingStatus.Idle(
            SynthesizerModuleStateModel(
                radiationMode = RadiationMode.CW,
                cwFrequency = 13_285_000_000
            ),
            OutputModuleState()
        )
        fakeStatusFlow.emit(idleState)

        val scenario = launchFragment<SynthesizerBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )

        var cwModeRadioButtonId = 0
        var cwFrequencyTextInputLayoutId = 0
        var cwFrequencyTextInputEditTextId = 0
        var lfmModeRadioButtonId = 0
        var lfmLowFrequencyTextInputLayoutId = 0
        var lfmLowFrequencyTextInputEditTextId = 0
        var lfmHighFrequencyTextInputLayoutId = 0
        var lfmHighFrequencyTextInputEditTextId = 0
        var lfmPeriodTextInputLayoutId = 0
        var lfmPeriodTextInputEditTextId = 0
        var symmetricLfmCheckBoxId = 0
        var lfmExtTriggerSwitchCheckBoxId = 0
        var synthesizerParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentSynthesizerBottomSheetDialogBinding.bind(fragment.requireView())
            with(binding) {
                cwModeRadioButtonId = cwModeRadioButton.id
                cwFrequencyTextInputLayoutId = cwFrequencyTextInputLayout.id
                cwFrequencyTextInputEditTextId = cwFrequencyTextInputEditText.id
                lfmModeRadioButtonId = lfmModeRadioButton.id
                lfmLowFrequencyTextInputLayoutId = lfmLowFrequencyTextInputLayout.id
                lfmLowFrequencyTextInputEditTextId = lfmLowFrequencyTextInputEditText.id
                lfmHighFrequencyTextInputLayoutId = lfmHighFrequencyTextInputLayout.id
                lfmHighFrequencyTextInputEditTextId = lfmHighFrequencyTextInputEditText.id
                lfmPeriodTextInputLayoutId = lfmPeriodTextInputLayout.id
                lfmPeriodTextInputEditTextId = lfmPeriodTextInputEditText.id
                symmetricLfmCheckBoxId = symmetricLfmCheckBox.id
                lfmExtTriggerSwitchCheckBoxId = lfmExtTriggerSwitchCheckBox.id
                synthesizerParamsSendButtonId = synthesizerParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }

        Thread.sleep(400)

        onView(withId(synthesizerParamsSendButtonId))
            .perform(click())

        verify(exactly = 1) { mockViewModel.updateSynthesizerCwMode(13_285) }
    }

    @Test
    fun нажато_отправить_ЛЧМ_13285_13390_МГц_50_мс_СМ_внутр_зап(): Unit = runBlocking {
        val idleState = SynthesizerUpdatingStatus.Idle(
            SynthesizerModuleStateModel(
                radiationMode = RadiationMode.LFM,
                lowestLfmFrequency = 13_285_000_000,
                highestLfmFrequency = 13_390_000_000,
                lfmPeriod = 0.05,
                isSymmetricLfm = true
            ),
            OutputModuleState()
        )
        fakeStatusFlow.emit(idleState)

        val scenario = launchFragment<SynthesizerBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )

        var cwModeRadioButtonId = 0
        var cwFrequencyTextInputLayoutId = 0
        var cwFrequencyTextInputEditTextId = 0
        var lfmModeRadioButtonId = 0
        var lfmLowFrequencyTextInputLayoutId = 0
        var lfmLowFrequencyTextInputEditTextId = 0
        var lfmHighFrequencyTextInputLayoutId = 0
        var lfmHighFrequencyTextInputEditTextId = 0
        var lfmPeriodTextInputLayoutId = 0
        var lfmPeriodTextInputEditTextId = 0
        var symmetricLfmCheckBoxId = 0
        var lfmExtTriggerSwitchCheckBoxId = 0
        var synthesizerParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentSynthesizerBottomSheetDialogBinding.bind(fragment.requireView())
            with(binding) {
                cwModeRadioButtonId = cwModeRadioButton.id
                cwFrequencyTextInputLayoutId = cwFrequencyTextInputLayout.id
                cwFrequencyTextInputEditTextId = cwFrequencyTextInputEditText.id
                lfmModeRadioButtonId = lfmModeRadioButton.id
                lfmLowFrequencyTextInputLayoutId = lfmLowFrequencyTextInputLayout.id
                lfmLowFrequencyTextInputEditTextId = lfmLowFrequencyTextInputEditText.id
                lfmHighFrequencyTextInputLayoutId = lfmHighFrequencyTextInputLayout.id
                lfmHighFrequencyTextInputEditTextId = lfmHighFrequencyTextInputEditText.id
                lfmPeriodTextInputLayoutId = lfmPeriodTextInputLayout.id
                lfmPeriodTextInputEditTextId = lfmPeriodTextInputEditText.id
                symmetricLfmCheckBoxId = symmetricLfmCheckBox.id
                lfmExtTriggerSwitchCheckBoxId = lfmExtTriggerSwitchCheckBox.id
                synthesizerParamsSendButtonId = synthesizerParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }

        Thread.sleep(400)

        onView(withId(synthesizerParamsSendButtonId))
            .perform(click())

        verify(exactly = 1) {
            mockViewModel.updateSynthesizerLfmMode(
                startFrequency = 13_285,
                stopFrequency = 13_390,
                lfmPeriod = 50.0,
                isSymmetricLfm = true,
                isExtTriggerLfm = false
            )
        }
    }

    @Test
    fun нажато_отправить_ЛЧМ_13285_13315_МГц_1_мс_НСМ_внеш_зап(): Unit = runBlocking {
        val idleState = SynthesizerUpdatingStatus.Idle(
            SynthesizerModuleStateModel(
                radiationMode = RadiationMode.LFM,
                lowestLfmFrequency = 13_285_000_000,
                highestLfmFrequency = 13_315_000_000,
                lfmPeriod = 0.001,
                isSymmetricLfm = false
            ),
            OutputModuleState(
                lfmExtTriggerIsOn = true
            )
        )
        fakeStatusFlow.emit(idleState)

        val scenario = launchFragment<SynthesizerBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )
        var synthesizerParamsSendButtonId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentSynthesizerBottomSheetDialogBinding.bind(fragment.requireView())
            with(binding) {
                synthesizerParamsSendButtonId = synthesizerParamsSendButton.id
            }
        }

        Thread.sleep(400)

        onView(withId(synthesizerParamsSendButtonId))
            .perform(click())

        verify(exactly = 1) {
            mockViewModel.updateSynthesizerLfmMode(
                startFrequency = 13_285,
                stopFrequency = 13_315,
                lfmPeriod = 1.0,
                isSymmetricLfm = false,
                isExtTriggerLfm = true
            )
        }
    }

    @Test
    fun тест_элементов_отображения_в_состоянии_обновления(): Unit = runBlocking {
        val updateState = SynthesizerUpdatingStatus.Updating
        val scenario = launchFragment<SynthesizerBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )
        var synthesizerParamsSendButtonId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentSynthesizerBottomSheetDialogBinding.bind(fragment.requireView())
            with(binding) {
                synthesizerParamsSendButtonId = synthesizerParamsSendButton.id
            }
        }

        fakeStatusFlow.emit(updateState)

        onView(withId(synthesizerParamsSendButtonId))
            .check(matches(not(isEnabled())))

        onView(withId(synthesizerParamsSendButtonId))
            .check(matches(withText("")))

        onView(withId(synthesizerParamsSendButtonId))
            .check(matches(hasButtonIcon()))
    }

    @Test
    fun успешное_обновление_синтезатора(): Unit = runBlocking {
        val successState = SynthesizerUpdatingStatus.Success
        val scenario = launchFragment<SynthesizerBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )
        var synthesizerParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentSynthesizerBottomSheetDialogBinding.bind(fragment.requireView())
            with(binding) {
                synthesizerParamsSendButtonId = synthesizerParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }

        fakeStatusFlow.emit(successState)

        onView(withId(synthesizerParamsSendButtonId))
            .check(matches(not(isEnabled())))
        onView(withId(synthesizerParamsSendButtonId))
            .check(matches(withText("ОТПРАВИТЬ")))


        onView(withId(updatingStatusResultTextViewId))
            .check(matches(isDisplayed()))
        onView(withId(updatingStatusResultTextViewId))
            .check(matches(withTextColor(eucaliptusColor)))
        onView(withId(updatingStatusResultTextViewId))
            .check(matches(withText("Успешная отправка")))
    }

    @Test
    fun ошибка_ввода_в_поле_частоты_НГ(): Unit = runBlocking {
        val errorState = SynthesizerUpdatingStatus.Error(CW_FREQUENCY_UNDER_INPUT_ERROR)
        val scenario = launchFragment<SynthesizerBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )
        var cwFrequencyTextInputLayoutId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentSynthesizerBottomSheetDialogBinding.bind(fragment.requireView())
            with(binding) {
                cwFrequencyTextInputLayoutId = ru.profitsw2000.mainscreen.R.id.cw_frequency_text_input_layout
            }
        }

        fakeStatusFlow.emit(errorState)

        onView(withId(cwFrequencyTextInputLayoutId))
            .check(matches(hasTextInputLayoutError("Не менее 13250 МГц")))

    }

    @Test
    fun ошибка_ввода_в_поле_нижн_и_верхн_частоты_ЛЧМ(): Unit = runBlocking {
        val idleState = SynthesizerUpdatingStatus.Idle(
            synthesizerModuleStateModel = SynthesizerModuleStateModel(
                radiationMode = RadiationMode.LFM
            ),
            OutputModuleState()
        )
        val errorState = SynthesizerUpdatingStatus.Error(0xA)
        val scenario = launchFragment<SynthesizerBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )
        var lfmLowFrequencyTextInputLayoutId = 0
        var lfmHighFrequencyTextInputLayoutId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentSynthesizerBottomSheetDialogBinding.bind(fragment.requireView())
            with(binding) {
                lfmLowFrequencyTextInputLayoutId = lfmLowFrequencyTextInputLayout.id
                lfmHighFrequencyTextInputLayoutId = lfmHighFrequencyTextInputLayout.id
            }
        }

        fakeStatusFlow.emit(idleState)
        Thread.sleep(200)
        fakeStatusFlow.emit(errorState)
        Thread.sleep(200)

        onView(withId(lfmLowFrequencyTextInputLayoutId))
            .check(matches(hasTextInputLayoutError("Не более 13390 МГц")))
        onView(withId(lfmHighFrequencyTextInputLayoutId))
            .check(matches(hasTextInputLayoutError("Не менее 13260 МГц")))

    }

    @Test
    fun ошибка_ввода_в_поле_нижн_верхн_частоты_периода_ЛЧМ(): Unit = runBlocking {
        val idleState = SynthesizerUpdatingStatus.Idle(
            synthesizerModuleStateModel = SynthesizerModuleStateModel(
                radiationMode = RadiationMode.LFM
            ),
            OutputModuleState()
        )
        val errorState = SynthesizerUpdatingStatus.Error(0x31)
        val scenario = launchFragment<SynthesizerBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )
        var lfmLowFrequencyTextInputLayoutId = 0
        var lfmHighFrequencyTextInputLayoutId = 0
        var lfmPeriodTextInputLayoutId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentSynthesizerBottomSheetDialogBinding.bind(fragment.requireView())
            with(binding) {
                lfmLowFrequencyTextInputLayoutId = lfmLowFrequencyTextInputLayout.id
                lfmHighFrequencyTextInputLayoutId = lfmHighFrequencyTextInputLayout.id
                lfmPeriodTextInputLayoutId = lfmPeriodTextInputLayout.id
            }
        }

        fakeStatusFlow.emit(idleState)
        Thread.sleep(200)
        fakeStatusFlow.emit(errorState)
        Thread.sleep(200)

        onView(withId(lfmLowFrequencyTextInputLayoutId))
            .check(matches(hasTextInputLayoutError("Не менее 13250 МГц")))
        onView(withId(lfmHighFrequencyTextInputLayoutId))
            .check(matches(hasTextInputLayoutError("Не более 13400 МГц")))
        onView(withId(lfmPeriodTextInputLayoutId))
            .check(matches(hasTextInputLayoutError("Не более 100 мс")))

    }

    @Test
    fun ошибка_таймаута_приёма_ответного_пакета(): Unit = runBlocking {

        val errorState = SynthesizerUpdatingStatus.Error(0x80)
        val scenario = launchFragment<SynthesizerBottomSheetDialogFragment>(
            themeResId = R.style.Theme_RchmDissController
        )
        var synthesizerParamsSendButtonId = 0
        var updatingStatusResultTextViewId = 0

        scenario.onFragment { fragment ->
            val binding = FragmentSynthesizerBottomSheetDialogBinding.bind(fragment.requireView())
            with(binding) {
                synthesizerParamsSendButtonId = synthesizerParamsSendButton.id
                updatingStatusResultTextViewId = updatingStatusResultTextView.id
            }
        }

        fakeStatusFlow.emit(errorState)

        onView(withId(synthesizerParamsSendButtonId))
            .check(matches(isEnabled()))
        onView(withId(synthesizerParamsSendButtonId))
            .check(matches(withText("ОТПРАВИТЬ")))


        onView(withId(updatingStatusResultTextViewId))
            .check(matches(isDisplayed()))
        onView(withId(updatingStatusResultTextViewId))
            .check(matches(withTextColor(scarletColor)))
        onView(withId(updatingStatusResultTextViewId))
            .check(matches(withText("Ошибка приёма ответного байта данных")))
    }
}