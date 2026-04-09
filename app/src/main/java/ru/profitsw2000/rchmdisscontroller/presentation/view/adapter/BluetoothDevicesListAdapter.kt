package ru.profitsw2000.rchmdisscontroller.presentation.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.profitsw2000.data.model.bluetooth.BluetoothDeviceModel
import ru.profitsw2000.rchmdisscontroller.databinding.BluetoothDeviceItemViewBinding

class BluetoothDevicesListAdapter(
    private val onBluetoothDeviceClickListener: OnBluetoothDeviceClickListener
) : RecyclerView.Adapter<BluetoothDevicesListAdapter.ViewHolder>()  {
    private var data: List<BluetoothDeviceModel> = arrayListOf()

    fun setData(data: List<BluetoothDeviceModel>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BluetoothDeviceItemViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false)
        val bluetoothDeviceViewHolder = ViewHolder(binding)

        with(binding) {
            root.setOnClickListener {
                onBluetoothDeviceClickListener.onClick(data[bluetoothDeviceViewHolder.adapterPosition])
            }
        }

        return bluetoothDeviceViewHolder
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bluetoothDevice = data[position]

        holder.deviceName.text = bluetoothDevice.name
    }

    inner class ViewHolder(private val binding: BluetoothDeviceItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val deviceName = binding.deviceNameTextView
    }
}