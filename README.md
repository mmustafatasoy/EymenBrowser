# EymenBrowser

Bu depo **docs/SARTNAME.md** şartnamesiyle ve **docs/DURUM.md** ilerleme kaydıyla yürütülür.

Aşama 0: Boş Android/TV Compose, SwiftUI (iOS) ve React + castLabs Electron (masaüstü) iskeletleri,
`design/tokens.json` kaynaklı tema/simge üretimi, `shared/i18n` çevirileri ve GitHub Actions denetimleri.

> **2026-11-11 güncel durum:** S-001/S-002/S-003/S-004/S-005 kapsamında yalnız Android telefon geliştiriliyor. Son kararlı imzalı sürüm **[v1.0.23](https://github.com/mmustafatasoy/EymenBrowser/releases/tag/v1.0.23)**. Daha yeni uyumlu sürüm bulunamadığında gösterilen TR/EN güncelleme mesajı düzeltildi. Proton VPN Free hesabıyla tek seferlik kurulum rehberi ve indirilen WireGuard dosyasını açık onayla şifreli profile aktarma mevcuttur; ücretsiz hesabı kullanıcı resmî sağlayıcı sitesinde açar. Üç ABI orijinal üretim imzasıyla yayımlandı; imza/hizalama/boyut/SHA-256 ve v1.0.22 ile güncelleme metadata uyumu doğrulandı. Gerçek üretim **v1.0.22 → v1.0.23** uygulama içi indirme/Android izin ve kurulum/Aç onayı, ayar/yer imi koruması ve kurulu APK hash eşitliği Android 15/16 emülatörlerinde geçti. Bu sonuç fiziksel cihaz kabulü değildir. VPN saha denemesi kullanıcının isteğiyle sonraya bırakıldı. Fiziksel cihaz ve uygulama içi güncelleme kabulü için **[güncel durum kaydını](docs/DURUM.md)** esas al. TV, bilgisayar ve Apple çalışmaları beklemededir.

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
