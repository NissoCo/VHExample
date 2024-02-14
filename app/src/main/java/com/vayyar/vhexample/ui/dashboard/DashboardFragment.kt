package com.vayyar.vhexample.ui.dashboard

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView.RecyclerListener
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vayyar.vhexample.MainActivity
import com.vayyar.vhexample.R
import com.vayyar.vhexample.databinding.FragmentDashboardBinding
import com.walabot.home.ble.BleDevice
import com.walabot.home.ble.sdk.Operational.Operational

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    lateinit var dashboardViewModel: DashboardViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    val adapter = ScannedDevicesAdapter()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted ->
        if (isGranted.isNotEmpty()) {
            dashboardViewModel.fetchDevices()
        } else {
            // Explain to the user that the feature is unavailable because the
            // features requires a permission that the user has denied. At the
            // same time, respect the user's decision. Don't link to system
            // settings in an effort to convince the user to change their
            // decision.
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        dashboardViewModel.operational = Operational(requireContext())
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val recycler: RecyclerView = binding.devicesList
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter
        dashboardViewModel.devices.observe(viewLifecycleOwner) {
            adapter.devices = it?.values?.toList()
            activity?.runOnUiThread {
                adapter.notifyDataSetChanged()
            }
        }

        dashboardViewModel.isConnected.observe(viewLifecycleOwner) {
            _binding?.actions?.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }

        dashboardViewModel.log.observe(viewLifecycleOwner) {
            _binding?.logs?.text = it
        }

        dashboardViewModel.hasPermission.observe(viewLifecycleOwner) {
            val _permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                requestPermissionLauncher.launch(_permissions)
            } else {
                ActivityCompat.requestPermissions(requireActivity(), _permissions, 1)
            }
        }
        _binding?.start?.setOnClickListener {
            dashboardViewModel.fetchDevices()
        }
        _binding?.beep?.setOnClickListener {
            dashboardViewModel.operational?.doImHere()
        }
        _binding?.reboot?.setOnClickListener {
            dashboardViewModel.operational?.reboot()
        }
        _binding?.devInfo?.setOnClickListener {
            dashboardViewModel.operational?.getDevInfo()
        }
        _binding?.changeWifi?.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
            (activity as MainActivity).binding.fab.visibility = View.VISIBLE
        }
        _binding?.stop?.setOnClickListener {
            dashboardViewModel.stop()
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class ScannedDevicesAdapter: RecyclerView.Adapter<ScannedDevicesAdapter.ScannedDeviceViewHolder>() {
        var devices: List<BleDevice>? = null
        inner class ScannedDeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var device: BleDevice? = null
                set(value) {
                    field = value
                    itemView.findViewById<TextView>(R.id.title).text = value?.name
                    itemView.findViewById<TextView>(R.id.subTitle).text = value?.address
                    itemView.findViewById<TextView>(R.id.rssi).text = value?.rssi.toString()
                }

            init {
                itemView.setOnClickListener {
                    dashboardViewModel.operational?.connect(device!!)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedDeviceViewHolder {
            return ScannedDeviceViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.status_row, parent, false))
        }

        override fun getItemCount(): Int {
            return devices?.count() ?: 0
        }

        override fun onBindViewHolder(holder: ScannedDeviceViewHolder, position: Int) {
            holder.device = devices?.get(position)
        }
    }
}