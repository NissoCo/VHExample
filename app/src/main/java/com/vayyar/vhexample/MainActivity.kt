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
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.vayyar.vhexample.databinding.ActivityMainBinding
import com.walabot.home.ble.BleDevice
import com.walabot.home.ble.Result
import com.walabot.home.ble.pairing.ConfigParams
import com.walabot.home.ble.pairing.esp.WalabotDeviceDesc
import com.walabot.home.ble.sdk.*

class MainActivity : AppCompatActivity(), PairingListener, WifiPickerFragment.Listener,
    ScannedDevicesFragment.Listener {

    private val ServerBaseUrl = "https://us-central1-walabothome-app-cloud.cloudfunctions.net"
    private val RegistryRegion = "us-central1"
    private val CloudProject = "walabothome-app-cloud"
    private val MqttUrl = "mqtts://mqtt.googleapis.com"
    private val MqttPort = 443
    private val configParams = ConfigParams(ServerBaseUrl, RegistryRegion, CloudProject, MqttUrl, MqttPort)
    private lateinit var binding: ActivityMainBinding
    private var vPair: MassProvisioning? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var snackBar: Snackbar
    private var scanning = false
    private val credentials = CloudCredentials(null, null, configParams)


    val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { isGranted ->
            if (isGranted.isNotEmpty()) {
                vPair?.scan()
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
        binding.log.movementMethod = ScrollingMovementMethod()
        vPair?.listener = this
        vPair = MassProvisioning(this, CloudCredentials(null, null, configParams))

//        vPair.analyticsHandler = this
//        findViewById<SwitchCompat>(R.id.massProvision).setOnCheckedChangeListener { p0, p1 ->
//            vPair = if (p1) {
//                MassProvision()
//            } else {
//                VPairSDK()
//            }
//        }

        snackBar = Snackbar.make(findViewById(R.id.mainView), "Waiting for your action", Snackbar.LENGTH_INDEFINITE)
        snackBar.show()

        binding.fab.setOnClickListener { _ ->
            binding.fab.setImageIcon(Icon.createWithResource(this, if (scanning) android.R.drawable.ic_popup_sync else android.R.drawable.ic_menu_close_clear_cancel))
            if (scanning) {
                vPair?.stopPairing()
            } else {
                vPair?.listener = this
//                vPair.analyticsHandler = this
                update("Scanning")
                vPair?.scan()
            }
            scanning = !scanning
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun acquirePermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED) {
            vPair?.scan()
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
            vPair?.scan()
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
//        val textView = findViewById<TextView>(R.id.log)
        runOnUiThread {
            binding.log.text = "${binding.log.text}\n$message"
            snackBar.setText(message)
        }
    }

    override fun onScan(scannedDevices: List<BleDevice>) {
        val fragment = ScannedDevicesFragment()
        fragment.listener = this
        fragment.devices = ArrayList(scannedDevices.map {
            BleDeviceStatus(it, false)
        })
        runOnUiThread {
            supportFragmentManager.beginTransaction().add(R.id.container, fragment, null).commit()
        }
    }


    override fun onFinish(result: Result<String>) {
        if (result.isSuccessfull) {
            runOnUiThread {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Pairing Done")
                    .setMessage("The device was paired successfully.")
                    .setPositiveButton("OK"
                    ) { p0, p1 ->

                    }
                    .setCancelable(true)
                    .show()
            }
        }
    }

    override fun onEvent(event: EspPairingEvent, deviceId: String?) {
        update("device - $deviceId - ${event.name}")
        if (event == EspPairingEvent.Connected) {
            update("Fetching Wifi Around you")
        }
    }

    override fun shouldSelect(wifiList: List<EspWifiItem>) {
        val fragment = WifiPickerFragment()
        fragment.listener = this
        fragment.wifiOptions = wifiList
        update("Pick Wifi")
        binding.log.visibility = View.INVISIBLE
        runOnUiThread {
            supportFragmentManager.beginTransaction().add(R.id.container, fragment, null).commit()
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onMissingPermission(permission: String) {
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

    override fun onPicked(wifiItem: EspWifiItem, password: String) {
        supportFragmentManager.popBackStack()
        vPair?.resumeConnection(wifiItem, password)
    }

    override fun onPickedDevices(devices: List<BleDevice>?) {
        supportFragmentManager.popBackStack()
    }


}