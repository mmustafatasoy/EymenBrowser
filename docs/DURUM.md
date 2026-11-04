# Proje durumu

Son güncelleme: 2026-11-04

- Aşama: **1 — motor denemesi, devam ediyor.**
- Aşama 0: [Başarılı GitHub Actions #41801213920](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41801213920); altı iş geçti.
- Biten iş: GeckoView 159.0.20261022150525 sürümü `versions.toml` dosyasında sabitlendi; Android telefon ve Android TV GeckoView oturumları ve üç ABI ayrımı; iOS WKWebView; bilgisayarda sandbox'lı castLabs Electron WebContentsView motor denemeleri kodlandı.
- CI: İlk Android motor derlemesi, GeckoView 159'un Media3 bağımlılıkları compileSdk 36 gerektirdiği için başarısız oldu. GeckoView 159 daha yeni Android API 37 ve AGP 9.1+ gerektirdi. Android yapı araçları Gradle 9.3.1, AGP 9.1.1, Compose Kotlin 2.2.10 ve compileSdk 37 ile eşlendi; targetSdk=35 ve minSdk=26 korundu. İlk AGP 9.1 denemesinde GitHub Actions çalıştırıcısında `sdkmanager` PATH içinde olmadığı görüldü; araç tam Android SDK yoluyla çağrılacak. Sonraki denemede GitHub Actions çalışma ortamında SDK yöneticisi kurulu değildi. CI SDK yöneticisi kuruldu; Android 17 SDK paket yolu `platforms;android-37.0` olarak düzeltildi. Android Gradle 9.1 ve Kotlin 2.4.20 denemesinde eski Kotlin DSL'nin deprecation hatası verdiği görüldü. `android.newDsl=false` kaldırılıp modern `compilerOptions` diline geçildi; yeni derleme sonucu henüz doğrulanmadı.
- Sıradaki iş: GitHub Actions motor derleme sonuçlarını kontrol et, hataları düzelt, motorlu paketlerin gerçek boyutlarını kaydet.
- Açık sorunlar/denenmedi: Google girişi, korumalı video, banka, e-Devlet, gerçek cihaz ve emülatör testi, Kapı 1 kullanıcı onayı.
- Şartname: `docs/SARTNAME.md` ve onaylı `design/` kaynakları değiştirilmedi; Aşama 2 ekran kodlamasına başlanmadı.

- Son teknik düzeltme: AGP 9.1 new DSL ile bağımsız Kotlin Android eklentisi bağdaşmadığı için resmî AGP 9 yerleşik Kotlin modeline geçildi. Android kütüphane eklentisi de 9.1.1'e eşitlendi. Kotlin 2.4.20 Compose sürümü korunuyor; GitHub Actions doğrulaması henüz yapılmadı.
