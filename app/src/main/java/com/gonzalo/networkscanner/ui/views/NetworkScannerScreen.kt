package com.gonzalo.networkscanner.ui.views

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScannerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    val networkScannerViewModel: NetworkScannerViewModel =
        viewModel(factory = NetworkScannerViewModel.getFactory(application))

    val scanState by networkScannerViewModel.scanState.collectAsState()
    val devices by networkScannerViewModel.devices.collectAsState()
    val scanningMethods by networkScannerViewModel.scanningMethods.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Scanner") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { networkScannerViewModel.startCompleteScan() },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            ) {
                if (scanState == ScanState.SCANNING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Scan Network"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Card
            StatusCard(
                scanState = scanState,
                deviceCount = devices.size,
                scanningMethods = scanningMethods
            )

            // Device List or Empty State
            if (devices.isEmpty() && scanState != ScanState.SCANNING) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices) { device ->
                        DeviceCard(device = device)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    scanState: ScanState,
    deviceCount: Int,
    scanningMethods: Set<ScanMethod>,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (scanState) {
                ScanState.SCANNING -> MaterialTheme.colorScheme.secondaryContainer
                ScanState.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                ScanState.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (scanState) {
                        ScanState.IDLE -> "Ready to scan"
                        ScanState.SCANNING -> "Scanning network..."
                        ScanState.COMPLETED -> "Scan completed"
                        ScanState.ERROR -> "Scan failed"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (deviceCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text("$deviceCount devices")
                    }
                }
            }

            // Show active scanning methods
            AnimatedVisibility(visible = scanningMethods.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    scanningMethods.forEach { method ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = when (method) {
                                        ScanMethod.ARP -> "Scanning IPs..."
                                        ScanMethod.NSD -> "Discovering services..."
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        )
                    }
                }
            }

            // Progress indicator
            AnimatedVisibility(visible = scanState == ScanState.SCANNING) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCard(device: UnifiedNetworkDevice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* Handle device click if needed */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with hostname and discovery badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.hostname ?: device.vendor?.let { "$it Device" } ?: "Unknown Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = device.ipAddress,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Discovery method badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    device.discoveryMethod.forEach { method ->
                        Badge(
                            containerColor = when (method) {
                                DiscoveryMethod.ARP -> MaterialTheme.colorScheme.primary
                                DiscoveryMethod.NSD -> MaterialTheme.colorScheme.secondary
                            }
                        ) {
                            Text(
                                text = method.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // MAC Address if available
            device.macAddress?.let { mac ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MAC: ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = mac,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Services if available
            if (device.services.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Services:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                device.services.forEach { service ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ${service.name}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Port: ${service.port}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Connection status indicator
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(end = 4.dp)
                ) {
                    Badge(
                        containerColor = if (device.isReachable) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
                Text(
                    text = if (device.isReachable) "Online" else "Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share, // Need to add the wifi one
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No devices found",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tap the scan button to discover devices on your network",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}