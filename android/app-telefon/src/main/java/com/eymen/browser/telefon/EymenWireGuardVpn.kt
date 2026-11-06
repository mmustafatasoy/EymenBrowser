package com.eymen.browser.telefon

import android.content.Context
import android.net.Uri
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.abs

internal enum class EymenVpnStatus {
    NO_CONFIGURATION, READY, CONNECTING, TUNNEL_UNVERIFIED,
    HANDSHAKE_CONFIRMED, DISCONNECTING, DISCONNECTED, ERROR
}

/**
 * A real WireGuard userspace tunnel, not a fake on/off switch.
 *
 * GoBackend creates Android's VpnService and routes ONLY the app package
 * specified by EymenVpnConfigStore's validated Interface. The service can
 * be killed or revoked by Android; only a recent real peer handshake
 * earns HANDSHAKE_CONFIRMED. No config or VPN permission -> no connection.
 */
internal class EymenWireGuardVpn private constructor(context: Context) {
    companion object {
        @Volatile private var shared: EymenWireGuardVpn? = null

        fun get(context: Context): EymenWireGuardVpn =
            shared ?: synchronized(this) {
                shared ?: EymenWireGuardVpn(context.applicationContext)
                    .also { shared = it }
            }
    }

    private val app = context.applicationContext
    private val configStore = EymenVpnConfigStore(app)
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { work ->
        Thread(work, "EymenVpnTunnel").apply { isDaemon = true }
    }
    private var backend: GoBackend? = null
    private val tunnel = object : Tunnel {
        override fun getName(): String = "EymenBrowser"
        override fun onStateChange(newState: Tunnel.State) {
            if (newState == Tunnel.State.DOWN) main.post {
                // Android can revoke the VPN separately from our UI.
                status = if (configStore.exists())
                    EymenVpnStatus.DISCONNECTED else EymenVpnStatus.NO_CONFIGURATION
            }
        }
    }

    var status by mutableStateOf(
        if (configStore.exists()) EymenVpnStatus.READY
        else EymenVpnStatus.NO_CONFIGURATION
    )
        private set
    var configPresent by mutableStateOf(configStore.exists())
        private set

    /** No secret text is stored in state, included in errors or printed. */
    var message by mutableStateOf("")
        private set

    private fun show(state: EymenVpnStatus, description: String = "") {
        main.post {
            status = state
            message = description
            configPresent = configStore.exists()
        }
    }

    /**
     * The Android Storage Access Framework supplies a .conf. It is never
     * copied to cache, external storage, SharedPreferences or a log file.
     */
    fun importConfig(uri: Uri) {
        if (status !in listOf(
                EymenVpnStatus.NO_CONFIGURATION,
                EymenVpnStatus.READY,
                EymenVpnStatus.DISCONNECTED
            )
        ) {
            message = "Önce açık VPN tünelini kapat."
            return
        }
        worker.execute {
            var bytes = byteArrayOf()
            try {
                app.contentResolver.openInputStream(uri).use { input ->
                    require(input != null) { "Dosya okunamadı." }
                    val buffer = ByteArray(4096)
                    val output = ByteArrayOutputStream()
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        require(output.size() + count <= 24 * 1024) {
                            "VPN yapılandırması 24 KB sınırını aşıyor."
                        }
                        output.write(buffer, 0, count)
                    }
                    bytes = output.toByteArray()
                    buffer.fill(0)
                }
                configStore.importBytes(bytes)
                show(
                    EymenVpnStatus.READY,
                    "Şifreli WireGuard yapılandırması içe aktarıldı. Sadece EYMEN Browser kullanılacak."
                )
            } catch (_: Exception) {
                show(
                    if (configStore.exists()) EymenVpnStatus.READY
                    else EymenVpnStatus.NO_CONFIGURATION,
                    "Dosya okunamadı veya güvenli çift yığınlı VPN yapılandırması geçersiz."
                )
            } finally {
                bytes.fill(0)
            }
        }
    }

    /** The host Activity must call VpnService.prepare() and get consent first. */
    fun connectWithUserPermission() {
        if (!configStore.exists()) {
            status = EymenVpnStatus.NO_CONFIGURATION
            message = "Önce kendi WireGuard sunucu dosyanı içe aktar."
            return
        }
        if (status == EymenVpnStatus.CONNECTING ||
            status == EymenVpnStatus.TUNNEL_UNVERIFIED ||
            status == EymenVpnStatus.HANDSHAKE_CONFIRMED
        ) return
        show(EymenVpnStatus.CONNECTING)
        worker.execute {
            try {
                check(VpnService.prepare(app) == null) {
                    "VPN izni verilmedi."
                }
                val validated: Config = configStore.load()
                val activeBackend = backend ?: GoBackend(app).also { backend = it }
                val result = activeBackend.setState(tunnel, Tunnel.State.UP, validated)
                if (result == Tunnel.State.UP) {
                    // WireGuard can activate a local TUN even when the remote
                    // peer is offline. Never claim "connected" yet.
                    show(
                        EymenVpnStatus.TUNNEL_UNVERIFIED,
                        "Tünel açıldı; sunucunun yanıtı henüz doğrulanmadı."
                    )
                } else {
                    show(EymenVpnStatus.ERROR, "VPN tüneli açılamadı.")
                }
            } catch (_: Exception) {
                show(
                    EymenVpnStatus.ERROR,
                    "VPN kurulamadı. Android iznini, sunucu erişimini ve .conf dosyasını kontrol et."
                )
            }
        }
    }

    fun refreshState() {
        if (status != EymenVpnStatus.TUNNEL_UNVERIFIED &&
            status != EymenVpnStatus.HANDSHAKE_CONFIRMED
        ) return
        worker.execute {
            try {
                val activeBackend = backend ?: return@execute
                if (activeBackend.getState(tunnel) != Tunnel.State.UP ||
                    VpnService.prepare(app) != null
                ) {
                    show(EymenVpnStatus.DISCONNECTED, "VPN artık etkin değil.")
                    return@execute
                }
                val config = configStore.load()
                val peerKey = config.peers.single().publicKey
                val handshake = activeBackend.getStatistics(tunnel)
                    .peer(peerKey)?.latestHandshakeEpochMillis() ?: 0L
                val recent = handshake > 0L &&
                    abs(System.currentTimeMillis() - handshake) < 3 * 60 * 1000L
                if (recent) {
                    show(EymenVpnStatus.HANDSHAKE_CONFIRMED,
                        "WireGuard sunucusundan yakın zamanda gerçek yanıt alındı.")
                } else {
                    show(EymenVpnStatus.TUNNEL_UNVERIFIED,
                        "Tünel açık; güncel sunucu el sıkışması doğrulanmadı.")
                }
            } catch (_: Exception) {
                show(EymenVpnStatus.ERROR, "VPN durumu doğrulanamıyor.")
            }
        }
    }

    fun disconnect() {
        show(EymenVpnStatus.DISCONNECTING)
        worker.execute {
            try {
                backend?.setState(tunnel, Tunnel.State.DOWN, null)
                show(
                    if (configStore.exists()) EymenVpnStatus.DISCONNECTED
                    else EymenVpnStatus.NO_CONFIGURATION
                )
            } catch (_: Exception) {
                show(EymenVpnStatus.ERROR, "VPN kapatılamadı; Android VPN ayarlarından kontrol et.")
            }
        }
    }

    fun deleteConfiguration() {
        show(EymenVpnStatus.DISCONNECTING)
        worker.execute {
            try {
                backend?.setState(tunnel, Tunnel.State.DOWN, null)
                configStore.clear()
                show(EymenVpnStatus.NO_CONFIGURATION, "Şifreli sunucu yapılandırması silindi.")
            } catch (_: Exception) {
                show(EymenVpnStatus.ERROR,
                    "VPN yapılandırması silinemedi. Önce Android VPN bağlantısını kapat.")
            }
        }
    }
}
