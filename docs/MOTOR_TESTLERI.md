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
| Gerçek cihazda HTTPS sayfa | Başarılı: example.org ve Google (paylaşılan link) | Denenmedi | Denenmedi | Denenmedi |
| Google hesabı giriş | Denenmedi | Denenmedi | Denenmedi | Denenmedi |
| Korumalı video/DRM | Denenmedi | Denenmedi | Denenmedi | Denenmedi |
| e-Devlet | Denenmedi | Denenmedi | Denenmedi | Denenmedi |
| İki banka ve ödeme sayfası | Denenmedi | Denenmedi | Denenmedi | Denenmedi |
| YouTube, video, sosyal medya | YouTube video: görüntü + ses başarılı (kullanıcı doğruladı); diğerleri denenmedi | Denenmedi | Denenmedi | Denenmedi |
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

## Kullanıcının doğruladığı ilk fiziksel test

- 2026-11-04: Android telefonda `https://example.org/` örnek sayfasının yüklenip ekranda görüntülendiği fotoğrafla doğrulandı. Bu, yalnız basit HTTPS sayfa gösterimini doğrular; Google, YouTube, banka, giriş, DRM veya indirme için test sonucu değildir.
- Aynı fotoğraf Android modelini/ABI'yi kanıtlamaz; cihaz ayrıntıları kesinleştirilmeden üretim uygunluğu kabul edilmez.

## ADB kullanmadan yeni test yöntemi

- GitHub Actions'tan yeni **test** APK'sı kurulduktan sonra Google, YouTube, e-Devlet veya başka HTTPS sitesinin bağlantısını başka bir uygulamada açın.
- Bağlantıyı **Paylaş → EymenBrowser** ile gönderin. Desteklenen Android cihazlarda "Birlikte aç → EymenBrowser" de kullanılabilir.
- Uygulama yalnızca HTTPS adresi kabul eder; HTTP, `file:`, `javascript:` vb. adresler reddedilir ve otomatik yürütülmez.
- Test için aynı uygulamayı yeniden açmak `https://example.org/` ile başlayabilir; test bağlantısı paylaşmak daha doğrudan bir yöntemdir.
- Yeni test yolunun uygulanması, başarılı derleme veya gerçek cihazda uyumluluk sonucu sayılmaz. Denemelerin sonucu cihaz başına kaydedilmelidir.
- Google girişi veya banka sayfası denenirse **şifre, SMS kodu, çerez veya özel hesap sayfası ekran görüntüsü paylaşmayın**. Yalnız "açıldı/açılmadı" ve hata türünü yazın.

## Güncel kullanıcı testi — Google bağlantı paylaşımı

- 2026-11-04: Kullanıcı, yeni Android telefon motor testi APK'sını kurduğunu ve Chrome'dan **Paylaş → EymenBrowser** ile Google sayfasının açıldığını doğruladı.
- **Başarılı:** Android ACTION_SEND HTTPS bağlantı kabulü, GeckoView'de Google sayfası görüntüleme.
- **Denenmedi:** Google hesabına giriş, DRM, banka/e-Devlet oturumları ve indirme. YouTube video oynatma sonraki testte başarılı doğrulandı.
- Sonraki kullanıcı testi: YouTube videosunda tam ekranı ayrı kontrol et; diğer video hizmetleri ve DRM ayrıca test edilecek. Telif hakkı korumalı/DRM akışına dair sonuç çıkarma.

## Güncel kullanıcı testi — YouTube video ve ses

- 2026-11-04: Kullanıcı, YouTube video bağlantısını EymenBrowser içinde test edip **çalıştığını** bildirdi. Bu yanıt, sorulan görüntülü ve sesli oynatma denemesi için başarılı kabul edildi.
- **Başarılı:** Android telefon GeckoView üzerinden YouTube videosunun görüntülü ve sesli oynatılması (kullanıcı geri bildirimi).
- **Henüz doğrulanmadı:** YouTube tam ekranı, arka planda oynatma, oturum açma, korumalı video/DRM, TV ve diğer platformlarda video.
- **Sıradaki deneme:** Aynı videonun tam ekran düğmesini kullanarak ekranda gerçekten tam ekran açıldığını test et.
