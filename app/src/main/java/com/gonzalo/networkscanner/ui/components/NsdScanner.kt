package com.gonzalo.networkscanner.ui.components

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class NsdScanner(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val discoveredServices = mutableListOf<NsdServiceInfo>()

    private var isDiscovering = false

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {
            Log.d("NSD", "Discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d("NSD", "Service found: ${service.serviceName}")
            resolveService(service)
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            discoveredServices.remove(service)
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.d("NSD", "Discovery stopped")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private fun resolveService(service: NsdServiceInfo) {
        nsdManager.resolveService(service, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("NSD", "Resolve failed: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d("NSD", "Resolved service: ${serviceInfo.serviceName}")
                Log.d("NSD", "Host: ${serviceInfo.host}")
                Log.d("NSD", "Port: ${serviceInfo.port}")

                // Add the resolved service to our list
                discoveredServices.add(serviceInfo)

                // Notify listeners or update UI
                onServiceDiscovered?.invoke(serviceInfo)
            }
        })
    }

    var onServiceDiscovered: ((NsdServiceInfo) -> Unit)? = null

    fun startDiscovery() {
        nsdManager.discoverServices(
            "_http._tcp", // Service type
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    fun stopDiscovery() {
        if (!isDiscovering) {
            Log.d("NSD", "Discovery not active, nothing to stop")
            return
        }

        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
            isDiscovering = false
            Log.d("NSD", "Discovery stopped successfully")
        } catch (e: IllegalArgumentException) {
            // This can happen if Android already stopped the listener
            Log.w("NSD", "Discovery listener not registered: ${e.message}")
            isDiscovering = false
        } catch (e: Exception) {
            Log.e("NSD", "Error stopping discovery: ${e.message}")
            // Still set to false as discovery is likely stopped anyway
            isDiscovering = false
        }
    }

    fun isDiscovering(): Boolean = isDiscovering
}