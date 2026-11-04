# Android Aşama 2 uygulama planı

Durum: 2026-11-04 — [S-001 kapsam kararı](SAPMA.md#s-001--yalnız-android-arayüzüne-geçiş) onaylandı; yalnız Android telefon Aşama 2'ye geçildi. Bu belge tek başına aşama kabulü değildir.

## Başlangıç kanıtı

Mevcut Android telefon APK'sında HTTPS sayfaları, Google hesabıyla giriş ve aynı oturumda bağlantıyı yeniden açma, YouTube görüntü/ses ve tam ekran/yatay dönüş, Sintel Widevine görüntü/ses, sosyal medya ve örnek PDF görüntüleme/indirme/açma kullanıcı tarafından başarılı bildirildi. e-Devlet ve iki banka için herkese açık ana sayfalar açıldı. Cihaz/sürüm kapsamı, kişisel banka işlemleri ve diğer platformlar bu sonuçlara dahil değildir.

## Uygulama kaynakları

11 ekranın PNG ve HTML dosyaları mevcut; her PNG 780 × 1688 piksel. Görseller hedef düzeni, HTML yerleşim ayrıntılarını, `design/tokens.json` renk/ölçü/ikonları, `shared/i18n` arayüz metinlerini sağlar. Şartname çelişki durumunda önceliklidir.

| Ekran | PNG | HTML |
| --- | --- | --- |
| T01 Açılış | [Görsel](../design/ekranlar/T01-Acilis.png) | [Kaynak](../design/kaynak/T01-Acilis.html) |
| T02 İlk kurulum | [Görsel](../design/ekranlar/T02-Ilk-kurulum.png) | [Kaynak](../design/kaynak/T02-Ilk-kurulum.html) |
| T03 Yeni sekme | [Görsel](../design/ekranlar/T03-Yeni-sekme.png) | [Kaynak](../design/kaynak/T03-Yeni-sekme.html) |
| T04 Sayfa | [Görsel](../design/ekranlar/T04-Sayfa.png) | [Kaynak](../design/kaynak/T04-Sayfa.html) |
| T05 Kalkan | [Görsel](../design/ekranlar/T05-Kalkan.png) | [Kaynak](../design/kaynak/T05-Kalkan.html) |
| T06 Sekmeler | [Görsel](../design/ekranlar/T06-Sekmeler.png) | [Kaynak](../design/kaynak/T06-Sekmeler.html) |
| T07 Menü | [Görsel](../design/ekranlar/T07-Menu.png) | [Kaynak](../design/kaynak/T07-Menu.html) |
| T08 İndirmeler | [Görsel](../design/ekranlar/T08-Indirmeler.png) | [Kaynak](../design/kaynak/T08-Indirmeler.html) |
| T09 Gizli sekme | [Görsel](../design/ekranlar/T09-Gizli.png) | [Kaynak](../design/kaynak/T09-Gizli.html) |
| T10 Ayarlar | [Görsel](../design/ekranlar/T10-Ayarlar.png) | [Kaynak](../design/kaynak/T10-Ayarlar.html) |
| T11 Gündüz teması | [Görsel](../design/ekranlar/T11-Gunduz.png) | [Kaynak](../design/kaynak/T11-Gunduz.html) |

## Uygulama sırası

1. Ortak tokenlardan Compose teması, onaylı Inter/Sora yazı tipleri, ikonlar, Türkçe/İngilizce metinler ve sistem güvenli alanlarını kur. Yeni bağımlılık gerekiyorsa sürümünü ayrı commit'te `versions.toml` içine sabitle.
2. T01 ve T02: onaylı açılış hareketi, imza/kanal adı ve ilk kurulum tercihleri. Kullanıcının hareketi azaltma ayarına uy.
3. T03, T04 ve T11: yeni sekme, adres/arama, gezinme çubukları, gerçek GeckoView sayfası ve gündüz teması. Çalışan tam ekran, dönüş, DRM ve indirme akışlarını koru.
4. T06 ve T09: gerçek sekme açma/kapatma, gruplar ve gizli oturum ayrımı. T07 menüsünü gerçek sekme ve sayfa işlemlerine bağla.
5. T08 ve T10: gerçek dosya durumlarına bağlı indirmeler ve şartnamedeki ayar düzeni. Yer imleri, geçmiş, paylaşım, arama motoru ve Çekirdek paketinin diğer Android özelliklerini tamamla.
6. T05 yerleşimini onaylı kaynağa göre hazırla; Kalkan paketinin gerçek filtreleme, izin ve sayaç işlevlerinin kabulü Aşama 3'e aittir. Çalışmayan koruma veya temsili sayaç üretim işlevi gibi sunulmaz.

## Kabul kontrolleri

- Her ekran aynı ölçekte kendi PNG/HTML kaynağıyla yan yana karşılaştırılır; mevcut görseller yeniden tasarlanmaz.
- Çekirdek paketinde Android için Var/Kısıtlı yazan özellikler şartnameye göre tek tek doğrulanır.
- Android 8, 11, 14 ve 16'da açılış, sekme ve indirme denemeleri kaydedilir. Çalıştırılamayan cihaz testi "denenmedi" kalır.
- Soğuk açılış 2,5 saniye, yeni sekme 150 ms ve kurulum dosyası 500 MB sınırları ölçülür; derlenmesi tek başına kabul değildir.
- Ekran okuyucu, büyük yazı, açık/koyu tema ve sistem güvenli alanları kontrol edilir.
- TV, bilgisayar ve Apple için yeni kodlama/derleme/test başlatılmaz. Aşama 2 tamamlandı veya genel Kapı 1 geçti denmesi ayrı kanıt ve kabul gerektirir.
