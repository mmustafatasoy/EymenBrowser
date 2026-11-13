# Android güvenli DNS ve yazdırma — özellik 40 / 22

Yalnız Android telefon; yeni bağımlılık, üretim imzası veya yayın akışı değişmez.

## Güvenli DNS

Varsayılan cihazın DNS ayarıdır. Kullanıcı Cloudflare veya Quad9 seçerse
Gecko runtime'da yalnız sabit HTTPS endpoint ve TRR_ONLY modu uygulanır;
DoH otomatik sağlayıcı seçimi kapalıdır. Sistem seçimi TRR_DISABLED'dır.
Seçim kalıcıdır, telefonun genel DNS yapılandırması değiştirilmez.

Bu özellik Gecko'nun tarayıcı sayfası çözümlemesini yapılandırır; VPN veya IP
adresi değiştirme hizmeti değildir. Yerel/özel alanlar ve sağlayıcı bootstrap
kuralları Gecko/Android'e bağlıdır. Seçili sağlayıcıya ulaşılamıyorsa genel
alan adları açılmayabilir. Paket düzeyinde DNS/IPv6/WebRTC saha ölçümü bu ayar
ve CI kontrolüyle tamamlanmış sayılmaz.

## Yazdırma

Menüdeki Yazdır, mevcut gerçek Gecko sayfasının PDF çıktısını platformun
PrintDelegate'ine verir. Android PrintSpooler kullanıcıya yazıcı/Save as PDF,
önizleme ve son onayı gösterir. Uygulama sessiz yazdırmaz, yeni indirme
kaydı üretmez. İptal mevcut sekme ve adresi korur. Var olan PDF kaydetme
indirme akışı ayrı kalır.

View ActivityContextDelegate geçerli Activity'yi sağlar. Menü ve sayfanın
window.print çağrıları yalnız aktif/önde/yüklenmiş sayfada çalışır; çocuk
modu ve kilitli gizli sayfadan dış yazdırma ekranı açılmaz. Yanıt geldiğinde
sayfa/Activity artık uygun değilse PDF stream kapatılır.

## Kabul

UI'da gerçek Gecko runtime URI/TRR modu ve kalıcı tercih okunur; iki sağlayıcı
ve sistem seçimi doğrulanır. Gerçek HTTPS sayfası Android PrintSpooler
önizlemesine gönderilir, gerçek Back ile iptal edilir; aynı oturum/adres ve
indirme kayıtları kontrol edilir. İki native/ayar PNG'si zorunludur. Adayın
derleme/UI sonuçları henüz denenmedi; gerçek DNS paket ölçümü ve fiziksel
yazıcı denemesi ayrı açık kabullerdir.

## Birincil kaynaklar

- [Mozilla DNS runtime ayarları](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoRuntimeSettings.html)
- [Cloudflare resmi DoH endpoint'i](https://developers.cloudflare.com/1.1.1.1/encryption/dns-over-https/make-api-requests/)
- [Quad9 resmi hizmetleri](https://docs.quad9.net/services/)
- [Mozilla GeckoView native print kaynağı](https://searchfox.org/mozilla-central/source/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoView.java)

Güncel Javadoc sabitlenmiş motor sürümünden yeni olabilir; destek derleme ve
gerçek sabitlenmiş APK üzerindeki kabul ile doğrulanır.
