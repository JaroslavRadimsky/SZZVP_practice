# Vyvojarska dokumentace

## Architektura

Aplikace navazuje na jednoduche vrstveni z projektu `05-rss-reader-android`:

- `MainActivity` zobrazuje historii mereni a spousti samostatnou obrazovku mereni.
- `TrackingActivity` bezi po dobu aktivity, cte senzory a GPS, uklada vzorky a zobrazuje notifikaci.
- `MeasurementDetailActivity` zobrazuje detail jednoho mereni, prepinatelny graf a export CSV.
- `MeasurementAdapter` prevadi zaznamy z databaze na radky v `ListView` a predava pozadavek na smazani.
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
- `distance_meters` - soucet GPS vzdalenosti v metrech,
- `average_intensity` - prumerna intenzita ze vzorku,
- `sample_count` - pocet ulozenych vzorku.

Tabulka `samples`:

- `id` - primarni klic,
- `measurement_id` - odkaz na mereni,
- `measured_at` - cas vzorku v milisekundach,
- `steps` - prirustek kroku od posledniho vzorku,
- `intensity` - orientacni intenzita za interval.
- `latitude` - posledni znama sirka pri ulozeni vzorku,
- `longitude` - posledni znama delka pri ulozeni vzorku,
- `distance_meters` - prirustek GPS vzdalenosti od minuleho vzorku.
- `speed_kmh` - rychlost vzorku v kilometrech za hodinu,
- `pace_seconds_per_km` - tempo vzorku v sekundach na kilometr.

Po kazdem vlozeni vzorku se aktualizuje souhrn v tabulce `measurements`.

## Senzory

Aplikace preferuje `Sensor.TYPE_STEP_COUNTER`, pokud je dostupny a aplikace ma opravneni `ACTIVITY_RECOGNITION`. Intenzita se meri z `TYPE_LINEAR_ACCELERATION`, pripadne z `TYPE_ACCELEROMETER`.

Pokud krokomer neni dostupny, aplikace odhaduje kroky z vyraznych pohybovych spic. Tento odhad je orientacni, ale splnuje pozadavek na relevantni pohybova data ze senzoru.

## GPS vzdalenost

`TrackingActivity` pouziva `LocationManager` s providery `GPS_PROVIDER` a `NETWORK_PROVIDER`. Pri prvni pouzitelne poloze se ulozi referencni bod. Kazda dalsi poloha s presnosti do 50 metru prida vzdalenost od predchozi polohy, pokud prirustek neni nerealisticky velky. Pritom se prubezne aktualizuje celkova vzdalenost na obrazovce i v notifikaci.

Pri ulozeni vzorku se do SQLite ulozi prirustek vzdalenosti od minuleho vzorku a posledni znama poloha. Pokud poloha neni povolena nebo neni k dispozici, aplikace stale uklada kroky a intenzitu, ale vzdalenost zustava 0.

Rychlost vzorku se pocita z prirustku vzdalenosti a delky intervalu od predchoziho ulozeneho vzorku. Tempo je stejna hodnota prepoctena na sekundy na kilometr.

## Persistencni chovani

Mereni se zalozi v databazi po otevreni `TrackingActivity`. Vzorky se ukladaji pravidelne v intervalu 5 sekund. UI casovac a GPS vzdalenost se aktualizuji nezavisle, aby uzivatel videl plynule bezici cas a aktualni vzdalenost. Pri ukonceni se ulozi posledni vzorek, mereni se oznaci casem konce a znovu se prepocitaji statistiky.

## Notifikace

Behem mereni `TrackingActivity` vytvori ongoing notifikaci `Aktivita probiha`. Na Androidu 8 a novejsim se pouziva notification channel `active_measurement`. Notifikace se aktualizuje spolu s casovacem a zrusi se po ukonceni mereni.

## Export

Export pouziva standardni Android `ACTION_SEND`. CSV se posila jako text v `Intent.EXTRA_TEXT`, takze neni potreba `FileProvider` ani zapis do externiho uloziste. CSV obsahuje souhrn mereni a vzorky vcetne rychlosti, tempa, vzdalenosti a GPS souradnic.

## Mazani historie

Mazani probiha pres `ActivityDatabase.deleteMeasurement()`. Metoda nejprve smaze vzorky z tabulky `samples` a potom souhrn z tabulky `measurements`, aby mazani fungovalo i bez spolehani na SQLite foreign key cascade.

## Testovani

Unit test `ActivityStatsCalculatorTest` overuje vypocet statistik, format delky aktivity a slovni popis intenzity. Test je bez Android UI zavislosti.
