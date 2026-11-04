# Android ekranları — onaylı görselle eşleştirme

Durum: 2026-11-04 — **otomatik karşılaştırma altyapısı eklendi, tasarım kabulü verilmedi.**

## Sabit kaynaklar

Telefon için tek doğru referans `design/ekranlar/T01-Acilis.png` ile `T11-Gunduz.png` arasındaki 780×1688 PNG'ler ve aynı kodlu `design/kaynak/T01-...` HTML kaynaklarıdır. Bu kaynaklar kullanıcı onayı olmadan değiştirilmez. Android Compose arayüzünün kaynaklara uyduğu varsayılmaz.

## Karşılaştırma yöntemi

`tools/compare_approved_screenshots.py` gerçek ekran görüntüsünü ve sabit PNG'yi okur; gerekirse **yalnız karşılaştırma için** 780×1688'e ölçekler ve her ekran için üçlü (referans, uygulama, fark) PNG ve JSON raporu üretir. Özgün yakalama boyutu ile referans boyutu da ayrıca kaydedilir. RGB hata ortalaması ve fark eşiğini aşan piksel oranı hesaplanır. Bu araç yalnız **görsel tanı/karşılaştırma aracıdır**; eşikler uygulanmış, kullanılabilirlik ve canlı işlev kabulü anlamına gelmez.

İş akışı `.github/workflows/android-visual-audit.yml`, uygulamanın x86_64 **debug** APK'sını bir Android 35 Google APIs emülatörüne yükleyip ilk kurulum (T02) ve başlangıç ekranı (T03) için ekran görüntüsü yakalamayı dener; [Actions](https://github.com/mmustafatasoy/EymenBrowser/actions) altında `EymenBrowser-Android-Real-UI-Visual-Audit` adlı karşılaştırma artefaktını oluşturur. İlk çalıştırma tamamlandıktan sonra rapor okunmalı ve yerleşim farklarının ne olduğuna bakılmalıdır. **Bu workflow üretim imzası/Release değildir**.

## Kabul için gerekenler

- T01–T11 için normal ve büyük yazı sistem ayarlarında gerçek cihaz ekran görüntüleri, cihaz boyutu/density ve tam ekran/sistem çubuğu güvenli alanları kaydedilir.
- Türkçe/İngilizce taşmalar, sistem çubuğu, kaydırma, 44dp ve 48dp erişim alanları, klavye açılması, dik/yatay tam ekran incelenir.
- T03/T04 canlı web sayfasının içeriği referanstaki örnek içerikle **birebir aynı olamaz**; karşılaştırma tarayıcı kaplama arayüzü ve sabit öğelerde yapılır. Rastgele internet içeriği test sonucunu yanlış göstermemelidir.
- Özel oturum, giriş yapılmış banka ekranı, şifre, hesap kodu, kişisel dosya, iletişim bilgisi ve gizli iş verisi içeren ekran görüntüsü **rapor artefaktlarına koyulmaz**.
- Tasarıma aykırı işlevi düzeltmek için onaylı görsel değiştirilmez; Compose kodu ve token kullanımı düzeltilir.
- Her ekran ve gerçek cihaz matrisi geçmeden `tam eşleşti`, `kararlı`, `release imzalı` denmez.

## Sonraki işler

Önce T02/T03 emulator çıktılarını incele ve görülen farkları düzelt. Sonra T01 ve T04–T11 için giriş/ekran yakalama otomasyonunu kademeli ekle. Fiziksel Android 8/11/14/16 testleri ayrıca kullanıcı cihazında veya uygun test laboratuvarında yapılmalıdır.
