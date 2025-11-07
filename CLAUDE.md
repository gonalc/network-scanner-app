# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Network Scanner is an Android application that discovers devices on a local network using two parallel scanning methods:
1. **ARP scanning**: Scans IP addresses in the subnet range, checks reachability, and reads MAC addresses from `/proc/net/arp`
2. **NSD (Network Service Discovery)**: Uses Android's NsdManager to discover HTTP services via DNS-SD

The app merges results from both methods into a unified device list, displaying IP addresses, hostnames, MAC addresses, discovered services, and online status.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean build artifacts
./gradlew clean

# Run a single test class
./gradlew test --tests com.gonzalo.networkscanner.ExampleUnitTest

# Run linting checks
./gradlew lint
```

## Architecture

**Technology Stack:**
- Kotlin with Jetpack Compose for UI (Material 3)
- MVVM architecture with AndroidViewModel
- Kotlin Coroutines for async operations with StateFlow for reactive state management
- minSdk: 24, targetSdk: 35, compileSdk: 35
- Java 11 compatibility

**Key Components:**

### Network Scanning Layer
- `NetworkScanner` (ui/services/NetworkScanner.kt): ARP-based scanner that scans subnet ranges (1-254), performs ICMP reachability checks (100ms timeout), and parses `/proc/net/arp` for MAC addresses
- `NsdScanner` (ui/components/NsdScanner.kt): Wraps Android NsdManager to discover `_http._tcp` services via DNS-SD protocol with service resolution callbacks

### ViewModel Layer
- `NetworkScannerViewModel` (ui/views/NetworkScannerViewModel.kt): Orchestrates both scanners in parallel, maintains separate collections (`arpDevices` map, `nsdServices` map), merges results into `UnifiedNetworkDevice` list, and tracks scan state (IDLE/SCANNING/COMPLETED/ERROR)
- Factory pattern used for ViewModel instantiation to inject scanner dependencies

### Data Flow
1. User triggers scan via FAB button
2. ViewModel launches both scanners concurrently using coroutine `launch` blocks
3. `NetworkScanner` scans all 254 IPs in parallel using `async`/`awaitAll`
4. `NsdScanner` runs for 10 seconds with callback-based service discovery
5. Results are merged by IP address - devices found by both methods have combined data
6. `StateFlow` emits updates to UI showing real-time progress and final results

### UI Layer
- `NetworkScannerScreen` (ui/views/NetworkScannerScreen.kt): Main screen with status card, device list, and scan FAB
- Displays discovery method badges (ARP/NSD), service information (name, type, port), and online/offline status indicators

## Required Permissions

The app requires these Android permissions (defined in AndroidManifest.xml):
- `ACCESS_WIFI_STATE`: Read WiFi connection info to get local IP
- `CHANGE_WIFI_STATE`: WiFi operations
- `ACCESS_NETWORK_STATE`: Check network connectivity
- `INTERNET`: Perform network operations

## Project Structure Notes

- Package namespace: `com.gonzalo.networkscanner`
- Dependencies managed via Gradle version catalog (libs.versions.toml)
- Compose BOM version: 2024.09.00
- The `ui/components/NetworkScanner.kt` file appears to be unused (placeholder component)
