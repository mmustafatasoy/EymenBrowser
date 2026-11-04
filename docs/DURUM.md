# Proje durumu

Son güncelleme: 2026-11-04

- Aşama: **0 — hazırlık, devam ediyor**.
- Biten iş: 41 dosyalık başlangıç paketi orijinal biçimde aktarıldı. Telefon/TV için Kotlin/Compose, iOS için SwiftUI, masaüstü için TypeScript/React/castLabs Electron boş proje iskeletleri eklendi. Sürüm kilidi, ortak i18n, tema ve simge üretici, boyut ve yasak bağımlılık kontrolleri, GitHub Actions iş akışları hazırlandı.
- Sıradaki iş: GitHub Actions'ta Android, TV, iOS ve bilgisayar boş uygulama derlemelerinin sonuçlarını doğrula; hataları şartnameye uygun şekilde gider.
- Açık sorun: Token/çeviri üretimi ve bağımlılık denetimi GitHub Actions'ta **geçti**. İlk Windows derlemesinde Windows terminali UTF-8 hatası ve Linux derlemesinde castLabs Electron indirme adresi hatası gözlendi; CI ayarı düzeltildi. Android ve iOS sonuçları bekleniyor. İmzalı üretim sürümleri bu aşamanın kapsamı dışındadır.
- Aşama 0 kabul:
  - [ ] Boş Android, TV, iOS ve Electron uygulamaları GitHub Actions'ta derlendi.
  - [ ] design/tokens.json değişince Android, iOS ve bilgisayar temaları otomatik üretildi (CI henüz denenmedi).
  - [ ] Yasak bağımlılık denetimi ve sürüm kontrolü çalıştı; gerçek kurulum dosyalarında 500 MB ölçümü **henüz denenmedi**.
  - [x] DURUM.md ilk kayıtla duruyor.
- Aşama 1 motor denemesine ve 16 ekranın kodlanmasına **başlanmadı**.
- Değişmeyen dosya: `docs/SARTNAME.md`.
