# Android tam sayfa yakalama ve koyu sayfalar — özellik 23 / 27

Yalnız Android telefon ve mevcut tasarım. Yeni harici bağımlılık, hizmet, hesap,
üretim imzası veya yayın akışı eklenmez.

## Tam sayfa PNG

Menü işlemi Android CreateDocument ile kullanıcının seçtiği dosyaya yazar.
Gerçek GeckoView `captureFullPage()` kullanılır; görünen pencereyi yakalayıp
“tam sayfa” diye sunmaz. Seçim iptalinde işlem yapılmaz. Seçici dönüşünde aynı
aktif sekme/adres ve açık Activity gerekir; çocuk modu, kilitli özel sayfa ve
arka plan için mevcut dış yazdırma politikası uygulanır.

PNG sıkıştırma ve ContentResolver yazması worker'dadır. Yalnız kullanıcının
verdiği content URI kullanılır, uygulamada ekran görüntüsü arşivi tutulmaz.
Bitmap tamamlanınca serbest bırakılır. 32 milyon piksel üstü çıktı yazılmaz;
motor/cihaz çok uzun veya dinamik sayfayı yakalayamazsa hata gösterilir.
Bu çıktı sınırı motorun ilk Bitmap tahsisi için bellek garantisi değildir.

## Koyu sayfalar

Görünüm ayarında varsayılan kapalı, uygulama temasından ayrı bir tercihtir.
Ayrı APK-gömülü `eymen-page-tools` bileşeni yalnız HTTPS üst belgeye isteğe
bağlı ters renk/ton döndürme stili ekler. Native izin, gerçek content-script
ortamını, sahibi canlı GeckoSession'ı ve HTTPS adresini doğrular. Bileşen ağ,
dosya, gezinme kaydı veya sayfa/form verisi toplama yetkisi istemez.
Mevcut kozmetik reklam/çerez eklentisi değiştirilmez.

Motor, efekt açıkken açık renk tabanını ister; kapalıyken sistem içerik temasına
döner. Görsel/video/canvas renkleri ikinci dönüşümle korunur. Karmaşık arka
planlar, karışım efektleri ve gömülü belgeler sitelere göre farklı görünebilir;
bu durum tercih açıklamasında görünürdür. Tercih değişince etkin belge yeniden
yüklenir, diğer açık belgeler seçildiklerinde yenilenir; yeni belgeler güncel
izni alır. Gizli tercihlerde de sayfa adresi kalıcı kayda eklenmez.

## Kabul

Gerçek HTTPS test belgesinde 2200 CSS piksel içerik ve ekran dışı mavi alt alan
üretilir. Üretim dışa aktarma callback'i gerçek ContentResolver dosyasına çağrılır;
PNG decode, iki viewport'tan uzun yükseklik, alt alanın gerçek pikseli ve aynı
oturum/adres doğrulanır. `full-page-saved.png` gerçek çıktı dosyasıdır.

Gerçek Görünüm anahtarıyla koyu etki açılıp kapatılır, sayfanın computedStyle
sonucu ve motorun içerik renk tercihi okunur; `dark-page.png` yakalanır.
Bunlar test şartlarıdır; derleme/UI sonucu henüz kabul edilmedi. Fiziksel Android
8/11/14/16, dosya seçicisi, çok uzun/dinamik sayfa ve erişilebilirlik ayrıca açıktır.

## Birincil kaynaklar

- [Mozilla GeckoView full-page capture](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoView.html#captureFullPage())
- [Mozilla screenshot builder](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoDisplay.ScreenshotBuilder.html)
- [Mozilla web content color preference](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoRuntimeSettings.html#setPreferredColorScheme(int))

Sabitlenmiş Mozilla 159.0.20261022150525 sources.jar doğrudan resmi Maven
kaynağından okunup captureFullPage ve içerik renk API'leri doğrulandı. Motor, Bitmap
tahsisindeki OutOfMemoryError sonucunu GeckoResult hatası olarak döndürür.
Derleme ve gerçek emülatör çıktısı yine zorunludur.
