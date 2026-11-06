# EYMEN Browser v1.0.9 — GeckoView izleyici koruması

## Uygulanan kapsam

- `Standart` / `Sıkı` seçenekleri GeckoView'un resmî `ContentBlocking.AntiTracking` ve `EtpLevel` ayarlarına doğrudan bağlıdır; sırasıyla bilinen standart takipçi listeleri ve daha geniş sıkı listeler kullanılır.
- Kullanıcı seçimi `shield_strict_mode` olarak uygulama tercihlerinde tutulur ve açılışta GeckoRuntime'a uygulanır. Sıkı mod bazı siteleri bozabilir; Standart moddan başlamak önerilir.
- GeckoView'un gerçekten engellediği kaynakları bildiren `onContentBlocked` çağrısı ve `isBlocking` doğrulaması kullanılır. Sadece gözlenen engellenen olaylar sayılır; uydurma reklam sayacı yoktur.
- Reklam takipçileri, analiz takipçileri ve diğer engellenenler üç kategoriye ayrılır. Her olay kategoriye bir kez yazılır. Tekil şirket, reklam alanı veya toplam reklam sayısı değildir.
- Sekme bazlı gerçek engelleme sayaçları koruma panelinde gösterilir. Güncel sayfadaki ve sekmedeki toplam engellenen istekler de yerinde kalır.
- Her sitenin koruması açılıp kapatılabilir; normal sekmelerde site tercihi saklanır, gizli sekme site tercihi yalnız bellekte tutulur ve son gizli sekme kapanınca temizlenir.
- Açık renkli tarayıcının **Ayarlar > Koruma** bölümü, ilgili paneli açar. Panelde hem global korumayı açma/kapatma hem Standart/Sıkı modu seçme hem de geçerli sitede korumayı açma/kapatma işlevleri vardır.
- Üst adres/sekme çubuğuna, site logosuna, güncelleme imzasına ve onaylanan açık renkli ana sayfaya değişiklik yapılmadı.

## Dürüst kapsam sınırı

Bu sürüm, **Mozilla GeckoView listeleriyle bilinen takipçileri engeller**. Tüm reklamlar, özellikle ilk taraflı reklamlar, site içine gömülü sponsorlu içerikler, açılır pencereler veya reklam boşlukları otomatik olarak kaldırılmış kabul edilmez. Özel EasyList/uBlock kozmetik filtreleme motoru, reklam alanı gizleme ve düzenli indirilen harici filtre listeleri bu sürüme dahil değildir. Diğer yandan, altyapıda zaten bulunan Safe Browsing koruması ve izin akışı korunur.

## Güvenlik ve kullanıcı kabulü

1. Standart modda takipçi içeren açık HTTPS sayfası açın, T05 panelinde sayılan engellenen olayları ve kategorilerini kontrol edin; gerçek olay yoksa 0 gösterilmeli.
2. Sıkı moda geçin ve aynı sayfayı yükleyin; uygulamanın çökmediğini, bağlantıların/oynatıcıların çalışabildiğini ve kategori sayaçlarının gerçekten bloklama raporu geldikçe değiştiğini doğrulayın.
3. Bilerek korumayı kapattığınız sitenin normal sekmelerde aynı tercihle açıldığını, başka siteleri etkilemediğini kontrol edin.
4. Gizli sekmede site korumasını kapatın; son gizli sekme kapatılıp yenisi açıldığında bu tercihin silindiğini doğrulayın.
5. Global korumayı kapatın/açın; kullanıcı tarafından belirlenmiş site istisnaları korunmalı.
6. Uygulamayı yeniden başlatın; Standart/Sıkı tercihinin korunduğunu doğrulayın. Normal gezinme geçmişi ve sekmeler kalırken gizli URL ve sayaç verileri kalıcı kayda yazılmamalı.
7. Android 8–16, YouTube tam ekran, DRM, indirmeler, ekran döndürme, üst çentik ve durum çubuğu uyumluluğu için fiziksel cihaz regresyonu gereklidir.

## Derleme / doğrulama

`python tools/generate.py --check`, `python tools/check_stage2_features.py`, `.github/workflows/phase0.yml` ve Android 15/16 görsel denetimi. Üretim yayını ayrıca orijinal imza, sürüm kodu ve mevcut v1.0.8 APK üzerine güncelleme uyumluluğu kontrollerini geçmelidir.
