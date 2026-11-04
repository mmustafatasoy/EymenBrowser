# Aşama 1 — motor denemeleri

Bu proje, onaylı 16 ekranın uygulaması değildir. **Gerçek sitelerin yüklenmesi,
Google girişi ve DRM sonuçları henüz denenmedi**. Derleme başarısı bu işlevlerin
çalıştığına kanıt sayılmaz.

## Motor denemeleri

- Android telefon ve TV: GeckoView Stable `versions.toml` kaynaklı.
  İlk açılışta `https://example.org/` yüklenir. ADB ile site değiştirmek için:
  `adb shell am start -n com.eymen.browser.telefon/.MainActivity --es test_url https://www.google.com/`.
  TV uygulamasında paket `com.eymen.browser.tv`.
- iOS/iPadOS: WKWebView; simülatörde `EYMEN_TEST_URL` ortam değişkeniyle
  HTTPS adresi tanımlanabilir.
- Windows, macOS, Linux: castLabs Electron WebContentsView; ortam değişkeni
  `EYMEN_TEST_URL=https://www.google.com/`.
- Bu denemelerde şifre, hesap, çerez veya banka verisi kaydedilmez.
  Sertifika doğrulaması ve sandbox kapatılmaz.

## Kapı 1 test listesi

| Test | Android telefon | Android TV | Masaüstü | iOS |
| --- | --- | --- | --- | --- |
| Gerçek cihazda HTTPS sayfa | Denenmedi | Denenmedi | Denenmedi | Denenmedi |
| Google hesabı giriş | Denenmedi | Denenmedi | Denenmedi | Denenmedi |
| Korumalı video/DRM | Denenmedi | Denenmedi | Denenmedi | Denenmedi |
| e-Devlet | Denenmedi | Denenmedi | Denenmedi | Denenmedi |
| İki banka ve ödeme sayfası | Denenmedi | Denenmedi | Denenmedi | Denenmedi |
| YouTube, video, sosyal medya | Denenmedi | Denenmedi | Denenmedi | Denenmedi |
| PDF ve dosya indirme | Denenmedi | Denenmedi | Denenmedi | Denenmedi |

Samsung Android 14, Tecno Android 16, Philips Android TV ve Android TV emülatörü
ayrı ayrı kontrol edilir. Masaüstünde Windows 10/11, macOS 13+ ve Ubuntu; iOS'ta
iOS 15 simülatörü ve gerçek iPhone kontrol edilir. Gerçek sonuçları ve APK boyutları
`docs/DURUM.md` içinde tutulur.

**Kapı 1: Açık.** Cihaz/site testleri tamamlanıp kullanıcı onayı alınmadan Aşama 2
ekran kodlamasına başlanmaz.

## GitHub Actions test APK'ları — gerçek boyut kaydı

[GeckoView test derlemesi #41804932852](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41804932852) içindeki `EymenBrowser-Phase1-GeckoView-ABI-Debug` artefaktı altı APK içerir:

| Paket | arm64-v8a | armeabi-v7a | x86_64 |
| --- | --- | --- | --- |
| `app-telefon` | 204171144 B | 164976220 B | 223765710 B |
| `app-tv` | 205407384 B | 166212460 B | 225001950 B |

Tek APK 500 MiB altında; bütün APK'ları içeren toplu ZIP bu miktarın üstüne çıkabilir. Boyutlar GitHub Actions Android derleme günlüklerinden alınmıştır.

**Cihaz kurulumuna dikkat:** `arm64-v8a` çoğu modern 64 bit Android telefon/TV,
`armeabi-v7a` 32 bit ARM TV/telefon, `x86_64` emülatör içindir.
Cihazın ABI'si doğrulanmadan rastgele APK yüklenmemeli. Bunlar debug/test imzasıdır;
kararlı uygulama değildir. Hata ayıklama olmadan gerçek siteler ve DRM testleri
otomatik olarak başarıyla tamamlanmış sayılmaz.
