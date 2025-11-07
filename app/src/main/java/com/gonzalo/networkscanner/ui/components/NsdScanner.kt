package com.gonzalo.networkscanner.ui.components

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class NsdScanner(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val discoveredServices = mutableListOf<NsdServiceInfo>()
    private val activeListeners = mutableListOf<NsdManager.DiscoveryListener>()

    // Common mDNS service types to discover
    private val serviceTypes = listOf(
        "_http._tcp",           // Web servers
        "_https._tcp",          // Secure web servers
        "_ssh._tcp",            // SSH servers (Raspberry Pi, Linux machines)
        "_sftp-ssh._tcp",       // SFTP servers
        "_smb._tcp",            // Samba/Windows file sharing
        "_afpovertcp._tcp",     // Apple File Protocol
        "_airplay._tcp",        // Apple AirPlay devices
        "_googlecast._tcp",     // Google Cast devices (Chromecast)
        "_printer._tcp",        // Printers
        "_ipp._tcp",            // Internet Printing Protocol
        "_scanner._tcp",        // Scanners
        "_workstation._tcp",    // Workstations
        "_device-info._tcp",    // Device information
        "_raop._tcp"            // Remote Audio Output Protocol (Apple)
    )

    private fun createDiscoveryListener(serviceType: String) = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(type: String) {
            Log.d("NSD", "Discovery started for $serviceType")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d("NSD", "Service found [$serviceType]: ${service.serviceName}")
            resolveService(service)
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.d("NSD", "Service lost: ${service.serviceName}")
            discoveredServices.remove(service)
        }

        override fun onDiscoveryStopped(type: String) {
            Log.d("NSD", "Discovery stopped for $serviceType")
        }

        override fun onStartDiscoveryFailed(type: String, errorCode: Int) {
            Log.e("NSD", "Start discovery failed for $serviceType: $errorCode")
            try {
                nsdManager.stopServiceDiscovery(this)
            } catch (e: Exception) {
                Log.e("NSD", "Error stopping failed discovery", e)
            }
        }

        override fun onStopDiscoveryFailed(type: String, errorCode: Int) {
            Log.e("NSD", "Stop discovery failed for $serviceType: $errorCode")
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
        Log.d("NSD", "Starting discovery for ${serviceTypes.size} service types")

        // Start discovery for each service type
        serviceTypes.forEach { serviceType ->
            try {
                val listener = createDiscoveryListener(serviceType)
                nsdManager.discoverServices(
                    serviceType,
                    NsdManager.PROTOCOL_DNS_SD,
                    listener
                )
                activeListeners.add(listener)
            } catch (e: Exception) {
                Log.e("NSD", "Failed to start discovery for $serviceType", e)
            }
        }
    }

    fun stopDiscovery() {
        if (activeListeners.isEmpty()) {
            Log.d("NSD", "No active discoveries to stop")
            return
        }

        Log.d("NSD", "Stopping ${activeListeners.size} active discoveries")

        activeListeners.forEach { listener ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: IllegalArgumentException) {
                Log.w("NSD", "Listener not registered: ${e.message}")
            } catch (e: Exception) {
                Log.e("NSD", "Error stopping discovery: ${e.message}")
            }
        }

        activeListeners.clear()
        Log.d("NSD", "All discoveries stopped")
    }

    fun isDiscovering(): Boolean = activeListeners.isNotEmpty()
}