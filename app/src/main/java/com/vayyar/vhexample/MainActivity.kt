package com.vayyar.vhexample

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.vayyar.vhexample.databinding.ActivityMainBinding
import com.walabot.home.ble.Result
import com.walabot.home.ble.pairing.ConfigParams
import com.walabot.home.ble.pairing.esp.WalabotDeviceDesc
import com.walabot.home.ble.sdk.*

class MainActivity : AppCompatActivity(), PairingListener, AnalyticsHandler {

    private val ServerBaseUrl = "https://us-central1-vayyar-care.cloudfunctions.net"
    private val RegistryRegion = "us-central1"
    private val CloudProject = "vayyar-care"
    private val MqttUrl = "mqtts://mqtt.googleapis.com"
    private val MqttPort = 443
    private val configParams = ConfigParams(ServerBaseUrl, RegistryRegion, CloudProject, MqttUrl, MqttPort)
    private lateinit var binding: ActivityMainBinding
    private var vPair = VPairSDK()
    private var wifiOptions: List<EspWifiItem>? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var snackBar: Snackbar
    private var scanning = false
    private val credentials = CloudCredentials("pRkprGHnS4TSCoaL8P8e2jsXd1Z2", "eyJhbGciOiJSUzI1NiIsImtpZCI6ImFlYjMxMjdiMjRjZTg2MDJjODEyNDUxZThmZTczZDU4MjkyMDg4N2MiLCJ0eXAiOiJKV1QifQ.eyJkZXZpY2UiOnRydWUsImRldmljZUlkIjoiaWRfTjBNNk9VVTZRa1E2UVVZNk1VUTZNakEiLCJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vdmF5eWFyLWNhcmUiLCJhdWQiOiJ2YXl5YXItY2FyZSIsImF1dGhfdGltZSI6MTY3NjQ3NDM2NSwidXNlcl9pZCI6ImlkX04wTTZPVVU2UWtRNlFVWTZNVVE2TWpBIiwic3ViIjoiaWRfTjBNNk9VVTZRa1E2UVVZNk1VUTZNakEiLCJpYXQiOjE2NzY0NzQzNjUsImV4cCI6MTY3NjQ3Nzk2NSwiZmlyZWJhc2UiOnsiaWRlbnRpdGllcyI6e30sInNpZ25faW5fcHJvdmlkZXIiOiJjdXN0b20ifX0.5mj_393u9BNQLJmybZDcsV7wtLqCQrqy1yyY5glqHHy7iKP9xuuyie6t54mlesxUBuDsq6hcvT1mC8_P8Ym3QFeA31WkbfTMlI2bQY3NllXjkcifFfHTY7zBEbuFs-3ZwTGj847-10s6MjHahqxZ9QH4P7zIM1MysePYGGRsZsPtfXVDKfJDgvjOeX3lDwlnjNj9Zdhosq3kGeBXsMnG9OKTWnVU-icjDfInOHcex8TiiRb05_wpxZRrI2PPPlmqJ3cVjCSgCd3qOydaFtwdFD1KJ5JqGLhd3tU91yaeYCwPrNGq1PdIeJc0QtwbFHwkEG3ZTstqe7v5ZAIOx5Kuug", configParams)


    val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { isGranted ->
            if (isGranted.isNotEmpty()) {
                vPair.startPairing(this, credentials)
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
        vPair.listener = this
        vPair.analyticsHandler = this
        findViewById<SwitchCompat>(R.id.massProvision).setOnCheckedChangeListener { p0, p1 ->
            vPair = if (p1) {
                MassProvision()
            } else {
                VPairSDK()
            }
        }

        snackBar = Snackbar.make(findViewById(R.id.mainView), "Waiting for your action", Snackbar.LENGTH_INDEFINITE)
        snackBar.show()
        recyclerView = findViewById(R.id.wifiScannedRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = WifiAdapter()
        binding.fab.setOnClickListener { _ ->
            binding.fab.setImageIcon(Icon.createWithResource(this, if (scanning) android.R.drawable.ic_popup_sync else android.R.drawable.ic_menu_close_clear_cancel))
            if (scanning) {
                vPair.stopPairing()
            } else {
                vPair.listener = this
                vPair.analyticsHandler = this
                vPair.startPairing(this, credentials)
            }
            scanning = !scanning
        }
    }

    private fun acquirePermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED) {
            vPair.startPairing(this, credentials)
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
            vPair.startPairing(this, credentials)
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
            snackBar.setText(message)
        }
    }



    override fun onFinish(result: Result<WalabotDeviceDesc>) {
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
        update(event.name)
        if (event == EspPairingEvent.Connected) {
            update("Fetching Wifi Around you")
        }
    }

    override fun shouldSelect(wifiList: List<EspWifiItem>) {
        wifiOptions = wifiList
        update("Pick Wifi")
        runOnUiThread {
            recyclerView.adapter?.notifyDataSetChanged()
        }

    }

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

    override fun log(components: ArrayList<AnalyticsComponents>) {

    }

    inner class WifiAdapter: RecyclerView.Adapter<WifiAdapter.WifiViewHolder>() {

        inner class WifiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private var textView: TextView
            var item: EspWifiItem? = null
            set(value) {
                field = value
                textView.text = value?.ssid.toString()
            }
            init {
                textView = itemView.findViewById(R.id.textView)
                val editText = EditText(this@MainActivity)
                textView.setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Wifi Code")
                        .setMessage("Please enter the code for the selected WiFi")
                        .setView(editText)
                        .setPositiveButton("Submit"
                        ) { p0, p1 ->
                            recyclerView.visibility = View.INVISIBLE
                            vPair.resumeConnection(item!!, editText.text.toString())
                        }
                        .setCancelable(true)
                        .show()

                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiAdapter.WifiViewHolder {
            return WifiViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.wifi_view_holde, parent, false))
        }

        override fun onBindViewHolder(holder: WifiAdapter.WifiViewHolder, position: Int) {
            holder.item = wifiOptions?.get(position)
        }

        override fun getItemCount(): Int {
            return wifiOptions?.size ?: 0
        }

    }
}