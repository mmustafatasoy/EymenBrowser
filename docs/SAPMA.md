# Sapma kayıtları

Burada kayıt yoksa şartnameden sapma yoktur. Yazılım aracı onay almadan sapma yapamaz.

## S-001 — Yalnız Android arayüzüne geçiş

- Tarih: 2026-11-04.
- Şartnamedeki ilgili bölüm: Aşama 1 / Kapı 1; "Kapı 1 geçilmeden ekran kodu yazılmaz." Kabul listesi Android telefon, TV ve Electron motor testlerini birlikte kapsar.
- İstenen: Kullanıcının yalnız Android telefonla devam etme kararı korunarak T01–T11 Android ekranlarını uygulamaya başlamak. [Somut uygulama planı](ANDROID_ASAMA2_PLANI.md) hazırdır.
- Neden olmuyor: Mevcut telefonda HTTPS, Google hesap girişi, YouTube/tam ekran/dönüş, Sintel Widevine, sosyal medya ve PDF indirme kontrolleri başarılı bildirildi. Diğer cihaz/platform testleri tamamlanmadı; TV, bilgisayar ve Apple kullanıcı tarafından beklemeye alındı. Tüm platformları kapsayan Kapı 1 tamamlanmış sayılamaz.
- Seçenek 1: Kullanıcı mevcut Android telefon sonuçlarını başlangıç için kabul eder ve yalnız Android Aşama 2 uygulamasına geçişi onaylar. Diğer platformlar bekler; denenmeyen cihaz/site/performans kontrolleri açık kalır. Genel Kapı 1 veya nihai ürün kabulü geçmiş sayılmaz.
- Seçenek 2: Mevcut aşama korunur; ekran kodlaması tüm platformları kapsayan Kapı 1'in tamamlanmasını bekler. Diğer platformlar kullanıcı yeniden başlatana kadar beklemede kalır.
- Karar: **Seçenek 1 ONAYLANDI — 2026-11-04.** Kullanıcı önceki sohbette açıkça "Onay verildi" diyerek yalnız Android telefon için Aşama 2 arayüz uygulamasına erken geçişi kabul etti. Android TV, bilgisayar ve Apple beklemede kalır. Genel Kapı 1 geçilmiş sayılmaz. Şartname, tasarım kaynakları ve çalışan motor özellikleri korunur.

## Şablon

- Tarih:
- Şartnamedeki ilgili bölüm:
- İstenen:
- Neden olmuyor:
- Seçenek 1:
- Seçenek 2:
- Karar:

## S-002 — Chrome benzeri ayarlar, geçmiş ve kaydırılan araç çubuğu

- Tarih: 2026-11-07.
- Karar: **Kullanıcı doğrudan ONAYLADI.** Dört telefon ekran görüntüsüyle ayarları ve üç nokta menüsünü sade liste olarak istedi; Geçmişte gerçek ziyaret bağlantıları, aşağı kaydırınca animasyonla gizlenen ve küçük yukarı harekette dönen adres/menü çubuğu istedi.
- Yeni referans T07/T10 için önceki kart/ızgara ve 148 dp küçük menü kararının yerini alır. Ana sayfa/A logosu korunur; mevcut açık/koyu tema seçimi sürer. Google hesabı veya hizmeti varmış gibi gösterilmez. Yeni nötr liste renkleri/ölçüleri ortak tokens.json içindedir.
- Yalnız Android telefon; TV/PC/Apple kapsamı açılmadı. Telemetri veya harici geçmiş hizmeti yoktur. Gizli ve çocuk gezinmesi geçmişe yazılmaz.

## S-003 — Cihaz geri bildirimi: adres girişi ve Geri/İleri

- Tarih: 2026-11-07. Kullanıcı doğrudan düzeltme istedi; arama alanı ekran görüntüsü ile önceki siteye geri dönemediğini bildirdi. Banka ve ekran uyumunu onayladı.
- Mevcut S-002 arayüzünün adres girişini okunur ortak ölçüye getirme, native Ara/Git eylemi ve standart gerçek tarayıcı geri-ileri düzeltmesidir. Yeni hizmet, hesap, tema, telemetri veya platform eklenmez. Orijinal üretim imzası korunur; tek resmi APK güncellemesi hazırlanır.

## S-004 — Paylaşım, Hakkında, lisanslar ve uyarıların ürün düzeni

- Tarih: 2026-11-07. **Kullanıcı doğrudan ONAYLADI:** Chrome benzeri paylaş menüsü, biçimli uyarılar, gerçek açık kaynak lisansları, Hakkında/geri bildirimde yayıncı sitesi ve uygulamada GitHub tanıtımı olmaması istendi.
- Yalnız Android telefonun mevcut paylaş/Hakkında akışları düzenlenir. Çevrimdışı QR oluşturma mevcut ZXing bağımlılığını kullanır; yeni motor, kitaplık, hesap, telemetri veya platform eklenmez. Chrome/Chromium tasarım referansıdır; Chromium motoru kullanıldığı veya Google ile bağlantı olduğu iddia edilmez.
- E-posta yalnız kullanıcının düğmesiyle düzenlenebilir taslak olarak açılır; tarama geçmişi, site adresi, hata günlüğü ve VPN yapılandırması otomatik eklenmez. Destek/iletişim/gizlilik sayfaları ve destek@atasoyyazilim.com adresi yayıncının çalışan sitesinden doğrulandı.
- Native izin/PIN/onay kararları ve orijinal üretim imzası korunur. Gerçek kullanılan dağıtımların lisans metinleri yerel dosyalardan okunur; zorunlu upstream kaynak/telif bildirimleri saklanır. Bu çalışma mağaza onayı veya tüm özelliklerin tamamlandığı anlamına gelmez.

## S-005 — Ücretsiz VPN sağlayıcısıyla ilk kurulum

- Tarih: 2026-11-10. **Kullanıcı doğrudan istedi:** VPN hesabı/VPS olmadığını belirterek “ikiside yok ücretsiz bir şey yap” dedi. Yalnız Android telefon VPN akışı ilerler.
- Proton VPN Free hesabından kullanıcının kendi WireGuard dosyasını elde etmesi için uygulama içi kurulum rehberi ve resmî hesap sayfasına kullanıcının dokunmasıyla açılan bağlantı eklendi. Ücretsiz hesap açma işlemini kullanıcı sağlayıcı sitesinde yapar. Otomatik kayıt, ortak hesap, APK içine sunucu/özel anahtar veya belgelenmemiş sağlayıcı API'si eklenmez.
- İndirilen .conf dosyası listeden açıldığında açık bir içe aktarma onayı istenir. Mevcut tek peer/çift yığın/DNS, sadece uygulama paketi, Android VPN izni ve Keystore şifreleme kuralları korunur. İçe aktarma VPN'i kendiliğinden başlatmaz; çocuk modu dosya açma kısıtı korunur.
- Tasarım kaynakları, motor, bağımlılıklar, yayın akışı ve üretim imzası değişmez. Fiziksel sunucu bağlantısı ve sızıntı kabulü, hesap kurulmadan tamamlanmış sayılmaz. Uygulamanın Proton tarafından üretildiği veya ortaklık olduğu iddia edilmez.
