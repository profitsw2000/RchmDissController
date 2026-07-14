package ru.profitsw2000.mainscreen.presentation.view.dialogs

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.BoundedMatcher
import com.google.android.material.button.MaterialButton
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.hamcrest.Description
import org.junit.After
import org.junit.Before
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import ru.profitsw2000.core.R
import ru.profitsw2000.data.model.bluetooth.state.rcd.OutputModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
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

}