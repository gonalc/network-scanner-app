package com.gonzalo.networkscanner.ui.views

import android.app.Application
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gonzalo.networkscanner.ui.components.NsdScanner
import com.gonzalo.networkscanner.ui.services.NetworkDevice
import com.gonzalo.networkscanner.ui.services.NetworkScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworkScannerViewModel(
    application: Application,
    private val arpScanner: NetworkScanner,
    private val nsdScanner: NsdScanner,
) : AndroidViewModel(application) {

    // Unified device list combining results from both scanners
    private val _devices = MutableStateFlow<List<UnifiedNetworkDevice>>(emptyList())
    val devices: StateFlow<List<UnifiedNetworkDevice>> = _devices.asStateFlow()

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // Track which scanning methods are active
    private val _scanningMethods = MutableStateFlow(setOf<ScanMethod>())
    val scanningMethods: StateFlow<Set<ScanMethod>> = _scanningMethods.asStateFlow()

    // Separate collections for each scanner
    private val arpDevices = mutableMapOf<String, NetworkDevice>() // Keyed by IP
    private val nsdServices = mutableMapOf<String, NsdServiceInfo>() // Keyed by IP

    init {
        setupNsdListener()
    }

    private fun setupNsdListener() {
        nsdScanner.onServiceDiscovered = { serviceInfo ->
            serviceInfo.host?.hostAddress?.let { ipAddress ->
                Log.d("NetworkScanner", "NSD discovered: ${serviceInfo.serviceName} at $ipAddress (${serviceInfo.serviceType})")
                nsdServices[ipAddress] = serviceInfo
                mergeAndUpdateDevices()
            }
        }
    }

    /**
     * Extract a clean device name from mDNS service name.
     * Service names often contain extra info like "[b8:27:eb:xx:xx:xx]" or device type.
     */
    private fun cleanServiceName(serviceName: String): String {
        // Remove MAC address in brackets like "[b8:27:eb:11:22:33]"
        var cleaned = serviceName.replace(Regex("""\[([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}\]"""), "").trim()

        // Remove common prefixes/suffixes
        cleaned = cleaned.replace(Regex("""^(\._.*|_.*\.local\.?)"""), "").trim()

        return if (cleaned.isNotEmpty()) cleaned else serviceName
    }

    fun startCompleteScan() {
        viewModelScope.launch {
            _scanState.value = ScanState.SCANNING
            _devices.value = emptyList()
            arpDevices.clear()
            nsdServices.clear()

            // Start both scanning methods in parallel
            launch { startArpScan() }
            launch { startNsdScan() }
        }
    }

    private suspend fun startArpScan() {
        _scanningMethods.value += ScanMethod.ARP

        try {
            val devices = arpScanner.scanNetwork()
            devices.forEach { device ->
                arpDevices[device.ipAddress] = device
            }
            mergeAndUpdateDevices()
        } catch (e: Exception) {
            Log.e("NetworkScanner", "ARP scan failed", e)
        } finally {
            _scanningMethods.value -= ScanMethod.ARP
            checkIfScanComplete()
        }
    }

    private fun startNsdScan() {
        _scanningMethods.value += ScanMethod.NSD
        nsdScanner.startDiscovery()

        // Stop NSD scan after a timeout (e.g., 10 seconds)
        viewModelScope.launch {
            delay(10000)
            stopNsdScan()
        }
    }

    private fun stopNsdScan() {
        nsdScanner.stopDiscovery()
        _scanningMethods.value -= ScanMethod.NSD
        checkIfScanComplete()
    }

    private fun mergeAndUpdateDevices() {
        val mergedDevices = mutableMapOf<String, UnifiedNetworkDevice>()

        // Add all ARP-discovered devices
        arpDevices.forEach { (ip, device) ->
            mergedDevices[ip] = UnifiedNetworkDevice(
                ipAddress = ip,
                hostname = device.hostname,
                macAddress = device.macAddress,
                vendor = device.vendor,
                isReachable = device.isReachable,
                services = emptyList(),
                discoveryMethod = setOf(DiscoveryMethod.ARP)
            )
        }

        // Merge NSD services
        nsdServices.forEach { (ip, service) ->
            val cleanedName = cleanServiceName(service.serviceName)
            val existingDevice = mergedDevices[ip]
            if (existingDevice != null) {
                // Device was found by both methods - merge the data
                mergedDevices[ip] = existingDevice.copy(
                    services = existingDevice.services + ServiceInfo(
                        name = cleanedName,
                        type = service.serviceType,
                        port = service.port
                    ),
                    discoveryMethod = existingDevice.discoveryMethod + DiscoveryMethod.NSD,
                    hostname = existingDevice.hostname ?: cleanedName
                )
            } else {
                // Device only found by NSD
                mergedDevices[ip] = UnifiedNetworkDevice(
                    ipAddress = ip,
                    hostname = cleanedName,
                    macAddress = null,
                    vendor = null,
                    isReachable = true,
                    services = listOf(
                        ServiceInfo(
                            name = cleanedName,
                            type = service.serviceType,
                            port = service.port
                        )
                    ),
                    discoveryMethod = setOf(DiscoveryMethod.NSD)
                )
            }
        }

        _devices.value = mergedDevices.values
            .sortedBy { it.ipAddress.split(".").last().toIntOrNull() ?: 0 }
    }

    private fun checkIfScanComplete() {
        if (_scanningMethods.value.isEmpty()) {
            _scanState.value = ScanState.COMPLETED
        }
    }

    override fun onCleared() {
        super.onCleared()
        nsdScanner.stopDiscovery()
    }

    companion object {
        fun getFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(NetworkScannerViewModel::class.java)) {
                        val nsdScanner = NsdScanner(application.applicationContext)
                        val arpScanner = NetworkScanner(application.applicationContext)

                        return NetworkScannerViewModel(
                            application,
                            arpScanner,
                            nsdScanner
                        ) as T
                    }

                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

// Data classes for the unified model
data class UnifiedNetworkDevice(
    val ipAddress: String,
    val hostname: String?,
    val macAddress: String?,
    val vendor: String?,
    val isReachable: Boolean,
    val services: List<ServiceInfo>,
    val discoveryMethod: Set<DiscoveryMethod>
)

data class ServiceInfo(
    val name: String,
    val type: String,
    val port: Int
)

enum class DiscoveryMethod {
    ARP, NSD
}

enum class ScanMethod {
    ARP, NSD
}

enum class ScanState {
    IDLE,        // Not scanning, waiting for user action
    SCANNING,    // Currently scanning the network
    COMPLETED,   // Scan finished successfully
    ERROR        // Scan failed with an error
}