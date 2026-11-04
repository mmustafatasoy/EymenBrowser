# Uygulama kararları

- 2026-11-04: Aşama 0 şartnameye göre başlatıldı. Kullanıcı tarafından sağlanan 16 ekran HTML/PNG'si değiştirilmez.
- Üç ayrı kod tabanı: `android/`, `ios/`, `desktop/`. Ortak dosyalar yalnız tasarım tokenları, metinler, sözleşmeler, filtreler ve ikonlardır.
- Sürüm kilitleri `versions.toml` içindedir. Android 8+, iOS 15+, 64 bit masaüstü.
- Aşama 0 boş derlemeleri kabul edilmeden tarayıcı ekranı veya motor kodu yazılmayacak.
- Debug imzalı uygulama sadece CI test çıktısıdır; Releases veya kararlı sürüm olarak sunulmaz.
