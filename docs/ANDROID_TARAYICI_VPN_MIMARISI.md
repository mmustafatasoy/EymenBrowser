# EYMEN Browser — Yalnız uygulama trafiği için VPN mimarisi

Durum: 2026-11-04 — **MİMARİ HAZIRLIK; ÇALIŞAN VPN YOK.** Tasarım ve aşama kararı mevcut onaylı browser ekranlarını değiştirmez.

## Ürünün sınırı

Kullanıcının isteği: Android tarayıcının web trafiğini VPN'e yönlendirmek, telefondaki diğer uygulamaların trafiğine dokunmamak; hazır bir üçüncü taraf VPN sağlayıcısının, kullanıcının kendine ait hesabını/konfigürasyonunu bağlamak. TV/PC/iOS çalışmaları şimdilik bekliyor.

GeckoView'un herkese açık 159 SDK'sı üzerinden güvenilir bir genel HTTP/SOCKS proxy değiştirme arayüzü bulunmadığı için kaynağı belirsiz `about:config` pref-hack'leri, TLS sertifika denetimini devre dışı bırakma veya sahte `bağlandı` gösterimi kullanılmaz. Android'in resmî `VpnService` API'sindeki `Builder.addAllowedApplication(applicationId)` yönlendirme kapsamı **yalnız EymenBrowser uygulamasının UID'sini** VPN'e katabilir; diğer uygulamalar normal ağı kullanır.

Not: Bu yönlendirme *uygulama* kapsamındadır; aynı uygulamanın gelecekteki ağ istekleri de etkilenebilir. GeckoView'in sadece bazı isteklerini seçme garantisi değildir.

## Güvenlik ve mimari

1. **Gerçek sağlayıcı:** Önce yalnız somut ve desteklenen bir WireGuard veya OpenVPN tünel istemcisi arka ucu/SDK'sı seçilir. Sağlayıcı hesabı tek başına tünel kurmaz; gerçek sunucu uç noktası, yapılandırma ve uygun kimlik doğrulama gerekir. Kod depo içinde hiçbir kişisel anahtar, token veya parola barındırmaz.
2. **Sistem izni:** Bağlanma düğmesi kullanıcı hareketiyle `VpnService.prepare(context)` çağırır; Android'in kendi sistem VPN onay ekranı gösterilir. Reddedilirse doğrudan mevcut ağ kullanılır ve UI kesinlikle `bağlandı` demez.
3. **Dar uygulama listesi:** Yalnız `Builder.addAllowedApplication("com.eymen.browser.telefon")` çağrılır; bütün telefonu VPN'e almak için `addAllowedApplication` atlanmaz.
4. **Gerçek veri düzlemi:** VPN servisinin `establish()` dosya tanımlayıcısından çıkan paketler şifreli tünel backend'i üzerinden sağlayıcıya iletilir; sunucudan gelenler tünele döner. Paket alışverişi olmadan yalnız TUN oluşturmak **çalışan VPN değildir**.
5. **Bağlantı bütünlüğü:** DNS ile IPv4/IPv6 kapsamı, tünel kopmalarında kaçak riski, sistem VPN izninin geri alınması, Android arka plan servis/foreground bildirim kuralları, ağ değişimi ve öldürülme senaryoları ayrıca tasarlanır/test edilir. Çift yığın veya DNS kapsamı sağlanmadan 'sızıntısız' sözü verilmez.
6. **Kimlik bilgileri:** Hesap gizli anahtarları Android Keystore korumasıyla ve uygun şifreli saklama modelinde tutulmalı; GitHub Actions secrets içine bile kullanıcının kişisel yapılandırması yazılmamalı. Bağlantı günlükleri URL, parola veya kullanıcı kimliğini sızdırmamalı.
7. **Kullanıcı kontrolleri:** Bağlantı kuruldu/kuruluyor/bağlantı koptu/hata/kapalı durumları gerçek tünel olaylarıyla doğrulanır; kullanıcı istediğinde ayarlar panelinden kapatılır. Başlangıçta uygulama koruması T05 Gecko takip engelidir; VPN'den ayrı gösterilir.

## Kabul testleri

- Sağlayıcıya gerçekten bağlanıp tarayıcıda dış IP'nin beklenen uç nokta ile değiştiği görülür.
- Başka uygulamanın çıkış IP'si aynı kalır; uygulama kapsamı doğrulanır.
- DNS ve IPv6, yeniden bağlanma ve ağ kesintilerinde sınanır.
- İzin reddi/geri alma, başka VPN'nin aktif olması, oturumdan çıkış, bağlantı kesme ve sürüş/uyku dönüşü incelenir.
- YouTube tam ekran, DRM, PDF, indirme, sosyal medya ve Google oturumu regresyonla doğrulanır.
- Üç Android ABI test APK, güvenli yapılandırma, telefonda gözlem, onaylı UI referanslarıyla eşleşme tamamlanmadan üretim imzalı Release yayımlanmaz.

## Resmî teknik referanslar

- Android VpnService: https://developer.android.com/reference/android/net/VpnService
- Uygulama bazlı izin listesi: https://developer.android.com/reference/android/net/VpnService.Builder#addAllowedApplication(java.lang.String)
- Android VPN mimarisi: https://developer.android.com/develop/connectivity/vpn
- GeckoView runtime ayarları: https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoRuntimeSettings.Builder.html

**Sonuç:** Bu doküman mimari kısıtları çözer fakat henüz yeni bir servis, tünel motoru, sağlayıcı bağlantısı veya etkileşimli VPN düğmesi eklenmedi. Çalışma sürüyor; teknik kabul kararı verilmedi.
