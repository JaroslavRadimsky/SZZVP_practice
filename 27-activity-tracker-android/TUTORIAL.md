# Tutorial - Android Activity Tracker

## Cil ulohy

Cilem je vytvorit mobilni aplikaci, ktera umi zahajit a ukoncit mereni fyzicke aktivity, pravidelne uklada data ze senzoru, drzi je v lokalni databazi a zobrazuje seznam, detail a zakladni statistiky.

Projekt je psany v Jave a je pripraveny pro Android Studio 4.0.1.

## Teoreticky zaklad

### Activity

`Activity` reprezentuje jednu obrazovku aplikace. Projekt ma dve obrazovky:

- `MainActivity` pro mereni a seznam,
- `MeasurementDetailActivity` pro detail, graf a CSV export.

### SensorManager

Android poskytuje pristup k pohybovym senzoram pres `SensorManager`. Aplikace pouziva:

- `TYPE_STEP_COUNTER` pro skutecny pocet kroku, pokud je dostupny,
- `TYPE_LINEAR_ACCELERATION` nebo `TYPE_ACCELEROMETER` pro orientacni intenzitu pohybu.

### SQLite

SQLite je lokalni databaze primo v Androidu. Trida `ActivityDatabase` dedi z `SQLiteOpenHelper` a vytvari tabulky `measurements` a `samples`.

### Intent

`Intent` se pouziva pro otevreni detailu a pro sdileni CSV. Detail dostane pouze `measurementId` a data si docte z databaze.

## Postup reseni

1. Zalozit Android projekt s Gradle pluginem 4.0.1.
2. Pridat manifest s hlavni aktivitou, detailni aktivitou a opravnenim `ACTIVITY_RECOGNITION`.
3. Vytvorit modely `ActivityRecord` a `ActivitySample`.
4. Implementovat `ActivityDatabase` se dvema tabulkami.
5. Pridat hlavni layout se start/stop tlacitky, stavem mereni a seznamem.
6. V `MainActivity` ziskat senzory pres `SensorManager`.
7. Pri stisku `Zahajit` zalozit mereni v databazi a registrovat posluchace senzoru.
8. Kazdych 5 sekund ulozit vzorek: cas, kroky nebo odhad kroku a intenzitu.
9. Pri stisku `Ukoncit` ulozit posledni vzorek, doplnit cas konce a prepocitat souhrn.
10. Vytvorit adapter pro seznam mereni.
11. V detailu zobrazit souhrn, graf intenzity a vzorky.
12. Pridat sdileni CSV pres `ACTION_SEND`.
13. Doplnit unit test pro vypocet statistik.

## Mapovani na pozadavky

- Zahajit a ukoncit mereni: `MainActivity.startMeasurement()` a `stopMeasurement()`.
- Pravidelne ukladani casu a dat ze senzoru: `sampleRunnable` a `storeSample()`.
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
3. klepnout na `Zahajit`,
4. nekolik sekund se hybat,
5. klepnout na `Ukoncit`,
6. otevrit detail a zkontrolovat statistiky, graf a CSV sdileni.
