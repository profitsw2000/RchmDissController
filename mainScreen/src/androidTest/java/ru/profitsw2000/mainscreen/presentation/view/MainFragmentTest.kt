package ru.profitsw2000.mainscreen.presentation.view

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.mockk.Matcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
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
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissStateModel
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

    fun withIconColor(expectedColor: Int): BoundedMatcher<View, RfChannelNumberIconView> {
        return object : BoundedMatcher<View, RfChannelNumberIconView>(RfChannelNumberIconView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with icon color: $expectedColor")
            }

            override fun matchesSafely(item: RfChannelNumberIconView): Boolean {
                return item.rfChannelIconColor == expectedColor
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
    fun активен_канал_3_передатчика(): Unit = runBlocking {
        launchFragmentInContainer<MainFragment>(themeResId = R.style.Theme_RchmDissController)
        val testState = RchmDissStateModel(
            transmitterModuleState = TransmitterModuleState(enabledChannelNumber = 3)
        )
        fakeRchmDissStateModelFlow.emit(testState)

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_third_channel_icon_view))
            .check(matches(withIconColor(activeColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_first_channel_icon_view))
            .check(matches(withIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_second_channel_icon_view))
            .check(matches(withIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_fourth_channel_icon_view))
            .check(matches(withIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_fifth_channel_icon_view))
            .check(matches(withIconColor(inactiveColor)))
    }

    @Test
    fun неактивны_все_каналы_передатчика(): Unit = runBlocking {
        launchFragmentInContainer<MainFragment>(themeResId = R.style.Theme_RchmDissController)
        val testState = RchmDissStateModel(
            transmitterModuleState = TransmitterModuleState(enabledChannelNumber = 0)
        )
        fakeRchmDissStateModelFlow.emit(testState)

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_third_channel_icon_view))
            .check(matches(withIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_first_channel_icon_view))
            .check(matches(withIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_second_channel_icon_view))
            .check(matches(withIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_fourth_channel_icon_view))
            .check(matches(withIconColor(inactiveColor)))

        onView(withId(ru.profitsw2000.mainscreen.R.id.tx_fifth_channel_icon_view))
            .check(matches(withIconColor(inactiveColor)))
    }
}