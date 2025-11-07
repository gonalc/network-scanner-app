package com.gonzalo.networkscanner.ui.services

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Utility for looking up device manufacturer from MAC address OUI (first 3 bytes).
 * OUI = Organizationally Unique Identifier assigned by IEEE.
 *
 * Loads vendor database from assets/oui.csv on first use and caches in memory.
 */
class MacVendorLookup(private val context: Context) {

    private val ouiDatabase: MutableMap<String, String> = mutableMapOf()
    private var isLoaded = false

    /**
     * Look up the manufacturer/vendor name from a MAC address.
     *
     * @param macAddress MAC address in any common format (e.g., "AA:BB:CC:DD:EE:FF", "AA-BB-CC-DD-EE-FF", "aabbccddeeff")
     * @return Vendor name if found, null otherwise
     */
    fun getVendorName(macAddress: String?): String? {
        if (macAddress == null || macAddress.length < 8) {
            Log.d("MacVendorLookup", "Invalid MAC address: $macAddress")
            return null
        }

        // Load database on first use
        if (!isLoaded) {
            loadDatabase()
        }

        // Normalize MAC address to "XX:XX:XX" format for the first 3 bytes
        val normalized = macAddress
            .replace("-", ":")
            .replace(".", ":")
            .uppercase()
            .split(":")
            .take(3)
            .joinToString(":")

        val vendor = ouiDatabase[normalized]
        Log.d("MacVendorLookup", "Lookup: $macAddress -> $normalized -> $vendor")
        return vendor
    }

    /**
     * Load the OUI database from assets/oui.csv
     * Format: OUI,Vendor (e.g., "00:03:93,Apple")
     */
    private fun loadDatabase() {
        try {
            Log.d("MacVendorLookup", "Loading OUI database from assets...")
            val inputStream = context.assets.open("oui.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            reader.useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split(",")
                    if (parts.size == 2) {
                        ouiDatabase[parts[0].trim()] = parts[1].trim()
                    }
                }
            }

            Log.d("MacVendorLookup", "Loaded ${ouiDatabase.size} vendors from database")
            isLoaded = true
        } catch (e: Exception) {
            // If file doesn't exist or can't be read, just use empty database
            Log.e("MacVendorLookup", "Failed to load OUI database", e)
            e.printStackTrace()
            isLoaded = true
        }
    }
}