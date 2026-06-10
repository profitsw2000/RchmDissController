package ru.profitsw2000.mainscreen.presentation.view

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import ru.profitsw2000.core.drawable.RfChannelNumberIconView
import ru.profitsw2000.core.drawable.utils.RX_CHANNEL_1
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_1
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_2
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_3
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_4
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_5
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissState
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissStateModel
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.mainscreen.R
import ru.profitsw2000.mainscreen.databinding.FragmentMainBinding
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainViewModel
import ru.profitsw2000.navigator.Navigator

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val navigator: Navigator by inject()
    private val mainViewModel: MainViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMainBinding.bind(inflater.inflate(R.layout.fragment_main, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() = with(binding) {
        transmitterConstraintLayout.setOnClickListener {
            navigator.navigateToTransmitterSettingsDialog()
        }
        receiverConstraintLayout.setOnClickListener {
            navigator.navigateToReceiverSettingsDialog()
        }
        synthesizerConstraintLayout.setOnClickListener {
            navigator.navigateToSynthesizerSettingsDialog()
        }
    }

    private fun observeFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.rchmDissState.collect { state ->
                    renderData(rchmDissStateModel = state)
                }
            }
        }
    }

    private fun renderData(rchmDissStateModel: RchmDissStateModel) {
        renderTransmitterData(rchmDissStateModel.transmitterModuleState, rchmDissStateModel.isActualTransmitterData)
        renderReceiverData(rchmDissStateModel.receiverModuleState, rchmDissStateModel.isActualReceiverData)
        renderSynthesizerData(rchmDissStateModel.synthesizerModuleState, rchmDissStateModel.isActualSynthesizerData)
    }

    private fun renderTransmitterData(
        transmitterModuleState: TransmitterModuleState,
        isActualData: Boolean
    ) = with(binding) {
        disableAllChannels(
            arrayListOf(
                txFirstChannelIconView,
                txSecondChannelIconView,
                txThirdChannelIconView,
                txFourthChannelIconView,
                txFifthChannelIconView
            )
        )
        transmitterConstraintLayout.alpha = if (isActualData) 1f
        else 0.5f

        when(transmitterModuleState.enabledChannelNumber) {
            1 -> txFirstChannelIconView.setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            2 -> txSecondChannelIconView.setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            3 -> txThirdChannelIconView.setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            4 -> txFourthChannelIconView.setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            5 -> txFifthChannelIconView.setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            else -> {}
        }
    }

    private fun renderReceiverData(
        receiverModuleState: ReceiverModuleState,
        isActualData: Boolean
    ) = with(binding) {
        disableAllChannels(
            arrayListOf(
                rxFirstChannelIconView,
                rxSecondChannelIconView,
                rxThirdChannelIconView,
                rxFourthChannelIconView,
                rxFifthChannelIconView
            )
        )
        transmitterConstraintLayout.alpha = if (isActualData) 1f
        else 0.5f

        when(receiverModuleState.enabledChannelNumber) {
            1 -> rxFirstChannelIconView.setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            2 -> rxSecondChannelIconView.setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            3 -> rxThirdChannelIconView.setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            4 -> rxFourthChannelIconView.setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            5 -> rxFifthChannelIconView.setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            else -> {}
        }
    }

    private fun renderSynthesizerData(
        synthesizerModuleStateModel: SynthesizerModuleStateModel,
        isActualData: Boolean
    ) {

    }

    private fun disableAllChannels(channelsList: List<RfChannelNumberIconView>) {
        channelsList.forEach { channel ->
            channel.setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        }
    }

    private fun getActiveIndicatorColor(isActualData: Boolean): Int {
        return if (isActualData) requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary)
        else requireContext().getThemeColor(com.google.android.material.R.attr.colorSecondaryContainer)
    }

    @ColorInt
    fun Context.getThemeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }
}


/*private fun List<View>.setInteractions(enabled: Boolean) {
    forEach {
        it.isEnabled = enabled
        it.alpha = if (enabled) 1f else 0.5f // визуальный отклик
    }
}

// Теперь в коде это выглядит очень просто:
val myLayouts = listOf(binding.layout1, binding.layout2, binding.layout3)

// Когда началось подключение:
myLayouts.setInteractions(false)

// Когда подключение завершилось:
myLayouts.setInteractions(true)
Используйте код с осторожностью.

Важный момент:
Если вы используете Coroutines (корутины) для подключения, не забудьте поместить включение обратно в блок finally. Это гарантирует, что лэйауты разблокируются, даже если произойдет ошибка сети:
kotlin
viewLifecycleOwner.lifecycleScope.launch {
    try {
        myLayouts.setInteractions(false)
        // Логика подключения...
    } catch (e: Exception) {
        // Обработка ошибки
    } finally {
        // Выполнится в любом случае (успех или ошибка)
        myLayouts.setInteractions(true)
    }
}
Используйте код с осторожностью.

Вы используете Coroutines или Callbacks (слушатели) для обработки процесса подключения?*/




