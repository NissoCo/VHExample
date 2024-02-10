package com.vayyar.vhexample

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.vayyar.vhexample.databinding.ActivityMainBinding
import com.vayyar.vhexample.ui.dashboard.DashboardFragment
import com.walabot.home.ble.sdk.Config
import com.walabot.home.ble.sdk.EspPairingEvent
import com.walabot.home.ble.sdk.EspWifiItem
import com.walabot.home.ble.sdk.MassProvisioning
import com.walabot.home.ble.sdk.PairingEvents
import java.lang.Exception

class MainActivity : AppCompatActivity(), PairingEvents {

    private var configParams = Config.prod
    lateinit var binding: ActivityMainBinding
    private var vPair: MassProvisioning? = null

    private var scanning = false
    private val statuses:  ArrayList<DeviceStatus> by lazy {
        ArrayList()
    }


    private val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { isGranted ->
            if (isGranted.isNotEmpty()) {
                vPair?.startMassProvision()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportFragmentManager.beginTransaction().add(R.id.container, DashboardFragment(), "").addToBackStack("main").commit()
        binding.log.movementMethod = ScrollingMovementMethod()
        binding.fab.visibility = View.INVISIBLE
        vPair?.eventsHandler = this
        configParams = Config.custom("{\"env\":\"dev\",\"apiURL\":\"https://dev.vayyarhomeapisdev.com\",\"cloud\":{\"registryId\":\"walabot_home_gen2\",\"cloudRegion\":\"us-central1\",\"projectName\":\"walabothome-app-cloud\",\"cloudType\":0},\"mqtt\":{\"hostUrl\":\"mqtts://mqtt.googleapis.com\",\"port\":443,\"username\":\"unused\",\"password\":\"unused\",\"clientId\":\"unused\",\"ntpUrl\":\"pool.ntp.org\"}}")!!

        vPair = MassProvisioning(this, configParams)
        binding.fab.setOnClickListener { _ ->
            binding.fab.setImageIcon(Icon.createWithResource(this, if (scanning) android.R.drawable.ic_popup_sync else android.R.drawable.ic_menu_close_clear_cancel))
            if (scanning) {
                vPair?.stopPairing()
            } else {
                vPair?.eventsHandler = this
//                vPair.analyticsHandler = this
                update("Scanning")
                vPair?.startMassProvision()
            }
            scanning = !scanning
        }
        binding.recycler.adapter = StatusAdapter()
        binding.recycler.layoutManager = LinearLayoutManager(this)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun acquirePermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED) {
            vPair?.startMassProvision()
        } else {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                requestPermissionLauncher.launch(permissions)
            } else {
                ActivityCompat.requestPermissions(this, permissions, 1)
            }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            vPair?.startMassProvision()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun update(message: String) {
        runOnUiThread {
            binding.log.text = "${binding.log.text}\n$message"
//            snackBar.setText(message)
        }
    }

    override fun onError(error: Throwable) {
        TODO("Not yet implemented")
    }

    override fun onWifiCredentialsFail(wifiList: List<EspWifiItem>) {
        shouldSelect(wifiList)
    }


    override fun onEvent(
        event: EspPairingEvent,
        isError: Boolean,
        message: String,
        deviceInfo: Map<String, Any>?,
        deviceId: String
    ) {
        try {
            val current = statuses.first { deviceId == it.deviceId }
            current.status = message
        } catch (e: Exception) {
            statuses.add(DeviceStatus(deviceId, message))
        }

        runOnUiThread {
            binding.recycler.adapter?.notifyDataSetChanged()
        }
    }

    override fun shouldSelect(wifiList: List<EspWifiItem>) {
        // setup the alert builder
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose Wifi")

// add a radio button list
        val wifiNames = wifiList.map { it.ssid }.toTypedArray()
        var index = 0
        builder.setSingleChoiceItems(wifiNames, index) { dialog, which ->
            index = which
        }


// add OK and Cancel buttons
        builder.setPositiveButton("OK") { dialog, which ->
            val editText = EditText(this@MainActivity)
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Wifi Code")
                .setMessage("Please enter the code for ${wifiNames[index]}")
                .setView(editText)
                .setPositiveButton("Submit"
                ) { p0, p1 ->
                    val item = wifiList.get(index)
                    vPair?.resumeConnection(item.ssid, item.bssid, editText.text.toString())
                }
                .setCancelable(true)
                .show()
        }
        builder.setNegativeButton("Cancel", null)

// create and show the alert dialog


        runOnUiThread {
            val dialog = builder.create()
            dialog.show()
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onMissingPermission(permissions: List<String>) {
        val _permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            requestPermissionLauncher.launch(_permissions)
        } else {
            ActivityCompat.requestPermissions(this, _permissions, 1)
        }
    }

    inner class StatusAdapter: Adapter<StatusAdapter.StatusItem>() {

        inner class StatusItem(itemView: View) : ViewHolder(itemView) {
            var status: DeviceStatus? = null
                set(value) {
                    field = value
                    itemView.findViewById<TextView>(R.id.title).text = value?.deviceId
                    itemView.findViewById<TextView>(R.id.subTitle).text = value?.status
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusItem {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.status_row, parent, false)

            return StatusItem(view)
        }

        override fun getItemCount(): Int {
            return statuses.size
        }

        override fun onBindViewHolder(holder: StatusItem, position: Int) {
            holder.status = statuses[position]
        }

    }


}