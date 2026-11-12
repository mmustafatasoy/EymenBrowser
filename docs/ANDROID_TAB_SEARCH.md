# Android açık sekmelerde arama — özellik 4

Şartnamedeki özellik 4 ve Android Aşama 2/T06 kapsamında yalnız açık sekmeler
aranır. Mevcut normal/gizli GeckoSession'lar, gruplar, geri alma ve kalıcı normal
sekme kaydı korunur. [Önceki sekme sözleşmesi](ANDROID_TABS_V108.md).

- Sekmeler ekranındaki mevcut ortak arama bileşeni, başlık veya tam adreste
  büyük/küçük harften bağımsız eşleşme yapar. Türkçe harfler ve eşdeğer Unicode
  yazımları desteklenir; sorgu 256 UTF-16 birimiyle sınırlıdır.
- Önce normal/gizli bölüm ve mevcut normal grup seçilir; arama yalnız bu
  kapsamda yapılır. Sonuç sayısı diğer bölümdeki sekmeleri saymaz. Cihazlar
  bölümüne arama veya bulut eşitlemesi eklenmez.
- Arama metni yalnız RAM'deki Compose state'idir; kalıcı kayda, geçmişe,
  geri alma listesine, URL'ye veya bir ağa gönderilmez. Bölüm değişince ve
  ekran kapatılıp yeniden açılınca sorgu ve editör durumu sıfırlanır.
- Bir sonuca dokunmak var olan gerçek sekmeyi etkinleştirir; yeni sekme veya
  yeni gezinme oluşturmaz. Klavye açıkken Geri önce klavyeyi kapatır; sonuçlar
  korunur. Aramayı temizleme mevcut bölüm/grup kapsamına geri döner.
- Hızlı geçiş yalnız eşleşen sekmeleri kullanır. Etkin sekme aramanın dışında
  kalırsa seçili konum 0 gösterilir; Sonraki ilk, Önceki son eşleşmeye geçer.
  Tek eşleşme de etkin sekmeden farklıysa açılabilir.
- Filtreli aramada Tümünü kapat pasiftir; görünmeyen sekmeler kapatılmaz.
  Tek tek kapatma, gerçek sekme sırası ve normal sekmelerin geri alınması
  önceki sözleşmeyi izler.
- TR/EN metinleri ortak i18n kaynağındadır. Yeni renk/ölçü/ikon değeri,
  bağımlılık, motor, hesap veya telemetri eklenmez; tasarım kaynakları değişmez.

## Kabul

Yedi JVM testi metin/adres, Türkçe büyük/küçük harf, Unicode, boş sorgu ve
girdi sınırını sınar. Tam Android UI testine gerçek HTTPS normal/gizli sayfalar,
gerçek klavyeyle arama, sonuç izolasyonu/sayısı, boş sonuç, filtreli toplu
kapatmanın etkisizliği, mevcut oturuma dönüş ve sorgu sıfırlama eklenir.
Açık/koyu dört yeni PNG zorunludur; önceki kabuller silinmez. Yeni adımlar için
instrumentation 900 s, host betiği 1080 s sınırında kalır.

Yerel kaynak/i18n/şartname, 11 regresyon denetimi ve kozmetik JS kontrolü geçti.
JVM/üç ABI, Android 15/16 tam UI/görsel ve indirme CI sonuçları henüz denenmedi.
Fiziksel Android 8/11/14/16, gerçek cihazda
güncelleme ve VPN saha kabulü ayrıca açıktır. Son kararlı sürüm v1.0.23'tür;
bu özellik henüz yayımlanmamıştır.
