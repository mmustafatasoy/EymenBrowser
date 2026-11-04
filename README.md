# EymenBrowser

Bu depo **docs/SARTNAME.md** şartnamesiyle ve **docs/DURUM.md** ilerleme kaydıyla yürütülür.

Aşama 0: Boş Android/TV Compose, SwiftUI (iOS) ve React + castLabs Electron (masaüstü) iskeletleri,
`design/tokens.json` kaynaklı tema/simge üretimi, `shared/i18n` çevirileri ve GitHub Actions denetimleri.

> **2026-11-04 güncel durum:** Kullanıcının S-001 kapsam onayıyla yalnız Android telefonda Aşama 2 geliştiriliyor. T01–T04, **T05 gerçek GeckoView izleyici engelleme**, **T06 çoklu sekme**, **T07 menü**, **T08 gerçek kaydedilen dosyalar**, **T09 gizli sekme**, **T10 ayarlar** ve **T11 tema** için çalışan başlangıç kodları eklendi. Üç ABI debug APK için derleme/statik kontroller [başarılı](https://github.com/mmustafatasoy/EymenBrowser/actions/runs/41850087552). Emülatör PNG fark denetimi başlatıldı; fiziki cihaz kabulü ve tüm ekranların onaylı PNG ile birebir eşleşmesi henüz doğrulanmadı. **VPN tüneli, Kalkan'ın gelişmiş paketleri ve üretim imzalı kararlı APK hazır değil.** Genel Kapı 1 açık; TV, bilgisayar ve iOS duraklatıldı.

## Klasörler

- `design/`: Onaylı, değiştirilmemiş 16 ekran + marka görseli ve HTML kaynakları.
- `docs/`: Şartname, durum, sapma ve karar kayıtları.
- `android/`: Ayrı telefon ve TV Compose uygulamaları; yalnız telefon arayüzünde T01–T11'in mevcut aşama kodları aktif.
- `ios/`: SwiftUI tabanlı iOS 15 ve iPadOS iskeleti.
- `desktop/`: TypeScript, React ve castLabs Electron iskeleti.
- `shared/`: Metinler, veri sözleşmeleri, filtre ve simge kaynakları.
- `tools/`: Tema üretici, 500 MB tavanı, regresyon ve onaylı PNG/gerçek ekran fark analizi.
- `versions.toml`: Sürüm ve araç kilitleri.
- `.github/workflows/phase0.yml`: Android telefon aşama 2 derleme ve denetimleri. Görsel emülatör iş akışı ayrıca `android-visual-audit.yml` altında.

## Yerel komutlar

```bash
python3 tools/generate.py
python3 tools/generate.py --check
python3 tools/verify.py
cd android && gradle :app-telefon:assembleDebug :app-tv:assembleDebug
cd desktop && npm install && npm run build
```

iOS projesi `ios/project.yml` üzerinden XcodeGen ile üretilir. Apple imzası olmadan yalnız
simülatör için derleme yapılabilir. Başarılı debug derlemesi **kararlı sürüm** değildir.
