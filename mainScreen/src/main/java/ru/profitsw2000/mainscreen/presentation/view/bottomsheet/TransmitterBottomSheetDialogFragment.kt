package ru.profitsw2000.mainscreen.presentation.view.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import ru.profitsw2000.core.drawable.utils.RESPONSE_PACKET_TIMEOUT_ERROR_CODE
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_1
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_2
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_3
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_4
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_5
import ru.profitsw2000.data.model.bluetooth.state.rcd.OutputModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.mainscreen.databinding.FragmentTransmitterBottomSheetDialogBinding
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainViewModel
import ru.profitsw2000.mainscreen.state.TransmitterUpdatingStatus

class TransmitterBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentTransmitterBottomSheetDialogBinding? = null
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
        _binding = FragmentTransmitterBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheetBehavior = BottomSheetBehavior.from(view.parent as View)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        initViews()
        observeFlows()
    }

    private fun initViews() {
        initSendButton()
    }

    private fun initSendButton() = with(binding) {
        transmitterParamsSendButton.setOnClickListener {
            mainViewModel.updateTransmitter(
                getTransmitterByteFromSelectedChip(rxChannelSelectionChipGroup.checkedChipId),
                switchTransmitterOnCheckBox.isChecked
            )
        }
    }

    private fun observeFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.transmitterUpdatingStatusFlow.collect { state ->
                    when(state) {
                        is TransmitterUpdatingStatus.Error -> handleError(state.errorCode)
                        is TransmitterUpdatingStatus.Idle -> setForms(state.transmitterModuleState, state.outputModuleState)
                        is TransmitterUpdatingStatus.Success -> setStatusText(
                            resources.getColor(ru.profitsw2000.core.R.color.scarlet),
                            ru.profitsw2000.core.R.string.packet_send_successfull_status_text.toString()
                        )
                        TransmitterUpdatingStatus.Updating -> setProgressBar(true)
                    }
                }
            }
        }
    }

    private fun handleError(errorCode: Int) = with(binding.updatingStatusResultTextView) {
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

    private fun setForms(
        transmitterModuleState: TransmitterModuleState,
        outputModuleState: OutputModuleState
    ) = with(binding) {

        setProgressBar(false)
        rxChannelSelectionChipGroup.clearCheck()
        when(transmitterModuleState.enabledChannelNumber) {
            TX_CHANNEL_1 -> firstChannelSelectionChip.isChecked = true
            TX_CHANNEL_2 -> secondChannelSelectionChip.isChecked = true
            TX_CHANNEL_3 -> thirdChannelSelectionChip.isChecked = true
            TX_CHANNEL_4 -> fourthChannelSelectionChip.isChecked = true
            TX_CHANNEL_5 -> fifthChannelSelectionChip.isChecked = true
            else -> rxChannelSelectionChipGroup.clearCheck()
        }
        switchTransmitterOnCheckBox.isChecked = outputModuleState.rchmDissDigitalOutput.toInt().and(0x2) == 0
        updatingStatusResultTextView.visibility = View.GONE
    }

    private fun setStatusText(color: Int, statusText: String) = with(binding.updatingStatusResultTextView) {
        visibility = View.VISIBLE
        setTextColor(color)
        text = statusText
    }

    private fun getTransmitterByteFromSelectedChip(selectedChipId: Int): Byte = with(binding) {
        return when(selectedChipId) {
            firstChannelSelectionChip.id -> TX_CHANNEL_1.shl(1).toByte()
            secondChannelSelectionChip.id -> TX_CHANNEL_2.shl(1).toByte()
            thirdChannelSelectionChip.id -> TX_CHANNEL_3.shl(1).toByte()
            fourthChannelSelectionChip.id -> TX_CHANNEL_4.shl(1).toByte()
            fifthChannelSelectionChip.id -> TX_CHANNEL_5.shl(1).toByte()
            else -> 0
        }
    }

    private fun Int.dpToPx(): Int {
        val density = android.content.res.Resources.getSystem().displayMetrics.density
        return (this * density).toInt()
    }

}