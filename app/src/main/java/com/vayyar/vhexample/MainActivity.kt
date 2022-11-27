package com.vayyar.vhexample

import android.Manifest
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.vayyar.vhexample.databinding.ActivityMainBinding
import com.walabot.home.ble.Result
import com.walabot.home.ble.pairing.esp.ProtocolMediator
import com.walabot.home.ble.pairing.esp.WalabotDeviceDesc
import com.walabot.home.ble.sdk.*

class MainActivity : AppCompatActivity(), PairingListener, AnalyticsHandler {

    private lateinit var binding: ActivityMainBinding
    private val vPair = VPairSDK()
    private var wifiOptions: List<EspWifiItem>? = null
    private lateinit var recyclerView: RecyclerView


    val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { isGranted ->
            if (isGranted.isNotEmpty()) {
                vPair.startPairing(this, CloudCredentials(null, null, false))
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

        recyclerView = findViewById(R.id.wifiScannedRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = WifiAdapter()
        binding.fab.setOnClickListener { view ->
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // You can use the API that requires the permission.
//                }
//                shouldShowRequestPermissionRationale(...) -> {
//                // In an educational UI, explain to the user why your app requires this
//                // permission for a specific feature to behave as expected. In this UI,
//                // include a "cancel" or "no thanks" button that allows the user to
//                // continue using your app without granting the permission.
//                showInContextUI(...)
                    vPair.startPairing(this, CloudCredentials(null, null, false))
            }

                else -> {
                    // You can directly ask for the permission.
                    // The registered ActivityResultCallback gets the result of this request.
                    requestPermissionLauncher.launch(
                        arrayOf(Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ))
                }
            }
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



    override fun onFinish(result: Result<WalabotDeviceDesc>) {
        if (result.isSuccessfull) {
            binding.fab.isEnabled = true
            Snackbar.make(findViewById(R.id.mainView), "Wifi configured successfully", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onEvent(event: EspPairingEvent, deviceDesc: WalabotDeviceDesc?) {
        if (event == EspPairingEvent.Connecting) {
            binding.fab.isEnabled = false
        }
        Snackbar.make(findViewById(R.id.mainView), event.name, Snackbar.LENGTH_SHORT).show()
    }

    override fun shouldSelect(wifiList: List<EspWifiItem>) {
        wifiOptions = wifiList
        recyclerView.adapter?.notifyDataSetChanged()
    }

    override fun log(components: ArrayList<AnalyticsComponents>) {

    }

    inner class WifiAdapter: RecyclerView.Adapter<WifiAdapter.WifiViewHolder>() {

        inner class WifiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private var textView: TextView
            var item: EspWifiItem? = null
            set(value) {
                field = value
                textView.text = value?.rssi.toString()
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
                        ) { p0, p1 -> vPair.resumeConnection(item!!, editText.text.toString()) }
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