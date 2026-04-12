package ru.profitsw2000.rchmdisscontroller.presentation.view

import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import ru.profitsw2000.data.model.bluetooth.BluetoothDeviceModel
import ru.profitsw2000.data.model.bluetooth.status.BluetoothConnectionStatus
import ru.profitsw2000.rchmdisscontroller.R
import ru.profitsw2000.rchmdisscontroller.databinding.FragmentBluetoothDeviceSelectionDialogBinding
import ru.profitsw2000.rchmdisscontroller.presentation.view.adapter.BluetoothDevicesListAdapter
import ru.profitsw2000.rchmdisscontroller.presentation.view.adapter.OnBluetoothDeviceClickListener
import ru.profitsw2000.rchmdisscontroller.presentation.viewmodel.MainActivityViewModel

class BluetoothDeviceSelectionDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentBluetoothDeviceSelectionDialogBinding? = null
    private val binding get() = _binding!!
    private val mainActivityViewModel: MainActivityViewModel by activityViewModel()
    private val bluetoothDevicesListAdapter: BluetoothDevicesListAdapter by lazy {
        BluetoothDevicesListAdapter(
            onBluetoothDeviceClickListener = object : OnBluetoothDeviceClickListener {
                override fun onClick(bluetoothDevice: BluetoothDeviceModel) {
                    mainActivityViewModel.connectBluetoothDevice(bluetoothDevice.address)
                    this@BluetoothDeviceSelectionDialogFragment.dismiss()
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentBluetoothDeviceSelectionDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheetBehavior = BottomSheetBehavior.from(view.parent as View)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        val layout: CoordinatorLayout = binding.rootCoordinatorLayout
        layout.minimumHeight = 1500

        initViews()
        observeData()
    }

    private fun initViews() = with(binding) {
        pairedDevicesListRecyclerView.adapter = bluetoothDevicesListAdapter
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainActivityViewModel.bluetoothConnectionStatus.collect { bluetoothConnectionStatus ->
                    if (bluetoothConnectionStatus is BluetoothConnectionStatus.DeviceSelection)
                        bluetoothDevicesListAdapter.setData(bluetoothConnectionStatus.devicesNameList)
                }
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        mainActivityViewModel.initBluetoothConnection()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}