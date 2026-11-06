# EYMEN Browser v1.0.12 — VPN güvenilirliği ve tanı kontrolleri

## Uygulanan değişiklikler

**Bu güncelleme v1.0.11'in gerçek WireGuard GoBackend tünelini geliştirir.** Kendi WireGuard sunucunuzun anahtarları/bağlantısı olmadan uygulama VPN bağlantısı oluşturamaz.

1. **Yanıtı otomatik denetleme:** VPN ayarlar ekranı açıkken WireGuard peer bilgileri yaklaşık **15 saniyede bir** yalnız yerel GoBackend istatistiklerinden okunur. Ekran kapanınca bu UI yoklaması durur. Tünel açık olsa da sunucunun güncel handshake yanıtı yoksa “bağlantı doğrulanmadı” gösterilir.
2. **Zaman doğruluğu:** Gerçek sunucu handshake'leri 3 dakika sonra eski kabul edilir. Saatin ilerisinden gelen zaman damgaları asla güvenilir olarak değerlendirilmez. Eksik, gelecekte ve 3 dakika sınırındaki yanıtları kapsayan yürütülebilir JUnit testleri eklendi.
3. **Asenkron işlem tutarlılığı:** Kullanıcı kapat dedikten sonra önceki bağlantı veya istatistik isteğinin gecikmiş sonucu “bağlandı” durumunu geri getiremez. Normal içe aktarma sırasında aynı dosya tekrar içe aktarılmaz.
4. **Gerçek sayaçlar:** Alınan/gönderilen WireGuard byte sayıları ve son sunucu yanıtının kaç saniye önce geldiği uygulamada gösterilir. Sunucu adresi, genel/özel anahtar veya ziyaret edilen sayfalar tanı panelinde gösterilmez.
5. **Anahtar ve bellek temizliği:** Şifreli dosya bozuksa/deşifre edilemiyorsa hassas bayt tamponları hata yolunda da temizlenir. Sunucu yapılandırması silindiğinde Android Keystore şifreleme anahtarı da kaldırılır.
6. **Desteklenmeyen her zaman açık modu:** Android `VpnService.SUPPORTS_ALWAYS_ON=false` bildirimi eklendi. Önceki sürümde Android'in sistemden otomatik başlattığı VPN oturumunu yeniden kuran kod yokken sistem “her zaman açık/lockdown” sunabilirdi. **Bu mod, güvenli sistem açılışı ve yeniden bağlanma uygulanıp gerçek cihazda doğrulanıncaya kadar bilinçli olarak kapalıdır.**

## Önemli sınır: kill-switch henüz yok

Bu sürüm **sistem düzeyinde trafik engellemesi/kill-switch sağlamaz.** Android VPN tüneli düşerse tarayıcı İnternet'e normal bağlantıyla erişebilir. Yanlış güvenlik beklentisi oluşturmak yerine kullanıcıya bu gerçek anlatılır. Sürekli koruma için Android sisteminin desteklenen ve test edilmiş `Always-on + Block connections without VPN` akışını düzgün kurmak gerekir. Per-app VPN ile bu özelliğin yanlış etkinleştirilmesi diğer uygulamaların İnternetini de kesebilir; v1.0.12'de kullanıcıya sunulmuyor.

VPN ekranındaki 15 saniyelik izleme **tünelin arka planda otomatik bağlanması anlamına gelmez**. Veri/handshake sayaçları da DNS, IPv6 veya WebRTC sızıntısının olmadığını kanıtlamaz.

## Bağlantı ve cihaz kabulü

- Kullanıcıdan sunucu yapılandırması asla sohbet veya GitHub üzerinden istenmez. Geçerli WireGuard `.conf` yalnız uygulama içi dosya seçiciden içe aktarılır.
- Android 8/11/14/15/16 fiziksel cihazlarda VPN izni, tünel açılışı, gerçek sunucu el sıkışması, trafik gönderme/alma ve kapatma ayrı kontrol edilir.
- Wi‑Fi ↔ hücresel geçiş, cihaz uyku/uyanması, uçak modu, işlem ölümü, servis iptali ve tünelin kopması sonrasında VPN durumunun ve dışa çıkan ağın gerçekte ne yaptığı test edilmelidir.
- IPv4/IPv6 dış IP, DNS ve WebRTC yönlendirme sızıntısı, VPN altındaki tarayıcının trafiği ile diğer uygulamaların normal bağlantısı gerçek ağ seviyesinde test edilmelidir.
- Yetkili sunucu olmadan bu maddeler otomatik GitHub Actions tarafından tamamlanmış sayılamaz.

## Otomatik güvenlik kapıları

- `tools/check_vpn_contract.py` — şifreli anahtar, Keystore silme, VPN izinleri, auto-poll, epoch ve tanı statik doğrulaması
- `android/app-telefon/src/test/java/com/eymen/browser/telefon/EymenHandshakePolicyTest.kt` — JUnit gerçek zaman aralığı sınırları
- `.github/workflows/phase0.yml` — `testDebugUnitTest` ve `assembleDebug`
- `.github/workflows/android-visual-audit.yml` — Android 15/16 emülatör açılış, çentik/üst araç çubuğu, sekme görünümü
- `.github/workflows/android-stable-release.yml` — tekrar JUnit, orijinal üretim imzası, ABI APK ve v1.0.11 yükseltme kontrolleri

Onaylı açık renk EYMEN Browser ana sayfası ve atasoyyazilim.com logo kimliği değişmedi.
