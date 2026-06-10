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
import ru.profitsw2000.data.model.bluetooth.state.rcd.RadiationMode
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
        observeFlow()
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
        val channelsList = arrayListOf(
            txFirstChannelIconView,
            txSecondChannelIconView,
            txThirdChannelIconView,
            txFourthChannelIconView,
            txFifthChannelIconView
        )

        disableAllChannels(channelsList)
        highlightActiveChannel(
            transmitterModuleState.enabledChannelNumber,
            channelsList
        )
        setTransparencyToView(transmitterConstraintLayout, !isActualData)
    }

    private fun renderReceiverData(
        receiverModuleState: ReceiverModuleState,
        isActualData: Boolean
    ) = with(binding) {
        val channelList = arrayListOf(
            rxFirstChannelIconView,
            rxSecondChannelIconView,
            rxThirdChannelIconView,
            rxFourthChannelIconView,
            rxFifthChannelIconView
        )

        disableAllChannels(channelList)
        highlightActiveChannel(receiverModuleState.enabledChannelNumber, channelList)
        setAttenuatorValue(receiverModuleState.inputAttenuationValue)
        setCrossToLockedChannels(channelList, receiverModuleState.lockedInputChannels)
        setTestSignalIndicator(receiverModuleState.testSignalIsEnabled)
        setTransparencyToView(receiverConstraintLayout, !isActualData)
    }

    private fun renderSynthesizerData(
        synthesizerModuleStateModel: SynthesizerModuleStateModel,
        isActualData: Boolean
    ) {
        when(synthesizerModuleStateModel.radiationMode) {
            RadiationMode.NONE -> indicateSynthesizerNoneRadiationMode()
            RadiationMode.CW -> indicateSynthesizerCwRadiationMode(synthesizerModuleStateModel.cwFrequency)
            RadiationMode.LFM -> indicateSynthesizerLfmRadiationMode(
                lowFrequency = synthesizerModuleStateModel.lowestLfmFrequency,
                highFrequency = synthesizerModuleStateModel.highestLfmFrequency,
                lfmPeriod = synthesizerModuleStateModel.lfmPeriod,
                isSymmetricLfm = synthesizerModuleStateModel.isSymmetricLfm,
                lfmExtTrigger = false,
                extTriggerPeriod = 0.0
            )
        }
        setTransparencyToView(binding.synthesizerConstraintLayout, !isActualData)
    }

    private fun disableAllChannels(channelsList: List<RfChannelNumberIconView>) {
        channelsList.forEach { channel ->
            channel.setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        }
    }

    private fun highlightActiveChannel(
        channelNumber: Int, channelsList: List<RfChannelNumberIconView>
    ) {
        when(channelNumber) {
            1 -> channelsList[0].setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            2 -> channelsList[1].setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            3 -> channelsList[2].setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            4 -> channelsList[3].setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            5 -> channelsList[4].setIconColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            else -> {}
        }
    }

    private fun setCrossToLockedChannels(
        channelsList: List<RfChannelNumberIconView>,
        lockedChannels: BooleanArray
    ) {
        channelsList.forEachIndexed { index, channel ->
            channel.setCrossVisible(lockedChannels[index])
        }
    }

    private fun setAttenuatorValue(attenuationValue: Int) = with(binding) {
        attenuationValueTextView.text = attenuationValue.toString()
        if (attenuationValue != 0) attenuationValueTextView.setTextColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
        else attenuationValueTextView.setTextColor(requireContext().getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
    }

    private fun setTestSignalIndicator(testSignalIsOn: Boolean) = with(binding) {
        val color = if (testSignalIsOn) requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary)
        else requireContext().getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)

        receiverTestSignalStateIconView.setLabelColor(color)
    }

    private fun setTransparencyToView(view: View, isTransparent: Boolean) {
        view.alpha = if (isTransparent) 0.5f
        else 1f
    }

    private fun indicateSynthesizerNoneRadiationMode() = with(binding) {
        lfmExternalTriggerStateImageView.visibility = View.GONE
        lfmSwingTypeIconView.visibility = View.GONE
        periodValueTextView.visibility = View.GONE
        synthesizerModeIconView.setLabelText(resources.getString(ru.profitsw2000.core.R.string.synthesizer_not_active_icon_label_text))
        frequencyValueTextView.text = resources.getString(ru.profitsw2000.core.R.string.synthesizer_generation__absent_warning_text)
    }

    private fun indicateSynthesizerCwRadiationMode(frequency: Long) = with(binding) {

    }

    private fun indicateSynthesizerLfmRadiationMode(
        lowFrequency: Long,
        highFrequency: Long,
        lfmPeriod: Double,
        isSymmetricLfm: Boolean,
        lfmExtTrigger: Boolean,
        extTriggerPeriod: Double
    ) = with(binding) {

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




