# FitFlow Pro

Kişisel antrenman takip uygulaması — Kotlin + Jetpack Compose + Room.

Eski FitFlow'un yeniden yazılmış hâli. En büyük fark: **artık her set ayrı ayrı kaydediliyor.**
Eskiden ağırlık ve tekrar bilgisi serbest metindi (`"8 kg / el"`, `"11"`) ve geçmiş tek bir
`@@@` ile ayrılmış string'e sıkıştırılıyordu; bu yüzden gerçek bir analiz yapmak mümkün değildi.
Yeni veri modelinde ağırlık `Float`, tekrar `Int`, RPE `Float` olarak tutuluyor — hacim, tahmini
1RM, kas grubu dengesi ve rekor tespiti bu veriden hesaplanıyor.

---

## Kurulum

**Android Studio ile:**

1. Android Studio'yu aç → **Open** → bu klasörü seç.
2. Gradle senkronizasyonunun bitmesini bekle (ilk sefer birkaç dakika sürebilir).
3. Yayınlanabilir sürüm alacaksan `app/build.gradle.kts` içindeki
   `signingConfig = signingConfigs.getByName("debugConfig")` satırını sil.
4. Emülatörde veya telefonda çalıştır.

**AI Studio ile:** Klasörü GitHub'a yükle, AI Studio'da depoyu içe aktar.
Uygulama Gemini API kullanmıyor, bu yüzden `.env` / API anahtarı zorunlu değil.

Gereksinimler: `minSdk 24`, `targetSdk 36`, Java 11.

---

## Eski verinin aktarılması

Uygulama ilk açılışta cihazdaki eski `workout_database` dosyasını arar. Bulursa
günlerini, hareketlerini, tamamlanan seanslarını ve rekorlarını yeni şemaya aktarır
(`data/LegacyImporter.kt`). Serbest metindeki sayılar (`"8 kg / el"` → `8.0`) regex ile
çıkarılır; birebir doğruluk garanti edilmez ama geçmiş kaybolmaz. Aktarım başarısız
olursa sessizce atlanır ve varsayılan 3 günlük program kurulur.

> Eski uygulamayı silersen veritabanı da silinir. Aktarımın çalışması için yeni
> uygulamayı **eski uygulama hâlâ kuruluyken aynı paket adıyla** güncelleme olarak yüklemen gerekir.
> Aksi hâlde Profil → *Verimi dışa aktar* ile aldığın JSON yedeği kullanabilirsin.

---

## Ekranlar

| Ekran | İçerik |
|---|---|
| **Yan menü** | Sol üstteki ☰ butonu — tema (koyu/açık/sistem), vurgu rengi, AMOLED, dinlenme sayacı, telefondan zil sesi seçme + önizleme, ses/titreşim, bölüm kısayolları, serbest antrenman, yedekleme |
| **Bugün** | Günün antrenmanı / devam eden seans, haftalık hedef şeridi, özet kartlar, hacim trendi, kas dengesi, son antrenmanlar, hızlı erişim |
| **Program** | Birden fazla program, gün kartları, gün düzenleyici (set/tekrar aralığı/ağırlık/dinlenme/ısınma işareti, sıralama) |
| **Aktif seans** | Set bazlı kayıt tablosu, önceki seans değerleri, RPE, ısınma seti işaretleme, progresif yüklenme önerisi, dinlenme sayacı, bitirme özeti |
| **İlerleme** | Haftalık/aylık trend (hacim, set, seans, süre), 12 haftalık aktivite ısı haritası, kas grubu dağılımı (halka + çubuk), 1RM sıralaması, rekor listesi |
| **Hareket** | 150+ hazır hareket, arama/filtre, favoriler, hareket detayı (teknik, video, gelişim grafiği, seans geçmişi), özel hareket ekleme |
| **Profil** | Profil bilgileri, VKİ, kısayollar, JSON yedekleme |
| **Geçmiş** | Aya göre gruplanmış seanslar, arama, seans detayı, metin olarak paylaşma |
| **Ölçümler** | Kilo/yağ/çevre ölçümü takibi ve grafikleri |
| **Notlar** | Kategorili, renkli, sabitlenebilir notlar |
| **Araçlar** | 1RM hesaplayıcı + yüzde tablosu, plaka hesaplayıcı, ısınma piramidi, VKİ/BMR/TDEE |
| **Ayarlar** | Tema (koyu/açık/sistem), 10 vurgu rengi, AMOLED, yazı boyutu, dinlenme sayacı, alarm sesi, titreşim, bar ağırlığı, artış adımı, uygulama kilidi |

---

## Öne çıkan mantık

**Progresif yüklenme önerisi** (`Calc.suggestNextWeight`)
Son seanstaki tekrarlar ve RPE'ye bakar:
- Tüm setlerde hedef aralığın üstü + RPE düşük → ağırlığı artır
- Hedefe ulaşıldı ama RPE ≥ 9 → yarım adım artır
- Hedefin altında + zorlanmış → ağırlığı düşür
- Arada → aynı ağırlıkta kal, tekrar sayısını yükselt

**Tahmini 1RM** — Epley, Brzycki, Lombardi ve O'Conner formüllerinin ortalaması.
Tek tekrarda ağırlığın kendisi, 20 tekrarın üstünde yalnızca Epley kullanılır.

**Rekor tespiti** — Seans bitince her hareket için en ağır set, en yüksek tahmini 1RM
ve seans hacmi geçmişle karşılaştırılır; kırılan rekorlar kutlama ekranında gösterilir.

**Kas dengesi** — Haftalık set sayısı kas grubu bazında toplanır ve yaygın hipertrofi
aralıklarıyla (`Muscles.weeklyTarget`) karşılaştırılır.

---

## Proje yapısı

```
app/src/main/java/com/example/
├── MainActivity.kt              Giriş noktası, tema bağlama
├── core/
│   ├── Calc.kt                  1RM, plaka, ısınma, VKİ/BMR, öneri motoru, biçimlendirme
│   └── Analytics.kt             Özet metrikler, dönem serileri, ısı haritası, dağılım
├── data/
│   ├── Entities.kt              Room varlıkları (9 tablo)
│   ├── FitDao.kt / FitDatabase.kt
│   ├── FitRepository.kt         Tek veri giriş noktası, seans akışı, rekor hesabı
│   ├── ExerciseSeed.kt          Hazır hareket kütüphanesi
│   ├── LegacyImporter.kt        Eski veritabanından aktarım
│   └── SettingsStore.kt         Ayarlar (SharedPreferences + StateFlow)
└── ui/
    ├── AppViewModel.kt          Durum, dinlenme sayacı, ses/titreşim
    ├── FitFlowApp.kt            Navigasyon, alt menü, sayaç çubuğu, kilit ekranı
    ├── Backup.kt                JSON dışa aktarma
    ├── theme/Theme.kt           Renk sistemi, tipografi, vurgu rengi
    ├── components/              Tasarım sistemi + Canvas tabanlı grafikler
    └── screens/                 Ekranlar
```

Veri tabanı sürümü 1, adı `fitflow_pro.db`. Eski `workout_database` dosyasına
yalnızca okuma amacıyla dokunulur, silinmez.

---

## Uygulama ikonu

Özel tasarım — hazır Android şablonu kullanılmadı.

- **İşaret:** camgöbeği degradeli, 35 derece yukarı eğimli halter silueti (güç + ilerleme)
- **Zemin:** derin gece mavisi degrade, üst solda camgöbeği, alt sağda lacivert parıltı
- `drawable/ic_launcher_background.xml` + `ic_launcher_foreground.xml` — Android 8.0+ uyarlanabilir ikon (her launcher maskesine uyar)
- `drawable/ic_launcher_monochrome.xml` — Android 13+ temalı ikon katmanı
- `mipmap-*/ic_launcher.webp` ve `ic_launcher_round.webp` — Android 7.x için 48/72/96/144/192 px kayıpsız bitmap

İşaretin geometrisi 108 birimlik alanda 66dp güvenli bölge içinde kalır; dairesel maskede
kırpılmaz. Rengi değiştirmek istersen `ic_launcher_foreground.xml` içindeki üç `<item android:color>`
değerini düzenlemen yeterli.

---

## Kas haritası ve gelişmiş analizler (bu sürümde eklendi)

### Anatomik kas haritası

`ui/components/BodyMap.kt` + `BodyData.kt` — ön ve arka vücut, 18 ayrı kas
bölgesi olarak Compose Canvas üzerinde çizilir. **Uygulamaya hiçbir görsel
dosyası eklenmez**; her şey vektörel çizildiği için APK büyümez ve her ekran
yoğunluğunda net kalır.

Geometri, anatomik bir segmentasyon görselinden piksel bazlı iz sürülerek
çıkarıldı: her kas ayrı düz renkle boyanmış referans görsel Lab renk uzayında
18 bölgeye sınıflandırılıp konturları çıkarıldı ve 114 x 210 birimlik normalize
alana taşındı.

Çizim dört katmanda yapılır — düz renk yerine derinlik veren bu sıralama:
gövde silueti (dikey degrade + dış hat), çalışılmayan kaslar (siluetten bir ton
açık), vurgulanan kaslar (üstten aydınlık alta koyu degrade), kas sınırları
(arka plan renginde ince oluk). Seçili kas parlak hatla çerçevelenir.

Doğrulama: sol-sağ simetri örtüşmesi 0.90–0.99, ön/arka siluet örtüşmesi 0.96,
siluet dışına taşma %0.3, kollar gövdeden ve bacaklar birbirinden ayrık,
2015 kontur noktası.

Geometriyi değiştirmek istersen `BodyData.kt` elle düzenlenmemeli — yeni bir
segmentasyon görselinden yeniden üretilmelidir.

Kullanıldığı yerler:

| Ekran | Ne gösteriyor |
|---|---|
| **Hareket detayı** | O hareketin çalıştırdığı kaslar — birincil kaslar tam renkte, destek kaslar soluk |
| **İlerleme → Kaslar** | Haftalık etkin set sayısının önerilen aralığa göre durumu (az / ideal / yüksek / aşırı). Kasa dokununca detay kartı açılır |
| **Ana sayfa** | Küçük harita + "hazır" ve "toparlanıyor" kas listesi |

### Detaylı kas taksonomisi

`core/MuscleMap.kt` — ana kas grubu (Göğüs / Sırt / Bacak / Omuz / Kol / Karın)
analiz için fazla kabaydı. Artık 18 bölge var: göğüs, ön/yan/arka omuz, biceps,
triceps, önkol, kanat, trapez, üst sırt, bel, karın, yan karın, kalça, ön bacak,
arka bacak, baldır, iç bacak.

Kütüphanedeki 114 hareketin tamamı elle birincil/ikincil kaslara eşlendi;
kullanıcının eklediği özel hareketler ad ve kas grubundan tahmin edilir.

**Kesirli set sayımı:** birincil kas 1.0 set, ikincil kas 0.5 set alır. Bench
press'i "sadece göğüs" saymak triceps hacmini görünmez yapıyordu; artık
bench press 4 set = göğüs 4.0, triceps 2.0, ön omuz 2.0 set.

### İlerleme ekranı — dört sekme

**Genel**
- Bu hafta / geçen hafta karşılaştırması (hacim, seans, set)
- **Yüklenme dengesi (akut : kronik oran)** — son 7 günün hacmi, son 4 haftanın
  haftalık ortalamasına bölünür. 0.8–1.3 sürdürülebilir, 1.5 üstü hızlı artış
  uyarısı verir. Sakatlık riski yönetiminde kullanılan yaklaşımdır
- Hacim / set / seans / süre trendi (haftalık ve aylık, geçmişe dönük kayıtlar dahil)
- **Tutarlılık** — son 8 haftada haftalık hedefe uyum yüzdesi ve hafta hafta çubuk
- Aktivite ısı haritası
- **Tekrar aralığı dağılımı** — 1-5 / 6-8 / 9-12 / 13-20 / 20+ bantlarına düşen
  set payı ve her bandın amacı
- **RPE eğilimi** — haftalık ortalama zorlanma; sürekli 9+ ise uyarır
- **İlerleme kaydettiklerin / takılan hareketler** — son seansta rekor kıranlar ve
  3+ haftadır rekor kırılmayan hareketler, ne yapılacağına dair öneriyle

**Kaslar**
- Kas haritası (bu hafta veya son 4 haftanın haftalık ortalaması)
- Seçilen kas için: etkin set, hedef aralık, son çalışma zamanı, öneri
- **Zayıf halkalar** — önerilen aralığın altında kalan kaslar
- Kas bazlı hacim dökümü (hedefe göre çubuk + durum etiketi)
- **Toparlanma durumu** — kas kas "hazır" / "N gün daha"

**Güç**
- Ana beş harekette (Squat, Bench, Deadlift, OHP, Row) vücut ağırlığına göre
  seviye: Başlangıç → Acemi → Orta → İleri → Çok İleri → Elit; bir üst seviyeye
  kaç kilo kaldığı. Cinsiyete göre eşik düzeltmesi yapılır
- Squat + Bench + Deadlift toplamı ve vücut ağırlığı katı
- **Lift dengesi** — squat'a göre beklenen oranlarla karşılaştırma, en zayıf
  halkanın tespiti
- Seçilen hareket için tahmini 1RM gelişim grafiği ve son 4 seans eğilimi
- Tahmini 1RM sıralaması

**Rekor**
- Türe göre filtrelenebilir rekor geçmişi (ağırlık / 1RM / hacim)

### Hareket detayına eklenenler

- Kas haritası + birincil/destek kas listesi
- Kütüphane listesinde artık "Göğüs, Ön omuz · Triceps" gibi detaylı özet
- **Tekrar aralığına göre en iyi setler** — 1-5, 6-8, 9-12, 13+ bantlarında ayrı
  ayrı en iyi performans
- Son 4 seans eğilimi (yüzde)

### Testler

`app/src/test/` altında 22 birim testi var. En kritik olanı:
kütüphanedeki her hareketin bir kasa eşlendiğini doğrular — `ExerciseSeed`'e yeni
hareket eklendiğinde `MuscleMap` tablosunu güncellemeyi unutmayı yakalar.

`./gradlew test` ile çalıştırabilirsin.

---

## Notlar

- Tüm veri cihazda kalır; internet izni yoktur.
- `VIBRATE` ve `WAKE_LOCK` dışında izin istenmez.
- Uygulama tamamen Türkçedir.
- Güç seviyesi sınıflandırması ve haftalık set aralıkları kaba referanslardır, kesin standart değildir.
