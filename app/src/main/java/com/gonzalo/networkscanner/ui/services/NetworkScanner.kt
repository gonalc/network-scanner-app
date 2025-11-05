package com.gonzalo.networkscanner.ui.services

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress

data class NetworkDevice(
    val ipAddress: String,
    val hostname: String?,
    val macAddress: String?,
    val isReachable: Boolean,
)

class NetworkScanner(private val context: Context) {
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    suspend fun scanNetwork(): List<NetworkDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<NetworkDevice>()
        val localIp = getLocalIpAddress()
        val subnet = localIp?.substringBeforeLast(".") ?: return@withContext emptyList()

        // Scan subnet range (typically .1 to .254)
        val scanJobs = (1..254).map { i ->
            async {
                val host = "$subnet.$i"
                if (isHostReachable(host)) {
                    val hostname = tryGetHostname(host)
                    val macAddress = getMacFromArp(host)
                    NetworkDevice(
                        ipAddress = host,
                        hostname = hostname,
                        macAddress = macAddress,
                        isReachable = true
                    )
                } else null
            }
        }

        devices.addAll(scanJobs.awaitAll().filterNotNull())
        return@withContext devices
    }

    private fun tryGetHostname(ipAddress: String): String? {
        return try {
            val inetAddress = InetAddress.getByName(ipAddress)
            val hostname = inetAddress.canonicalHostName
            // If the hostname is the same as IP, no hostname was found
            if (hostname == ipAddress) null else hostname
        } catch (e: Exception) {
            null
        }
    }

    private fun isHostReachable(host: String): Boolean {
        return try {
            val inet = InetAddress.getByName(host)
            inet.isReachable(100) // 100ms timeout
        } catch (e: Exception) {
            false
        }
    }

    private fun getMacFromArp(ipAddress: String): String? {
        return try {
            val br = BufferedReader(FileReader("/proc/net/arp"))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                val splitted = line!!.split(" +".toRegex())
                if (splitted.size >= 4 && splitted[0] == ipAddress) {
                    return splitted[3]
                }
            }
            br.close()
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun getLocalIpAddress(): String? {
        val wifiInfo = wifiManager.connectionInfo
        val ipInt = wifiInfo.ipAddress
        return if (ipInt != 0) {
            intToIp(ipInt)
        } else null
    }

    private fun intToIp(ipInt: Int): String {
        return "${ipInt and 0xFF}.${ipInt shr 8 and 0xFF}.${ipInt shr 16 and 0xFF}.${ipInt shr 24 and 0xFF}"
    }
}