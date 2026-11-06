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
import java.util.concurrent.atomic.AtomicLong

/** Safe, non-identifying diagnostics from WireGuard's real peer counters. */
internal data class EymenVpnPeerStats(
    val receivedBytes: Long,
    val sentBytes: Long,
    val lastHandshakeAgeSeconds: Long?
)

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
    // Reject results from operations that were superseded by Disconnect
    // or a later user action. The worker is serial; UI callbacks are not.
    private val operationEpoch = AtomicLong(0)
    private var monitoring = false
    private val monitorTick = object : Runnable {
        override fun run() {
            if (!monitoring) return
            refreshState()
            main.postDelayed(this, 15_000L)
        }
    }

    /** UI-only polling: never assume an old handshake stays fresh forever. */
    fun startUiMonitoring() {
        if (monitoring) return
        monitoring = true
        refreshState()
        main.postDelayed(monitorTick, 15_000L)
    }

    fun stopUiMonitoring() {
        monitoring = false
        main.removeCallbacks(monitorTick)
    }
    private val tunnel = object : Tunnel {
        override fun getName(): String = "EymenBrowser"
        override fun onStateChange(newState: Tunnel.State) {
            if (newState == Tunnel.State.DOWN) main.post {
                // Ignore expected DOWN when an explicit action is still in
                // progress; otherwise report an unexpected Android revoke.
                if (status == EymenVpnStatus.TUNNEL_UNVERIFIED ||
                    status == EymenVpnStatus.HANDSHAKE_CONFIRMED
                ) {
                    status = if (configStore.exists())
                        EymenVpnStatus.DISCONNECTED else EymenVpnStatus.NO_CONFIGURATION
                    message = "Android VPN tüneli kapandı. Koruma artık doğrulanmıyor."
                    peerStats = null
                }
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
    var importing by mutableStateOf(false)
        private set
    var peerStats by mutableStateOf<EymenVpnPeerStats?>(null)
        private set

    /** No secret text is stored in state, included in errors or printed. */
    var message by mutableStateOf("")
        private set

    private fun show(
        state: EymenVpnStatus,
        description: String = "",
        expectedEpoch: Long = operationEpoch.get(),
        diagnostics: EymenVpnPeerStats? = null
    ) {
        main.post {
            // Prevent stale statistics or a late UP callback from undoing
            // an explicit disconnect/delete that was requested later.
            if (operationEpoch.get() != expectedEpoch) return@post
            status = state
            message = description
            peerStats = diagnostics
            configPresent = configStore.exists()
        }
    }

    /**
     * The Android Storage Access Framework supplies a .conf. It is never
     * copied to cache, external storage, SharedPreferences or a log file.
     */
    fun importConfig(uri: Uri) {
        if (importing) return
        if (status !in listOf(
                EymenVpnStatus.NO_CONFIGURATION,
                EymenVpnStatus.READY,
                EymenVpnStatus.DISCONNECTED
            )
        ) {
            message = "Önce açık VPN tünelini kapat."
            return
        }
        importing = true
        val epoch = operationEpoch.incrementAndGet()
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
                    "Şifreli WireGuard yapılandırması içe aktarıldı. Sadece EYMEN Browser kullanılacak.",
                    epoch
                )
            } catch (_: Exception) {
                show(
                    if (configStore.exists()) EymenVpnStatus.READY
                    else EymenVpnStatus.NO_CONFIGURATION,
                    "Dosya okunamadı veya güvenli çift yığınlı VPN yapılandırması geçersiz.",
                    epoch
                )
            } finally {
                bytes.fill(0)
                main.post { if (operationEpoch.get() == epoch) importing = false }
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
        if (importing || status !in listOf(
                EymenVpnStatus.READY, EymenVpnStatus.DISCONNECTED
            )
        ) return
        val epoch = operationEpoch.incrementAndGet()
        status = EymenVpnStatus.CONNECTING
        message = ""
        peerStats = null
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
                        "Tünel açıldı; sunucunun yanıtı henüz doğrulanmadı.",
                        epoch
                    )
                } else {
                    show(EymenVpnStatus.ERROR, "VPN tüneli açılamadı.", epoch)
                }
            } catch (_: Exception) {
                show(
                    EymenVpnStatus.ERROR,
                    "VPN kurulamadı. Android iznini, sunucu erişimini ve .conf dosyasını kontrol et.",
                    epoch
                )
            }
        }
    }

    fun refreshState() {
        if (status != EymenVpnStatus.TUNNEL_UNVERIFIED &&
            status != EymenVpnStatus.HANDSHAKE_CONFIRMED
        ) return
        val epoch = operationEpoch.get()
        worker.execute {
            try {
                val activeBackend = backend ?: return@execute
                if (activeBackend.getState(tunnel) != Tunnel.State.UP ||
                    VpnService.prepare(app) != null
                ) {
                    show(EymenVpnStatus.DISCONNECTED, "VPN artık etkin değil.", epoch)
                    return@execute
                }
                val config = configStore.load()
                val peerKey = config.peers.single().publicKey
                val statistics = activeBackend.getStatistics(tunnel).peer(peerKey)
                val handshake = statistics?.latestHandshakeEpochMillis() ?: 0L
                // A clock-skewed handshake in the future must NOT count.
                val ageMillis = System.currentTimeMillis() - handshake
                val recent = handshake > 0L && ageMillis in 0L..179_999L
                val diagnostics = EymenVpnPeerStats(
                    receivedBytes = (statistics?.rxBytes() ?: 0L).coerceAtLeast(0L),
                    sentBytes = (statistics?.txBytes() ?: 0L).coerceAtLeast(0L),
                    lastHandshakeAgeSeconds = ageMillis.takeIf {
                        handshake > 0L && it >= 0L
                    }?.div(1000L)
                )
                if (recent) {
                    show(EymenVpnStatus.HANDSHAKE_CONFIRMED,
                        "WireGuard sunucusundan yakın zamanda gerçek yanıt alındı.",
                        epoch, diagnostics)
                } else {
                    show(EymenVpnStatus.TUNNEL_UNVERIFIED,
                        "Tünel açık; güncel sunucu el sıkışması doğrulanmadı.",
                        epoch, diagnostics)
                }
            } catch (_: Exception) {
                show(EymenVpnStatus.ERROR, "VPN durumu doğrulanamıyor.", epoch)
            }
        }
    }

    fun disconnect() {
        if (status == EymenVpnStatus.DISCONNECTING) return
        val epoch = operationEpoch.incrementAndGet()
        status = EymenVpnStatus.DISCONNECTING
        message = ""
        peerStats = null
        worker.execute {
            try {
                backend?.setState(tunnel, Tunnel.State.DOWN, null)
                show(
                    if (configStore.exists()) EymenVpnStatus.DISCONNECTED
                    else EymenVpnStatus.NO_CONFIGURATION,
                    expectedEpoch = epoch
                )
            } catch (_: Exception) {
                show(EymenVpnStatus.ERROR,
                    "VPN kapatılamadı; Android VPN ayarlarından kontrol et.", epoch)
            }
        }
    }

    fun deleteConfiguration() {
        if (importing || status == EymenVpnStatus.DISCONNECTING) return
        val epoch = operationEpoch.incrementAndGet()
        status = EymenVpnStatus.DISCONNECTING
        message = ""
        peerStats = null
        worker.execute {
            try {
                backend?.setState(tunnel, Tunnel.State.DOWN, null)
                configStore.clear()
                show(EymenVpnStatus.NO_CONFIGURATION, "Şifreli sunucu yapılandırması silindi.",
                    epoch)
            } catch (_: Exception) {
                show(EymenVpnStatus.ERROR,
                    "VPN yapılandırması silinemedi. Önce Android VPN bağlantısını kapat.", epoch)
            }
        }
    }
}
