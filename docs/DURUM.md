# Proje durumu

Son güncelleme: 2026-11-04

## Mevcut aşama
- **Aşama 1 — Motor denemesi: devam ediyor.**
- **Kapı 1: açılmadı.** Gerçek cihazda Google girişi, DRM, e-Devlet, bankalar, video, PDF/indirme testleri yapılmadı ve kullanıcı onayı alınmadı.
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

## Sıradaki iş
1. Fiziksel Android telefon ve Android TV'ye işlemciye uygun test APK'sı kurulup varsayılan HTTPS sayfası açma denenmeli.
2. Ardından Google girişi, korumalı video, e-Devlet, iki banka, PDF/indirme ve medya testleri her motor için gerçek sonuçlarla kaydedilmeli.
3. Gerçek motor testleri ve kullanıcı onayı olmadan Kapı 1 kapanmayacak.

## Açık konular
- Gerçek cihaz motor uyumu, Google oturum açma, DRM/korumalı video, e-Devlet, bankalar, TV kumandasıyla test yolu ve iPhone testleri **denenmedi**.
- Apple Developer/EVS gerektiren üretim imzaları yok. Test APK'ları `Releases` içinde yayınlanmadı.
