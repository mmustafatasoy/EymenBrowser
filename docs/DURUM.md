# Proje durumu

Son güncelleme: 2026-11-04

## Mevcut aşama
- **Aşama 1 motor denemesi genel Kapı 1 açısından açık; S-001 kullanıcı onayıyla yalnız Android telefon Aşama 2 arayüz kodlaması BAŞLADI.**
- **Kapı 1: açık.** Kullanıcı Android telefonda örnek HTTPS sayfası, **Google hesabıyla giriş ve aynı bağlantı yeniden açıldığında oturumun korunmasını**, YouTube görüntü/ses, tam ekran/yatay dönüş, e-Devlet ve iki bankanın herkese açık sayfalarını, **Sintel Widevine DRM görüntü/ses oynatımını**, **PDF görüntüleme, indirme ve indirilen dosyayı açmayı** ve **sosyal medya web kullanımını** doğruladı. Banka/e-Devlet kişisel oturumları, diğer cihaz/platform testleri ve tüm platformları kapsayan Kapı 1 kabulü tamamlanmadı. TV, masaüstü ve Apple çalışmaları kullanıcı kararıyla beklemede.
- Aşama 2 T01/T02 açılış ve ilk kurulum, T03 yeni sekme, T04 gerçek GeckoView sayfası ve T11 gündüz teması için **Android Compose başlangıç kodu eklendi**. T05–T10 ekranları henüz tamamlanmadı. `docs/SARTNAME.md` ve onaylı `design/` dosyaları değiştirilmedi.

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
1. [Android Aşama 2 uygulama planını](ANDROID_ASAMA2_PLANI.md) ve [kapsam kararını](SAPMA.md#s-001--yalnız-android-arayüzüne-geçiş) kullanıcıya sun. Onay istenen karar: diğer platformlar beklerken mevcut telefon sonuçlarını esas alıp yalnız Android arayüz uygulamasına başlamak.
2. Kullanıcı bu kapsam değişikliğini kabul ederse kararı kaydet ve onaylı T01–T11 kaynaklarıyla Aşama 2'ye başla. Onaya kadar ekran kodu yazılmayacak; genel Kapı 1 tüm platformlar için geçmiş sayılmayacak.
3. Diğer Android cihaz/sürüm kontrolleri, büyük dosyalar ve diğer dosya türleri açık kalır. TV, bilgisayar ve Apple kullanıcı yeniden başlatana kadar beklemede.

## Açık konular
- Diğer Android cihaz/sürüm kapsamı, büyük dosya ve diğer dosya türleri henüz doğrulanmadı. Banka ve e-Devlet için yalnız herkese açık ana sayfalar doğrulandı. Google giriş testi aynı uygulama oturumunda tekrar bağlantı açmayı kapsar; uygulamanın öldürülmesi/yeniden başlatılmasını kapsamaz. TV/masaüstü/iOS testleri beklemede.
- **S-001 kapsam kararı bekliyor:** Şartnamedeki tüm platformları kapsayan Kapı 1 yerine mevcut Android telefon sonuçlarıyla yalnız Android ekran uygulamasına geçilmesi henüz onaylanmadı.
- Apple Developer/EVS gerektiren üretim imzaları yok. Güncel Android telefon motor deneme APK'sı [v0.0.1-motor-test.4 Releases sayfasında](https://github.com/mmustafatasoy/EymenBrowser/releases/tag/v0.0.1-motor-test.4) **debug/test imzalı** yayımlandı; kararlı sürüm değildir.

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

- **Yayın betiği düzeltmesi:** Android telefon APK derlemesi [GitHub Actions #41832912227](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41832912227) üzerinde başarılıydı; ancak RELEASE_NOTES here-document kapanışı ile sonraki shell komutu aynı satıra birleştiği için yayın adımı gerçekte çalışmamıştı. Bu durum giderildi; yeni Releases varlığı doğrulanana kadar APK yayımlandı sayılmıyor.

- **Yeni APK doğrulandı ve yayımlandı:** [GitHub Releases v0.0.1-motor-test.4](https://github.com/mmustafatasoy/EymenBrowser/releases/tag/v0.0.1-motor-test.4) içinde yalnızca `EymenBrowser-Android-Telefon-arm64-PDF-INDIRME-TEST.apk` (**204193824 bayt**) bulunmaktadır. [GitHub Actions #41833503736](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41833503736) dosya boyutu, statik kontroller, Android APK derlemesi ve yayın adımları **başarılı**. İndirme işleminin gerçek telefonda çalıştığı **henüz test edilmedi**. Sadece Android telefon debug/test APK'dır.

## Android telefon — PDF indirme tekrar testi başarılı, 2026-11-04

- Kullanıcıya `v0.0.1-motor-test.4` APK'sı ile `dummy.pdf` belgesini açma, Download → İndir seçme, Dosyalar → İndirilenler'de dosyayı bulup açma adımları verildi. Kullanıcı **"Tamam oldu devam et"** yanıtıyla bu denemenin başarılı olduğunu bildirdi.
- **Güncel sonuç:** Örnek PDF'nin görüntülenmesi, telefona indirilmesi ve indirilen dosyanın açılması kullanıcı teyidiyle başarılı. Önceki başarısız sonuçlar yukarıda tarihçe olarak korunur. Bu geri bildirim Android 8/9 SAF, bütün dosya türleri veya arka planda büyük dosya indirme sonucu değildir.
- **Sıradaki iş:** Aynı APK üzerinde Instagram web sayfasını yükleme/kaydırma testi. Yeni APK kurulumu gerekmiyor; sosyal medya sonucu henüz denenmedi.
- **Açık durum:** Kapı 1 açık, Aşama 2 başlamadı. TV, bilgisayar ve Apple kullanıcı kararıyla beklemede. Bu kayıt yalnız test belgelerini günceller.

## Android telefon — sosyal medya testi başarılı, 2026-11-04

- Instagram bağlantısını mevcut APK'da açma ve kaydırma kontrolünün ardından kullanıcı **"Sosyal medyaya dair herşey çalışıyor"** dedi. Android telefon sosyal medya web kullanım testi kullanıcı bildirimiyle **başarılı** olarak kaydedildi.
- Ayrı sosyal medya servisleri veya kamera/mikrofon/bildirim gibi özellikler için bağımsız sonuç verilmedi. Bildirim, Google hesabıyla gerçek oturum açma testinin yerine geçmez.
- **Sıradaki iş:** Aynı Android APK'sında Google hesabıyla gerçek giriş ve sayfa yenileme sonrası oturum kontrolü. Yeni derleme gerekmiyor.
- **Açık durum:** Aşama 1 devam ediyor; Kapı 1 açık. TV, bilgisayar ve Apple kullanıcı kararıyla beklemede; Aşama 2 başlamadı.

## Android telefon — Google hesabıyla giriş başarılı, 2026-11-04

- Kullanıcıya mevcut APK'da Google hesabına giriş yapma ve aynı bağlantıyı tekrar Paylaş → EymenBrowser ile açıp oturumun korunduğunu kontrol etme adımları verildi. Kullanıcı **"Tamam oldu"** dedi.
- **Güncel sonuç:** Google hesabıyla giriş ve aynı uygulama oturumunda bağlantıyı yeniden açarken girişin korunması kullanıcı teyidiyle başarılı. Hesap bilgileri veya doğrulama kodları alınmadı. Soğuk başlatma, süreç ölümü ve diğer platformlar için sonuç çıkarılmadı.
- **Hazırlanan iş:** T01–T11'in 11 PNG/HTML çiftinin tamamı bulundu; PNG'lerin her biri 780 × 1688 piksel. [Aşama 2 planı](ANDROID_ASAMA2_PLANI.md) kaynakları ve uygulama sırasını gösterir. Bu inceleme mevcut uygulamanın görsellerle eşleştiği anlamına gelmez; ekranlar henüz uygulanmadı.
- **Sıradaki iş:** [S-001](SAPMA.md#s-001--yalnız-android-arayüzüne-geçiş) için kullanıcı kararı. Android test sonuçları hazır; tüm platformları kapsayan Kapı 1 açık. TV, bilgisayar ve Apple beklemede.

## Android telefon — S-001 onayıyla Aşama 2 başlangıcı (2026-11-04)

- Kullanıcı önceki VPN/tarayıcı sohbetinde **"Onay verildi"** diyerek S-001 Seçenek 1'i kabul etti. Eski **S-001 bekliyor** kayıtları tarihçedir, güncel karar `docs/SAPMA.md` içindedir.
- Yalnızca Android telefon: `OnboardingScreens.kt` dosyasında T01 açılış marka/halka düzeni ve T02 üç seçenekli ilk kurulum başlangıcı eklendi. Renk/ölçüler `design/tokens.json` üretilmiş değerlerinden kullanılır; süre de token üreticisinden alınır. Türkçe/İngilizce yeni metinler `shared/i18n` içinde tanımlandı.
- `MainActivity`, ilk kurulum üst katmanını çalışan GeckoView üstünde açar; seçilen reklam engeli tercihi, arama motoru ve adres çubuğu konumunu yerel tercih dosyasına kaydeder. **Henüz gerçek reklam filtresi, seçilen arama motoruyla arama ve adres çubuğu yoktur.** Filtre gerçek olmadığı için ilk kurulumda açık uyarı gösterilir. Varsayılan tarayıcı düğmesi yalnız Android sistem seçim ekranını açmayı dener; varsayılan olduğu iddia edilmez.
- GeckoSession'ın DRM, tam ekran dönüş, paylaşım/HTTPS açma ve PDF indirme bağlantıları korunmuştur. Ancak yeni ekranlar için cihaz üstünde regresyon testi **denenmedi**.
- Otomatik GitHub Actions denetiminde yalnız Android telefon derlemesi çalıştırıldı. [CI #41840006639](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41840006639) **başarılı**: telefon ABI APK derlemesi, 500 MB sınırı, kaynak/i18n ve önceki motor regresyon statik kontrolleri geçti. TV, bilgisayar ve Apple işleri otomatik akışta atlandı. Bu başarı **cihaz üstü test değildir**; üretim imzalı APK/Release **yayımlanmadı**.
- **Görsel eşleşme eksikleri:** T01/T02 için Inter ve Sora yazı tiplerinin lisanslı yerel dosyaları henüz eklenmedi, sistem yazı tipi kullanılıyor; ikonların bazıları ilk geçici yer tutucudur. Ekranların PNG ile birebir karşılaştırması ve cihaz üzeri denetimleri henüz **denenmedi**. Bu dilim tasarımın tamamı veya kararlı sürüm değildir.
- **Sıradaki iş:** T01/T02'yi 780×1688 onaylı kaynaklarla eşleştir, yazı tiplerini ve token ikonlarını yerleştir; gerçek telefonda test et. Sonra T03/T04/T11'e geç; çalışan motoru koru. Genel Kapı 1 açık; TV, masaüstü ve Apple beklemede.

- **Derleme onarımı:** Yeni onboarding metinlerindeki kesme işaretleri nedeniyle AAPT kaynak derlemesi ilk denemede hata verdi. `tools/generate.py` içindeki Android string kaçışlama sırası düzeltildi; son otomatik CI bu hatanın giderildiğini doğruladı.

## Android telefon Aşama 2 — T03/T04/T11 kabuk bağlantısı (2026-11-04)

- `BrowserChrome.kt`: Onaylı `design/kaynak/T03-Yeni-sekme.html`, `T04-Sayfa.html`, `T11-Gunduz.html` kaynaklarından üst marka alanı, sekiz kısayollu 4×2 ızgara, gerçek ziyaret edilen son iki HTTPS URL'si bölümü, Kalkan durum kartı, 48 birim adres alanı ve beş düğmeli alt gezinme satırı Compose ile başlatıldı.
- `tools/generate.py` doğrudan `design/tokens.json` içindeki ikon yollarından `EymenPaths.kt` üretir. `EymenVector` bu yolları kullanır; tasarım resimleri ve kaynakları değişmedi.
- `MainActivity`: Gerçek GeckoSession korunarak HTTPS adres açma, dört seçili arama motoruyla metin araması, geri/ileri, yenileme ve ana sayfaya dönüş bağlandı. Android paylaşım/açma niyetleri yeni arayüzden de korunur. Tam ekranda gezinme çubuğu gizlenir ve GeckoView alanı açılır. Gündüz/gece teması yerel ayarda tutulur, ilk kurulumdaki adres çubuğu konumu artık Compose durumuna bağlıdır.
- T03 tasarımında yer alan **örnek senkron bilgisayar sekmeleri** ve örnek `1.284` engel sayısı **gerçek veriymiş gibi gösterilmedi**. Uygulamanın mevcut oturumunda açılan son iki HTTPS adresi gösterilir; Kalkan motoru henüz kurulmadığı açıkça yazılır. T06 sekme, T07 menü, eşitleme ve Kalkan için işlev gösteren geçici mesajlar dışında üretim işlevi yoktur. Örnek kısayol başlıklarının dış bağlantı URL'leri ilk varsayılanlardır; kullanıcı tarafından seçilmiş yer imleri değildir.
- Gündüz teması T11 tasarım tokenlarından `gunduz_` değerleriyle çizilir, geçici Android menü diyalogundan seçilir. Gerçek T07 menü görünümü sonraki adımdır.
- Ekran okuyucuya alt gezinme düğmeleri için TR/EN etiketler eklendi. Metinler `shared/i18n` kaynaklarında; çevrilen XML'ler CI sırasında üretilir.
- Android telefon ABI derlemesi ve mevcut statik motor kontrolü [Actions #41842138955](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41842138955) üzerinde **başarılı**. Erişilebilirlik ve sonraki küçük düzeltmeler için ayrı iş akışı sonuçları ayrıca kontrol edilmeli.
- **Eksikler:** T01–T02 ve T03–T04–T11 ekranlarının 780×1688 görselleriyle cihaz ekran görüntüsü üzerinden piksel eşleştirmesi **denenmedi**. Inter/Sora yazı tipi dosyaları henüz uygulamaya eklenmedi; sistem yazı tipi yedeği kullanılır. Tablet, Android 8/11/14/16 cihaz matrisi, gerçek telefon regresyonu, performans ve erişilebilirlik testleri **denenmedi**. VPN tüneli, gerçek Kalkan reklam filtresi, gerçek T06/T07/T08/T09/T10 ve üretim imzalı kararlı sürüm **hazır değildir**. Android Aşama 2 kabulü tamamlanmadı.
- **Sıradaki iş:** T01–T04/T11 görsel eşleştirme ve gerçek cihaz testleri, ardından T06 çoklu sekme ve T09 gizli oturumun gerçek GeckoSession yaşam döngüsüyle uygulanması. TV, masaüstü ve Apple beklemede; genel Kapı 1 açıktır.
