#!/usr/bin/env python3
"""Static, fail-closed tests for user-supplied WireGuard tunnel integration.

Does not claim a real peer was contacted. Actual external VPN consent,
handshake, IPv4/IPv6/DNS leak and device switchovers require a live server.
"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
base = root / "android/app-telefon"
manifest = (base / "src/main/AndroidManifest.xml").read_text()
gradle = (base / "build.gradle.kts").read_text()
settings = (base / "src/main/java/com/eymen/browser/telefon/SettingsScreen.kt").read_text()
activity = (base / "src/main/java/com/eymen/browser/telefon/MainActivity.kt").read_text()
ui = (base / "src/main/java/com/eymen/browser/telefon/VpnSettingsScreen.kt").read_text()
store = (base / "src/main/java/com/eymen/browser/telefon/EymenVpnConfigStore.kt").read_text()
backend = (base / "src/main/java/com/eymen/browser/telefon/EymenWireGuardVpn.kt").read_text()

assert 'implementation("com.wireguard.android:tunnel:1.0.20260102")' in gradle
assert 'isCoreLibraryDesugaringEnabled = true' in gradle
assert 'coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:' in gradle
assert 'com.wireguard.android.backend.GoBackend$VpnService' in manifest
assert 'android.permission.BIND_VPN_SERVICE' in manifest
assert 'android:exported="false"' in manifest
assert 'android.net.VpnService' in manifest
assert 'android:allowBackup="false"' in manifest
assert 'android:usesCleartextTraffic="false"' in manifest

# No server details, private credentials, hardcoded IP or provider shipped in APK.
for source in (store, backend):
    assert 'PrivateKey = ' not in source
    assert 'https://api.' not in source
    assert 'BEGIN PRIVATE KEY' not in source
assert 'getInstance("AndroidKeyStore")' in store
assert 'KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT' in store
assert 'AES/GCM/NoPadding' in store
assert 'GCMParameterSpec(128,' in store
assert 'noBackupFilesDir.resolve("eymen-wg-v1.bin")' in store
assert 'file.startWrite()' in store
assert 'file.finishWrite(output)' in store
assert 'file.failWrite(output)' in store
assert 'plaintext.fill(0)' in store
assert 'source.peers.size == 1' in store
assert 'peer.endpoint.isPresent' in store
assert 'it.address is Inet4Address && it.mask == 0' in store
assert 'it.address is Inet6Address && it.mask == 0' in store
assert 'original.dnsServers.isNotEmpty()' in store
assert '.includeApplication(context.packageName)' in store
assert 'Config.Builder()' in store
assert 'getSharedPreferences' not in store
assert 'Log.' not in store

assert 'registerForActivityResult(' in activity
assert 'ActivityResultContracts.OpenDocument()' in activity
assert 'ActivityResultContracts.StartActivityForResult()' in activity
assert 'VpnService.prepare(this)' in activity
assert 'vpn.connectWithUserPermission()' in activity
assert 'SettingsAction.VPN ->' in activity
assert 'VpnSettingsScreen(' in activity
assert 'onDisconnect = vpn::disconnect' in activity
assert 'onRefresh = vpn::refreshState' in activity
assert 'onDelete = ::confirmDeleteVpnConfiguration' in activity
assert 'settingsShowing && vpnScreenVisible -> vpnScreenVisible = false' in activity
assert 'SettingsAction.VPN' in settings
assert 'SettingRow(R.string.vpn_title' in settings

assert 'GoBackend(app)' in backend
assert 'backend.setState(' not in backend or 'activeBackend.setState(' in backend
assert 'activeBackend.setState(tunnel, Tunnel.State.UP, validated)' in backend
assert 'backend?.setState(tunnel, Tunnel.State.DOWN, null)' in backend
assert 'check(VpnService.prepare(app) == null)' in backend
assert 'EymenVpnStatus.TUNNEL_UNVERIFIED' in backend
assert 'EymenVpnStatus.HANDSHAKE_CONFIRMED' in backend
assert 'latestHandshakeEpochMillis()' in backend
assert 'configStore.load()' in backend
assert 'configStore.importBytes(bytes)' in backend
assert 'bytes.fill(0)' in backend
assert 'app.contentResolver.openInputStream(uri)' in backend
assert 'no real server' not in backend.lower()

assert 'EymenVpnStatus.TUNNEL_UNVERIFIED' in ui
assert 'EymenVpnStatus.HANDSHAKE_CONFIRMED' in ui
assert 'vpn_no_fake' in ui
assert 'vpn_dual_stack' in ui
assert 'vpn_privacy_note' in ui
assert 'onImport: () -> Unit' in ui
assert 'onConnect: () -> Unit' in ui
assert 'onDelete: () -> Unit' in ui

tr = json.loads((root / "shared/i18n/tr.json").read_text())
en = json.loads((root / "shared/i18n/en.json").read_text())
for k in (
    "vpn_title", "vpn_import", "vpn_connect", "vpn_disconnect", "vpn_refresh",
    "vpn_remove", "vpn_status_unverified", "vpn_status_verified",
    "vpn_no_fake", "vpn_dual_stack", "vpn_saved_crypt", "vpn_privacy_note"
):
    assert k in tr and tr[k] and k in en and en[k], k

print("WireGuard integration, encrypted configuration, app-only routing,")
print("explicit consent, truthful handshake and dual-stack static checks passed.")
