# EYMEN Browser v1.0.8 — Kalıcı normal sekmeler, önizleme ve geri alma

## Kapsam

Bu Android telefon sürümü, onaylanmış açık temayı ve sistem çentiği/durum çubuğu güvenli alanını değiştirmeden sekme yönetimini geliştirir.

- **Kalıcı normal sekmeler:** Uygulama durduktan veya cihaz yeniden açıldıktan sonra açık normal sekmelerin HTTPS adresleri, sıralaması, seçili sekme ve sekme grupları yeniden kurulur.
- **Hızlı ve kontrollü açılış:** En fazla 32 normal sekmenin yalnız seçili olanı hemen yüklenir; arka plan sekmeleri ilk seçildiklerinde yüklenir. Bu bellek ve ağ tüketimini düşürür.
- **Kalıcı grup bilgisi:** En fazla 20 normal sekme grubu, 32 karakterlik ad ve grup üyelikleri saklanır; sekme grupları yalnız normal oturumlar içindir.
- **Gerçek sayfa önizlemesi:** GeckoView'un `capturePixels()` çıktısı küçük Bitmap'e dönüştürülür ve yalnız *normal* sekmelerde bellekte tutulur. Web sayfası render edilmediyse eski nötr yer tutucu görünür. Önizleme dosya olarak saklanmaz.
- **Çoklu geri alma:** Son kapatılmış normal sekmelerin en fazla 10 tanesi, aynı URL yeni bir GeckoSession'da yüklenerek sırayla geri açılabilir.
- **Sıralama:** Sekme kartlarındaki sola/sağa hareket kontrolleri mevcut sekmelerin sırasını değiştirir. Normal sekmelerin sırası kalıcı depolamaya kaydedilir.

## Gizlilik ve güvenlik

- **Gizli sekmelerin adresi, grup bilgisi veya görseli asla `TabWorkspaceStore`a yazılmaz.** Gizli sekmelerin geri alma geçmişi de yoktur.
- Yalnız doğrulanmış `https:` URL'leri kabul edilir. URL'lerde kullanıcı adı/parola bölümüne izin verilmez; 2048 karakter sınırı vardır. Bozulmuş kayıt uygulamayı kilitlemez.
- Kayıt `MODE_PRIVATE` kapsamındaki Android uygulama tercihlerindedir. Harici sunucuya aktarım veya yeni telemetri yoktur.
- Önizleme her sekmede ancak sayfa gerçekten render edilmişse alınır; geçici Bitmap işlenerek RAM'de tutulur. Gizli modda ekran görüntüsü alınmaz.
- Kalıcı kayıt **yalnız URL tabanlı yeniden açma** yapar; Gecko'nun tam geri/ileri navigasyon ağacı, açık formlar, videonun oynatma pozisyonu veya oturum-cookie dökümü kopyalanmaz.
- Grup, sekme ve kapanan sekme sınırları kaynakta uygulanır; sınırsız bellek büyümesi ve aşırı kayıt boyutu engellenir.

## Test ve kabul

1. Üç normal HTTPS sekmesi açın, ikisini yeni gruba taşıyın, adını değiştirin; uygulamayı tamamen kapatıp tekrar açtığınızda sekmelerin, grubun ve sıranın korunduğunu denetleyin.
2. Bir normal sekmede adres değiştirin, tarayıcıyı durdurup açın; yeni adres geri gelmeli. Seçilmeyen arka plan sayfaları gereksiz yüklenmemeli.
3. Bir gizli sekme açın, HTTPS sayfası yükleyin, uygulamayı tamamen sonlandırıp açın; gizli sekme geri gelmemeli. Gizli URL normal geri alma listesinde bulunmamalı.
4. Üç normal sekmeyi kapatın ve art arda üç kez geri açın; on birinci en eski sekmenin tutulmadığını da kontrol edin.
5. Sekme kartlarından sıralamayı değiştirin, uygulamayı yeniden başlatın ve sırayı doğrulayın.
6. Gerçek sayfalarda gezinin, sekmeler arasında geçin ve kartlarda GeckoView'dan gelen önizlemeleri denetleyin. Ana sayfa, henüz yüklenmeyen sayfa ve gizli sekmelerde temsili içeriğe izin vermeyin.
7. Android 8/11/14/15/16 cihazlarında sistem çentiği, ekran dönüşü, DRM/YouTube, indirme, özel oturum, güncelleme ve büyük yazı kullanımını ayrı ayrı doğrulayın.

## Kaynaklar

- Kod: `android/app-telefon/src/main/java/com/eymen/browser/telefon/TabWorkspaceStore.kt`
- Entegrasyon: `android/app-telefon/src/main/java/com/eymen/browser/telefon/MainActivity.kt`
- Sekme kartları: `android/app-telefon/src/main/java/com/eymen/browser/telefon/TabOverview.kt`
- Statik regresyon: `python tools/check_tab_sessions.py`
- CI: `.github/workflows/phase0.yml` ve `.github/workflows/android-visual-audit.yml`

**Kabul sınırı:** Derlemenin veya başlangıç emülatör görüntüsünün başarılı olması, gerçek cihazlarda tüm 1–7 senaryolarının geçtiği anlamına gelmez. Fiziksel cihaz kabulü ayrıca gereklidir.
