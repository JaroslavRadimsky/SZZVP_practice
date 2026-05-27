# Tutorial - Android Activity Tracker

## Cíl úlohy

Cílem je vytvořit mobilní aplikaci, která umí zahájit a ukončit měření fyzické aktivity, pravidelně ukládá data ze senzoru, drží je v lokální databázi a zobrazuje seznam, detail a základní statistiky.

Projekt je psaný v Javě a je připravený pro Android Studio 4.0.1.

## Teoretický základ

### Activity

`Activity` reprezentuje jednu obrazovku aplikace. Projekt má tři obrazovky:

- `MainActivity` pro historii a spuštění měření,
- `TrackingActivity` pro samotné probíhající měření,
- `MeasurementDetailActivity` pro detail, graf a CSV export.

### SensorManager

Android poskytuje přístup k pohybovým senzorům přes `SensorManager`. Aplikace používá:

- `TYPE_STEP_COUNTER` pro skutečný počet kroků, pokud je dostupný,
- `TYPE_LINEAR_ACCELERATION` nebo `TYPE_ACCELEROMETER` pro orientační intenzitu pohybu.

### LocationManager

GPS vzdálenost se měří přes `LocationManager`. Aplikace poslouchá GPS a síťové polohy, filtruje nepřesné body a pomocí `Location.distanceTo()` sčítá přírůstky vzdálenosti mezi polohami.

### SQLite

SQLite je lokální databáze přímo v Androidu. Třída `ActivityDatabase` dědí z `SQLiteOpenHelper` a vytváří tabulky `measurements` a `samples`.

### Intent

`Intent` se používá pro otevření měření, otevření detailu a pro sdílení CSV. Detail dostane pouze `measurementId` a data si dočte z databáze.

### Notification

Android notifikace se používá jako viditelný indikátor, že aktivita právě probíhá. Na Androidu 8 a novějším je nutné před zobrazením vytvořit `NotificationChannel`.

## Postup řešení

1. Založit Android projekt s Gradle pluginem 4.0.1.
2. Přidat manifest s hlavní aktivitou, detailní aktivitou a oprávněními `ACTIVITY_RECOGNITION` a `ACCESS_FINE_LOCATION`.
3. Vytvořit modely `ActivityRecord` a `ActivitySample`.
4. Implementovat `ActivityDatabase` se dvěma tabulkami.
5. Přidat hlavní layout se seznamem a tlačítkem `Zahajit aktivitu` dole.
6. Přidat `TrackingActivity` se samostatnou obrazovkou měření.
7. V `TrackingActivity` získat senzory přes `SensorManager` a polohu přes `LocationManager`.
8. Při otevření `TrackingActivity` založit měření v databázi a registrovat posluchače senzoru a GPS.
9. Každých 5 sekund uložit vzorek: čas, kroky nebo odhad kroků, intenzitu, souřadnice, přírůstek vzdálenosti, rychlost a tempo.
10. Každou sekundu aktualizovat časovač na obrazovce a v notifikaci.
11. Při stisku `Ukoncit a ulozit` uložit poslední vzorek, doplnit čas konce a přepočítat souhrn.
12. Vytvořit adapter pro seznam měření.
13. V detailu zobrazit souhrn a přepínatelný graf bez dlouhého seznamu vzorků.
14. Přidat sdílení CSV přes `ACTION_SEND`.
15. Přidat mazání měření z historie.
16. Doplnit unit test pro výpočet statistik.

## Mapování na požadavky

- Zahájit a ukončit měření: `MainActivity` otevře `TrackingActivity`, ukončení řeší `TrackingActivity.stopMeasurementAndFinish()`.
- Pravidelné ukládání času a dat ze senzoru: `TrackingActivity.sampleRunnable` a `storeSample()`.
- Plynule běžící čas: `TrackingActivity.timerRunnable`.
- Notifikace v liště: `TrackingActivity.showOrUpdateNotification()`.
- GPS vzdálenost: `TrackingActivity.onLocationChanged()`.
- Rychlost a tempo vzorku: `ActivityStatsCalculator.calculateSpeedKmh()` a `calculatePaceSecondsPerKm()`.
- Persistentní databáze: `ActivityDatabase`.
- Seznam měření: `ListView` s `MeasurementAdapter`.
- Detail měření: `MeasurementDetailActivity`.
- Statistika délky, intenzity, kroků a vzdálenosti: `ActivityRecord` a `ActivityStatsCalculator`.
- Přepínatelný graf: `IntensityGraphView`.
- Mazání měření: `ActivityDatabase.deleteMeasurement()`.
- Export CSV: `MeasurementDetailActivity.shareCsv()`.

## Ověření

Spusťte:

```powershell
.\gradlew.bat test assembleDebug
```

V emulátoru nebo telefonu:

1. spustit aplikaci,
2. povolit rozpoznávání aktivity a polohu,
3. klepnout na `Zahajit aktivitu`,
4. několik sekund se hýbat,
5. ověřit, že čas běží po sekundách, přibývá vzdálenost a v liště je notifikace,
6. klepnout na `Ukoncit a ulozit`,
7. otevřít detail a zkontrolovat statistiky, přepínání grafu a CSV sdílení,
8. smazat vybrané měření v historii.
