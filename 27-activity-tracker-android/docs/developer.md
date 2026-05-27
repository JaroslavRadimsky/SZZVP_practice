# Vyvojarska dokumentace

## Architektura

Aplikace navazuje na jednoduche vrstveni z projektu `05-rss-reader-android`:

- `MainActivity` zobrazuje historii mereni a spousti samostatnou obrazovku mereni.
- `TrackingActivity` bezi po dobu aktivity, cte senzory, uklada vzorky a zobrazuje notifikaci.
- `MeasurementDetailActivity` zobrazuje detail jednoho mereni, graf a export CSV.
- `MeasurementAdapter` prevadi zaznamy z databaze na radky v `ListView`.
- `ActivityDatabase` uklada mereni a vzorky do SQLite pres `SQLiteOpenHelper`.
- `ActivityRecord` reprezentuje souhrn mereni.
- `ActivitySample` reprezentuje jeden casovy vzorek.
- `ActivityStatsCalculator` obsahuje testovatelnou logiku pro souhrny, delku a popis intenzity.
- `IntensityGraphView` kresli jednoduchy graf intenzity bez externi knihovny.

## Datovy model

Tabulka `measurements`:

- `id` - primarni klic,
- `started_at` - cas zahajeni v milisekundach,
- `ended_at` - cas ukonceni v milisekundach, muze byt prazdny,
- `total_steps` - soucet ulozenych kroku,
- `average_intensity` - prumerna intenzita ze vzorku,
- `sample_count` - pocet ulozenych vzorku.

Tabulka `samples`:

- `id` - primarni klic,
- `measurement_id` - odkaz na mereni,
- `measured_at` - cas vzorku v milisekundach,
- `steps` - prirustek kroku od posledniho vzorku,
- `intensity` - orientacni intenzita za interval.

Po kazdem vlozeni vzorku se aktualizuje souhrn v tabulce `measurements`.

## Senzory

Aplikace preferuje `Sensor.TYPE_STEP_COUNTER`, pokud je dostupny a aplikace ma opravneni `ACTIVITY_RECOGNITION`. Intenzita se meri z `TYPE_LINEAR_ACCELERATION`, pripadne z `TYPE_ACCELEROMETER`.

Pokud krokomer neni dostupny, aplikace odhaduje kroky z vyraznych pohybovych spic. Tento odhad je orientacni, ale splnuje pozadavek na relevantni pohybova data ze senzoru.

## Persistencni chovani

Mereni se zalozi v databazi po otevreni `TrackingActivity`. Vzorky se ukladaji pravidelne v intervalu 5 sekund. UI casovac se aktualizuje nezavisle kazdou sekundu, aby uzivatel videl plynule bezici cas. Pri ukonceni se ulozi posledni vzorek, mereni se oznaci casem konce a znovu se prepocitaji statistiky.

## Notifikace

Behem mereni `TrackingActivity` vytvori ongoing notifikaci `Aktivita probiha`. Na Androidu 8 a novejsim se pouziva notification channel `active_measurement`. Notifikace se aktualizuje spolu s casovacem a zrusi se po ukonceni mereni.

## Export

Export pouziva standardni Android `ACTION_SEND`. CSV se posila jako text v `Intent.EXTRA_TEXT`, takze neni potreba `FileProvider` ani zapis do externiho uloziste.

## Testovani

Unit test `ActivityStatsCalculatorTest` overuje vypocet statistik, format delky aktivity a slovni popis intenzity. Test je bez Android UI zavislosti.
