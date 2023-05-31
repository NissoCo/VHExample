package com.vayyar.vhexample

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.walabot.home.ble.sdk.EspWifiItem


/**
 * A simple [Fragment] subclass.
 * Use the [WifiPickerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WifiPickerFragment : Fragment() {
    interface Listener {
        fun onPicked(wifiItem: EspWifiItem, password: String)
    }
    var wifiOptions: List<EspWifiItem>? = null
    var listener: Listener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_wifi_picker, container, false)
            view.findViewById<RecyclerView>(R.id.wifiScannedRecycler).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = WifiAdapter()
            }

        return view
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
                val editText = EditText(context)
                textView.setOnClickListener {
                    context?.let { it1 ->
                        AlertDialog.Builder(it1)
                            .setTitle("Wifi Code")
                            .setMessage("Please enter the code for the selected WiFi")
                            .setView(editText)
                            .setPositiveButton("Submit"
                            ) { p0, p1 ->
                                listener?.onPicked(item!!, editText.text.toString())
//                                binding.log.visibility = View.VISIBLE
//                                recyclerView.visibility = View.INVISIBLE
//                                vPair?.resumeConnection(item!!, editText.text.toString())
                            }
                            .setCancelable(true)
                            .show()
                    }

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