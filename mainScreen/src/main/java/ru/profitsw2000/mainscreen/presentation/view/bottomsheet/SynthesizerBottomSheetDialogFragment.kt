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
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import ru.profitsw2000.core.drawable.utils.CW_FREQUENCY_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.CW_FREQUENCY_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.HIGH_FREQUENCY_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.HIGH_FREQUENCY_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.LOW_FREQUENCY_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.LOW_FREQUENCY_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.LOW_FREQ_HIGHER_THAN_HIGH_FREQ_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.MODULATION_PERIOD_ABOVE_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.MODULATION_PERIOD_UNDER_INPUT_ERROR
import ru.profitsw2000.core.drawable.utils.REGISTERS_CALCULATION_ERROR_CODE
import ru.profitsw2000.core.drawable.utils.RESPONSE_PACKET_TIMEOUT_ERROR_CODE
import ru.profitsw2000.core.drawable.utils.UNKNOWN_ERROR_CODE
import ru.profitsw2000.core.drawable.utils.dpToPx
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel
import ru.profitsw2000.mainscreen.R
import ru.profitsw2000.mainscreen.databinding.FragmentSynthesizerBottomSheetDialogBinding
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainViewModel
import ru.profitsw2000.mainscreen.state.SynthesizerUpdatingStatus
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.getValue

class SynthesizerBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSynthesizerBottomSheetDialogBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModel()
    private val spec by lazy {
        CircularProgressIndicatorSpec(requireContext(), null).apply {
            indicatorSize = 24.dpToPx()
            trackThickness = 3.dpToPx()

            indicatorColors = intArrayOf(resources.getColor(ru.profitsw2000.core.R.color.eucaliptus))
        }
    }
    val progressIndicator by lazy {
        IndeterminateDrawable.createCircularDrawable(requireContext(), spec)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSynthesizerBottomSheetDialogBinding.inflate(inflater, container, false)
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
        initRadioGroup()
        initButton()
    }

    private fun observeFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.synthesizerUpdatingStatusFlow.collect { state ->
                    when(state) {
                        is SynthesizerUpdatingStatus.Error -> handleError(state.errorCode)
                        is SynthesizerUpdatingStatus.Idle -> setForms(
                            state.synthesizerModuleStateModel
                        )
                        is SynthesizerUpdatingStatus.Success -> setStatusText(
                            resources.getColor(ru.profitsw2000.core.R.color.eucaliptus),
                            ru.profitsw2000.core.R.string.packet_send_successfull_status_text.toString()
                        )
                        SynthesizerUpdatingStatus.Updating -> setProgressBar(true)
                    }
                }
            }
        }
    }

    private fun initRadioGroup() = with(binding) {
        synthesizerModeSelectionRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.cw_mode_radio_button -> {
                    cwFrequencyTextInputLayout.visibility = View.VISIBLE
                    setLfmSettingsViewsVisibility(false)
                }
                R.id.lfm_mode_radio_button -> {
                    cwFrequencyTextInputLayout.visibility = View.GONE
                    setLfmSettingsViewsVisibility(true)
                }
            }
            rootCoordinatorLayout.requestLayout()
        }
    }

    private fun initButton() = with(binding) {
        synthesizerParamsSendButton.setOnClickListener {
            clearInputFormsErrors()
            when(synthesizerModeSelectionRadioGroup.checkedRadioButtonId) {
                R.id.cw_mode_radio_button -> sendCwParameters()
                R.id.lfm_mode_radio_button -> sendLfmParameters()
            }
        }
    }

    private fun setProgressBar(isUpdating: Boolean) = with(binding.synthesizerParamsSendButton) {
        if (isUpdating) {
            text = ""
            icon = progressIndicator
            progressIndicator.mutate()
            progressIndicator.setVisible(true, true)
            iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
            iconPadding = 0
            iconSize = 24.dpToPx()
            isEnabled = false
        } else {
            progressIndicator.stop()
            icon = null
            text = resources.getString(ru.profitsw2000.core.R.string.send_button_text)
            isEnabled = true
        }
    }

    private fun handleError(errorCode: Int) {
        setProgressBar(false)

        if ((errorCode and CW_FREQUENCY_UNDER_INPUT_ERROR) != 0 ||
            (errorCode and CW_FREQUENCY_ABOVE_INPUT_ERROR) != 0)
            handleCwFrequencyInputError(errorCode)

        if ((errorCode and LOW_FREQUENCY_UNDER_INPUT_ERROR) != 0 ||
            (errorCode and LOW_FREQUENCY_ABOVE_INPUT_ERROR) != 0 ||
            (errorCode and LOW_FREQ_HIGHER_THAN_HIGH_FREQ_INPUT_ERROR) != 0)
            handleLowFrequencyInputError(errorCode)

        if ((errorCode and HIGH_FREQUENCY_UNDER_INPUT_ERROR) != 0 ||
            (errorCode and HIGH_FREQUENCY_ABOVE_INPUT_ERROR) != 0)
            handleHighFrequencyInputError(errorCode)

        if ((errorCode and MODULATION_PERIOD_ABOVE_INPUT_ERROR) != 0 ||
            (errorCode and MODULATION_PERIOD_UNDER_INPUT_ERROR) != 0)
            handleLfmPeriodInputError(errorCode)

        if((errorCode and REGISTERS_CALCULATION_ERROR_CODE) != 0 ||
            (errorCode and RESPONSE_PACKET_TIMEOUT_ERROR_CODE) != 0 ||
            (errorCode and UNKNOWN_ERROR_CODE) != 0)
            handlePacketSendingError(errorCode)
    }

    private fun setForms(synthesizerModuleStateModel: SynthesizerModuleStateModel) = with(binding) {
        val format = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))

        setProgressBar(false)
        cwFrequencyTextInputEditText.setText((synthesizerModuleStateModel.cwFrequency/1_000_000).toString())
        lfmLowFrequencyTextInputEditText.setText((synthesizerModuleStateModel.lowestLfmFrequency/1_000_000).toString())
        lfmHighFrequencyTextInputEditText.setText((synthesizerModuleStateModel.highestLfmFrequency/1_000_000).toString())
        lfmPeriodTextInputEditText.setText(format.format(synthesizerModuleStateModel.lfmPeriod*1_000))
        symmetricLfmCheckBox.isChecked = synthesizerModuleStateModel.isSymmetricLfm
    }

    private fun handleCwFrequencyInputError(errorCode: Int) = with(binding) {
        var errorString = ""

        if ((errorCode and CW_FREQUENCY_UNDER_INPUT_ERROR) != 0) errorString = resources.getString(ru.profitsw2000.core.R.string.low_freq_under_input_error_text)
        if ((errorCode and CW_FREQUENCY_ABOVE_INPUT_ERROR) != 0) errorString = resources.getString(ru.profitsw2000.core.R.string.high_freq_above_input_error_text)

        cwFrequencyTextInputLayout.error = errorString
    }

    private fun handleLowFrequencyInputError(errorCode: Int) = with(binding) {
        var errorString = ""

        if ((errorCode and LOW_FREQUENCY_UNDER_INPUT_ERROR) != 0) errorString = resources.getString(ru.profitsw2000.core.R.string.low_freq_under_input_error_text)
        if ((errorCode and LOW_FREQUENCY_ABOVE_INPUT_ERROR) != 0) errorString = resources.getString(ru.profitsw2000.core.R.string.low_freq_above_input_error_text)
        if ((errorCode and LOW_FREQ_HIGHER_THAN_HIGH_FREQ_INPUT_ERROR) != 0) errorString = resources.getString(ru.profitsw2000.core.R.string.low_freq_higher_than_high_freq_input_error_text)

        lfmLowFrequencyTextInputLayout.error = errorString
    }

    private fun handleHighFrequencyInputError(errorCode: Int) = with(binding) {
        var errorString = ""

        if ((errorCode and HIGH_FREQUENCY_UNDER_INPUT_ERROR) != 0) errorString = resources.getString(ru.profitsw2000.core.R.string.high_freq_under_input_error_text)
        if ((errorCode and HIGH_FREQUENCY_ABOVE_INPUT_ERROR) != 0) errorString = resources.getString(ru.profitsw2000.core.R.string.high_freq_above_input_error_text)

        lfmHighFrequencyTextInputLayout.error = errorString
    }

    private fun handleLfmPeriodInputError(errorCode: Int) = with(binding) {
        var errorString = ""

        if ((errorCode and MODULATION_PERIOD_ABOVE_INPUT_ERROR) != 0) errorString = resources.getString(ru.profitsw2000.core.R.string.modulation_period_above_input_error_text)
        if ((errorCode and MODULATION_PERIOD_UNDER_INPUT_ERROR) != 0) errorString = resources.getString(ru.profitsw2000.core.R.string.modulation_period_under_input_error_text)

        lfmPeriodTextInputLayout.error = errorString
    }

    private fun handlePacketSendingError(errorCode: Int) = with(binding) {
        val statusText = if ((errorCode and RESPONSE_PACKET_TIMEOUT_ERROR_CODE) != 0)
            resources.getString(ru.profitsw2000.core.R.string.response_packet_timeout_error)
        else if ((errorCode and REGISTERS_CALCULATION_ERROR_CODE) != 0)
            resources.getString(ru.profitsw2000.core.R.string.register_calculation_error_text)
        else resources.getString(ru.profitsw2000.core.R.string.unknown_error)

        setStatusText(resources.getColor(ru.profitsw2000.core.R.color.scarlet), statusText)
    }

    private fun setStatusText(color: Int, statusText: String) = with(binding.updatingStatusResultTextView) {
        visibility = View.VISIBLE
        setTextColor(color)
        text = statusText
    }

    private fun sendCwParameters() = with(binding) {
        val cwInputFrequencyIsEmpty = inputIsEmpty(cwFrequencyTextInputLayout, cwFrequencyTextInputEditText)

        if (!cwInputFrequencyIsEmpty) {
            mainViewModel.updateSynthesizerCwMode(
                cwFrequencyTextInputEditText.text.toString().toLong()
            )
        }
    }

    private fun sendLfmParameters() = with(binding) {
        val lowestLfmInputFrequencyIsEmpty = inputIsEmpty(lfmLowFrequencyTextInputLayout, lfmLowFrequencyTextInputEditText)
        val highestLfmInputFrequencyIsEmpty = inputIsEmpty(lfmHighFrequencyTextInputLayout, lfmHighFrequencyTextInputEditText)
        val lfmPeriodInputFrequencyIsEmpty = inputIsEmpty(lfmPeriodTextInputLayout, lfmPeriodTextInputEditText)

        if (!lowestLfmInputFrequencyIsEmpty && !highestLfmInputFrequencyIsEmpty && !lfmPeriodInputFrequencyIsEmpty) {
            mainViewModel.updateSynthesizerLfmMode(
                startFrequency = lfmLowFrequencyTextInputEditText.text.toString().toLong(),
                stopFrequency = lfmHighFrequencyTextInputEditText.text.toString().toLong(),
                lfmPeriod = lfmPeriodTextInputEditText.text.toString().toDouble(),
                isSymmetricLfm = symmetricLfmCheckBox.isChecked
            )
        }
    }

    private fun inputIsEmpty(
        textInputLayout: TextInputLayout,
        textInputEditText: TextInputEditText
    ): Boolean = with(binding) {
        return if (textInputEditText.text?.isEmpty() == true) {
            textInputLayout.error = resources.getString(ru.profitsw2000.core.R.string.empty_input_error_text)
            true
        } else {
            false
        }
    }

    private fun clearInputFormsErrors() = with(binding) {
        updatingStatusResultTextView.visibility = View.GONE
        cwFrequencyTextInputLayout.error = null
        lfmLowFrequencyTextInputLayout.error = null
        lfmHighFrequencyTextInputLayout.error = null
        lfmPeriodTextInputLayout.error = null
    }

    private fun setLfmSettingsViewsVisibility(isVisible: Boolean) = with(binding) {
        if (isVisible) {
            lfmLowFrequencyTextInputLayout.visibility = View.VISIBLE
            lfmHighFrequencyTextInputLayout.visibility = View.VISIBLE
            lfmPeriodTextInputLayout.visibility = View.VISIBLE
            lfmExtTriggerSwitchCheckBox.visibility = View.VISIBLE
        } else {
            lfmLowFrequencyTextInputLayout.visibility = View.GONE
            lfmHighFrequencyTextInputLayout.visibility = View.GONE
            lfmPeriodTextInputLayout.visibility = View.GONE
            lfmExtTriggerSwitchCheckBox.visibility = View.GONE
        }
    }
}