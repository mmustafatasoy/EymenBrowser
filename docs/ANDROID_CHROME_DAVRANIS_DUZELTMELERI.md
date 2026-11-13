# Android telefon – Chrome benzeri davranış düzeltme sözleşmesi

13 Kasım 2026. Kullanıcıda bildirilen dört hata hedeflenir; Google Chrome'un özel kaynak kodu, uygulama adı, grafik varlıkları veya tescilli bileşenleri alınmaz.

## 1. Sekme izolasyonu
GeckoView releaseSession ve setSession gerçek GeckoSession örneklerini yeniden kullanır; sekme kimliği/URL/DOM ayrı tutulur. Değişimde coverUntilFirstPaint uygulanan yüzey eski sekme piksellerini göstermemelidir. Asenkron thumbnail doğru aktif tab, sesion kimliği ve URL değişmediğinde kullanılır.

## 2. WhatsApp Web
Yalnız kesin HTTPS web.whatsapp.com alanında ilk girişte Gecko desktop UA ve viewport uygulanır. Başka alan adları etkilenmez. Kullanıcı masaüstü görünümünü elle kapatabilir. WhatsApp hesabı ve gerçek QR oturumu emülatör testlerinde taklit edilmez; gerçek cihazda kullanıcı kabulü gereklidir.

## 3. Arama
Normal Google seçiliyken 280 ms debounce ve in-flight ticket iptaliyle suggestqueries.google.com tamamlamaları gösterilir. Tam kelime/daha fazla kelime için tıklama standart açık aramayı başlatır. Ağ hatasında geçmiş/yer imi önerileri korunur. Gizli/çocuk, farklı motor, URL ve devre dışı ayarda uzaktan sorgu gönderilmez. Uç nokta Google'ın resmî belgeli API'si değildir; hiçbir servis garantisi verilmez.

## 4. PDF
Gerçek Android içerik URI'sinden PdfRenderer PDF okumaya çalışır; Android uygulama seçicisine çıkmadan sayfalar gösterilir. Tek worker, azami piksel ve zoom sınırlarıyla OOM riski azaltılır. Bozuk/şifreli/seekable olmayan belge için hata mesajı verilir. Orijinal Gecko oturumu kapanmaz. Diğer MIME türlerinin ACTION_VIEW davranışı korunur.

## Kabul
Saf JVM: alan adı kısıtı, PDF tanıma, ağ talebi gizlilik/arama kapsamı. Android API35 ve API36: gerçek iki farklı DOM'un sekmeler arasında korunması, MediaStore iki sayfalı PDF'nin okuyucuda gerçek açılması/ileri/geri, karışık web/uygulama teması ekran kanıtı ve önceki testlerin tamamı. Üç APK ABI/JVM, iki API görsel, indirme ve tam UI CI grupları aynı committe geçmedikçe yayın/main yok. Fiziksel cihaz kabulü ayrıca açık kalır.

## Sınırlar
GeckoView Chromium/Blink değildir; Chrome'un tüm uygulama/site davranışlarını bire bir garanti edemez. Google öneri sağlayıcısı ayrıca değişebilir. Kullanıcıya başarı iddia edilmeden gerçek cihaz PDF/WhatsApp ve çökme loglarıyla kontrol edilmeli.
