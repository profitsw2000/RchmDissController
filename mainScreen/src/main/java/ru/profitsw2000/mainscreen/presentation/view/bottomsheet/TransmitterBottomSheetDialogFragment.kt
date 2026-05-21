package ru.profitsw2000.mainscreen.presentation.view.bottomsheet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_1
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_2
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_3
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_4
import ru.profitsw2000.core.drawable.utils.TX_CHANNEL_5
import ru.profitsw2000.mainscreen.R
import ru.profitsw2000.mainscreen.databinding.FragmentTransmitterBottomSheetDialogBinding
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainViewModel

class TransmitterBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentTransmitterBottomSheetDialogBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModel()

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
    }

    private fun initViews() {
        initSendButton()
    }

    private fun initSendButton() = with(binding) {
        transmitterParamsSendButton.setOnClickListener {
            mainViewModel.updateTransmitter(
                getTransmitterByteFromSelectedChip(rxChannelSelectionChipGroup.checkedChipId)
            )
        }
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



}