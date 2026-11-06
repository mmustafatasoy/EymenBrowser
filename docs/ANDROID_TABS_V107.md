# EYMEN Browser v1.0.7 — Android telefon sekme yöneticisi

## Uygulanan işlemler

- **Canlı sekme grupları:** Normal GeckoSession sekmelerine grup atanır. 20 gruba kadar yeni grup oluşturma, grubu yeniden adlandırma, grup sekmelerini filtreleme, gruptan çıkarma ve grup silme mevcut. Grup silinince sekmeler kapanmaz.
- **Grup içinden yeni sekme:** Grup filtresindeyken yeni sekme aynı gruba katılır.
- **Hızlı geçiş:** Sekme ekranında önceki/sonraki düğmeleri filtrede görünen gerçek GeckoSession'a geçer; sahte sayfa veya sekme açmaz.
- **Kapatılan son sekmeyi geri açma:** Yalnızca normal sekmenin doğrulanmış HTTPS adresi hatırlanır. Geri açma yeni GeckoSession oluşturur; kapanmış GeckoSession'a erişim yapılmaz.
- **Gizlilik:** Gizli sekmeler gruplara girmez, son kapatılan sekme yuvasına URL yazmaz. Son gizli sekme kapatıldığında gizli site koruma tercihleri temizlenir.
- **Mevcut tasarım:** Üst arama/sekme çubuğu, açık renkli ana sayfa, durum çubuğu/çentik güvenli alanı, YouTube/DRM/indirme akışları üzerinde değişiklik yapılmadı.
- **Çeviriler:** Yeni kontrollerin Türkçe ve İngilizce kaynak anahtarları `shared/i18n/` içinde yer alır ve `tools/generate.py` tarafından platform kaynaklarına üretilir.

## Bilinen sınırlar (özellikle önemli)

- Sekmeler ve gruplar mevcut uygulama çalışması içindir. Süreç öldüğünde/seans kapandığında **kalıcı sekme veya grup geri yüklemesi yoktur**.
- Son kapatılan geri alma **yalnızca bir** normal sekmeyi tutar; Gecko geri/ileri geçmişini, form durumunu, özel oturumu veya oynatılan videonun anlık konumunu geri yüklemez.
- Sekme kartları **gerçek web sayfası ekran görüntüsü değildir**; önceki sürümden kalan nötr yer tutucu önizlemeler kullanılır.
- Android emülatöründe açılış/görsel kontrolü, gerçek cihazdaki çoklu sekme/stres ve oturum izolasyonu denemesinin yerine geçmez.
- TV/iOS/masaüstü, gerçek VPN tüneli, biyometrik gizli oturum kilidi bu sürümün kapsamına dahil değildir.

## Gerçek cihaz kabul kontrolü

1. Üç normal sekme açın; grup oluşturun, iki sekmeyi bu gruba taşıyın, sekmeler görünürken filtreleyin ve bir sekmeyi gruptan çıkarın.
2. Grubun adını değiştirin; grup filtresinde yeni sekme açınca grubun içinde göründüğünü doğrulayın.
3. Grubu silin ve iki sekmenin de kapatılmadığını doğrulayın.
4. Önceki/sonraki düğmeleriyle iki açık sayfa arasında geçin; oturumların gerçekten korunduğunu doğrulayın.
5. Bir normal HTTPS sayfasını kapatın ve geri açın; aynı URL yeni oturumda açılmalı.
6. Bir gizli sekmeyi kapatın; normal geri alma kutusunda gizli sekmeye ait bir seçenek ve adres görünmemeli.
7. Ekranı döndürün, videoyu tam ekrana alın, indirme ve güncelleme kontrollerini test edin. Şebeke/saat/kamera alanı ile tarayıcı çubuğu çakışmamalı.

## Doğrulama

Kod kontrolü: `python tools/generate.py && python tools/check_tab_sessions.py && python tools/check_stage2_features.py`.

Derleme/CI: `.github/workflows/phase0.yml`.

Ekran görüntüsü ve emülatör: `.github/workflows/android-visual-audit.yml`.

İmzalı sürüm için ayrıca orijinal üretim imzası, üç mimari APK, paket adı, sürüm kodu, SHA-256 ve önceki kararlı APK uyumluluğu geçmelidir.
