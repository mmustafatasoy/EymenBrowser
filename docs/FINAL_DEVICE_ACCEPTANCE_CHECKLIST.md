# EYMEN Browser — son fiziksel cihaz kabul kontrol listesi

**Durum (v1.0.15, 2026-11-06):** İmzalı Android APK ve kaynak/JVM/derleme kontrolleri GitHub CI'da yürütülmüştür. Aşağıdaki `[ ]` maddeleri **henüz fiziksel cihazda doğrulanmamış** kullanıcı kabul senaryolarıdır. İşaretlenmediği sürece "başarılı" sayılmaz.

## 1. Kurulum, güncelleme, arayüz

- [ ] v1.0.14'ten v1.0.15'e uygulamayı silmeden geçiş; imza/paket, normal sekmeler ve tercihler korunuyor mu?
- [ ] Android 16 TECNO cihazda ön kamera/durum çubuğunun altındaki üst araç çubuğu; yatay/dikey dönüş, klavye ve tam ekran davranışı.
- [ ] Ana sayfa, arama, adres girişi, sesli arama, geri/ileri, dar üç nokta menüsü, açık/koyu görünüm.
- [ ] Android 8–15 cihazlarında yükleme ve Android 15/16 emülatör sonucu ile karşılaştırma.

## 2. Sekmeler, gizlilik ve oturum geri yükleme

- [ ] Normal sekme aç/kapat, sekme grubu oluştur/taşı/yeniden adlandır, sıralama ve son 10 normal sekmeyi geri alma.
- [ ] Uygulama yeniden açılınca normal sekme adresleri/gruplar/sıra korunuyor, arka plan sekmeleri ihtiyaç halinde mi yükleniyor?
- [ ] Gizli sekmenin URL'si, görüntüsü ve istisnası uygulama kapatılınca kalıcı geçmişe taşınmıyor mu?
- [ ] Gerçek Gecko sekme önizlemeleri ve bellek/performans kontrolü.

## 3. Bankacılık / kimlik doğrulama (açık sorun)

- [ ] Bankanın **resmî HTTPS** web giriş sayfası, doğrulama pencereleri (`window.open`, `target=_blank`), `alert/confirm/prompt` işlemleri.
- [ ] Bankanın desteklediği resmî Android uygulama yönlendirmesi (yalnız kullanıcı onayıyla); uygulamanın gerçekten bankaya ait olduğu kullanıcı tarafından denetlenir.
- [ ] `⋮ → Başka tarayıcıda aç` seçeneğinde HTTPS bağlantısı Android'in uygulama seçicisiyle doğru tarayıcıda açılıyor mu?
- [ ] Girişe engel olabilecek VPN/Standart-Sıkı takipçi engelleme/kozmetik filtre farkları ayrı ayrı test ediliyor mu?
- [ ] Banka adı, **gizlenmiş** hata mesajı, akışın durduğu adım not edilir. **Şifre, SMS kodu, kart ve hesap bilgisi paylaşılmaz.**
- [ ] Bankanın resmî Android uygulamasının cihaz kaydı, biyometri, Play Integrity ve imza kontrolleri **asla aşılmaya çalışılmaz**.

## 4. İndirilenler / PDF (v1.0.15)

- [ ] PDF, küçük resim ve büyük dosya indirme; gerçek yazılan bayt sayısı ilerliyor mu?
- [ ] Sunucu Content-Length bildirirse yüzde; bildirmezse **boyut bilinmiyor**, sahte yüzde yok.
- [ ] Aktarım ortasında **İptal**: yarım MediaStore dosyası silinmiş, dosya kayıt geçmişine eklenmemiş mi?
- [ ] Android 8/9 SAF dosya seçimi ve sağlayıcı eksik dosyayı silemezse uyarı.
- [ ] Tamamlanan dosya İndirilenler ekranı açıkken anında görünüyor, açılıyor ve geçmiş temizleme gerçek dosyayı silmiyor mu?
- [ ] Uygulama işlemi kapanınca indirme devam etmiyor; arka plan kuyruğu **henüz geliştirilmedi**.

## 5. Reklam/takipçi koruma

- [ ] Standart/Sıkı GeckoView engelleme listeleri, gerçek reklam/analiz/diğer sayaçları.
- [ ] Siteye özel kapatma/açma, özel sekmede geçici istisnalar, gizlilik ve oturum davranışı.
- [ ] İsteğe bağlı `Reklam alanlarını temizle (Beta)` filtresi ve yanlış gizlenen öğeleri geri alma.
- [ ] YouTube, DRM, ödeme ve giriş sayfalarında kullanıcı deneyimi; tüm reklamların engellendiği iddia edilmez.

## 6. WireGuard VPN (açık güvenlik kabulü)

- [ ] Gerçek, kullanıcıya ait güvenilir WireGuard sunucusunun çift-yığınlı `.conf` dosyasının Android dosya seçicisinden içe aktarılması (özel anahtar sohbete/GitHub'a gönderilmez).
- [ ] Android'in gerçek VPN izin penceresi reddedildiğinde tünel başlamıyor mu?
- [ ] Yerel TUN açıkken **gerçek sunucu el sıkışması** ayrı gösteriliyor; bağlantı kopunca 15 saniyelik güncellemede yanlış "bağlı" görünmüyor mu?
- [ ] WireGuard gelen/giden veri sayaçları, iptal, tüneli kapatma ve şifreli profili silme.
- [ ] Wi‑Fi ↔ mobil ağ, uçak modu, uyku/uyanma, Android işlem kapatma ve sistem VPN iptali.
- [ ] **IPv4, IPv6, DNS, WebRTC sızıntısı** ve sadece tarayıcı paketinin VPN'e alınması ağ üzerinden doğrulandı mı?
- [ ] **Kill-switch / Always-on otomatik geri bağlanma henüz yoktur.** VPN kopunca doğrudan trafik çıkabilir; bu testler geçilmeden "sızıntısız" denmez.

## 7. Ses, video ve diğer

- [ ] YouTube, video tam ekran, ses, ileri/geri, DRM ve uygulama ön/arka plan geçişi.
- [ ] Birkaç sekmede video, indirme ve bağlantı değişimi birlikte performans/çökme denemesi.
- [ ] Telefon ve TV/Android Auto projelerinin ayrı olduğu, bu Android telefon APK'sının onların işlevlerini otomatik içermediği doğrulanır.

## Kayıt yöntemi

Her başarısızlık için **özellik adı + Android sürümü + yapılan işlem + beklenen/gözlenen sonuç + ekran görüntüsü (kişisel bilgiler kapatılmış)** yazılır. Hata düzeltilir, GitHub kodu derlenir ve yeni imzalı sürüm yayımlanmadan önce ilgili madde tekrar sınanır.

**Kabul ilkesi:** GitHub derlemesi, emülatör ekran görüntüsü veya kaynak testlerinin başarısı, gerçek cihazda bu listenin otomatik olarak işaretlendiği anlamına gelmez.
