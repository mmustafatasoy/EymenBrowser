# EYMEN Browser v1.0.13 — bankacılık web giriş / resmî mobil uygulama uyumluluğu

## Kullanıcı tarafından bildirilen sorun

Bankaların **web siteleri açılırken mobil bankacılık girişine** ilerlenemeyebiliyor. Bankalar yerel Android uygulamalarında cihaz bütünlüğü, cihaz kaydı, resmî paket imzası, uygulamaya özel doğrulama, passkey/biyometri ve kendi güvenlik politikalarını kullanabilir. Web sitesinin açılması, mobil uygulamanın tarayıcı içinde çalışabileceği anlamına gelmez.

**Bankacılık uygulamasının güvenlik kilidi veya resmî bankanın kimlik doğrulama kontrolleri atlatılmayacaktır.** Varsa kurulu resmî Android uygulamaya normal Android intent yönlendirmesi yapılabilir; desteklemeyen bankalarda tarayıcıdan yerel mobil giriş garantisi yoktur.

## v1.0.13 için eklenenler

1. **GeckoView NavigationDelegate:** Bankanın web sitesinde kullanıcı tarafından tıklanan ve Android uygulamasına yönlendiren `intent://` veya özel uygulama şeması, tarayıcının Android yerel uygulama açma akışına bağlandı. HTTPS ve tarayıcı içi JavaScript gezinmesi normal GeckoView'da kalır.
2. **İzin/onay:** Uygulama bağlantısı yalnızca gerçek kullanıcı etkileşimi ve HTTPS kaynak üzerinden geldiğinde incelenir. Uygulama kendiliğinden açılmaz; kaynak site ve hedef uygulama paketi/şeması gösterilerek ayrı Android onayı istenir.
3. **Sıfır yetki yükseltmesi:** Sayfadan gelen `Intent` bileşen, selector, flag, ekstra parametre, dosya/klip yetkisi ve diğer hassas alanlar **taşınmaz**; yalnız doğrulanmış hedef URI, `ACTION_VIEW`, `BROWSABLE` ve varsa doğrulanmış uygulama paket adıyla yeni bir `Intent` oluşturulur. `file:`, `content:`, `javascript:`, `data:` vb. güvenli olmayan dış şemalar uygulamaya teslim edilmez.
4. **Menü → Mobil uygulamada aç:** Mevcut HTTPS sayfası, kullanıcı bu düğmeye basarsa Android üzerinden kurulu ve ilgili bağlantıyı destekleyen uygulamaya devredilebilir. Android 11+ cihazlarda `FLAG_ACTIVITY_REQUIRE_NON_BROWSER` tarayıcıya geri açılmasını önler. Kurulu resmî uygulama yoksa işlem gerçekleşmez.
5. **Web JavaScript pencereleri:** GeckoView'un kendi tarayıcı arayüzü olmadığından `alert()`, `confirm()`, `prompt()` çağrıları, mevcut HTTPS hostu belirtilen Android onay pencerelerinde gösterilir. Giriş şifreleri, SMS kodları veya bu alanlara yazılan içerikler loga/analitiğe kaydedilmez.
6. **Tasarım:** EYMEN Browser açık renkli ana sayfası, üst araç çubuğu, çentik güvenli alanı, HTTPS güvenliği, özel sekme ayrımı, takipçi engelleme ve WireGuard ayarları değişmedi. İlgili menü öğesi mevcut alt menü listesine eklendi.

## Hâlâ kesinlikle garanti edilmeyenler

- Bankanın **yerel Android mobil bankacılık uygulaması tarayıcı içinde çalıştırılmaz**; banka kurulu değilse EYMEN Browser onun yerine geçemez.
- Bankanın PIN/biyometri, cihaz sertifikası, Play Integrity, kimlik kartı, SMS onayı, FIDO2/WebAuthn veya Chrome Custom Tabs entegrasyonu bankaya özgüdür. Bu sürüm güvenlik doğrulamalarını taklit etmez veya devre dışı bırakmaz.
- Bazı bankalar `window.open()` üzerinden yeni GeckoSession, ek izin/sertifika akışları veya üçüncü taraf çerezleri isteyebilir. Bu sürümde otomatik tüm popup ve FIDO2 akışlarının tamamlandığı iddia edilmez.
- VPN, sıkı takipçi koruması ve reklam alanı temizleme bazı girişleri etkileyebilir. Sorunu ayırmak için **önce VPN kapalı ve koruma Standart** seçilerek bankanın kendi resmî HTTPS adresiyle deneyin. Bankanın kimlik doğrulama güvenliği devre dışı bırakılmamalıdır.
- Banka adı, Android sürümü, girilen adresin türü ve hatanın hangi adımda oluştuğu belirtilmeden tüm bankalarda sorunun çözüldüğü öne sürülmez. **Banka şifresi, SMS doğrulama kodu, kart veya kimlik bilgileri hiçbir destek görüşmesine gönderilmez.**

## CI/test

- `python tools/check_banking_compat.py` güvenli dış bağlantı, açık onay ve GeckoView prompt bağlamasını denetler.
- `python tools/generate.py` ve `python tools/check_menu_actions.py` menü ve Türkçe/İngilizce kaynakları denetler.
- `.github/workflows/phase0.yml` Android APK derlemesi, `.github/workflows/android-visual-audit.yml` Android 15/16 açılış/güvenli alan ekran testi gerçekleştirir.
- Gerçek banka hesaplarında otomatik giriş **test edilmez** ve şifre istenmez. Kurulu bankanın resmî uygulamasıyla bağlantı aktarımı, kullanıcı tarafından fiziksel cihazda doğrulanmalıdır.

## Kullanım

1. EYMEN Browser'da bankanın resmî HTTPS sayfasını açın.
2. Bankanın `Mobil Uygulamayı Aç` bağlantısına dokunun; desteklenen bir uygulama bağlantısı varsa kaynak site ve **sayfanın talep ettiği** hedef uygulama bilgisi çıkacaktır. EYMEN Browser, bu uygulamanın gerçekten bankaya ait olduğunu doğrulayamaz; kurulu resmî uygulama olduğundan emin olmadan onay vermeyin.
3. Sayfada yönlendirme yoksa **⋮ → Mobil uygulamada aç** seçeneğini kullanın. Android'de bankanın kurulu resmî uygulaması bağlantıyı desteklemiyorsa bilgilendirme görünür.
4. Yine açılmıyorsa bankanın resmî uygulamasını doğrudan açarak giriş yapın. Bu, tarayıcının bankanın mobil uygulamasının yerini aldığı anlamına gelmez.
