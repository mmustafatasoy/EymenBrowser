# Eymen Browser — Google Play 1.0.0 hazırlığı

Durum: yalnız hazırlık dalı; yayımlanmış AAB veya Google Play onayı YOK. Tarih: 2026-11-14.

## Kapsam ve sürüm güvenliği

- Çalışma dalı `feat/google-play-v1`; esas kod `feat/android-web-apps` adayından türedi. `main`, `v1.0.0` ve sonraki GitHub release etiketleri değiştirilmez.
- Paket kimliği `com.eymen.browser.telefon` korunur; ayrı uygulama kimliği üretilmez.
- `-PeymenGooglePlay=true -PeymenReleaseVersion=1.0.0 -PeymenReleaseCode=1000000` seçenekleri yalnız Google Play hazırlığı için kullanılır.
- Google Play `versionName=1.0.0`, `versionCode=1000000`. GitHub `v1.0.23` sayısal kodu 10023 olduğundan geriye gitmez. Play güncellemeleri kendi kod dizisine göre artar; Google Play ve GitHub paketlerini rastgele çapraz güncellemek desteklenmez.
- Google Play derlemesi `targetSdk=36`, normal GitHub derlemesi `targetSdk=35`; her iki dağıtımın `minSdk=26`, namespace/paket kimliği aynıdır. Play'de 16 KB sayfa ve kenardan kenara/predictive-back gerçek cihaz testleri gerekir.
- Play derlemesinde uygulama içi GitHub APK güncelleme denetimi/kurucusu devre dışıdır; Hakkında sayfasındaki manuel kontrol yalnız Google Play'in güncellemeyi yönettiğini açıklar.

## Ayrı ve denetlenebilir AAB oluşturma

- `tools/build_google_play_bundle.sh` **yalnız temiz/disposable checkout** üzerinde, özel üretim anahtarı sağlandığında çalışır. Önce ortak kaynak/çeviri/statik denetimlerini yapar.
- `tools/prepare_google_play_manifest.py` yalnız paket yükleme izni satırını geçici olarak manifestten çıkarır; kaynak manifest dosyası `trap` ile geri yüklenir. Bu izin GitHub sideload dağıtımında korunur.
- Script `:app-telefon:bundleRelease` üretir, AAB JAR imzası/zip bütünlüğünü ve Android'in birleştirilmiş manifestinde `REQUEST_INSTALL_PACKAGES` bulunmadığını doğrular. Dosya veya imza anahtarı GitHub'a commit edilmez.
- İmzalama girişleri `EY_SIGNING_KEYSTORE_FILE`, `EY_SIGNING_STORE_PASSWORD`, `EY_SIGNING_KEY_ALIAS`, `EY_SIGNING_KEY_PASSWORD` ortam değişkenleridir. Sertifika parmak izi `docs/ANDROID_RELEASE_CERT_SHA256.txt` ile eşleşmelidir; Play App Signing kaydı ayrı Play Console işlemi gerektirir.

## Yayına geçmeden kapılar

1. Notun P0 kabul kapısı tamamlanacak; Android API35/API36 UI, görsel, arka plan indirme, kaynak/JVM/ABI grupları **aynı committe** başarılı olacak. Eski `a3a23d23` UI CI sonucu başarısızdır.
2. Notun 3.1–3.8 maddeleri veya onaylı sürüm dışı bırakma/SAPMA kararı tamamlanacak. Testleri silmek, assertion gevşetmek veya süreyi artırmak yasak.
3. Google Play API36 hedefli derlemede edge-to-edge, geri hareketi, bağlantı, bildirim, PDF, arka plan, kilit ve VPN davranışları gerçek cihazda test edilecek. AAB hazırlanması Play onayı değildir.
4. Play Console'da paket adının uygunluğu, geliştirici hesap doğrulaması, veri güvenliği, gizlilik bağlantısı, konum/kamera/mikrofon bildirimi, VPN/VpnService açıklaması ve gerekiyorsa izin beyanları tamamlanacak.
5. Play App Signing tercihi: mevcut GitHub kullanıcılarının uygulama verilerini kaybetmeden Play'e geçmesi isteniyorsa Google Play **uygulama imzalama sertifikası** mevcut üretim imzasıyla eşleşmelidir; yalnız AAB yükleme anahtarını eşleştirmek yetmez. Bu seçim Play Console'da açıkça yapılmadan çapraz güncelleme garanti edilmez.
6. Önce Google Play **dahili test** kanalında doğrulama; kullanıcı onayı olmadan herkese açık dağıtım yoktur.

Resmî kaynaklar: https://support.google.com/googleplay/android-developer/answer/11926878 ; https://developer.android.com/studio/publish/app-signing ; https://support.google.com/googleplay/android-developer/answer/12085295 ; https://support.google.com/googleplay/android-developer/answer/12564964 ; https://developer.android.com/about/versions/16/behavior-changes-16
