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

**Güncel çift 1.0.22 → 1.0.23:** Android 15/16 kabulü henüz denenmedi;
test dalının CI sonucu beklenir. Son kurulu sürüm kontrolü artık 1.0.23'teki
düzeltilmiş TR/EN durum metnini gerçek üretim APK'sından okur.

**Önceki çiftin kabulü başarılı:** `b85a57311bf6b043cd1d9252b0ef39fc24e56571` üzerinde #42904693106 iki API'de geçti. Gerçek üretim 10021 → 10022, imza, sistem izin/kurulum, ilk kurulum/Bing/yer imi koruması ve kurulu APK SHA-256 eşitliği doğrulandı. Artefaktlar: API35 13803703351, API36 13804597911. Fiziksel telefon kabulü açık kalır. Ayrıntılı kanıt ve sınırlar `DURUM.md` içindedir.
