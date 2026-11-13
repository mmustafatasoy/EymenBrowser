# Android yer imi aktarımı ve okuma listesi

Şartname özellik 13 ve 15, yalnız Android telefon. Ücretli hizmet, yeni
bağımlılık veya tasarım kaynağı eklenmez. Son yayımlanmış sürüm v1.0.23.

## Yer imi dosyaları

Kullanıcı Android'in dosya seçicisiyle kaynak/hedef seçer; iptal işlem yapmaz.
JSON yedeği `eymen-bookmarks` / sürüm 1 biçiminde başlık, HTTPS adresi ve
klasörleri korur. Yeni kayıtlar eklenir, mevcut URL'ler korunur; yerel başlık
ve klasör üzerine yazılmaz. Hata veya kapasite aşımı transaction'ı geri alır.

Diğer tarayıcılar için Netscape yer imi HTML dışa aktarımı klasör başlıklarıyla
üretilir. HTML içe aktarımı Android'in metin ayrıştırıcısıyla yapılır; içerik
çalıştırılmaz. Geçerli HTTPS bağlantıları klasörsüz köke alınır, desteklenmeyen
adresler atlanır; bu fark içe aktarma ekranında açıkça yazılır. Eklenen ve
atlanan/var olan sayıları gerçek işlem sonucudur.

Dosya en fazla 8 MiB, sıkı UTF-8, 2000 kayıt/100 klasördür. Geçersiz sürüm,
klasör referansı veya JSON adresi dosyanın tamamını reddeder. Dışa aktarım da
aynı boyut sınırına uyar; uyumsuz büyük bir yedek başarı gibi sunulmaz.
Dosya sağlayıcısının yazma hatası durumunda hedef dosyanın atomik değişimi
sağlayıcıya bağlıdır; uygulamanın SQLite kayıtları değişmez.

## Okuma listesi

Kullanıcı mevcut sayfayı menüden kaydeder. Başlık/adres, okundu durumu ve
silme yer imi kayıtlarından ayrı tutulur. Tümü/Okunmadı/Okundu filtresi ve
başlık/adres araması birlikte çalışır. Kaydı açmak gerçek adresine gider ve
okundu işaretler. Liste bir bağlantı listesidir; çevrimdışı makale indirdiği
veya cihazlar arası eşitlediği iddia edilmez.

SQLite şeması 2'dir; önceki şema 1'den yükseltme yalnız okuma tablosu ekler,
yer imi ve klasörleri silmez. Önceki URL kümesi göçü korunur.

## Kabul

Üç JVM testi UTF-8/BOM, bozuk UTF-8 ve boyut sınırında akışın kapatılmasını
sınar. Tam UI gerçek şema-1 dosyasından yükseltme, gerçek ContentResolver
JSON/HTML dosyaları, klasör/başlık içe ve dışa aktarımı, tekrar içe aktarmada
tekilleştirme, geçersiz sürümde kayıt koruma, güvensiz HTML bağlantısını atlama,
okuma listesi durumu/filtre/arama/açma/silmeyi içerir. `reading-list.png`
zorunludur; önceki kabuller korunur. Dosya seçici etkileşimi ayrıca fiziksel
cihazda denenmelidir; CI dosya I/O'su gerçek sağlayıcı üzerinden yapılır.
Bu adayın derleme/UI sonuçları henüz denenmedi; belge kabul sayılmaz.
