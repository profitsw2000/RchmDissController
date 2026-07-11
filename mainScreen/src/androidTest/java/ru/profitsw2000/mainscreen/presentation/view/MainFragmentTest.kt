package ru.profitsw2000.mainscreen.presentation.view

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.test.KoinTest
import ru.profitsw2000.core.R
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissStateModel
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainViewModel
import ru.profitsw2000.navigator.Navigator

class MainFragmentTest : KoinTest {

    private val mockViewModel: MainViewModel = mockk(relaxed = true)
    private val mockNavigator: Navigator = mockk(relaxed = true)

    private val fakeRchmDissStateModelFlow = MutableStateFlow(RchmDissStateModel())
    private val fakeIsReceivedOutputControlPacket = MutableStateFlow(false)
    private val fakeClickActionSharedFlow = MutableSharedFlow<Int>(0)

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
}