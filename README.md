# EymenBrowser

Bu depo **docs/SARTNAME.md** şartnamesiyle ve **docs/DURUM.md** ilerleme kaydıyla yürütülür.

Aşama 0: Boş Android/TV Compose, SwiftUI (iOS) ve React + castLabs Electron (masaüstü) iskeletleri,
`design/tokens.json` kaynaklı tema/simge üretimi, `shared/i18n` çevirileri ve GitHub Actions denetimleri.

> **2026-11-04 güncel durum:** Kullanıcının S-001 kapsam onayıyla yalnız Android telefonda Aşama 2 geliştiriliyor. T01/T02 ilk kurulum, T03 yeni sekme, T04 GeckoView web sayfası ve T11 gündüz teması için kod eklendi; cihaz üstünde görsel karşılaştırma, gerçek çoklu sekme/Kalkan/VPN ve üretim imzalı kararlı APK **henüz tamamlanmadı**. Genel Kapı 1 açıktır. TV, bilgisayar ve iOS duraklatıldı.

## Klasörler

- `design/`: Onaylı, değiştirilmemiş 16 ekran + marka görseli ve HTML kaynakları.
- `docs/`: Şartname, durum, sapma ve karar kayıtları.
- `android/`: Ayrı telefon ve TV Compose uygulamaları; yalnız telefon arayüzünde T01–T04/T11 kodu aktif.
- `ios/`: SwiftUI tabanlı iOS 15 ve iPadOS iskeleti.
- `desktop/`: TypeScript, React ve castLabs Electron iskeleti.
- `shared/`: Metinler, veri sözleşmeleri, filtre ve simge kaynakları.
- `tools/`: Tema üretici, 500 MB tavanı ve yasak bağımlılık denetimi.
- `versions.toml`: Sürüm ve araç kilitleri.
- `.github/workflows/phase0.yml`: Aşama 0 derleme ve denetimleri.

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
