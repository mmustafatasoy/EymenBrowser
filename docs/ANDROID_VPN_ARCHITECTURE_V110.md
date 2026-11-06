# EYMEN Browser — VPN entegrasyonu güvenlik ve ürün planı (v1.0.10 hazırlık)

## Mevcut durum

**Henüz VPN bağlantısı, sunucu veya kullanıcıya açılmış bir VPN düğmesi yoktur.** v1.0.10'daki mevcut iş; GeckoView korumasına ayrı, APK içinde taşınan opt-in reklam alanı temizleme modülü ve daha sonra kurulacak gerçek VPN için tanımlanmış gereksinimlerdir. İzin ekranı ve bağlantı durumunun yanlış gösterilmesi kabul edilmez.

## Hedef mimari

1. Ayrı ağ modülü ve gerçek bir `VpnService`. Geçerli sunucu yapılandırması yoksa servis başlatılmaz, “Bağlandı” durumu gösterilmez.
2. Android `VpnService.prepare(context)` ile sistemin gerçek VPN onayı alınır; kullanıcının onayı olmadan geçiş yapılmaz. Gerekli foreground service/notification izinleri ve Android 8–16 uygulama yaşam döngüsü denetimi yapılır.
3. İstek **yalnız EYMEN Browser için** olduğundan `VpnService.Builder.addAllowedApplication("com.eymen.browser.telefon")` ile yalnızca desteklenen uygulama trafiği hedeflenir. Bu davranış farklı Android/Gecko ağ süreçlerinde fiziksel cihazda ayrıca doğrulanmadan “yalnız tarayıcıyı koruyor” denmez.
4. Uzak ağ geçidi, açıkça yapılandırılan ve yetkilendirilen gerçek bir VPN sunucusu olmalıdır. Protokol için güvenlik değerlendirmesi yapılmış bir uygulama (ör. WireGuard) ve sunucunun sabitlenmiş kimlik doğrulama anahtarı hedeflenir. Hazır sunucu, erişim bilgisi, anahtar yönetimi ve trafik politikası olmadan geliştirilen DNS proxy bir VPN olarak pazarlanmaz.
5. IPv4/IPv6 ve DNS'nin aynı koruma sınırından geçtiği doğrulanır. Bağlantı kopunca trafik sızıntısı, paket yönlendirme döngüsü ve “bağlı” gibi yanlış durum raporlaması engellenir. VPN servisinin kendi sunucu soketi Android `VpnService.protect(...)` ile tünel dışına çıkarılarak yönlendirme döngüsünden kaçılır.
6. Android Keystore içinde cihazla ilişkili anahtar saklama; sunucu IP, anahtar ve uygulama sürümü için güvenilir yerel yapılandırma; tokenları loga, APK içine veya GitHub'a koymama; sunucuda kullanıcı trafiği ve DNS kaydı tutmama politikası.
7. “Bağlanıyor”, “Bağlı”, “Bağlantı koptu”, “Sunucu bulunamadı”, “İzin reddedildi” ayrı gerçek durumlar olur. Sunucu kimlik doğrulaması başarısızsa fail-closed davranışı zorunludur.

## Test kapıları

- Gerçek Android VPN izin ekranı ve kullanıcının iptali.
- Sunucu kimlik doğrulama reddi ve yeniden bağlanma.
- Bağlıyken 50 kez ağ geçişi (Wi‑Fi / mobil), uçak modu, ekran kilidi, uygulama sürecinin kapanması.
- IPv4, IPv6, DNS ve WebRTC IP sızıntısı; tünel dışı trafik kontrolü.
- EYMEN Browser trafiği şifrelendiğinde diğer uygulamaların yönlendirilmediğinin ağ seviyesinde doğrulanması.
- YouTube, DRM, indirme, uyku/uyanma ve Android 8–16 cihaz kabulü.
- Pil kullanımı, gecikme, bağlantı hızı, hata kodları ve kullanıcının VPN'i kapatma yetkisi.

## Öncelik sırası

Önce v1.0.10 opt-in kozmetik temizleme ve v1.0.9 takipçi engelleme regresyon testleri; sonraki sürümde sunucu ve protokol seçimi kesinleştirilerek ayrı VPN modülü ve cihaz testleri. **Bu belge gerçek bir VPN kurulduğuna dair kanıt değildir.**
