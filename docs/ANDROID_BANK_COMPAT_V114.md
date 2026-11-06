# EYMEN Browser v1.0.14 — HTTPS giriş penceresi uyumluluğu

## Yeni teknik düzeltme

v1.0.13, Android uygulamasına yönlendirme ve JavaScript promptlarını içeriyordu; ancak **`window.open()` ve `target=_blank` ile açılan yeni giriş/kimlik doğrulama pencerelerine** `GeckoSession.NavigationDelegate.onNewSession` sağlamıyordu. GeckoView bu durumda yeni pencere isteğini karşılayamaz; bazı bankalar ve OAuth giriş sistemleri sessizce durabilir.

v1.0.14 bunu tamamlamak için:
- `GeckoMotorDenemesi.attach()` artık `onUserOpenedWindow` callback'i kabul eder.
- `NavigationDelegate.onLoadRequest()` bir isteğin gerçekten **kullanıcı tıklamasıyla yeni pencere** açtığını doğrular. Sadece geçerli HTTPS URL'leri ve Geckonun `about:blank` başlangıç penceresine izin verilir.
- `onNewSession()` yalnız onaylanmış URI için **yeni, ayrı `GeckoSession`** döndürür. Yeni oturum URL'si elle yüklenmez: GeckoView yüklemeyi kendisi yapar.
- Android uygulamasındaki gerçek sekme listesi yeni pencereyi saklar, **gizli sekmelerde gizlilik modunu korur** ve normal sekme sayısını 32 ile sınırlar.
- Otomatik, tıklamasız pop-up'lar ve HTTPS dışı yeni pencereler açılmaz. Harici Android uygulamaları v1.0.13'teki ayrı kullanıcı onayından geçer.

## Sınırlar

- Bu **banka güvenlik doğrulamasını atlamaz**. Bankanın yerel Android uygulamasındaki PIN, biyometri, cihaz bütünlüğü ve Play Integrity kontrolleri ayrı sistemlerdir.
- Bu çözüm, web sayfasının mobil bankacılık uygulamasını doğrudan taklit etmez. Banka sadece kendi uygulamasında oturum açılmasını istiyorsa resmi mobil uygulama kullanılmalıdır.
- Bazı bankalar OAuth veya yeni pencerede `postMessage`/opener ilişkisine dayanır; bu tür akışlar fiziksel cihazda ve bankanın kendi test ortamında ayrıca doğrulanmalıdır.
- VPN, reklam/izleyici engelleme, sıkı mod ve yanlış site istisnası giriş akışını etkileyebilir. Bankanın **doğru resmi HTTPS adresinde**, isteğe bağlı koruma istisnasını ve VPN kapalı denemesini kullanarak sorun ayrıştırılmalıdır. Bu, bankanın kendi doğrulama kurallarını kapatmaz.
- Banka adı, hata mesajı ve işlemin hangi noktada durduğu bilinmeden hangi uyumsuzluğun kök neden olduğunu kesin belirlemek veya **bütün bankalar için düzeldi** demek mümkün değildir. **Şifre/SMS kodu/hesap bilgisi paylaşılmamalıdır.**

## Otomatik kabul sınırı

`python tools/check_banking_compat.py`, Android `assembleDebug` ve `.github/workflows/android-visual-audit.yml` ile kaynak/görsel test yapılır. Gerçek bir bankanın oturum açma testinin geçtiği anlamına gelmez. Üretim sertifikası ve önceki v1.0.13 üzerine güncelleme kontrolü olmadan kararlı APK yayımlanmaz.
