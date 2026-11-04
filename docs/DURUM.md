# Proje durumu

Son güncelleme: 2026-11-04

## Mevcut aşama
- **Aşama 1 — Motor denemesi: devam ediyor.**
- **Kapı 1: açılmadı.** Gerçek Android telefonda `https://example.org/` sayfasının açıldığı fotoğrafla ve Google sayfasının paylaşım yoluyla açıldığı kullanıcı geri bildirimiyle doğrulandı. Google girişi, DRM, e-Devlet, bankalar, YouTube tam ekranı ve PDF/indirme testleri yapılmadı; kullanıcı onayı alınmadı.
- Aşama 2 arayüz kodlamasına **başlanmadı**. `docs/SARTNAME.md` ve `design/` dosyaları değişmedi.

## Tamamlanan adımlar
- Aşama 0'ın altı işi [GitHub Actions #41801213920](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41801213920) üzerinde geçti.
- Android telefon ve Android TV: Mozilla GeckoView Stable **159.0.20261022150525**, üç ayrı ABI APK'sı.
- iOS/iPadOS: sistem WKWebView motor deneme uygulaması; iOS simülatör derlemesi başarılı.
- Windows/macOS/Linux: sandbox'lı castLabs Electron Chromium `WebContentsView` ile HTTPS motor denemesi; platform derlemeleri [#41802838886](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41802838886) üzerinde başarılı.
- Android GeckoView motor APK'ları **ilk kez başarıyla derlendi**, 6 adet APK ve tüm 500 MB dosya denetimleri [#41804932852](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41804932852) üzerinde geçti. Bu test çalıştırmasındaki Android, Linux, Windows ve ortak kaynak işleri başarılı; son macOS ve iOS işleri son sorguda bekliyordu.

## Android motor deneme APK'larının ölçülen boyutları

| İşlemci | Telefon (bayt) | TV (bayt) |
| --- | ---: | ---: |
| arm64-v8a | 204171144 | 205407384 |
| armeabi-v7a | 164976220 | 166212460 |
| x86_64 | 223765710 | 225001950 |

Her APK ayrı ayrı 500 MiB üst sınırının altında. GitHub Actions'ın altı APK'yı birleştiren indirilebilir artefakt ZIP'i daha büyük olabilir; **sınır APK başınadır**. Bunlar debug imzalı ve henüz fiziksel cihazda açılışı doğrulanmamış deneme uygulamalarıdır.

## Derleme araçları
- Gradle 9.3.1, Android Gradle eklentisi 9.1.1, Android `compileSdk 37`, `targetSdk 35`, `minSdk 26`.
- AGP 9 yerleşik Kotlin sistemi ve Compose plugin 2.4.20. SDK37 için Android 17 `platforms;android-37.0`.
- Şartnameye aykırı Android sürüm tabanı değişikliği veya güvenlik devre dışı bırakması yapılmadı.

## Yeni adım — telefondan çoklu site testi

- GeckoView'e yalnızca doğrulanmış HTTPS adreslerini alan Android `ACTION_SEND` ve `ACTION_VIEW` deneme niyetleri eklendi. Böylece telefondan başka uygulama üzerinden `Paylaş → EymenBrowser` kullanılarak Google, YouTube, e-Devlet vb. siteler ADB gerekmeksizin manuel denenebilecek.
- Uygulama **yalnız motor testidir**: T01–T11/V01–V03 ekranları, adres çubuğu ve sekmeler şartnamedeki Kapı 1 tamamlanmadan oluşturulmadı.
- Android telefon ve TV motor testinin **yeni APK derlemesi başarılı**: [GitHub Actions #41808478602](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41808478602). Altı işlemciye özel debug APK hazır; 500 MB dosya başı sınırının tamamı geçti. **Telefon paylaşım testi kullanıcı tarafından başarılı doğrulandı:** Google bağlantısı Chrome'dan Paylaş → EymenBrowser ile açıldı. Kullanıcı ayrıca YouTube videosunun **görüntülü ve sesli oynatıldığını** doğruladı. Bu, Google hesabı oturum açma, tam ekran ya da DRM'nin çalıştığı anlamına gelmez.
- Bu test sürümünün gerçek APK ölçümleri (bayt): telefon arm64-v8a **204171320**, armeabi-v7a **164976396**, x86_64 **223765886**; TV arm64-v8a **205423940**, armeabi-v7a **166229016**, x86_64 **225018506**.

## Sıradaki iş
1. Fiziksel Android telefon ve Android TV'ye işlemciye uygun test APK'sı kurulup varsayılan HTTPS sayfası açma denenmeli.
2. Android telefonda YouTube video/ses, tam ekran ve yatay dönüşte aynı sayfada kalma **başarılı**. Google hesabına gerçek giriş (yalnız form açılışı başarılı), korumalı video/DRM, e-Devlet kişisel oturumları (ana sayfa başarılı), banka hesap/ödeme uyumu (iki herkese açık ana sayfa başarılı), PDF/indirme ve diğer platform testleri ayrı ayrı kaydedilmeli.
3. Gerçek motor testleri ve kullanıcı onayı olmadan Kapı 1 kapanmayacak.

## Açık konular
- Gerçek cihaz motor uyumu, Google oturum açma, DRM/korumalı video, e-Devlet, bankalar, TV kumandasıyla test yolu ve iPhone testleri **denenmedi**.
- Apple Developer/EVS gerektiren üretim imzaları yok. Yalnız arm64 Android telefon motor deneme APK'sı [test Releases sayfasında](https://github.com/mmustafatasoy/EymenBrowser/releases/tag/v0.0.1-motor-test.1) **debug imzalı** olarak yayımlandı; kararlı sürüm değildir.

## Yeni Android tam ekran/yatay döndürme hatası — 2026-11-04

- **Kullanıcı testi:** YouTube videosu tam ekran açılıyor; telefon yatay dönünce uygulama YouTube'da kalmak yerine Google sayfasına dönüyor. Tam ekran *görünümü başarılı*, dönüşte *oturumun korunması başarısız*.
- **Muhtemel neden:** Android konfigürasyon değişiminde Activity yeniden yaratılması ve GeckoSession'ın başlangıç/paylaşım URL'sinden yeniden yüklenmesi. Test cihazı üzerinde hata günlüğü henüz alınmadı.
- **Düzeltme:** Android telefon ve TV Activity manifestlerine ekran yönü/boyutu konfigürasyon yönetimi eklendi. GeckoRuntime'a konfigürasyon değişimi iletiliyor. GeckoView tam ekran ContentDelegate geri çağrısı uygulama penceresini tam ekran yapıyor; telefonda tam ekran video yatay yön istemek için mevcut oturum korunuyor.
- **Yeni test sürümü:** [v0.0.1-motor-test.2](https://github.com/mmustafatasoy/EymenBrowser/releases/tag/v0.0.1-motor-test.2) GitHub Releases üzerinde doğrudan indirilen **tek arm64 APK** olarak yayımlandı (204171344 bayt). GitHub Actions derlemesi başarılı ve kullanıcı gerçek Android telefonunda YouTube tam ekran/yatay dönüş sırasında Google sayfasına geri atma hatasının **giderildiğini doğruladı**. Debug/test imzalıdır.
- **Kapı 1:** Android telefonda tam ekran yatay dönüş ve oturum korunması başarılı; Google giriş formu açıldı; e-Devlet herkese açık ana sayfası **başarıyla açıldı**. Hesapla giriş, DRM ve diğer platform testleri yapılmadığından **Kapı 1 açık**. İki banka ana sayfası görüntüleme testi başarılıdır; banka girişi/ödeme denenmedi.
- **Google giriş formu testi (2026-11-04):** Kullanıcı, EymenBrowser Android telefonunda `accounts.google.com` giriş ekranının açıldığını doğruladı. Giriş formu yüklemesi başarılı; hesapla oturum açma **denenmedi**.
- **e-Devlet ana sayfa testi (2026-11-04):** Kullanıcı, resmî e-Devlet ana sayfasının EymenBrowser Android telefonda sorunsuz açıldığını doğruladı. Oturum açma denenmedi.
- **Banka ana sayfaları testi (2026-11-04):** Kullanıcı, EymenBrowser Android telefonda **Ziraat Bankası** ve **Türkiye İş Bankası** herkese açık ana sayfalarının her ikisinin de sorunsuz açıldığını doğruladı. Banka oturumu, ödeme, SMS ve kimlik doğrulama **denenmedi**.
- **Sonraki iş:** [Shaka Player Demo](https://shaka-project.github.io/shaka-player/demo/) üzerindeki **Sintel** Widevine DRM korumalı örneği EymenBrowser Android telefonunda denenecek. Sitenin açılması ile şifrelenmiş videonun gerçekten oynatılması ayrı ayrı değerlendirilecek. Bir hata çıkması tek başına tarayıcının DRM desteğinin kesin bulunmadığı anlamına gelmez.

## Widevine Sintel testi — 2026-11-04

- **Gerçek Android kullanıcı ekranı:** Shaka Player web demo sayfası yükleniyor; **Sintel** kutusunda **NOT AVAILABLE** yazıyor. Video oynatımı **başarısız / kullanılamıyor**, bu görüntü tek başına GeckoView'un tüm Widevine desteğini reddetmeye yetmez.
- **Kod incelemesi:** Aşama 1 Android GeckoSession için `PermissionDelegate` henüz tanımlanmamıştı. Mozilla API'sinde DRM sistem medya anahtarına erişim (`PERMISSION_MEDIA_KEY_SYSTEM_ACCESS`) bir site iznidir; Shaka'nın uygun key system seçememesi diğer olası nedenlerden biridir.
- **Düzeltme denemesi:** Her HTTPS sitesinden DRM izni gerektiğinde **kullanıcı onaylı** yerel Android istemi, reddedilirse hiçbir erişim yok. Kamera/mikrofon/konum veya başka izinler otomatik açılmaz. `shared/i18n` Türkçe/İngilizce metinleri; yeni statik regresyon kontrolü.
- **Yeni sürüm yayımlandı:** [v0.0.1-motor-test.3](https://github.com/mmustafatasoy/EymenBrowser/releases/tag/v0.0.1-motor-test.3) içinde yalnızca `EymenBrowser-Android-Telefon-arm64-DRM-IZIN-TEST.apk` (**204173292 bayt**) doğrudan indirilebilir. [GitHub Actions #41816736583](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41816736583) derleme, dosya boyutu denetimi ve yayın adımlarının tamamı **başarılı**. Android telefonda yeni DRM sonucu **henüz doğrulanmadı**. Debug imzalı motor testidir, üretim değildir; Kapı 1 açık.
- **Bir sonraki gözlem:** Önce Chrome üzerinde Sintel'in aynı durumda olup olmadığı; sonra yeni EymenBrowser sürümünde DRM izin isteminin çıkması, Sintel durumunun değişmesi, başka bir şifreli örneğin çalışması.
