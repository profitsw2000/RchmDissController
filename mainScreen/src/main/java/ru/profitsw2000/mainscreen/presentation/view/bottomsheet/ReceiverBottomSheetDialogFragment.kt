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
import ru.profitsw2000.core.drawable.utils.RESPONSE_PACKET_TIMEOUT_ERROR_CODE
import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
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

    private fun setForms(receiverModule: ReceiverModuleState) = with(binding) {
        setProgressBar(false)
    }

    private fun Int.dpToPx(): Int {
        val density = android.content.res.Resources.getSystem().displayMetrics.density
        return (this * density).toInt()
    }
}