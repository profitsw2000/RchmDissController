package ru.profitsw2000.mainscreen.presentation.view

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import ru.profitsw2000.core.drawable.RfChannelNumberIconView
import ru.profitsw2000.data.model.bluetooth.state.rcd.RadiationMode
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissStateModel
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.mainscreen.R
import ru.profitsw2000.mainscreen.databinding.FragmentMainBinding
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainViewModel
import ru.profitsw2000.navigator.Navigator

private const val TRANSMITTER_LAYOUT_CLICK = 1
private const val RECEIVER_LAYOUT_CLICK = 2
private const val SYNTHESIZER_LAYOUT_CLICK = 3

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val navigator: Navigator by inject()
    private val mainViewModel: MainViewModel by activityViewModel()
    private var statusMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
        setMenuProvider()
        initViews()
        observeFlows()
    }

    private fun setMenuProvider() {
        requireActivity().addMenuProvider( object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_fragment_menu, menu)
                statusMenuItem = menu.findItem(R.id.data_exchange_status)
                updateStatusIcon(mainViewModel.isReceivedOutputControlPacket.value)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun initViews() = with(binding) {
        transmitterConstraintLayout.setOnClickListener {
            mainViewModel.layoutClicked(TRANSMITTER_LAYOUT_CLICK)
        }
        receiverConstraintLayout.setOnClickListener {
            mainViewModel.layoutClicked(RECEIVER_LAYOUT_CLICK)
        }
        synthesizerConstraintLayout.setOnClickListener {
            mainViewModel.layoutClicked(SYNTHESIZER_LAYOUT_CLICK)
        }
    }

    private fun observeFlows() = with(binding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.rchmDissStateModelFlow.collect { state ->
                    renderData(rchmDissStateModel = state)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.isReceivedOutputControlPacket.collect { status ->
                    setTransparencyToView(transmitterConstraintLayout, !status)
                    setTransparencyToView(receiverConstraintLayout, !status)
                    setTransparencyToView(synthesizerConstraintLayout, !status)
                    requireActivity().invalidateMenu()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainViewModel.clickActionSharedFlow.collect { clickedLayout ->
                    when(clickedLayout) {
                        TRANSMITTER_LAYOUT_CLICK -> navigator.navigateToTransmitterSettingsDialog()
                        RECEIVER_LAYOUT_CLICK -> navigator.navigateToReceiverSettingsDialog()
                        SYNTHESIZER_LAYOUT_CLICK -> navigator.navigateToSynthesizerSettingsDialog()
                        else -> {}
                    }
                }
            }
        }
    }

    private fun renderData(rchmDissStateModel: RchmDissStateModel) {
        renderTransmitterData(rchmDissStateModel.transmitterModuleState)
        renderReceiverData(rchmDissStateModel.receiverModuleState)
        renderSynthesizerData(rchmDissStateModel.synthesizerModuleState)
    }

    private fun renderTransmitterData(
        transmitterModuleState: TransmitterModuleState
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
    }

    private fun renderReceiverData(
        receiverModuleState: ReceiverModuleState
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
    }

    private fun renderSynthesizerData(
        synthesizerModuleStateModel: SynthesizerModuleStateModel
    ) {
        when(synthesizerModuleStateModel.radiationMode) {
            RadiationMode.NONE -> indicateSynthesizerNoneRadiationMode()
            RadiationMode.CW -> indicateSynthesizerCwRadiationMode(synthesizerModuleStateModel.cwFrequency)
            RadiationMode.LFM -> indicateSynthesizerLfmRadiationMode(
                lowFrequency = synthesizerModuleStateModel.lowestLfmFrequency,
                highFrequency = synthesizerModuleStateModel.highestLfmFrequency,
                lfmPeriod = synthesizerModuleStateModel.lfmPeriod,
                isSymmetricLfm = synthesizerModuleStateModel.isSymmetricLfm,
                lfmExtTrigger = false
            )
        }
    }

    private fun disableAllChannels(channelsList: List<RfChannelNumberIconView>) {
        channelsList.forEach { channel ->
            channel.setIconColor(getIndicatorColor(false))
        }
    }

    private fun highlightActiveChannel(
        channelNumber: Int, channelsList: List<RfChannelNumberIconView>
    ) {
        when(channelNumber) {
            1 -> channelsList[0].setIconColor(getIndicatorColor(true))
            2 -> channelsList[1].setIconColor(getIndicatorColor(true))
            3 -> channelsList[2].setIconColor(getIndicatorColor(true))
            4 -> channelsList[3].setIconColor(getIndicatorColor(true))
            5 -> channelsList[4].setIconColor(getIndicatorColor(true))
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
        attenuationValueTextView.text = resources.getString(ru.profitsw2000.core.R.string.receiver_attenuation_value_text, attenuationValue)
        attenuationValueTextView.setTextColor(getIndicatorColor(attenuationValue != 0))
    }

    private fun setTestSignalIndicator(testSignalIsOn: Boolean) = with(binding) {
         receiverTestSignalStateIconView.setLabelColor(getIndicatorColor(testSignalIsOn))
    }

    private fun indicateSynthesizerNoneRadiationMode() = with(binding) {
        lfmExternalTriggerStateImageView.visibility = View.GONE
        lfmSwingTypeIconView.visibility = View.GONE
        periodValueTextView.visibility = View.GONE
        synthesizerModeIconView.setLabelText(resources.getString(ru.profitsw2000.core.R.string.synthesizer_not_active_icon_label_text))
        frequencyValueTextView.text = resources.getString(ru.profitsw2000.core.R.string.synthesizer_generation__absent_warning_text)
    }

    private fun indicateSynthesizerCwRadiationMode(frequency: Long) = with(binding) {
        lfmExternalTriggerStateImageView.visibility = View.GONE
        lfmSwingTypeIconView.visibility = View.GONE
        periodValueTextView.visibility = View.GONE
        synthesizerModeIconView.setLabelText(resources.getString(ru.profitsw2000.core.R.string.cw_synthesizer_mode_text))
        frequencyValueTextView.text = resources.getString(ru.profitsw2000.core.R.string.cw_frequency_parameter_text, frequency/1_000_000)
    }

    private fun indicateSynthesizerLfmRadiationMode(
        lowFrequency: Long,
        highFrequency: Long,
        lfmPeriod: Double,
        isSymmetricLfm: Boolean,
        lfmExtTrigger: Boolean
    ) = with(binding) {
        lfmExternalTriggerStateImageView.visibility = View.VISIBLE
        lfmExternalTriggerStateImageView.imageTintList = ColorStateList.valueOf(getIndicatorColor(lfmExtTrigger))
        lfmSwingTypeIconView.visibility = View.VISIBLE
        lfmSwingTypeIconView.setLabelText(
            if (isSymmetricLfm) resources.getString(ru.profitsw2000.core.R.string.symmetric_lfm_icon_text)
            else resources.getString(ru.profitsw2000.core.R.string.symmetric_lfm_icon_text)
        )
        periodValueTextView.visibility = View.VISIBLE
        periodValueTextView.text =
            resources.getString(ru.profitsw2000.core.R.string.lfm_period_value_text, lfmPeriod)
        synthesizerModeIconView.setLabelText(resources.getString(ru.profitsw2000.core.R.string.lfm_synthesizer_mode_text))
        frequencyValueTextView.text = resources.getString(ru.profitsw2000.core.R.string.lfm_frequency_parameter_text, lowFrequency/1_000_000, highFrequency/1_000_000)
    }

    private fun getIndicatorColor(isActive: Boolean): Int {
        return if (isActive) requireContext().getThemeColor(com.google.android.material.R.attr.colorOnPrimary)
        else requireContext().getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
    }

    private fun setTransparencyToView(view: View, isTransparent: Boolean) {
        view.alpha = if (isTransparent) 0.5f
        else 1f
    }

    private fun updateStatusIcon(isConnected: Boolean) {
        val item = statusMenuItem ?: return
        val color = if (isConnected) {
            resources.getColor(ru.profitsw2000.core.R.color.eucaliptus)
        } else {
            resources.getColor(ru.profitsw2000.core.R.color.guardsman_red)
        }

        item.icon?.mutate()?.setTint(color)
    }

    @ColorInt
    fun Context.getThemeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }
}
