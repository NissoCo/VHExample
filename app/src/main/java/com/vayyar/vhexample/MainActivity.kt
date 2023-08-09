package com.vayyar.vhexample

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.vayyar.vhexample.databinding.ActivityMainBinding
import com.walabot.home.ble.BleDevice
import com.walabot.home.ble.Result
import com.walabot.home.ble.pairing.ConfigParams
import com.walabot.home.ble.sdk.*

class MainActivity : AppCompatActivity(), PairingEvents {

    private val configParams = Config.dev
    private lateinit var binding: ActivityMainBinding
    private var vPair: MassProvisioning? = null

    private lateinit var snackBar: Snackbar
    private var scanning = false


    val requestPermissionLauncher = registerForActivityResult(
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
        binding.log.movementMethod = ScrollingMovementMethod()
        vPair?.eventsHandler = this
        vPair = MassProvisioning(this, configParams)

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
                vPair?.eventsHandler = this
//                vPair.analyticsHandler = this
                update("Scanning")
                vPair?.startMassProvision()
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
            snackBar.setText(message)
        }
    }

    override fun onError(error: Throwable) {
        // handle errors
    }

    override fun onWifiCredentialsFail(wifiList: List<EspWifiItem>) {
        shouldSelect(wifiList)
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
        var updateText = event.name
        deviceId?.let {
            updateText = "device - $deviceId - ${event.name}"
        }
        update(updateText)
        if (event == EspPairingEvent.Connected) {
            update("Fetching Wifi Around you")
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
                    vPair?.resumeConnection(wifiList.get(index), editText.text.toString())
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



}