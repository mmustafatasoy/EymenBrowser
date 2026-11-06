# EYMEN Browser v1.0.11 — kullanıcı sunucusuyla gerçek WireGuard VPN

## Neler eklendi?

- Resmî `com.wireguard.android:tunnel:1.0.20260102` kütüphanesinin **GoBackend** kullanıcı alanı tüneli Android telefon uygulamasına bağlandı. Bu gerçek WireGuard + Android `VpnService` uygulamasıdır; basit bir proxy/VPN simülasyonu değildir.
- **Ayarlar → VPN — Yalnız EYMEN Browser** ekranı: Kendi WireGuard `.conf` dosyanı seç, Android VPN izin penceresini kabul et, tüneli aç/kapat, durumunu yenile veya şifreli yapılandırmayı sil.
- Kullanıcının getirdiği gerçek sunucu yapılandırması **olmadan bağlantı kurulmaz**. APK içine VPN sunucusu, giriş şifresi veya özel anahtar konulmadı.
- Yapılandırma `AndroidKeyStore` AES‑256‑GCM ile şifrelenmiş biçimde `noBackupFilesDir` alanında saklanır; SharedPreferences, GitHub, indirme klasörü, dosya önbelleği veya telemetride düz metin anahtar tutulmaz.
- Dosya boyutu (24 KB), HTTPS'ten bağımsız WireGuard biçimi ve eksik sunucu ayarları kontrol edilir. Uygulama sadece **bir peer**, tam çift yığın (IPv4 ve IPv6 için `0/0`), her iki tünel adresi ve tünel DNS'i olan yapılandırmaları kabul eder.
- İçe aktarılan dosyadaki `IncludedApplications`/`ExcludedApplications` değerleri **yok sayılıp yeniden oluşturulur**. WireGuard GoBackend'in `VpnService.Builder.addAllowedApplication(...)` yoluna yalnız `com.eymen.browser.telefon` paketi iletilir; bu yöntem Android tarafından desteklenmektedir.
- Kullanıcıdan izin alınması Android'in gerçek `VpnService.prepare()` sistemi üzerinden yapılır. İzin verilmemişse tünel açılamaz.
- WireGuard motorunun **TUN açık** durumu ile uzaktaki sunucunun gerçekten yanıt verdiği **yakın tarihli handshake** ayrı tutulur. Tünelin açılmış olması otomatik olarak “VPN'e güvenli bağlanıldı” sayılmaz.
- Bağlantıyı kesme, sunucu yapılandırmasını kaldırma ve hata sonrasında bağlantıyı kapatma akışları bulunur. Uygulama kapalıyken VPN'e otomatik yeniden bağlanma **garanti edilmez**.
- Android 8+ desteği, ABI olarak arm64-v8a/armeabi-v7a/x86_64 hedeflenmektedir. Onaylı üst araç çubuğu ve EYMEN logosunda görsel değişiklik yok.

## Kullanmak için örnek .conf yapısı

Bu **sadece alanları gösteren örnektir**, çalışan sunucu/anahtar değildir. **Kendi özel anahtarını bu sohbete veya GitHub'a göndermemelisin.**

```ini
[Interface]
PrivateKey = <KENDİ_ÖZEL_ANAHTARIN>
Address = 10.10.0.2/32, fd12:3456:789a::2/128
DNS = 10.10.0.1

[Peer]
PublicKey = <SUNUCU_AÇIK_ANAHTARI>
Endpoint = vpn.senin-sunucun.example:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
```

Gerçek sunucu, geçerli anahtarlar, DNS ve yönlendirme olmadan bu dosya çalışmaz. Sunucu sağlayan tarafın WireGuard hizmetini doğru yapılandırması gerekir.

## Dikkat: doğrulanması gereken güvenlik sınırları

- **Sistem kill-switch henüz sağlanmıyor.** Kullanıcı manuel olarak bağlantıyı kapatırsa veya Android VPN servisini iptal ederse tarayıcı internete doğrudan çıkabilir; bu sürüm trafik sızıntısına karşı kesin garanti sunmaz.
- Android sistemindeki VPN izni, TUN'un açılması ve WireGuard handshake doğrulaması, bağlantının bütün IPv4/IPv6/WebRTC/DNS trafiğini beklendiği gibi koruduğunu **tek başlarına kanıtlamaz**.
- `GoBackend` tüneli cihaz ve süreç yaşam döngüsüne bağlıdır. Android 8–16 uyumu, servis arka plan ölümü, ağ değişimi, kilit ekranı, uçak modu ve otomatik yeniden bağlanma gerçek cihazda ayrı kabul testlerine ihtiyaç duyar.
- VPN sunucusunu yöneten kişi/kurum trafiği görebilir; servis sağlayıcısının gizlilik ve kayıt politikası ayrıca değerlendirilmelidir.
- Kullanıcının `.conf` dosyasını sohbet, web tarayıcısı veya GitHub üzerinden paylaşması önerilmez; uygulamada **Dosya seç** ile doğrudan telefon depolamasından içe aktarılır.

## Gerçek cihaz kabul testi

1. Bir sunucu hesabından geçerli, IPv4/IPv6 tam-tünel WireGuard dosyası elde edin.
2. Uygulama → Ayarlar → VPN → `.conf` içe aktar yoluyla seçin; uygulamayı yeniden açınca yapılandırmanın hazır olduğunu doğrulayın.
3. Bağlan düğmesine basın. Android'in **resmî VPN izin ekranını** reddedin; VPN başlamamalı. İzin verin ve yeniden bağlayın.
4. Tünel açılmış durumdayken bir HTTPS sayfası ziyaret edin, “Gerçek bağlantıyı kontrol et” diyerek güncel handshake raporunu doğrulayın.
5. Bağlantı açıkken başka uygulamaların VPN'e alınmadığını Android/Router trafiği üzerinden denetleyin.
6. IPv4/IPv6 dış IP, DNS, WebRTC ve sızıntı senaryolarını ölçün. Yanlış sonuçta sürüm “sızıntısız VPN” olarak pazarlanmaz.
7. Wi-Fi ↔ hücresel ağ geçişi, uçak modu, uyku/uyanma, işlem öldürme, Android 8/11/14/15/16 ve pil kullanımı testleri gerekir.
8. VPN'i kapatın, yapılandırmayı silin ve gizli anahtarların uygulama dosyalarından düz metin olarak çıkmadığını denetleyin.
9. YouTube, DRM, tam ekran video, indirmeler, sekmeler ve onaylanan çentik/üst çubuk yerleşimini regresyon testinden geçirin.

## Kod ve otomasyon

- `android/app-telefon/src/main/java/com/eymen/browser/telefon/EymenVpnConfigStore.kt`
- `android/app-telefon/src/main/java/com/eymen/browser/telefon/EymenWireGuardVpn.kt`
- `android/app-telefon/src/main/java/com/eymen/browser/telefon/VpnSettingsScreen.kt`
- `python tools/check_vpn_contract.py`
- `python tools/generate.py && python tools/check_stage2_features.py`
- `.github/workflows/phase0.yml` Android debug APK derlemesi
- `.github/workflows/android-visual-audit.yml` Android 15/16 açılış ve güvenli alan görsel kontrolü

**Dağıtım kapısı:** v1.0.10 ile aynı üretim sertifikası, yükselen sürüm kodu, doğru paket/ABI, SHA‑256; yeni APK ancak bu kontrollerden geçtikten sonra yayımlanır. Fiziksel cihazdaki gerçek sunucu handshake ve sızıntı kabulü otomatik CI tarafından yapılmış gibi gösterilmez.
