# Android yer imi kütüphanesi ve yerel adres önerileri

Şartname özellik 12 ve 9, yalnız Android telefon. Mevcut tasarım tokenları,
ortak liste bileşenleri ve TR/EN metin kaynağı kullanılır; diğer platformların
üretilen çevirileri dışında platform işi açılmaz.

- Yer imlerinin başlık, tam HTTPS adresi ve isteğe bağlı klasörü uygulama
  içi SQLite'ta tutulur. Eski üretim `eymen_browser_bookmarks/urls` kümesi
  bir kez transaction içinde göç eder; URL uyumluluk aynası korunur.
- Başlık geçmişe bağlı değildir. Klasör oluşturma/yeniden adlandırma,
  yer imi düzenleme/taşıma/silme ve klasör içinde arama gerçek kayıtları değiştirir.
- Klasör silme içindeki yer imlerini silmez; yabancı anahtar `SET NULL`
  ile onları köke taşır. Başarısız transaction mevcut kayıtları korur.
- Kullanıcı bilgisi içeren veya HTTPS olmayan adres kaydedilmez; adlar
  Unicode normalleştirmesiyle 120, adresler 16384 birimle sınırlıdır.
  Yeni eklemeler 2000 kayıt/100 klasör sınırına uyar. Eski göçte kayıt sayısı
  nedeniyle silme/kırpma yapılmaz.
- Adres önerileri yalnız yerel yer imi ve normal geçmişten gelir. İki
  harften sonra en fazla beş tekil sonuç gösterilir. Gizli girişte normal
  kitaplık önerisi yoktur; öneri için ağ servisi veya tuş kaydı kullanılmaz.
- Gizli sayfalar otomatik yer imi olmaz. Kullanıcının açık kaydetme eylemi
  mevcut tarayıcı davranışını sürdürür; normal geçmişe gizli ziyaret eklenmez.

## Kabul

Altı JVM testi güvenli adres, ad/başlık normalleştirme, yerel eşleşme/sınır
ve gizli öneri dışlamasını denetler. Mevcut tam UI testine gerçek üretim
anahtarından göç, gerçek sayfayı kaydetme, klasör oluştur/yeniden adlandır/taşı,
klasör araması, güvensiz düzenlemeyi reddetme, atomik başlık/adres düzenleme,
klasörü silince kayıt koruma, gerçek SQLite satırı ve öneriden gezinme eklenir.
`bookmarks-folder.png` ve `bookmarks-all.png` zorunludur; eski 26 PNG korunur.
CI ve fiziksel cihaz sonuçları henüz denenmedi; bu belge kabul verilmiş saymaz.
Son yayımlanmış sürüm v1.0.23; yenilik henüz yayımlanmadı.
