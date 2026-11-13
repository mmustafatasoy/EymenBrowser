# Android arka plan sekmesi belleği — özellik 6

Mevcut Android telefon yolu, seçilmeyen sekmeyi GeckoView'dan releaseSession
ile ayırır. Sabitlenmiş Mozilla 159.0.20261022150525 sources.jar incelendi:
GeckoView Display.release, `setActive(false)` çağırır. GeckoSession bu işlemde
sayfa durumunu flush eder ve 10 saniye sonra compositor'a memory-pressure
sinyali gönderir. Yeniden seçme aynı açık GeckoSession'ı görünür yapar.

Bu motorun görünmez sayfa için sağladığı bellek tasarrufudur. Formlar ve
JavaScript RAM durumu korunur; sekmeyi zorla kapatıp yeni belge yüklemek bu
özellik yerine kullanılmaz. Mevcut medya kabulü arka planda gerçek sesin
ilerlediğini ayrıca sınar. Kalıcı normal sekmelerin ilk seçilene kadar URL
bazında tembel yüklenmesi de önceki v108 sözleşmesinde kalır. Gizli sekme verisi
bu inceleme veya yeni kabul için disk kaydına eklenmez.

Yeni kabul gerçek HTTPS sayfasındaki web formuna Android klavyesiyle yazar,
başka sekmeye geçer; DOM visibilitychange hidden olmalıdır. On saniyelik gerçek
motor bellek döneminin ardından kullanıcı sekme aramasından aynı karta dokunur.
Aynı GeckoSession, görünür belge, yazılmış form ve RAM'deki sentetik işaretin
korunması zorunludur. `tab-memory-restored.png` geri dönmüş gerçek sayfadır.
Bununla fiziksel cihazın RAM/pil kazancına sayısal kabul verilmez.

Kaynaklar: [Mozilla GeckoView](https://github.com/mozilla-firefox/firefox/blob/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoView.java),
[GeckoSession.setActive](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoSession.html#setActive(boolean)),
[resmi sabitlenmiş kaynak paketi](https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview/159.0.20261022150525/geckoview-159.0.20261022150525-sources.jar).
