# Aşama 1 — motor denemeleri

Bu proje, onaylı 16 ekranın uygulaması değildir. **Android telefonda temel HTTPS sayfaları,
YouTube ve Sintel Widevine DRM oynatma kullanıcı tarafından doğrulandı.** Google hesabıyla
oturum açma, TV/masaüstü/iOS DRM, dosya indirme ve diğer motor testleri henüz tamamlanmadı.
Derleme başarısı gerçek cihaz doğrulaması yerine geçmez.

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
| Google hesabı giriş | Giriş formu açıldı; hesapla oturum açma DENENMEDİ | Denenmedi | Denenmedi | Denenmedi |
| Korumalı video/DRM | **Başarılı: Sintel Widevine görüntülü + sesli oynuyor** (`v0.0.1-motor-test.3`, kullanıcı teyidi); diğer DRM örnekleri denenmedi | Denenmedi | Denenmedi | Denenmedi |
| e-Devlet | Başarılı: herkese açık ana sayfa açıldı; oturum açma denenmedi | Denenmedi | Denenmedi | Denenmedi |
| İki banka ve ödeme sayfası | Ziraat ve İş Bankası herkese açık ana sayfaları başarılı; ödeme/giriş denenmedi | Denenmedi | Denenmedi | Denenmedi |
| YouTube, video, sosyal medya | YouTube video: görüntü + ses başarılı (kullanıcı doğruladı); diğerleri denenmedi | Denenmedi | Denenmedi | Denenmedi |
| PDF ve dosya indirme | **PDF görüntüleme başarılı** (Dummy PDF ekran fotoğrafı); Download düğmesi görünüyor, dosyanın gerçekten indirilmesi DENENMEDİ | Denenmedi | Denenmedi | Denenmedi |

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
- **Doğrulanan:** YouTube video/ses ve tam ekran çalışıyor; `v0.0.1-motor-test.2` ile yatay dönüşte Google sayfasına atma sorunu kullanıcı testinde **giderildi**. **Henüz doğrulanmadı:** Arka planda oynatma, oturum açma, DRM, TV ve diğer platformlarda video.
- **Sıradaki deneme:** Aynı videonun tam ekran düğmesini kullanarak ekranda gerçekten tam ekran açıldığını test et.

## Tam ekran dönüş hatası ve yeni regresyon testi (2026-11-04)

- Kullanıcı: YouTube videosu tam ekran açılıyor; telefon yan dönerken sayfa Google'a dönüyor. Mevcut sürümde **başarısız**.
- Olası teknik kök neden: Android ekran yönü değişiminde Activity/GeckoSession yeniden oluşturuluyor; başlangıç/paylaşım bağlantısı yükleniyor.
- Kod düzeltmesi: Android manifest `configChanges` (orientation/screenSize/smallestScreenSize/screenLayout/keyboardHidden), `onConfigurationChanged`, GeckoRuntime orientasyon bildirimi ve `ContentDelegate.onFullScreen` ile pencere/yatay mod yönetimi.
- `tools/check_rotation.py` statik yapılandırma regresyon denetimidir; cihaz testi yerine geçmez.
- Yeni [`v0.0.1-motor-test.2`](https://github.com/mmustafatasoy/EymenBrowser/releases/tag/v0.0.1-motor-test.2) tek arm64 telefon APK'sı yayımlandı; üretim imzalı değildir.
- **Tekrar test sonucu (kullanıcı, 2026-11-04):** Yeni APK'da YouTube tam ekran ve yataya dönme sırasında Google sayfasına geri atma sorunu **giderildi**. Video oturumu korunuyor. Tam ekrandan portreye dönüş ve başka platformlardaki davranış ayrıca ölçülmedi.

## Google giriş ekranı — 2026-11-04

- Kullanıcı, **EymenBrowser Android telefon** üzerinde Google hesap giriş ekranının açıldığını doğruladı.
- **Başarılı:** Google HTTPS giriş sayfasının ve formunun görüntülenmesi.
- **Denenmedi:** Gerçek hesap kimlik doğrulaması, oturum çerezi ve Electron oturum açma.
- Sıradaki test: `https://www.turkiye.gov.tr/` resmî e-Devlet sitesinin **herkese açık** ana sayfası. Chrome'dan bağlantıyı EymenBrowser'a paylaş. Yalnız sayfa açılma sonucunu bildir; **T.C. kimlik numarası, parola, SMS kodu veya özel belge paylaşma**.

## e-Devlet herkese açık sayfa testi — 2026-11-04

- Kullanıcı, `https://www.turkiye.gov.tr/` resmî ana sayfasının EymenBrowser Android telefon uygulamasında **sorunsuz açıldığını** doğruladı.
- **Başarılı:** Herkese açık e-Devlet ana sayfasının GeckoView ile yüklenmesi.
- **Denenmedi:** T.C. kimlik numarası/şifreyle oturum açma, kişisel hizmetler, TV/masaüstü/iOS.
- **Sıradaki test:** Ziraat Bankası `https://www.ziraatbank.com.tr/tr` ve Türkiye İş Bankası `https://www.isbank.com.tr/` herkese açık ana sayfaları. Hesaba giriş, ödeme veya kart/şifre/SMS bilgisi girişi yapılmayacak.

## İki banka — kamuya açık ana sayfalar — 2026-11-04

- Kullanıcı EymenBrowser Android telefonunda **Ziraat Bankası** ve **Türkiye İş Bankası** ana sayfalarını sorunsuz açtığını doğruladı.
- **Başarılı:** Her iki bankanın yalnız kamuya açık tanıtım/ana sayfalarının GeckoView ile yüklenmesi.
- **Denenmedi:** Banka hesap kimlik doğrulaması, şifre/SMS, ödeme, kart işlemleri, güvenli bankacılık uyumluluğu, TV/PC/iOS.
- Bu doğrulama bankacılık güvenliği veya oturum açma desteğinin sertifikası değildir.

## Sıradaki test — Widevine DRM korumalı video

- Kaynak: Shaka Player resmî demo `https://shaka-project.github.io/shaka-player/demo/`.
- `Sintel` örneği demo içinde **Widevine ile şifrelenmiş** içerik olarak etiketlenmiştir.
- Android'de Chrome ile demo linkini açıp **Paylaş → EymenBrowser** üzerinden gönder.
- Site yüklendikten sonra `Sintel` içeriğini seç ve **Play** düğmesine dokun.
- **Ayrı raporla:** (1) demo sayfası yüklendi mi, (2) DRM korumalı Sintel video görüntülü/sesli oynadı mı, (3) varsa hata mesajı.
- YouTube videosunun daha önce açılmış olması Widevine DRM uyumluluğunun kanıtı değildir. DRM lisans sunucusunun veya içerik dağıtım altyapısının geçici hatası da olasıdır.
- DRM'yi aşmaya yönelik denetim kapatma, lisans müdahalesi veya koruma devre dışı bırakma yapılmayacak. Sonuç **kullanıcı testine kadar denenmedi**.

## Shaka Player Sintel — gerçek kullanıcı ekranı (2026-11-04)

- Kullanıcının fotoğrafında resmî Shaka Player demo ana sayfası ve Widevine şifreli Sintel kartı görünür.
- **Sonuç:** Web sayfası başarılı yükleniyor; Sintel oynatılabilir değil; kartta `NOT AVAILABLE`. Bu testte **DRM oynatma başarılı değildir**.
- **Muhtemel nedenler:** GeckoView'da DRM site izin işleyicisinin eksik olması (tespit edildi), key-system/CDM desteğinin olmaması, format/DRM kısıtları veya demo servis durumu. Fotoğraf nedenini tek başına ayırmaz.
- **Test düzeltmesi:** `GeckoSession.PermissionDelegate` DRM anahtar erişimi isteyen HTTPS site için açık kullanıcı seçimi ile izin verebilir/reddedebilir; diğer izinleri otomatik açmaz. [Tek arm64 APK v0.0.1-motor-test.3](https://github.com/mmustafatasoy/EymenBrowser/releases/tag/v0.0.1-motor-test.3) **204173292 bayt** olarak yayımlandı; GitHub Actions derleme ve yayın adımları başarılı. Aynı site gerçek Android telefonda yeniden denenmeden DRM uyumluluğu **kanıtlanmış sayılmaz**.
- **Kontrol önerisi:** Aynı Sintel örneğinin Chrome'da da `NOT AVAILABLE` olup olmadığını karşılaştır. Yeni APK'da DRM izni açılırsa, kullanıcı özellikle **İzin ver** seçerse oynatma değişiyor mu kaydet. Yazılım/servis kaynaklı kısıtlarda kesin uyumluluk iddiasında bulunma.

## Widevine DRM izin düzeltmesi — kullanıcı geri bildirimi, 2026-11-04

- `v0.0.1-motor-test.3` tek APK yayımlandıktan sonra kullanıcı: **"Bu sefer açılıyor"**.
- Önceki `NOT AVAILABLE` göstergesine kıyasla DRM demo içerik açılışında **olumlu ilerleme** bildirildi.
- **Henüz açık:** Gerçek Sintel videosunun görüntü ve sesle oynatılıp oynatılmadığı, DRM izin iletişim kutusu, kesintisiz oynatma süresi ve Chrome karşılaştırması bu kısa geri bildirimden kesinleştirilemiyor.
- Sonuç bu nedenle **tam DRM oynatma başarılı** işaretlenmedi. Kullanıcıdan görüntü ve ses teyidi geldikten sonra matris güncellenecek.

## Widevine Sintel tekrar testi — TAM oynatma teyidi (2026-11-04)

- Kullanıcı, `v0.0.1-motor-test.3` sürümünde **Sintel DRM videosunun görüntülü ve sesli oynadığını** açıkça bildirdi.
- **Başarılı:** Android telefonda GeckoView ile Shaka Player Sintel **Widevine DRM korumalı örneğinde görüntü ve ses**.
- **Henüz doğrulanmadı:** Kesintisiz oynatma süresi, 4K kalitesi, Dolby özellikleri, diğer DRM servisleri (ör. Netflix), TV/PC/iOS korumalı içerik, DRM isteminin ne zaman gösterildiği.
- Önceki `NOT AVAILABLE` denemesi önceki test sürümünün tarihsel sonucu olarak korunur; güncel matriste başarılı sonuç geçerlidir.

## Sıradaki hedef — Philips Android TV GeckoView motor denemesi

- **GitHub Releases [v0.0.1-tv-motor-test.1](https://github.com/mmustafatasoy/EymenBrowser/releases/tag/v0.0.1-tv-motor-test.1)** üzerinde **iki ayrı APK yayımlandı**: `EymenBrowser-TV-arm64-v8a-MOTOR-TEST.apk` (**205425920 bayt**) ve `EymenBrowser-TV-armeabi-v7a-MOTOR-TEST.apk` (**166230996 bayt**). ZIP gerekmez. TV işlemci mimarisine uygun **tek** APK kurulmalı.
- Yalnız Android TV / Google TV (Android 8+) kapsamda; LG webOS, Samsung Tizen, Whale OS gibi diğer sistemler bu APK'yı doğrudan çalıştırmayabilir.
- Uygulama açılınca `https://example.org/` sayfası görüntülenirse **yalnız başlangıç HTTPS motor testi** başarılı sayılır. Daha fazla site açma ve TV kumanda testleri ayrıca yapılır.
- TV APK'sı **debug/test imzalıdır**, üretim sürümü değildir; Google hesabı/banka bilgileri TV üzerinde test amacıyla girilmemelidir.

## Android telefon — PDF sayfa görüntüleme (2026-11-04)

- Kullanıcı, `https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf` deneme bağlantısının EymenBrowser'da **uygulama içi PDF görüntüleyicide açıldığını** gösteren ekran fotoğrafı paylaştı.
- Görselde beyaz PDF sayfası üzerinde `Dummy PDF file` metni ve üst tarafta `Download` seçeneği açıkça görülüyor.
- **Başarılı:** GeckoView Android telefon içinde örnek PDF belgesi görüntüleme.
- **Henüz doğrulanmadı:** Download düğmesinin dosyayı telefonun İndirilenler klasörüne kaydetmesi, dosyanın sonradan açılması ve Android indirme yöneticisi uyumluluğu. Görünen düğme tek başına indirme başarısı kanıtı değildir.
- **Sonraki test:** PDF açıkken `Download` düğmesine dokun. Android dosyayı indirirse telefonun Dosyalar → İndirilenler bölümünden `dummy.pdf` dosyasını doğrula. Çıkarsa hata türünü bildir. Kişisel bir dosya kullanma.
- **Platform önceliği:** Kullanıcı isteği uyarınca yalnız Android telefon çalışmaları devam ediyor; TV, masaüstü ve Apple beklemede. Kapı 1 tüm platformlar için henüz açık.
