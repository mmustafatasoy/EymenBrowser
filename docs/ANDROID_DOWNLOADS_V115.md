# EYMEN Browser v1.0.15 — gerçek indirme ilerlemesi ve iptal

## Neler değişti?

- **İndirilenler** ekranı, GeckoView'un kendi `WebResponse.body` veri akışından diske **gerçekten yazılan baytları** artık canlı gösterir.
- Kaynak gerçek bir pozitif `Content-Length` bildiriyorsa, bu değer kullanılarak `0–100%` ilerleme gösterilir. Bilgi yoksa **Toplam boyut bilinmiyor** yazılır; uydurma bir yüzde, indirme süresi veya hız tahmini gösterilmez. `Content-Length` bir sunucu ipucudur; nihai dosya boyutu ondan farklı olabilir.
- **İndirmeyi iptal et** düğmesi var. Okuma döngüsü iptali denetler, GeckoView gövde akışı UI'ı kilitlemeden ayrı iş parçacığında kapatılabilir. Bir iptal veya hata sonrası yeni/yarım `MediaStore.Downloads` öğesi silinir; kesin olarak tamamlanmış gibi gösterilmez.
- Android 8–9 için `ACTION_CREATE_DOCUMENT` üzerinden kullanıcı seçtiği dosyanın iptal sonrasında silinmesi **en iyi çaba** ile denenir. Dosya sağlayıcısı silme yetkisi vermiyorsa eksik dosya kalabileceği kullanıcıya açıkça bildirilir.
- **Tamamlanan dosya anında listelenir.** Önceki sürümde İndirilenler ekranı açıksa liste yalnız ekranı tekrar açınca yenileniyordu. Sadece tam olarak başarıyla kaydedilen dosyalar `DownloadRegistry`ye yazılır.
- Önceden gelen PDF indirmeleri, izin/onay ekranı, Android 10+ `MediaStore.IS_PENDING`, Android 8–9 `Storage Access Framework`, dosya açma ve geçmiş temizleme işlevleri korunur.
- İndirme motoru **bir seferde tek transfer** kabul eder; istekleri paralel veya sınırsız kuyrukta tutmaz. İptal sırasında tekrar dosya seçimi başlaması engellenir.

## Güvenlik ve kapsam sınırı

- Dosyaları yeniden çekmek için ayrı HTTP istemcisi kullanılmaz: akış mevcut GeckoView yanıtından alınır. Çerez, bankacılık oturumu veya kimlik doğrulama bilgisi başka indirme servislerine aktarılmaz.
- Dosya indirmesi çalışırken uygulama işlemi Android tarafından öldürülürse indirme **devam etmez**; indirme yöneticisi WorkManager/foreground service tabanlı kalıcı kuyruk değildir. APK boyutu ve farklı Android sürümlerinde gerçek cihazla ayrıca test yapılmalıdır.
- İptal için ağ gövdesinin `close()` yanıt vermesi gerekebilir; bu işlem için UI iplikçiği bloklanmaz. Bir Android içerik sağlayıcısının eksik dosyayı silmesi her koşulda garanti edilmez.
- Bu sürüm banka girişini, VPN kill-switch davranışını veya web reklam engellemenin kapsamını değiştirmez; bankacılık sorunları son kabul turundadır.

## Otomatik test ve kabul

1. `python tools/check_download_handling.py` ve `python tools/check_stage2_features.py`: gerçek `WebResponse` akışı, iptal, `MediaStore` temizliği, SAF, canlı liste bağlantısı.
2. `android/app-telefon/src/test/java/com/eymen/browser/telefon/DownloadProgressMathTest.kt`: bilinmeyen boyut, bilinen boyut, büyük veri ve hatalı `Content-Length` için gerçek JUnit sınır kontrolleri.
3. `.github/workflows/phase0.yml`: kaynak, JVM birim testleri ve Android APK derleme kontrolleri.
4. `.github/workflows/android-visual-audit.yml`: Android 15/16 açılış/güvenli alan kontrolleri; gerçek indirme/cancel davranışını kanıtlamaz.
5. Fiziksel Android 8/9/14/15/16'da PDF, resim, büyük dosya, bilinmeyen uzunluk, iptal, ağ kopması, kullanıcı tarafından dosya yeri seçme, indirme sırasında ekran kapanması ve ardından liste yenileme denetimi gerekir.

**İmzalı sürüm kapısı:** Orijinal üretim sertifikası, eski v1.0.14 üzerine kurulum, paket/sürüm kimliği, tüm üç ABI APK ve SHA-256 doğrulanmadan yeni sürüm tamamlanmış sayılmaz.
