# Proje durumu

Son güncelleme: 2026-11-04

- Aşama: **1 — motor denemesi, devam ediyor.**
- Aşama 0: [Başarılı GitHub Actions #41801213920](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41801213920); altı iş geçti.
- Biten iş: GeckoView 159.0.20261022150525 sürümü `versions.toml` dosyasında sabitlendi; Android telefon ve Android TV GeckoView oturumları ve üç ABI ayrımı; iOS WKWebView; bilgisayarda sandbox'lı castLabs Electron WebContentsView motor denemeleri kodlandı.
- CI: İlk Android motor derlemesi, GeckoView 159'un Media3 bağımlılıkları compileSdk 36 gerektirdiği için başarısız oldu. Bu commit'te yalnızca derleme SDK'sı 35'ten 36'ya yükseltildi (targetSdk=35, minSdk=26 korundu); yeni derleme sonucu henüz doğrulanmadı.
- Sıradaki iş: GitHub Actions motor derleme sonuçlarını kontrol et, hataları düzelt, motorlu paketlerin gerçek boyutlarını kaydet.
- Açık sorunlar/denenmedi: Google girişi, korumalı video, banka, e-Devlet, gerçek cihaz ve emülatör testi, Kapı 1 kullanıcı onayı.
- Şartname: `docs/SARTNAME.md` ve onaylı `design/` kaynakları değiştirilmedi; Aşama 2 ekran kodlamasına başlanmadı.
