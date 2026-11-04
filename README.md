# EymenBrowser

Bu depo **docs/SARTNAME.md** şartnamesiyle ve **docs/DURUM.md** ilerleme kaydıyla yürütülür.

Aşama 0: Boş Android/TV Compose, SwiftUI (iOS) ve React + castLabs Electron (masaüstü) iskeletleri,
`design/tokens.json` kaynaklı tema/simge üretimi, `shared/i18n` çevirileri ve GitHub Actions denetimleri.

> Aşama 1 motor testleri kabul edilmeden tasarım ekranları veya bitmiş tarayıcı üretildiği iddia edilmez.

## Klasörler

- `design/`: Onaylı, değiştirilmemiş 16 ekran + marka görseli ve HTML kaynakları.
- `docs/`: Şartname, durum, sapma ve karar kayıtları.
- `android/`: Ayrı telefon ve TV Compose uygulamaları; ortak tema modülü.
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
