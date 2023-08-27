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
import com.walabot.home.ble.Message
import com.walabot.home.ble.Result
import com.walabot.home.ble.pairing.ConfigParams
import com.walabot.home.ble.sdk.*

class MainActivity : AppCompatActivity(), PairingEvents {

    private var configParams = Config.prod
    private lateinit var binding: ActivityMainBinding
    private var vPair: MassProvisioning? = null

//    private lateinit var snackBar: Snackbar
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
        configParams = Config.dev//Config.custom("{\"env\":\"dev\",\"apiURL\":\"https://dev.vayyarhomeapisdev.com\",\"userId\":\"QR7UAUp713aWIecEndwjEAhQp6p1\",\"accessToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6ImM2MGI5ZGUwODBmZmFmYmZjMTgzMzllY2Q0NGFjNzdmN2ZhNGU4ZDMiLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiTmlzc2ltIFBhcmRvIiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS9hL0FBY0hUdGNCRzBRZGJtT01ES0lFalp4am12c2tVSUFOUkFIM2M0UVNBRmtPVWc9czk2LWMiLCJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vd2FsYWJvdGhvbWUtYXBwLWNsb3VkIiwiYXVkIjoid2FsYWJvdGhvbWUtYXBwLWNsb3VkIiwiYXV0aF90aW1lIjoxNjkzMTU4MjQxLCJ1c2VyX2lkIjoiUVI3VUFVcDcxM2FXSWVjRW5kd2pFQWhRcDZwMSIsInN1YiI6IlFSN1VBVXA3MTNhV0llY0VuZHdqRUFoUXA2cDEiLCJpYXQiOjE2OTMxNTgyNDEsImV4cCI6MTY5MzE2MTg0MSwiZW1haWwiOiJhbmRyb2RvZ3NAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZW1haWwiOlsiYW5kcm9kb2dzQGdtYWlsLmNvbSJdfSwic2lnbl9pbl9wcm92aWRlciI6InBhc3N3b3JkIn19.Sbs8Krew3gwEmofA87Rjs4wAfcAgN4lQzHWy8Oe3kriYwzTkAw6bQ3XfJs5bCrmqijs5I5LYLB2t1UB-hC6jnPiezSSNtIOLxU4J47nSRfKsmxbZgswTtzxlAUyNc3zCZ9fek0OW5blp6ACWqUhd6syaivuHUy7Haz5Q6D5Ci3q72xtNTBPMeu4cGDjH5w-tRBzyKyS9n3Awia8l7cSUCpGe163tITnAPb5-vrmUbtlqkS55IXmwJHnbyj5lX3MSRep9dA1TPr1ZFCcAjgr5qZO80xQczOz7npifKs-8OzqLhkQwUijUkT9KKDJgPSQBdOpL01PbYTkT-gUeo137Iw\",\"wifi\":null,\"cloud\":{\"registryId\":\"walabot_home_gen2\",\"cloudRegion\":\"us-central1\",\"projectName\":\"walabothome-app-cloud\",\"cloudType\":0},\"mqtt\":{\"hostUrl\":\"mqtts://mqtt.googleapis.com\",\"port\":443,\"username\":\"unused\",\"password\":\"unused\",\"clientId\":\"unused\",\"ntpUrl\":\"pool.ntp.org\"}}")!!

        vPair = MassProvisioning(this, configParams)

//        vPair.analyticsHandler = this
//        findViewById<SwitchCompat>(R.id.massProvision).setOnCheckedChangeListener { p0, p1 ->
//            vPair = if (p1) {
//                MassProvision()
//            } else {
//                VPairSDK()
//            }
//        }

//        snackBar = Snackbar.make(findViewById(R.id.mainView), "Waiting for your action", Snackbar.LENGTH_INDEFINITE)
//        snackBar.show()

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
//            snackBar.setText(message)
        }
    }

    override fun onError(error: Throwable) {
        // handle errors
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
        val updateText = "device - $deviceId - $message\ndevInfo:: $deviceInfo"
        update(updateText)

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



}