package com.vayyar.vhexample

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.walabot.home.ble.sdk.*

class ConfigHandler(val env: String) {
    val baseUrl: String
        get() = if (env == "dev") "https://devaz.vayyarhomeapisdev.com" else "https://devaz.vayyarhomeapisdev.com"

    val region: String
        get() = "us-central1"

    val cloudProj: String
        get() = if (env == "dev") "vayyar-home-azure" else "walabot-home"

    val url: String
        get() = "mqtts://mqtt.googleapis.com"

    val port: Int
        get() = 443

    val configParams: ConfigParams
        get() = ConfigParams(baseUrl, region, cloudProj, url, port)

}

class MainActivity : AppCompatActivity(), PairingListener, AnalyticsHandler {
    private lateinit var binding: ActivityMainBinding
    private lateinit var vPair: VPairSDK
    private var wifiOptions: List<EspWifiItem>? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var snackBar: Snackbar
    private var scanning = false


    val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { isGranted ->
            if (isGranted.isNotEmpty()) {
                vPair.startPairing()
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
        vPair = VPairSDK(this)
        vPair.cloudCredentials = CloudCredentials(null, null, ConfigHandler("prod").configParams)
        Log.d("test", vPair.isBleOn().toString())
//        findViewById<SwitchCompat>(R.id.massProvision).setOnCheckedChangeListener { p0, p1 ->
//            vPair = if (p1) {
//                MassProvision(this, CloudCredentials(null, null, configParams))
//            } else {
//                VPairSDK(this, CloudCredentials(null, null, configParams))
//            }
//            vPair.listener = this
//            vPair.analyticsHandler = this
//        }

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
                acquirePermissions()
            }
            scanning = !scanning
        }
    }

    private fun acquirePermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED) {
            vPair.startPairing()
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
            vPair.startPairing()
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


    override fun shouldRetry() {

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
        update(event.name)
        if (event == EspPairingEvent.Connected) {
            update("Fetching Wifi Around you")
        }
    }

    override fun shouldSelect(wifiList: List<EspWifiItem>) {
        wifiOptions = wifiList
        update("Pick Wifi")
        runOnUiThread {
            recyclerView.visibility = View.VISIBLE
            recyclerView.adapter?.notifyDataSetChanged()
        }

    }

    override fun onMissingPermission(permission: String) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            TODO("VERSION.SDK_INT < S")
        }
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