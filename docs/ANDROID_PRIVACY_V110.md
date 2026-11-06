# EYMEN Browser v1.0.10 — İsteğe bağlı reklam alanı temizleme

## Gerçek değişiklik

- GeckoView'un v1.0.9 **Standart/Sıkı takipçi engellemesi** aynen korunur.
- APK içinde kendi WebExtension'ımız bulunmaktadır:
  `android/app-telefon/src/main/assets/eymen-cosmetic/`.
  GeckoView'a `ensureBuiltIn()` ile yüklenir; imzalı olmayan dış modüller, harici servisler ve indirilen filtre listeleri kullanılmaz.
- Kullanıcı **Ayarlar → Koruma → Reklam alanlarını temizle (Beta)** seçeneğiyle görsel temizlik özelliğini açıkça açar. Varsayılan **kapalıdır**; modül yüklenmemişse düğme aktif değildir.
- Yalnız belirli ve yüksek güvenli reklam yer tutucularının görünümü gizlenir: `ins.adsbygoogle`, çift parametreli `data-ad-client + data-ad-slot`, `div-gpt-ad-` ve Google reklam iframe ID önekleri. Bu, **ağ isteğini durdurmaz**, sayfanın zaten yüklenmiş HTML öğelerini gizler. Reklamların tümünü kaldırmaz.
- Görsel temizleme, Android tarafındaki sayfa/host koruma tercihini izler; bir site için koruma kapatılırsa yeniden yüklenen sayfada kozmetik filtre uygulanmaz.
- WebExtension'ın isteği yalnız `{type: "cosmetic-policy-v1"}` içerir. Android uygulaması Gecko'nun sağladığı gerçek `MessageSender.session`, `isTopLevel`, `environmentType`, doğrulanmış HTTPS kaynak hostu ve kullanıcı ayarını kontrol ederek yalnız `{enabled: boolean}` yanıtlar. Add-on'a ziyaret URL'si, özel sekme tarihi, çerez, token veya kimlik aktarılmaz.
- Native mesajlaşma veya modül kurulumu başarısızsa **filtre uygulanmaz** ve çalışıyormuş gibi gösterilmez. GeckoView takipçi koruması bağımsız çalışmaya devam eder.
- Gizli sekmelerin siteleri veya tercihleri diske kaydedilmez; korunma izni o GeckoSession için belirlenir. Özel sekmelere ait içerik telemetrisi yoktur.

## Teknik sınırlar

Bu modül bir **uBlock/EasyList ikamesi değildir**. Henüz ağ seviyesinde tüm reklam alan adlarını kesen bir `webRequest` kural motoru yoktur; filtre listesi güncelleme altyapısı veya site özel kozmetik filtre ekleme arayüzü de yoktur. Video reklamlarının, sponsorluk içeriklerinin veya YouTube reklamlarının kaldırıldığı iddia edilmez. Web siteleri yanlış görünebileceği için kullanıcıya geri alma seçeneği bırakılmıştır.

## Testler

- `python tools/check_cosmetic_extension.py`: manifest, izinler, HTTPS kısıtı, native policy koşulları ve gizlilik kuralları.
- `node --check android/app-telefon/src/main/assets/eymen-cosmetic/cosmetic.js`: JS söz dizimi.
- `node tools/test_cosmetic_extension.mjs`: izin verilmedi, izin verildi, native bağlantı reddi, iframe ve erken DOM vakaları.
- Android APK derlemesi + Android 15/16 T03/T06 görüntü ve çentik uyumu denetimi, ayrı CI.
- Fiziksel telefon kabulü: Standart/Sıkı; opt-in kapalı/açık; host korumasını kapatıp yeniden yükleme; Gizli sekme; PDF, DRM, YouTube, ödeme sayfaları, indirmeler ve arka plan sekmeleri.
- VPN için bir sonraki aşama ve güvenlik kapıları: `docs/ANDROID_VPN_ARCHITECTURE_V110.md`. Bu sürümde VPN bağlantısı **bulunmamaktadır**.

## Bilinçli kararlar

Açık renkli ana sayfa, atasoyyazilim.com logosu, üst çubuk, sistem durum çubuğu/çentik uyumu ve imzalı güncelleme sistemi aynen korunur. Üretim yayını için önceki v1.0.9 APK imzası, paket adı, sürüm kodu ve SHA-256 doğrulaması başarısızsa yeni APK yayımlanmaz.
