# Proje durumu

Son güncelleme: 2026-11-04

- Aşama: **0 — tamamlandı.**
- Tamamlanan iş: Başlangıç paketindeki 41 dosya orijinal biçimiyle korundu. Android telefon ve TV için ayrı Kotlin/Compose uygulama iskeletleri; iOS/iPadOS 15+ SwiftUI uygulama iskeleti; Windows, macOS ve Linux için TypeScript/React/castLabs Electron iskeleti oluşturuldu.
- Ortak kaynaklar: `versions.toml`, `shared/i18n/tr.json`, `shared/i18n/en.json`, `design/tokens.json` kaynaklı Android/iOS/masaüstü tema ve simge üreteci, bağımlılık/500 MB denetimi çalışır durumdadır.
- Doğrulama: [GitHub Actions Aşama 0, çalıştırma #41801213920](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41801213920) — **altı işin tamamı başarılı:** tema/metin/simge üretimi ve kontroller; Android telefon+TV derlemesi; iOS simülatör derlemesi; Windows, macOS ve Linux kurulum paketleri.
- Aşama 0 kabul:
  - [x] Boş Android, TV, iOS ve Electron uygulamaları GitHub Actions'ta derlendi.
  - [x] `design/tokens.json` üzerinden Android, iOS ve bilgisayar temaları otomatik üretildi; `--check` denetimi geçti.
  - [x] 500 MB kurulum dosyası sınırı ve yasak bağımlılık denetimi çalıştı ve derlemelerde geçti.
  - [x] DURUM.md güncel kabul kaydıyla duruyor.
- Üretilenler yalnızca **Aşama 0 test çıktılarıdır**: Android debug APK, iOS imzasız simülatör paketi, masaüstü imzasız paketleri. Bunlar kararlı veya tamamlanmış tarayıcı olarak yayımlanmadı.
- Sıradaki iş: **Aşama 1 — motor denemesi**. Android/TV GeckoView, iOS WKWebView, masaüstü castLabs Electron/Chromium için arayüzsüz deneme ve gerçek cihaz/site/DRM/Google giriş testleri.
- Açık sorunlar ve henüz denenmeyenler: Google hesabı oturum açma, korumalı video, e-Devlet/bankalar, gerçek telefon/TV/iPhone çalıştırma, motor kurulum boyutları ve Kapı 1 kullanıcı onayı **denenmedi**. Aşama 1 şartname ölçütleri yerine gelmeden ekran kodlarına geçilmeyecek.
- Şartname: `docs/SARTNAME.md` değiştirilmedi; orijinal `design/` dosyaları korunuyor.
