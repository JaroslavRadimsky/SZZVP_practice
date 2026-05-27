# Tutorial - Android Activity Tracker

## Cil ulohy

Cilem je vytvorit mobilni aplikaci, ktera umi zahajit a ukoncit mereni fyzicke aktivity, pravidelne uklada data ze senzoru, drzi je v lokalni databazi a zobrazuje seznam, detail a zakladni statistiky.

Projekt je psany v Jave a je pripraveny pro Android Studio 4.0.1.

## Teoreticky zaklad

### Activity

`Activity` reprezentuje jednu obrazovku aplikace. Projekt ma tri obrazovky:

- `MainActivity` pro historii a spusteni mereni,
- `TrackingActivity` pro samotne probihajici mereni,
- `MeasurementDetailActivity` pro detail, graf a CSV export.

### SensorManager

Android poskytuje pristup k pohybovym senzoram pres `SensorManager`. Aplikace pouziva:

- `TYPE_STEP_COUNTER` pro skutecny pocet kroku, pokud je dostupny,
- `TYPE_LINEAR_ACCELERATION` nebo `TYPE_ACCELEROMETER` pro orientacni intenzitu pohybu.

### SQLite

SQLite je lokalni databaze primo v Androidu. Trida `ActivityDatabase` dedi z `SQLiteOpenHelper` a vytvari tabulky `measurements` a `samples`.

### Intent

`Intent` se pouziva pro otevreni mereni, otevreni detailu a pro sdileni CSV. Detail dostane pouze `measurementId` a data si docte z databaze.

### Notification

Android notifikace se pouziva jako viditelny indikator, ze aktivita prave probiha. Na Androidu 8 a novejsim je nutne pred zobrazenim vytvorit `NotificationChannel`.

## Postup reseni

1. Zalozit Android projekt s Gradle pluginem 4.0.1.
2. Pridat manifest s hlavni aktivitou, detailni aktivitou a opravnenim `ACTIVITY_RECOGNITION`.
3. Vytvorit modely `ActivityRecord` a `ActivitySample`.
4. Implementovat `ActivityDatabase` se dvema tabulkami.
5. Pridat hlavni layout se seznamem a tlacitkem `Zahajit aktivitu` dole.
6. Pridat `TrackingActivity` se samostatnou obrazovkou mereni.
7. V `TrackingActivity` ziskat senzory pres `SensorManager`.
8. Pri otevreni `TrackingActivity` zalozit mereni v databazi a registrovat posluchace senzoru.
9. Kazdych 5 sekund ulozit vzorek: cas, kroky nebo odhad kroku a intenzitu.
10. Kazdou sekundu aktualizovat casovac na obrazovce a v notifikaci.
11. Pri stisku `Ukoncit a ulozit` ulozit posledni vzorek, doplnit cas konce a prepocitat souhrn.
12. Vytvorit adapter pro seznam mereni.
13. V detailu zobrazit souhrn, graf intenzity a vzorky.
14. Pridat sdileni CSV pres `ACTION_SEND`.
15. Doplnit unit test pro vypocet statistik.

## Mapovani na pozadavky

- Zahajit a ukoncit mereni: `MainActivity` otevre `TrackingActivity`, ukonceni resi `TrackingActivity.stopMeasurementAndFinish()`.
- Pravidelne ukladani casu a dat ze senzoru: `TrackingActivity.sampleRunnable` a `storeSample()`.
- Plynule bezici cas: `TrackingActivity.timerRunnable`.
- Notifikace v liste: `TrackingActivity.showOrUpdateNotification()`.
- Persistentni databaze: `ActivityDatabase`.
- Seznam mereni: `ListView` s `MeasurementAdapter`.
- Detail mereni: `MeasurementDetailActivity`.
- Statistika delky, intenzity a kroku: `ActivityRecord` a `ActivityStatsCalculator`.
- Graf: `IntensityGraphView`.
- Export CSV: `MeasurementDetailActivity.shareCsv()`.

## Overeni

Spustte:

```powershell
.\gradlew.bat test assembleDebug
```

V emulatoru nebo telefonu:

1. spustit aplikaci,
2. povolit rozpoznavani aktivity,
3. klepnout na `Zahajit aktivitu`,
4. nekolik sekund se hybat,
5. overit, ze cas bezi po sekundach a v liste je notifikace,
6. klepnout na `Ukoncit a ulozit`,
7. otevrit detail a zkontrolovat statistiky, graf a CSV sdileni.
