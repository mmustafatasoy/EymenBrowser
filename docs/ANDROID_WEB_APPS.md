# Android web uygulaması kurma — özellik 32

Yalnız Android telefon. Gecko'nun gerçek onWebAppManifest bildirimiyle bulunan,
standalone/minimal-ui/fullscreen bildiren web manifesti kullanılır. Start URL
ve scope, açık normal HTTPS belgesiyle aynı origin olmalıdır; kullanıcı bilgisi,
uygunsuz port, çapraz kaynak, kapsam dışı start ve kodlanmış path kaçışları
reddedilir. Ad Unicode normalize edilir, kontrol/yönlendirme karakterlerinden
arındırılır ve sınırlanır. Gizli sayfa veya çocuk modunda kurulum gösterilmez.

Kullanıcı ad ve kaynak origin'ini içeren native onayı verir; sonra Android'in
launcher onayı gelir. requestPinShortcut=true yalnız isteğin Android'e
iletildiğidir; son onay alınmadan “kuruldu” denmez. Desteklemeyen launcher veya
20 uygulama sınırı için hata gösterilir. Artık pinli olmayan kayıtlar sonraki
istekte temizlenir; kayıt başarısızsa launcher isteği yapılmaz. Saklanan veri
kullanıcının kurduğu normal uygulamanın adı/start/scope/kimliğidir.

Gerçek pin ayrı WebAppActivity ve Android document task'ına gider. Yalnız
kayıtlı kimlik açılır; Intent'ten keyfi URL alınmaz. GeckoSession ana tarayıcı
sekmesinden ayrıdır, Android sistem alanı/çentik ve klavye insets'i korunur.
Kapsam içi sayfa kendi penceresinde, kapsam dışı HTTPS/HTTP bağlantı tarayıcıda
açılır. Private/child dışlaması resume'da da kontrol edilir. Motor ve mevcut
site/Android izinleri, DRM onayı, indirme stream'i, medya, Autofill, koruma ve
opt-in yerel sayfa bileşenleri paylaşılır; bağımsız bir WebView eklenmez.

Bu Android'in kullanıcı tarafından pinlenen standalone web uygulamasıdır;
ayrı APK paketi/WebAPK veya mağaza dağıtımı üretmez. Site servis worker/cache
sağlıyorsa Gecko'nun site depolamasını kullanır; her sitenin çevrimdışı çalışma
veya kapalı uygulama WebPush desteği olduğu iddia edilmez. Kapsam dışındaki
OAuth penceresinin siteye özgü geri dönüşü fiziksel uyumluluk kabulünde kalır.

Kabul: pure JVM ile kaynak/scope/path kaçışı/etiket kontrolü. Resmi MDN A2HS
HTTPS örneğinde üretim callback'iyle gelen manifest, gerçek native ad/origin
onayı, gerçek Launcher3 son onayı ve OS pinned kayıt kontrolü. Home ekranındaki
gerçek ikona dokunularak ayrı Activity/task/GeckoSession ve gerçek sayfa
render'ı; gerçek kapsam dışı bağlantıya dokununca ana tarayıcıya dönüş zorunlu.
web-app-confirmation.png, web-app-launcher.png, web-app-standalone.png gerçek
UI görüntüleridir. Sonuçlar henüz kabul edilmedi; fiziksel Android 8/11/14/16,
diğer launcher'lar, süreç sonlanması, çevrimdışı/cache ve siteye özgü oturum
akışları ayrıca açıktır.

Birincil kaynaklar:
- [Mozilla gerçek manifest callback'i](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoSession.ContentDelegate.html#onWebAppManifest(org.mozilla.geckoview.GeckoSession,org.json.JSONObject))
- [Android pinned shortcut](https://developer.android.com/develop/ui/views/launch/shortcuts/creating-shortcuts)
- [Resmi MDN PWA örnekleri](https://mdn.github.io/pwa-examples/)
- [Gerçek kabul sayfası](https://mdn.github.io/pwa-examples/a2hs/)
