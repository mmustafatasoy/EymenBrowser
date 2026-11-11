# Yayımlanmış imzalı APK ile gerçek uygulama içi güncelleme kabulü

Bu kabul, devam notundaki güncelleme işini iki yayımlanmış üretim APK'sıyla
Android 15/16 emülatöründe dener. Fiziksel telefon kabulünün yerine geçmez.

- Kaynak: v1.0.22, hedef: en son kararlı v1.0.23. Etiket, boyut, SHA-256 ve
  üretim sertifikası sabitlenir. Son kararlı sürüm değişmişse test bu çifti kabul etmez.
- Tarayıcının kaynakları değiştirilmez, APK tekrar imzalanmaz ve debug tarayıcı
  kullanılmaz. `tools/signed-update-audit` yalnız ayrı bir UI sürücüsüdür;
  tarayıcının güncelleme sırasında kapanmasından etkilenmeden sonucu izler.
- Gerçek ilk kurulumdan sonra ön plana gelişte sunulan güncellemenin iptali,
  manuel yeniden kontrol ve tarayıcının kendi HTTPS indirmesi denenir.
- Android'in kaynak izni başlangıçta kapalı olmalıdır. Sistem ayarındaki izin
  ve PackageInstaller son onayı gerçek ekran dokunuşlarıyla verilir. Yeni APK
  `adb install`, sessiz kurulum veya imza/izin denetimini kapatarak yüklenmez.
- Güncellemenin sonunda kurulum ekranındaki gerçek **Aç** düğmesine basılır;
  tarayıcıya dönüş ve veri koruma denetimleri bundan sonra yapılır.
- Yer imi, ilk kurulum ve Bing arama motoru seçimi gerçek arayüzden kaydedilir;
  imzalı güncellemeden sonra aynı arayüzden tekrar doğrulanır. Üretim uygulamasının
  özel dosyalarına/prefs'lerine ayrı sürücüden erişilmez.
- Son kurulu APK'nin yalnız genel program dosyası alınarak yayımlanmış hedefin
  boyutu/SHA-256'sıyla birebir eşleştirilir. Yüklü sürümün tekrar sunulmaması da denenir.
- Sürücü fiziksel cihazda veya zaten tarayıcı kurulmuş bir emülatörde çalışmayı
  reddeder. İmza Secrets'ına, VPN hesabına ya da kişisel anahtara ihtiyacı yoktur.
- CI kanıtı: gerçek izin/kurulum/önce-sonra PNG'leri, paket sürümü/sertifika
  metadata'sı, yüklü APK hash'i ve instrumentation sonucu. Başarısız sonuç kabul edilmez.

İş akışı: `.github/workflows/android-signed-update-audit.yml`.
Üretim yayın workflow'u ve yayımlanmış APK'lar değişmez.

**Güncel çift 1.0.22 → 1.0.23 kabulü başarılı:** kaynak
`0040b8633f3a62f765b48316d86ff622dc8025e9`, CI #42970974445;
Android 15/API35 işi **130359125537**, Android 16/API36 işi **130359125932**.
İkisinde de gerçek üretim 10022 → 10023, sistem izin/kurulum/Aç onayı,
üretim sertifikası, ilk kurulum/Bing/yer imi koruması ve kurulu APK'nin
yayımlanmış x86_64 hedefle birebir SHA-256 eşitliği geçti.
Artefakt **13833795334** (API35) ve **13833108618** (API36) indirildi;
12 zorunlu PNG, metadata ve gerçek kurulum/veri koruma görüntüleri incelendi.
Son kurulu sürüm kontrolü 1.0.23'ün düzeltilmiş İngilizce durum metnini gerçek
üretim APK'sında doğruladı. Fiziksel telefon kabulü ayrıca açık kalır.

**Önceki çiftin kabulü başarılı:** `b85a57311bf6b043cd1d9252b0ef39fc24e56571` üzerinde #42904693106 iki API'de geçti. Gerçek üretim 10021 → 10022, imza, sistem izin/kurulum, ilk kurulum/Bing/yer imi koruması ve kurulu APK SHA-256 eşitliği doğrulandı. Artefaktlar: API35 13803703351, API36 13804597911. Fiziksel telefon kabulü açık kalır. Ayrıntılı kanıt ve sınırlar `DURUM.md` içindedir.
