package com.pierdr.tramontana.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pierdr.pierluigidallarosa.myactivity.R
import com.pierdr.tramontana.io.wifiIpAddress
import kotlinx.android.synthetic.main.fragment_ready.*

class ReadyFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ready, container, false)
    }

    private val connectivityChangesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateWifiAddressView()
        }
    }

    override fun onResume() {
        super.onResume()
        updateWifiAddressView()

        requireContext().registerReceiver(connectivityChangesReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    fun updateWifiAddressView() {
        ipAddress.text = wifiIpAddress(requireContext()) ?: "no WiFi connectivity?"
    }

    override fun onPause() {
        requireContext().unregisterReceiver(connectivityChangesReceiver)
        super.onPause()
    }
}