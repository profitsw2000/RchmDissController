package ru.profitsw2000.mainscreen.presentation.view

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import io.mockk.Matcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Description
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.test.KoinTest
import ru.profitsw2000.core.R
import ru.profitsw2000.core.drawable.RfChannelNumberIconView
import ru.profitsw2000.core.drawable.RfStateIndicatorIconView
import ru.profitsw2000.core.drawable.utils.ATTENUATOR_8_DECIBELS_BIT
import ru.profitsw2000.data.model.bluetooth.state.rcd.OutputModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.RadiationMode
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissStateModel
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainViewModel
import ru.profitsw2000.navigator.Navigator
import kotlin.getValue

class MainFragmentTest : KoinTest {

    private val mockViewModel: MainViewModel = mockk(relaxed = true)
    private val mockNavigator: Navigator = mockk(relaxed = true)

    private val fakeRchmDissStateModelFlow = MutableStateFlow(RchmDissStateModel())
    private val fakeIsReceivedOutputControlPacket = MutableStateFlow(false)
    private val fakeClickActionSharedFlow = MutableSharedFlow<Int>(0)
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>().apply {
        setTheme(R.style.Base_Theme_RchmDissController)
    }

    private val activeColor by lazy {
        context.getThemeColor(com.google.android.material.R.attr.colorOnSurface)
    }

    private val inactiveColor by lazy {
        context.getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
    }

    @Before
    fun setUp() {
        every { mockViewModel.rchmDissStateModelFlow } returns fakeRchmDissStateModelFlow
        every { mockViewModel.isReceivedOutputControlPacket } returns fakeIsReceivedOutputControlPacket
        every { mockViewModel.clickActionSharedFlow } returns fakeClickActionSharedFlow

        startKoin {
            modules(module {
                viewModel { mockViewModel }
                single<Navigator> { mockNavigator }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    fun withRfChannelIconColor(expectedColor: Int): BoundedMatcher<View, RfChannelNumberIconView> {
        return object : BoundedMatcher<View, RfChannelNumberIconView>(RfChannelNumberIconView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with icon color: $expectedColor")
            }

            override fun matchesSafely(item: RfChannelNumberIconView): Boolean {
                return item.rfChannelIconColor == expectedColor
            }
        }
    }

    fun withRfStateIndicatorColor(expectedColor: Int): BoundedMatcher<View, RfStateIndicatorIconView> {
        return object : BoundedMatcher<View, RfStateIndicatorIconView>(RfStateIndicatorIconView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with icon color: $expectedColor")
            }

            override fun matchesSafely(item: RfStateIndicatorIconView): Boolean {
                return item.rfStateIconColor == expectedColor
            }
        }
    }

    fun withRfStateIndicatorText(expectedText: String): BoundedMatcher<View, RfStateIndicatorIconView> {
        return object : BoundedMatcher<View, RfStateIndicatorIconView>(RfStateIndicatorIconView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with indicator text: $expectedText")
            }

            override fun matchesSafely(item: RfStateIndicatorIconView): Boolean {
                return item.rfStateText == expectedText
            }
        }
    }

    fun withRfChannelLockCrossVisibility(isVisible: Boolean): BoundedMatcher<View, RfChannelNumberIconView> {
        return object : BoundedMatcher<View, RfChannelNumberIconView>(RfChannelNumberIconView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with cross is visible: $isVisible")
            }

            override fun matchesSafely(item: RfChannelNumberIconView): Boolean {
                return item.showCross == isVisible
            }
        }
    }

    fun withAttenuationValueText(valueText: String): BoundedMatcher<View, TextView> {
        return object : BoundedMatcher<View, TextView>(TextView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with attenuation value text: $valueText")
            }

            override fun matchesSafely(item: TextView): Boolean {
                return item.text == valueText
            }
        }
    }

    fun withImageTint(expectedColor: Int): BoundedMatcher<View, ImageView> {
        return object : BoundedMatcher<View, ImageView>(ImageView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with image tint color: $expectedColor")
            }

            override fun matchesSafely(item: ImageView): Boolean {
                val tintList = item.imageTintList
                if (tintList != null) {
                    return tintList.defaultColor == expectedColor
                }
                return item.tag == expectedColor
            }
        }
    }

    @ColorInt
    fun Context.getThemeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    @Test
    fun запуск_настройки_передатчика_при_клике_на_него() = runBlocking {
        launchFragmentInContainer<MainFragment>(themeResId = R.style.Theme_RchmDissController)

        onView(withId(ru.profitsw2000.mainscreen.R.id.transmitter_constraint_layout)).perform(click())
        verify(exactly = 1) {mockViewModel.layoutClicked(1)}

        fakeClickActionSharedFlow.emit(1)
        verify(exactly = 1) {mockNavigator.navigateToTransmitterSettingsDialog()}
    }

    @Test
    fun запуск_настройки_приёмника_при_клике_на_него() = runBlocking {
        launchFragmentInContainer<MainFragment>(themeResId = R.style.Theme_RchmDissController)

        onView(withId(ru.profitsw2000.mainscreen.R.id.receiver_constraint_layout)).perform(click())
        verify(exactly = 1) {mockViewModel.layoutClicked(2)}

        fakeClickActionSharedFlow.emit(2)
        verify(exactly = 1) {mockNavigator.navigateToReceiverSettingsDialog()}
    }

    @Test
    fun запуск_настройки_синтезатора_при_клике_на_него() = runBlocking {
        launchFragmentInContainer<MainFragment>(themeResId = R.style.Theme_RchmDissController)

        onView(withId(ru.profitsw2000.mainscreen.R.id.synthesizer_constraint_layout)).perform(click())
        verify(exactly = 1) {mockViewModel.layoutClicked(3)}

        fakeClickActionSharedFlow.emit(3)
        verify(exactly = 1) {mockNavigator.navigateToSynthesizerSettingsDialog()}
    }

    @Test
    fun активен_канал_3_передатчика_выкл_прд(): Unit = runBlocking {
        launchFragmentInContainer<MainFragment>(themeResId = R.style.Theme_RchmDissController)
        val testState = RchmDissStateModel(
            transmitterModuleState = TransmitterModuleState(enabledChannelNumber = 3),
            outputModuleState = OutputModuleState(
                transmitterIsOn = false
            )
        )
        fakeRchmDissStateModelFlow.emit(testState)

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_third_channel_icon_view))
            .check(matches(withRfChannelIconColor(activeColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_first_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_second_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_fourth_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_fifth_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.transmitter_state_icon_view))
            .check(matches(withRfStateIndicatorColor(inactiveColor)))
    }

    @Test
    fun неактивны_все_каналы_передатчика_вкл_прд(): Unit = runBlocking {
        launchFragmentInContainer<MainFragment>(themeResId = R.style.Theme_RchmDissController)
        val testState = RchmDissStateModel(
            transmitterModuleState = TransmitterModuleState(enabledChannelNumber = 0)
        )
        fakeRchmDissStateModelFlow.emit(testState)

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_third_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_first_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_second_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_fourth_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_fifth_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.transmitter_state_icon_view))
            .check(matches(withRfStateIndicatorColor(activeColor)))
    }

    @Test
    fun вкл_канал_5_прм_запирание_канала_3_и_4_вкл_пилот_сигнал_затухание_12_дБ(): Unit = runBlocking {
        launchFragmentInContainer<MainFragment>(themeResId = R.style.Theme_RchmDissController)
        val testState = RchmDissStateModel(
            receiverModuleState = ReceiverModuleState(
                enabledChannelNumber = 5,
                testSignalIsEnabled = true,
                lockedInputChannels = booleanArrayOf(false, false, true, true, false),
                inputAttenuationValue = 12,
                inputAttenuatorsCode = 0x81
            )
        )
        fakeRchmDissStateModelFlow.emit(testState)

        //Цвет иконки
        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_first_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_second_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_third_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_fourth_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_fifth_channel_icon_view))
            .check(matches(withRfChannelIconColor(activeColor)))

        //Наличие креста у иконки
        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_first_channel_icon_view))
            .check(matches(withRfChannelLockCrossVisibility(false)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_second_channel_icon_view))
            .check(matches(withRfChannelLockCrossVisibility(false)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_third_channel_icon_view))
            .check(matches(withRfChannelLockCrossVisibility(true)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_fourth_channel_icon_view))
            .check(matches(withRfChannelLockCrossVisibility(true)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_fifth_channel_icon_view))
            .check(matches(withRfChannelLockCrossVisibility(false)))

        //Значение затухания
        onView(withId(ru.profitsw2000.mainscreen.R.id.attenuation_value_text_view))
            .check(matches(withAttenuationValueText("12 дБ")))

        //Пилот-сигнал
        onView(withId(ru.profitsw2000.mainscreen.R.id.receiver_test_signal_state_icon_view))
            .check(matches(withRfStateIndicatorColor(activeColor)))

    }

    @Test
    fun выкл_все_каналы_прм_запирание_канала_1_2_3_и_4_выкл_пилот_сигнал_затухание_56_дБ(): Unit = runBlocking {
        launchFragmentInContainer<MainFragment>(themeResId = R.style.Theme_RchmDissController)
        val testState = RchmDissStateModel(
            receiverModuleState = ReceiverModuleState(
                enabledChannelNumber = 0,
                testSignalIsEnabled = false,
                lockedInputChannels = booleanArrayOf(true, true, true, true, false),
                inputAttenuationValue = 56,
                inputAttenuatorsCode = 0x81
            )
        )
        fakeRchmDissStateModelFlow.emit(testState)

        //Цвет иконки
        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_first_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_second_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_third_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_fourth_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_fifth_channel_icon_view))
            .check(matches(withRfChannelIconColor(inactiveColor)))

        //Наличие креста у иконки
        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_first_channel_icon_view))
            .check(matches(withRfChannelLockCrossVisibility(true)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_second_channel_icon_view))
            .check(matches(withRfChannelLockCrossVisibility(true)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_third_channel_icon_view))
            .check(matches(withRfChannelLockCrossVisibility(true)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_fourth_channel_icon_view))
            .check(matches(withRfChannelLockCrossVisibility(true)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.rx_fifth_channel_icon_view))
            .check(matches(withRfChannelLockCrossVisibility(false)))

        //Значение затухания
        onView(withId(ru.profitsw2000.mainscreen.R.id.attenuation_value_text_view))
            .check(matches(withAttenuationValueText("56 дБ")))

        //Пилот-сигнал
        onView(withId(ru.profitsw2000.mainscreen.R.id.receiver_test_signal_state_icon_view))
            .check(matches(withRfStateIndicatorColor(inactiveColor)))

    }

    @Test
    fun синтезатор_ничего_не_генерит(): Unit = runBlocking {
        launchFragmentInContainer<MainFragment>(themeResId = R.style.Theme_RchmDissController)
        val testState = RchmDissStateModel(
            synthesizerModuleState = SynthesizerModuleStateModel(
                radiationMode = RadiationMode.NONE
            )
        )
        fakeRchmDissStateModelFlow.emit(testState)

        onView(withId(ru.profitsw2000.mainscreen.R.id.lfm_external_trigger_state_image_view))
            .check(matches(not(isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.lfm_swing_type_icon_view))
            .check(matches(not(isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.period_value_text_view))
            .check(matches(not(isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.synthesizer_mode_icon_view))
            .check(matches(withRfStateIndicatorText("Н/А")))

        onView(withId(ru.profitsw2000.mainscreen.R.id.frequency_value_text_view))
            .check(matches(withText("Нет генерации сигнала")))
    }

    @Test
    fun синт_режим_нг_частота_13325_мгц(): Unit = runBlocking {
        launchFragmentInContainer<MainFragment>(themeResId = R.style.Theme_RchmDissController)
        val testState = RchmDissStateModel(
            synthesizerModuleState = SynthesizerModuleStateModel(
                radiationMode = RadiationMode.CW,
                cwFrequency = 13_325_000_000
            )
        )
        fakeRchmDissStateModelFlow.emit(testState)

        onView(withId(ru.profitsw2000.mainscreen.R.id.lfm_external_trigger_state_image_view))
            .check(matches(not(isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.lfm_swing_type_icon_view))
            .check(matches(not(isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.period_value_text_view))
            .check(matches(not(isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.synthesizer_mode_icon_view))
            .check(matches(withRfStateIndicatorText("НГ")))

        onView(withId(ru.profitsw2000.mainscreen.R.id.frequency_value_text_view))
            .check(matches(withText("13325 МГц")))
    }

    @Test
    fun синт_режим_лчм_частота_13260_13330_мгц_период_6_мс_симметричная_внутр_запуск(): Unit = runBlocking {
        launchFragmentInContainer<MainFragment>(themeResId = R.style.Theme_RchmDissController)
        val testState = RchmDissStateModel(
            synthesizerModuleState = SynthesizerModuleStateModel(
                radiationMode = RadiationMode.LFM,
                lowestLfmFrequency = 13_260_000_000,
                highestLfmFrequency = 13_330_000_000,
                lfmPeriod = 0.006,
                isSymmetricLfm = true
            )
        )
        fakeRchmDissStateModelFlow.emit(testState)

        onView(withId(ru.profitsw2000.mainscreen.R.id.synthesizer_mode_icon_view))
            .check(matches((isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.frequency_value_text_view))
            .check(matches((isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.lfm_swing_type_icon_view))
            .check(matches((isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.period_value_text_view))
            .check(matches((isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.lfm_external_trigger_state_image_view))
            .check(matches((isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.synthesizer_mode_icon_view))
            .check(matches(withRfStateIndicatorText("ЛЧМ")))

        onView(withId(ru.profitsw2000.mainscreen.R.id.frequency_value_text_view))
            .check(matches(withText("13260 - 13330 МГц")))

        onView(withId(ru.profitsw2000.mainscreen.R.id.lfm_swing_type_icon_view))
            .check(matches(withRfStateIndicatorText("СИМ")))

        onView(withId(ru.profitsw2000.mainscreen.R.id.period_value_text_view))
            .check(matches(withText("6.0 мс")))

        onView(withId(ru.profitsw2000.mainscreen.R.id.lfm_external_trigger_state_image_view))
            .check(matches(withImageTint(inactiveColor)))
    }

    @Test
    fun синт_режим_лчм_частота_13300_13370_мгц_период_0_6_мс_несимметричная_внеш_запуск(): Unit = runBlocking {
        launchFragmentInContainer<MainFragment>(themeResId = R.style.Theme_RchmDissController)
        val testState = RchmDissStateModel(
            synthesizerModuleState = SynthesizerModuleStateModel(
                radiationMode = RadiationMode.LFM,
                lowestLfmFrequency = 13_300_000_000,
                highestLfmFrequency = 13_370_000_000,
                lfmPeriod = 0.0006,
                isSymmetricLfm = false
            ),
            outputModuleState = OutputModuleState(
                lfmExtTriggerIsOn = true
            )
        )
        fakeRchmDissStateModelFlow.emit(testState)

        onView(withId(ru.profitsw2000.mainscreen.R.id.synthesizer_mode_icon_view))
            .check(matches((isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.frequency_value_text_view))
            .check(matches((isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.lfm_swing_type_icon_view))
            .check(matches((isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.period_value_text_view))
            .check(matches((isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.lfm_external_trigger_state_image_view))
            .check(matches((isDisplayed())))

        onView(withId(ru.profitsw2000.mainscreen.R.id.synthesizer_mode_icon_view))
            .check(matches(withRfStateIndicatorText("ЛЧМ")))

        onView(withId(ru.profitsw2000.mainscreen.R.id.frequency_value_text_view))
            .check(matches(withText("13300 - 13370 МГц")))

        onView(withId(ru.profitsw2000.mainscreen.R.id.lfm_swing_type_icon_view))
            .check(matches(withRfStateIndicatorText("НСМ")))

        onView(withId(ru.profitsw2000.mainscreen.R.id.period_value_text_view))
            .check(matches(withText("0.6 мс")))

        onView(withId(ru.profitsw2000.mainscreen.R.id.lfm_external_trigger_state_image_view))
            .check(matches(withImageTint(activeColor)))
    }
}