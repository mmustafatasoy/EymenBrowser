# Android medya, küçük pencere ve sistem teması

Şartname 29 (Çekirdek), 28 ve 56; yalnız Android telefon. Yeni bağımlılık,
üretim anahtarı, tasarım kaynağı veya yayın davranışı değişmez.

- Gündüz/gece tercihleri korunur. Sisteme uy seçilirse Android uiMode değişimi
  ve ön plana dönüşte gerçek sistem teması uygulanır; Gecko oturumu yeniden
  kurulmaz. Kullanıcının açık gündüz/gece seçimi sistem modundan çıkarır.
- Medya servisi yalnız Gecko'nun gerçek oynatma olayıyla başlar. Android
  mediaPlayback foreground türü, ses odağı, gerçek MediaSession ve oynat/
  duraklat/durdur/sarma eylemleri gerçek Gecko oynatıcısına bağlıdır. Kulaklık
  çıkınca veya ses odağı kaybolunca duraklatır; kullanıcı yeniden başlatır.
- Duraklamada foreground bırakılır, kısa süre içinde tekrar oynatılmazsa
  servis/medya bildirimi kapanır. Sekme kapanınca ve Durdur eyleminde temizlenir.
  Servis dışa açık değildir; üretim imzası veya ağ yönlendirmesiyle ilgisi yoktur.
- Özel medya sistemde yalnız “Gizli sekme” başlığını ve gizli kilit ekranı
  görünürlüğünü kullanır. Gerçek başlık/adres sistem medya kaydına yazılmaz.
- Çalan tam ekran normal videoda Home, cihaz izin veriyorsa Android PiP'yi
  açar. Gizli/çocuk modunda kendiliğinden pencere açılmaz. Android politika/
  cihaz desteği PiP'yi engelleyebilir; uygulama yüzen-pencere izni istemez.
- Medya test klibi test APK'sında bulunan, kendimiz ürettiğimiz 3 s VP8/Vorbis
  renk/ses örneğidir; üretim APK'sına veya indirme hizmetine eklenmez.

## Kabul

Gerçek HTTPS belgesine test videosu yerleştirilir ve gerçek dokunuşla oynatılır.
Gecko zaman ilerlemesi, gerçek Activity onStop/Home sınırı, Android medya
bildirimi/oturum tokeni üzerinden duraklat/devam/durdur, tam ekran → sistem
PiP, özel başlığın gizlenmesi ve sistem teması seçimi UI testine eklenir.
`theme-system.png`, `media-background.png`, `media-pip.png` zorunludur.
Derleme, UI/cihaz ve PiP sonuçları henüz denenmedi; belge kabul sayılmaz.

## Birincil kaynaklar

- [Mozilla MediaSession ve Delegate](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/MediaSession.html)
- [Mozilla media metadata](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/MediaSession.Metadata.html)
- [Android PiP](https://developer.android.com/develop/ui/views/picture-in-picture)
- [Android foreground service türleri](https://developer.android.com/develop/background-work/services/fgs/service-types#media)

Güncel Javadoc, sabitlenmiş Gecko sürümünden yeni olabilir; gerçek sürümdeki
imza/çalışma uyumu derleme ve cihaz testleriyle doğrulanır.
