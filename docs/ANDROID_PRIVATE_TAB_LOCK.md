# Android gizli sekme kilidi — özellik 45

Yalnız Android telefon ve mevcut T09/Gizlilik düzeni. Varsayılan kapalıdır;
kullanıcı Android ekran kilidini doğruladıktan sonra etkinleşir. Yeni
bağımlılık, root, Shizuku veya uygulamanın aldığı bir PIN yoktur.

- Telefonun `KeyguardManager` doğrulama ekranı kullanılır. Başarılı Android
  sonucu ve cihazın hâlâ güvenli olması gerekir; iptal/hata kilidi açmaz.
  Devre dışı bırakma da aynı Android onayını ister. Ekran kilidi yoksa
  özellik açılmaz; önce cihaz ayarından kilit kurulması açıklanır.
- Arka plana geçince açık gizli sekmeler kilitlenir. Gizli medya duraklar,
  GeckoSession görünümden ayrılır; sayfanın kendisi kapatılmaz veya normal
  geçmişe kaydedilmez. Dönüşte onay verilene kadar yalnız kilit ekranı vardır.
- Kilitli özel sekme listesi adres/başlık/küçük resim göstermez. Kilit açılınca
  mevcut gerçek oturum yeniden bağlanır, yeni sekme veya yeni gezinme yaratılmaz.
- Kilit açıkken özel sayfa/sekme görünümünde Android `FLAG_SECURE` kullanılır.
  Normal sekmelere dönülebilir; çocuk modu normal gezintiye geçirilmez.
  Android ekran kilidi daha sonra kaldırılırsa önce yeniden cihaz kilidi
  kurulmalıdır. Özellik root erişimine veya işletim sistemi ihlaline karşı
  bir şifreli bellek kasası değildir; uygulama özel sekmeleri diske yazmaz.

## Kabul

Test yalnız boş kimlik bilgili, geçici AOSP emülatörde sentetik bir Android
PIN'i kurar; ranchu/goldfish dışında veya önceden kilitli cihazda reddeder.
PIN uygulamaya girilmez; gerçek Android credential penceresine gerçek tuşlarla
yazılır. Geçici test kilidi sonunda temizlenir. Fiziksel kullanıcı kilidi
oluşturulmaz/değiştirilmez.

Gerçek etkinleştirme, arka plana geçiş, GeckoSession'ın ayrılması, kilitli
başlık/adres dışlaması, doğrulamayı iptal, doğru doğrulama, aynı oturumun geri
bağlanması, koruma bayrağı ve kapatma onayı UI testindedir. Ayar görüntüsü normal
sekmede alınır; özel sayfa veya credential ekranı artefakta çekilmez.
Bu adayın derleme/UI ve gerçek telefon sonuçları henüz denenmedi.

## Birincil kaynaklar

- [Android KeyguardManager](https://developer.android.com/reference/android/app/KeyguardManager)
- [Android FLAG_SECURE](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_SECURE)

`createConfirmDeviceCredentialIntent` eski Android desteği için kullanılır;
Android tarafından doğrulanan sistem sonucu dışında bir kilit açma yolu yoktur.
