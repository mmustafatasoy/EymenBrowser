# GeckoView motor denemesi

Aşama 1: kullanıcı arayüzü değil, GeckoView ile HTTPS sayfa açma testi.

- GeckoView sürümü `versions.toml` kaynağından alınır.
- İlk test sitesi `https://example.org/`.
- ADB üzerinden deneme:
  `adb shell am start -n com.eymen.browser.telefon/.MainActivity --es test_url https://www.google.com/`
- TV uygulaması için paket adı `com.eymen.browser.tv`.
- Google girişi, banka siteleri ve korumalı video gerçek cihazda ayrıca test edilmelidir.
- HTTPS sertifika doğrulaması kapatılmadı; telemetri eklenmedi.
