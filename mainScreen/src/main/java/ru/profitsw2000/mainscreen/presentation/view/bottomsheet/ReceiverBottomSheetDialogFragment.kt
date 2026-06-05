package ru.profitsw2000.mainscreen.presentation.view.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import ru.profitsw2000.core.drawable.utils.ATTENUATOR_16_DECIBELS_BIT
import ru.profitsw2000.core.drawable.utils.ATTENUATOR_2_DECIBELS_BIT
import ru.profitsw2000.core.drawable.utils.ATTENUATOR_32_DECIBELS_BIT
import ru.profitsw2000.core.drawable.utils.ATTENUATOR_4_DECIBELS_BIT
import ru.profitsw2000.core.drawable.utils.ATTENUATOR_8_DECIBELS_BIT
import ru.profitsw2000.core.drawable.utils.FIFTH_CHANNEL_LOCK_BIT
import ru.profitsw2000.core.drawable.utils.FIRST_CHANNEL_LOCK_BIT
import ru.profitsw2000.core.drawable.utils.FOURTH_CHANNEL_LOCK_BIT
import ru.profitsw2000.core.drawable.utils.RESPONSE_PACKET_TIMEOUT_ERROR_CODE
import ru.profitsw2000.core.drawable.utils.RX_CHANNEL_1
import ru.profitsw2000.core.drawable.utils.RX_CHANNEL_2
import ru.profitsw2000.core.drawable.utils.RX_CHANNEL_3
import ru.profitsw2000.core.drawable.utils.RX_CHANNEL_4
import ru.profitsw2000.core.drawable.utils.RX_CHANNEL_5
import ru.profitsw2000.core.drawable.utils.RX_CHANNEL_MASK
import ru.profitsw2000.core.drawable.utils.SECOND_CHANNEL_LOCK_BIT
import ru.profitsw2000.core.drawable.utils.TEST_SIGNAL_BIT
import ru.profitsw2000.core.drawable.utils.THIRD_CHANNEL_LOCK_BIT
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_1
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_2
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_3
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_4
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_5
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.mainscreen.databinding.FragmentReceiverBottomSheetDialogBinding
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainViewModel
import ru.profitsw2000.mainscreen.state.ReceiverUpdatingStatus
import kotlin.getValue

class ReceiverBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentReceiverBottomSheetDialogBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModel()
    private val spec = CircularProgressIndicatorSpec(requireContext(), null).apply {
        indicatorSize = 24.dpToPx()
        trackThickness = 3.dpToPx()

        indicatorColors = intArrayOf(resources.getColor(ru.profitsw2000.core.R.color.splashed_white))
    }
    val progressIndicator = IndeterminateDrawable.createCircularDrawable(requireContext(), spec)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentReceiverBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheetBehavior = BottomSheetBehavior.from(view.parent as View)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        initViews()
    }

    private fun initViews() = with(binding) {
        transmitterParamsSendButton.setOnClickListener {
            //mainViewModel.updateReceiver()
        }
    }

    private fun observeFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.receiverUpdatingStatusFlow.collect { state ->
                    when(state) {
                        is ReceiverUpdatingStatus.Error -> handleError(state.errorCode)
                        is ReceiverUpdatingStatus.Idle -> TODO()
                        is ReceiverUpdatingStatus.Success -> TODO()
                        ReceiverUpdatingStatus.Updating -> setProgressBar(true)
                    }
                }
            }
        }
    }

    private fun handleError(errorCode: Int) {
        val statusText = when(errorCode) {
            RESPONSE_PACKET_TIMEOUT_ERROR_CODE -> ru.profitsw2000.core.R.string.response_packet_timeout_error.toString()
            else -> ru.profitsw2000.core.R.string.unknown_error.toString()
        }
        setProgressBar(false)
        setStatusText(resources.getColor(ru.profitsw2000.core.R.color.scarlet), statusText)
    }

    private fun setProgressBar(isUpdating: Boolean) = with(binding.transmitterParamsSendButton) {
        if (isUpdating) {
            text = ""
            icon = progressIndicator
            progressIndicator.start()
            isEnabled = false
        } else {
            progressIndicator.stop()
            icon = null
            text = resources.getString(ru.profitsw2000.core.R.string.send_button_text)
            isEnabled = true
        }
    }

    private fun setStatusText(color: Int, statusText: String) = with(binding.updatingStatusResultTextView) {
        visibility = View.VISIBLE
        setTextColor(color)
        text = statusText
    }

    private fun setForms(receiverModuleState: ReceiverModuleState) = with(binding) {
        setProgressBar(false)


    }

    private fun getReceiverSettingsByteArray(): ByteArray {
        val receiverSettingsIntCode = getReceiverSettingsIntValueFromSelectedChips()
        return byteArrayOf(
                receiverSettingsIntCode.shr(8).toByte(),
                receiverSettingsIntCode.toByte()
            )
    }

    private fun getReceiverSettingsIntValueFromSelectedChips(): Int {
        return getIncludedReceiverChannelCode() or
                getLockedReceiverChannelsCode() or
                getAttenuatorCode() or
                getTestSignalCode()
    }

    private fun getIncludedReceiverChannelCode(): Int = with(binding) {
        return when(rxChannelSelectionChipGroup.checkedChipId) {
            firstChannelSelectionChip.id -> RX_CHANNEL_1.shl(10)
            secondChannelSelectionChip.id -> RX_CHANNEL_2.shl(10)
            thirdChannelSelectionChip.id -> RX_CHANNEL_3.shl(10)
            fourthChannelSelectionChip.id -> RX_CHANNEL_4.shl(10)
            fifthChannelSelectionChip.id -> RX_CHANNEL_5.shl(10)
            else -> RX_CHANNEL_MASK.shl(10)
        }
    }

    private fun getLockedReceiverChannelsCode(): Int = with(binding) {
        return channelLockBitCode(channel1LockSelectionChip, FIRST_CHANNEL_LOCK_BIT) or
                channelLockBitCode(channel2LockSelectionChip, SECOND_CHANNEL_LOCK_BIT) or
                channelLockBitCode(channel3LockSelectionChip, THIRD_CHANNEL_LOCK_BIT) or
                channelLockBitCode(channel4LockSelectionChip, FOURTH_CHANNEL_LOCK_BIT) or
                channelLockBitCode(channel5LockSelectionChip, FIFTH_CHANNEL_LOCK_BIT)
    }

    private fun getAttenuatorCode(): Int = with(binding) {
        return attenuatorBitCode(twoDecibelSelectionChip, ATTENUATOR_2_DECIBELS_BIT) or
                attenuatorBitCode(fourDecibelSelectionChip, ATTENUATOR_4_DECIBELS_BIT) or
                attenuatorBitCode(eightDecibelSelectionChip, ATTENUATOR_8_DECIBELS_BIT) or
                attenuatorBitCode(sixteenDecibelSelectionChip, ATTENUATOR_16_DECIBELS_BIT) or
                attenuatorBitCode(thirtyTwoDecibelChip, ATTENUATOR_32_DECIBELS_BIT)
    }

    private fun getTestSignalCode(): Int = with(binding) {
        return if(receiverTestSignalSwitchCheckBox.isChecked) 1.shl(TEST_SIGNAL_BIT)
        else 0
    }

    private fun channelLockBitCode(chip: Chip, channelBitNumber: Int): Int {
        return if (!chip.isChecked) 1.shl(channelBitNumber)
        else 0
    }

    private fun attenuatorBitCode(chip: Chip, attenuatorBitNumber: Int): Int {
        return if (chip.isChecked) 1.shl(attenuatorBitNumber)
        else 0
    }

    private fun setReceiverIncludedChannelsChips(receiverModuleState: ReceiverModuleState) = with(binding) {
        when(receiverModuleState.enabledChannelNumber) {
            TX_CHANNEL_1 -> firstChannelSelectionChip.isChecked = true
            TX_CHANNEL_2 -> secondChannelSelectionChip.isChecked = true
            TX_CHANNEL_3 -> thirdChannelSelectionChip.isChecked = true
            TX_CHANNEL_4 -> fourthChannelSelectionChip.isChecked = true
            TX_CHANNEL_5 -> fifthChannelSelectionChip.isChecked = true
            else -> rxChannelSelectionChipGroup.clearCheck()
        }
    }

    private fun setReceiverLockedChannelsChips(receiverModuleState: ReceiverModuleState) = with(binding) {
        channel1LockSelectionChip.isChecked = receiverModuleState.lockedInputChannels[0]
        channel2LockSelectionChip.isChecked = receiverModuleState.lockedInputChannels[1]
        channel3LockSelectionChip.isChecked = receiverModuleState.lockedInputChannels[2]
        channel4LockSelectionChip.isChecked = receiverModuleState.lockedInputChannels[3]
        channel5LockSelectionChip.isChecked = receiverModuleState.lockedInputChannels[4]
    }

    private fun setAttenuatorsChips(receiverModuleState: ReceiverModuleState) = with(binding) {
        val attenuatorCode = receiverModuleState.inputAttenuatorsCode

        setChipState(twoDecibelSelectionChip, attenuatorCode and 1.shl(ATTENUATOR_2_DECIBELS_BIT) != 0)
        setChipState(fourDecibelSelectionChip, attenuatorCode and 1.shl(ATTENUATOR_4_DECIBELS_BIT) != 0)
        setChipState(eightDecibelSelectionChip, attenuatorCode and 1.shl(ATTENUATOR_8_DECIBELS_BIT) != 0)
        setChipState(sixteenDecibelSelectionChip, attenuatorCode and 1.shl(ATTENUATOR_16_DECIBELS_BIT) != 0)
        setChipState(thirtyTwoDecibelChip, attenuatorCode and 1.shl(ATTENUATOR_32_DECIBELS_BIT) != 0)
    }

    private fun setChipState(chip: Chip, isChecked: Boolean) {
        chip.isChecked = isChecked
    }

    private fun Int.dpToPx(): Int {
        val density = android.content.res.Resources.getSystem().displayMetrics.density
        return (this * density).toInt()
    }
}