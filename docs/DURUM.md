# Proje durumu

Son güncelleme: 2026-11-04

## Mevcut aşama
- **Aşama 1 — Motor denemesi: devam ediyor.**
- **Kapı 1: açık.** Kullanıcı Android telefonda örnek HTTPS sayfası, Google giriş formu, YouTube görüntü/ses, tam ekran/yatay dönüş, e-Devlet ve iki bankanın herkese açık sayfalarını ve **Sintel Widevine DRM görüntü/ses oynatımını** doğruladı. Gerçek Google/banka oturumu, **dosyanın indirilmesi**, sosyal medya, TV, masaüstü, iOS ve kullanıcı Kapı 1 onayı hâlâ tamamlanmadı. **PDF belgesini görüntüleme** Android telefon ekran fotoğrafıyla doğrulandı.
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
- **İkinci kullanıcı geri bildirimi (2026-11-04):** Kullanıcı `v0.0.1-motor-test.3` sürümünde Sintel DRM videosunun **görüntülü ve sesli oynadığını** açıkça doğruladı. Android telefonda **Sintel Widevine oynatma testi başarılı**. İzleme süresi, çözünürlük, lisans sürekliliği, Chrome karşılaştırması ve diğer platformlarda DRM hâlâ denenmedi.
- **Sonraki iş:** Android TV için işlemciye göre iki **ayrı, doğrudan indirilebilir APK** yayımla; gerçek Philips Android TV/Google TV'de ilk HTTPS açılışını dene. TV'de DRM ve kumanda akışı, ardından Electron masaüstü ve iOS motor testleri. Kapı 1 açık.

## Aşama 1 — Android TV test paketi (2026-11-04)

- Android TV GeckoView motor test APK'ları doğrudan Releases varlığı olarak `arm64-v8a` ve `armeabi-v7a` için **iki ayrı APK** şeklinde yayımlanacak. ZIP veya telefon APK'sı TV kullanıcısına dayatılmayacak.
- Test APK'sı açıldığında `https://example.org/` görüntülenmesi **beklenir**, gerçek TV'de başarılı olduğu henüz doğrulanmadı.
- **TV kumandasıyla tam gezinti, video DRM, giriş ekranları denenmedi**; bu aşama yalnız motor doğrulamasıdır ve V01–V03 ekranları Kapı 1 öncesi yapılmaz.
- **Android TV APK'ları yayımlandı:** [GitHub Releases v0.0.1-tv-motor-test.1](https://github.com/mmustafatasoy/EymenBrowser/releases/tag/v0.0.1-tv-motor-test.1) üzerinde **iki ayrı ZIP'siz APK** var: arm64-v8a **205425920 bayt**, armeabi-v7a **166230996 bayt**. GitHub Releases varlık durumları yüklenmiş ve yayın herkese açık; gerçek TV testleri **henüz denenmedi**. GitHub Actions iş akışı [#41826501884](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41826501884).

## Platform önceliği — 2026-11-04 (kullanıcı kararı)

- **Şimdilik yalnız Android telefon** çalışmalarına devam edilecek.
- **Android TV, Windows/Linux/macOS ve iOS/iPadOS çalışmaları duraklatıldı.** Kullanıcı açıkça "başlayalım" demedikçe bu platformlar için yeni kodlama, derleme veya test talebi başlatılmayacak. Mevcut kod ve yayınlar korunur.
- Aşama 1 Android telefon için sıradaki testler: sosyal medya web sayfası, PDF açma/indirme, kullanıcı verisi gerektirmeyen site uyumluluğu ve gerekli diğer motor kontrolleri.
- **Şartname değişmedi:** Kapı 1'in tüm platformları kapsayan kabul ölçütleri tamamlanmadan Aşama 2'nin resmî olarak tamamlandığı iddia edilmeyecek; onaylı arayüzlere başlamak için gerekli aşama onayı ayrıca alınmalı.

## Android telefon — PDF okuma testi (2026-11-04)

- Kullanıcı `dummy.pdf` belgesinin EymenBrowser'da açıldığını gösteren gerçek telefon ekran fotoğrafı paylaştı. Beyaz PDF belgesi ve `Dummy PDF file` içeriği görüntüleniyor: **Android GeckoView PDF okuma testi başarılı.**
- Ekranda üstte `Download` komutu var. **Bu düğmeye basılıp PDF'nin telefona kaydedildiği henüz doğrulanmadı.**
- **Sıradaki Android testi:** PDF ekranındaki `Download` düğmesini kullan, ardından Dosyalar/İndirilenler'de `dummy.pdf` dosyasının gerçekten oluştuğunu kontrol et. Çalışmazsa indirme olay işleyicisi motor testinden ayrı incelenecek.
- TV, PC ve Apple çalışmaları kullanıcının önceki isteği doğrultusunda **beklemede**. Kapı 1 açıktır.

## Android telefon — PDF indirme düzeltmesi, 2026-11-04

- **Hata kullanıcı tarafından doğrulandı:** GeckoView dahili PDF görüntüleyici `dummy.pdf` dosyasını görüntülüyor ancak **Download düğmesine basılınca telefona hiçbir dosya kaydedilmiyor**.
- **Teknik neden:** `GeckoSession.ContentDelegate.onExternalResponse` indirme olayları önceki motor testinde işlenmiyordu. GeckoView PDF.js'den gelen `WebResponse.body` akışını tüketen indirme işleyicisi eklenmediği için dosya kaydı gerçekleşmiyor.
- **Düzeltme:** Yalnız Android telefon için `AndroidDownloadDenemesi`. İndirme istekleri GeckoView'un orijinal `WebResponse.body` akışıyla kullanıcı onayından sonra kaydedilir. Android 10+ MediaStore Downloads `IS_PENDING` ile; Android 8-9 sistem SAF dosya seçicisiyle. Arka plan akışı, başarısız/kısmi kayıt temizliği, TR/EN yerelleştirme ve statik regresyon kontrolü eklendi. DRM/yatay dönüş değiştirilmedi.
- **Yeni sürüm hedefi:** `v0.0.1-motor-test.4`, yalnız arm64 Android telefon **tek doğrudan APK**. CI ve kullanıcı testi sonuçları doğrulanana kadar indirme **başarılı sayılmıyor**.
- **Sonraki test:** Yeni APK kur, `dummy.pdf` bağlantısını paylaş, PDF görüntüleyicide `Download` düğmesine bas, EYMEN Browser indirme onayını kabul et, Dosyalar → İndirilenler içinde `dummy.pdf` dosyasının varlığını doğrula.
- **Platform kararı korunur:** TV, PC ve Apple çalışmaları kullanıcı iznine kadar bekletilir. Kapı 1 açıktır.

- **CI denetimi düzeltmesi (2026-11-04):** İlk statik indirme kontrolü `HttpURLConnection` adının yalnız kod açıklamasında geçmesini hatalı olarak yasaklı çağrı saydı. Açıklama düzeltilerek işlevsel kod değiştirilmeden kontrol yeniden çalıştırıldı.

- **Derleyici düzeltmesi:** İlk Kotlin derlemesinde arka plan lambda'sından yakalanan `createdDownload` değişkeni için akıllı tür dönüşümü hatası oluştu. Nullable URI kullanımı yerel değişmez `downloadUri` ile düzeltildi; indirme özelliğinin gerçek cihaz sonucu hâlâ bekleniyor.

- **İkinci CI kontrol düzeltmesi:** URI işlemi Kotlin derleyicisi için `downloadUri` değişkenine taşınınca statik denetimin eski `createdDownload` aramasını güncellemek gerekti. Yeni derleme yeniden tetiklendi; gerçek telefon indirme sonucu hâlâ bekleniyor.
