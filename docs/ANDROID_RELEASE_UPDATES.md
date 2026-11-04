# EYMEN Browser — imzalı Android kararlı sürüm ve uygulama içi güncelleme

Tarih: 2026-11-04. Bu belge **Android telefon** içindir. TV, iOS ve masaüstü bu yayın hattına dahil değildir.

## Şu an ne hazır?

- `BrowserUpdateManager.kt` sürüm kontrolü: GitHub reposunun yalnız **en son kararlı** (`prerelease=false`, `draft=false`) Release bilgisini güvenli HTTPS ile kontrol eder. Sürüm etiketi `vMAJOR.MINOR.PATCH` olmalıdır.
- Uygulama **ön plana geldiğinde en fazla 6 saatte bir** kontrol eder. Ayarlar > Hakkında ve güncelleme, ayrıca anında manuel kontrol eder. **Arka planda sürekli çalışan push bildirim hizmeti yok**; en erken uygulama yeniden açıldığında kontrol edilir.
- En uygun dosya `Build.SUPPORTED_ABIS` sırasına göre seçilir: `arm64-v8a`, `armeabi-v7a`, `x86_64`. Kullanıcının sürekli GitHub web sitesine gitmesi gerekmez.
- Yükleme kullanıcı tarafından onaylanınca APK uygulamanın **özel önbelleğine** indirilir; ilerleme çubuğu gösterilir. Android 8 ve üstünde kurulum sırasında işletim sisteminin izin ekranı ve **kurulumun son onayı zorunludur**. Kullanıcı bilgisi olmadan APK yüklenmez.
- Boyut (azami 500 MiB), SHA-256 (GitHub Releases asset `digest`), paket adı, sürüm kodu ve **mevcut uygulamanın imza sertifikası** doğrulanır. Hatalı, eski veya başka uygulamaya ait APK kurulum için sunulmaz. HTTPS yönlendirmeleri sınırlı GitHub alan adlarından ayrılmaz.
- İndirme önbellek dosyasıdır; eski elle kurulmuş GitHub test dosyalarını veya kullanıcı dosyalarını silmez.
- Bilgi paylaşımı/telemetri yok: kontrol yalnız genel GitHub Releases API'sine HTTPS isteği yapar.

## İmzalama anahtarı — yalnız bir kez yapılacak

Yeni EYMEN Browser üretim imza anahtarı çevrimdışı üretilmiştir. **Anahtarın kendisi, parolası veya base64 içeriği GitHub deposuna yazılmamıştır.** Ayrı verilen `EymenBrowser-private-signing-backup.zip` kişisel yedek paketi, özel anahtar `EymenBrowser-release.p12` ve `GitHub-secrets-SETUP.txt` adlı gizli kurulum bilgilerini içerir.

**Bu ZIP'i herkese açık bir linke, GitHub Issues'a, commit'e veya Release'e asla yüklemeyin.** İki ayrı güvenli çevrimdışı kopya saklayın. Anahtar kaybolursa Android mevcut uygulamanın üzerine başka bir imzayla güncelleme kabul etmez.

GitHub'da `https://github.com/mmustafatasoy/EymenBrowser/settings/secrets/actions` adresini açın; `New repository secret` seçeneğiyle paketin içindeki metinde verilen **dört ayrı gizli değeri** ekleyin:

1. `EYMEN_ANDROID_KEYSTORE_BASE64` — tüm .p12 anahtar dosyasının base64 değeri.
2. `EYMEN_ANDROID_STORE_PASSWORD` — anahtar deposu şifresi.
3. `EYMEN_ANDROID_KEY_ALIAS` — imza anahtarının alias adı.
4. `EYMEN_ANDROID_KEY_PASSWORD` — imza anahtarının şifresi.

Özel anahtar **GitHub Actions Secrets** dışında herhangi bir GitHub kaynağında tutulmaz. İmza sertifikasının yalnız **halka açık SHA-256 özeti** `docs/ANDROID_RELEASE_CERT_SHA256.txt` dosyasındadır; GitHub Actions doğru anahtarı burada yazılı sertifikayla eşleştirir.

## İlk kararlı sürümü yayınlama

1. Geliştirme/değişiklikler `main` dalında birleştirilmiş olmalı. [GitHub Actions](https://github.com/mmustafatasoy/EymenBrowser/actions) bölümünde mevcut Android derleme, güvenlik ve emülatör açılış testlerinin başarılı olduğunu doğrulayın. Fiziksel telefon kontrolü de önerilir.
2. GitHub Actions > `EymenBrowser Android signed stable release` > `Run workflow` > Branch `main` > Version `1.0.0` girin.
3. İş akışı sürüm kodunu **10000** olarak hesaplar; kaynak denetimleri, özel keystore imzası ve kayıtlı orijinal sertifika parmak izi, üç ABI APK, `apksigner`, 500 MiB sınırı ve Android 35 AOSP emülatör açılış/görsel kimlik testini çalıştırır.
4. **Yalnız bu kontroller başarılıysa** `v1.0.0` kararlı GitHub Release açılır ve üç imzalı APK ile `SHA256SUMS.txt` yayımlanır. `production` ortamı için GitHub ayarlarında koruma/onay kuralları eklenebilir; bu özellikle kararlı yayın için tavsiye edilir.
5. Telefon işlemcisine uygun APK'yı **ilk kararlı kurulumda bir kez** kurun. Sonraki sürümleri `v1.0.1`, `v1.0.2`, `v1.1.0` vb. olarak **aynı dört secrets/aynı anahtarla** aynı iş akışıyla yayımlayın. Kurulu üretim sürümü artık bu güncellemeleri uygulama içinden indirebilir.

**Önemli:** Mevcut GitHub Actions `Debug` APK'ları Android'in test anahtarıyla imzalıdır. Bu yeni üretim anahtarıyla **mevcut debug kurulumunun üzerine doğrudan yüklenemez**. Debug uygulamasındaki verileri ve yer imlerini önceden yedekleyin; gerekiyorsa debug'u kaldırıp kararlı APK'yı ilk kez kurun. *Sonraki kararlı sürümler artık aynı üretim anahtarıyla birbiri üzerine kurulabilir.*

## Yayın durumuyla ilgili dürüst not

`.github/workflows/android-stable-release.yml` imzalı yayını **otomatik olarak her commit'te başlatmaz**; yanlışlıkla yarım sürüm üretmemek için sürüm sahibi `Run workflow` ile bir kez onay verir. GitHub repo/Environment secrets eklenmeden **gerçek imzalı APK veya yeni kararlı Release yayımlanamaz**. Anahtarları ChatGPT'ye mesajla göndermeyin; yalnız GitHub'ın Secret alanına ekleyin.

Emülatör testi tek başına cihaz uyumluluğunu kanıtlamaz. Android 8/11/14/16, DRM/YouTube, PDF/indirme, farklı GPU ve ekranlarda kalan testler şartnamedeki açık konulardır. Henüz tamamlanmayan VPN, cihaz eşitlemesi ve diğer menü özellikleri kararlı sürüm ilan edilse bile yapılmış sayılmaz.

## Resmî Android dayanakları

- [Uygulama imza belgeleri](https://developer.android.com/studio/publish/app-signing)
- [Uygulama kurulum izinleri ve PackageInstaller](https://developer.android.com/reference/android/content/pm/PackageInstaller)
- [Dosya paylaşımı için FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider)
