package com.vayyar.vhexample.ui.dashboard

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.walabot.home.ble.BleDevice
import com.walabot.home.ble.sdk.Operational.Operational
import com.walabot.home.ble.sdk.Operational.OperationalEvents

class DashboardViewModel : ViewModel(), OperationalEvents {



    var devices = MutableLiveData<Map<String, BleDevice>?>()
    var hasPermission = MutableLiveData<Boolean>()
    var log = MutableLiveData<String>()
    var operational: Operational? = null
    var isConnected = MutableLiveData<Boolean>()

    fun fetchDevices() {
        operational?.events = this
        operational?.scan(5000)
    }

    fun stop() {
        operational?.disconnect()
        this.devices.apply {
            value = null
        }
    }


    override fun onScanned(devices: Map<String, BleDevice>) {
        this.devices.apply {
            value = devices.toSortedMap()
        }

    }

    override fun onMissingPermission(permissions: List<String>) {
        hasPermission.apply {
            value = true
        }
    }

    override fun onAwaitForCommand() {
        isConnected.apply {
            value = true
        }
        log.apply {
            value = "Waiting for commands"
        }
    }

    override fun onDisconnected() {
        isConnected.apply {
            value = false
        }
    }

    override fun onEvent(event: String) {
        log.apply {
            value = event
        }
    }

}