# Android site bildirimleri — özellik 31

Gerçek Gecko WebNotificationDelegate, onaylı açık HTTPS sayfanın Notifications
API bildirimini Android'e taşır. Kullanıcı önce sitenin origin'ini görüp izin
verir; Android 13 ve üstünde POST_NOTIFICATIONS izni ayrıca sistem penceresinden
alınır. İptal/ret, pasif sekme veya başka izin isteği nedeniyle tamamlanamayan
onay izin vermez. Gizli/çocuk modunda site izni ve native gösterim reddedilir.

Bildirim ayrı yerelleştirilmiş site kanalındadır. Android izin/kanal engeliyse
Gecko'ya gösterildi denmez. HTTPS origin alt metinde görünür; başlık/gövde düz
metin ve uzunluk sınırlıdır. İkon için ağ isteği yoktur. En çok 32 canlı nesne
RAM'de tutulur; eski bildirim temizlenir. Başka uygulamalara açık receiver yoktur,
PendingIntent'ler explicit/immutable'dır. Site izni kaldırılınca o sitenin
mevcut bildirimleri de temizlenir. Çocuk moduna geçiş hepsini temizler.

Gerçek tıklamada canlı opak token eşleşmesi Gecko onclick'e iletilir ve bildirim
kapanır. Aynı kök sitedeki etkin veya son normal sekme öne alınır. Motor süreci
ölmüşse eski JS nesnesi canlandırılmaz; kullanıcı normal HTTPS köke döner.
Uygulama dışından uydurulmuş token, Gecko onclick olayını çalıştırmaz.

Bu aşama remote WebPush abonelik/taşıma hizmeti kurmaz. Sunucudan, uygulama
kapalıyken uzaktan bildirim teslimi ayrıca WebPush sağlayıcısı ve süreç/servis
entegrasyonu gerektirir. Ayarda bu sınır açıkça yazılıdır; sırf native bildirim
çalıştı diye kapalı uygulama push kabulü verilmez.

Kabul: Gerçek HTTPS Notification.requestPermission düğmesi, site onayı, gerçek
Android izin penceresi, NotificationManager kaydı, SystemUI çekmecesinde gerçek
tıklama, Gecko onclick, aynı normal sayfa, tekrar bildirim ve Ayarlar'dan izni
kaldırınca temizlenme. Gizli sayfada ayrı gerçek requestPermission ret kabulü
vardır. `web-notification.png` SystemUI ekranıdır. Derleme/UI sonucu henüz kabul
edilmedi; fiziksel Android 8/11/14/16 ve süreç sonlanması ayrıca açıktır.

Birincil kaynaklar: [Mozilla WebNotificationDelegate](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/WebNotificationDelegate.html),
[Mozilla WebNotification](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/WebNotification.html),
[Android bildirim izni](https://developer.android.com/develop/ui/views/notifications/notification-permission).
API imzaları sabitlenmiş Gecko 159 sources.jar ile de doğrulandı.
