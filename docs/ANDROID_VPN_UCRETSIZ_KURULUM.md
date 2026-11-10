# Ücretsiz VPN ilk kurulumu

## Kapsam

Proton VPN Free hesabıyla kullanıcının kendisi için oluşturduğu WireGuard dosyası Eymen Browser'ın mevcut gerçek GoBackend tüneline aktarılır. VPS, ikinci VPN uygulaması veya ücretli plan gerekmez. Bir kez ücretsiz Proton hesabı açmak gerekir. Bu bir Proton SDK/API entegrasyonu, otomatik hesap açma veya uygulamayla birlikte paylaşılan sunucu anahtarı değildir. Eymen Browser bağımsız bir istemcidir.

## Kullanıcı adımları

1. Ayarlar → VPN ekranında **Proton VPN hesabını aç** düğmesine dokunun. Hesap işlemleri yalnız sağlayıcının resmî HTTPS sitesinde yapılır; Free planını seçin.
2. Sağlayıcı hesabında Downloads → WireGuard configuration bölümüne gidin. Android ve ücretsiz sunucuyu seçip Create → Download ile yeni bir dosya oluşturun. Eski IPv6'sız dosyalar yerine güncel dosya indirin.
3. Eymen Browser İndirilenler listesinden .conf dosyasını açın. İçe aktarma onayını verin. Başka bir tarayıcıda indirildiyse Ayarlar → VPN → WireGuard .conf dosyası içe aktar ile Android dosya seçicisini kullanın.
4. Yapılandırma hazır olunca **Android VPN izniyle bağlan** düğmesine dokunun. Android'in gerçek VPN onayını verin. Daha sonraki bağlantılarda aynı profil cihazda şifreli saklandığından dosyayı tekrar seçmek gerekmez.
5. Sunucuyla gerçek ve güncel WireGuard el sıkışması ayrı gösterilir. Tarayıcıdan gezinip VPN ekranına dönerek sayaçları/yanıtı kontrol edin. IP/DNS/IPv6/WebRTC saha testleri ayrıca gerekir.
6. İçe aktarılan dosya kişisel erişim anahtarı içerir. İçe aktarmadan sonra ortak İndirilenler klasöründeki kopyasını kendiniz kaldırabilirsiniz; uygulamanın şifreli profili korunur. Dosyayı sohbete veya depoya koymayın.

## Güvenlik ve kabul

- Kullanıcı dokunmadan hesap sayfası açılmaz veya sağlayıcıya istek gönderilmez. Sağlayıcı girişi Gecko'nun normal HTTPS sayfasıdır; VPN modülü parolaları işlemez. Mevcut normal/gizli sekme tercihi korunur.
- İndirilen dosyayı açmak tek başına profil değiştirmez: açık onay gerekir. İptal mevcut profili değiştirmez. Çocuk modunda dosya açma engeli ve bağlantı açıkken içe aktarma engeli korunur.
- İçerik aynı 24 KiB sınırı, tek peer, endpoint, çift yığın tam rota/adres ve DNS kurallarından geçer. Geçersiz dosya mevcut şifreli profili bozmaz. İçeriğe bakmadan yalnız uzantı üzerinden güvenilmez.
- Sadece tarayıcı paketi yönlendirilir. İçe aktarma bağlantı veya Android VPN izni vermez. Bağlanma, anahtarların şifrelenmesi ve gerçek handshake ayrımı mevcut uygulamadaki gibidir.
- Ücretsiz planın sunucu/ülke/erişilebilirlik koşulları sağlayıcıya bağlıdır. Bu değişiklik kill-switch, otomatik yeniden bağlanma veya tüm sitelerin açılacağı garantisini eklemez.
- Android UI kabulündeki geçici anahtarlar yalnız çalışma anında üretilir, ayrılmış test IP'si kullanılır ve gerçek sunucuya bağlanılmaz. Bu test profil onayı/şifreleme/uygulama kapsamını doğrular; gerçek ücretsiz hesap bağlantısının kabulü değildir.

## Doğrulanan resmî kaynaklar — 2026-11-10

- https://protonvpn.com/support/wireguard-configurations — Free hesabıyla yapılandırma oluşturma ve standart üçüncü taraf WireGuard istemcileri.
- https://protonvpn.com/support/wireguard-linux — güncel yapılandırmaların IPv6 desteği; eski dosya yerine yeniden indirme.
- https://account.protonvpn.com/ — kullanıcı tarafından açılan resmî hesap sitesi.
