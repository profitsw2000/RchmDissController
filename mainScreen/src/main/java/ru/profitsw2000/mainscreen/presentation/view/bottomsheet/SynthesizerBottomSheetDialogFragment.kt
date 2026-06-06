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
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import ru.profitsw2000.mainscreen.R
import ru.profitsw2000.mainscreen.databinding.FragmentSynthesizerBottomSheetDialogBinding
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainViewModel
import ru.profitsw2000.mainscreen.state.SynthesizerUpdatingStatus
import kotlin.getValue

class SynthesizerBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSynthesizerBottomSheetDialogBinding? = null
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
        _binding = FragmentSynthesizerBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheetBehavior = BottomSheetBehavior.from(view.parent as View)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        initViews()
    }

    private fun initViews() {
        initRadioGroup()
    }

    private fun observeFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.synthesizerUpdatingStatusFlow.collect { state ->
                    when(state) {
                        is SynthesizerUpdatingStatus.Error -> TODO()
                        is SynthesizerUpdatingStatus.Idle -> TODO()
                        is SynthesizerUpdatingStatus.Success -> TODO()
                        SynthesizerUpdatingStatus.Updating -> TODO()
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

    private fun Int.dpToPx(): Int {
        val density = android.content.res.Resources.getSystem().displayMetrics.density
        return (this * density).toInt()
    }
}