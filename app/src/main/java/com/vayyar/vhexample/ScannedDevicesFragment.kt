package com.vayyar.vhexample

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.walabot.home.ble.BleDevice

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ScannedDevicesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

data class BleDeviceStatus(val device: BleDevice, var status: Boolean = false)

class ScannedDevicesFragment : Fragment() {
    interface Listener {
        fun onPickedDevices(devices: List<BleDevice>?)
    }
    var devices: ArrayList<BleDeviceStatus>? = null
    var listener: Listener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_scanned_devices, container, false)
        v.findViewById<RecyclerView>(R.id.devicesList).apply {
            layoutManager = LinearLayoutManager(this@ScannedDevicesFragment.context)
            adapter = DevicesAdapter()
        }
        v.findViewById<Button>(R.id.button).setOnClickListener {
            listener?.onPickedDevices(devices?.filter { it.status }?.map { it.device }
                ?.let { it1 -> ArrayList(it1) })
        }
        return v
    }

    inner  class DevicesAdapter: Adapter<DevicesAdapter.DeviceVH>() {


        inner class  DeviceVH(itemView: View) : ViewHolder(itemView) {
            private val checkBox: CheckBox

            var device: BleDeviceStatus? = null
            set(value) {
                field = value
                itemView.findViewById<TextView>(R.id.textView).text = value?.device?.name
                checkBox.isChecked = value?.status ?: false
            }

            init {
                checkBox = itemView.findViewById(R.id.checkbox)
                checkBox.setOnCheckedChangeListener { compoundButton, b ->
                    device?.status = b
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceVH {
            return DeviceVH(LayoutInflater.from(parent.context).inflate(R.layout.device_view_holder, parent, true))
        }

        override fun onBindViewHolder(holder: DeviceVH, position: Int) {
            holder.device = devices?.get(position)
        }

        override fun getItemCount(): Int {
            return devices?.count() ?: 0
        }
    }
}