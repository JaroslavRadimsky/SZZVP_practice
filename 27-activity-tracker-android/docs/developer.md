# Vývojářská dokumentace

## Architektura

Aplikace navazuje na jednoduché vrstvení z projektu `05-rss-reader-android`:

- `MainActivity` zobrazuje historii měření a spouští samostatnou obrazovku měření.
- `TrackingActivity` běží po dobu aktivity, čte senzory a GPS, ukládá vzorky a zobrazuje notifikaci.
- `MeasurementDetailActivity` zobrazuje detail jednoho měření, přepínatelný graf a export CSV.
- `MeasurementAdapter` převádí záznamy z databáze na řádky v `ListView` a předává požadavek na smazání.
- `ActivityDatabase` ukládá měření a vzorky do SQLite přes `SQLiteOpenHelper`.
- `ActivityRecord` reprezentuje souhrn měření.
- `ActivitySample` reprezentuje jeden časový vzorek.
- `ActivityStatsCalculator` obsahuje testovatelnou logiku pro souhrny, délku a popis intenzity.
- `IntensityGraphView` kreslí jednoduchý graf intenzity bez externí knihovny.

## Datový model

Tabulka `measurements`:

- `id` - primární klíč,
- `started_at` - čas zahájení v milisekundách,
- `ended_at` - čas ukončení v milisekundách, může být prázdný,
- `total_steps` - součet uložených kroků,
- `distance_meters` - součet GPS vzdálenosti v metrech,
- `average_intensity` - průměrná intenzita ze vzorků,
- `sample_count` - počet uložených vzorků.

Tabulka `samples`:

- `id` - primární klíč,
- `measurement_id` - odkaz na měření,
- `measured_at` - čas vzorku v milisekundách,
- `steps` - přírůstek kroků od posledního vzorku,
- `intensity` - orientační intenzita za interval,
- `latitude` - poslední známá šířka při uložení vzorku,
- `longitude` - poslední známá délka při uložení vzorku,
- `distance_meters` - přírůstek GPS vzdálenosti od minulého vzorku,
- `speed_kmh` - rychlost vzorku v kilometrech za hodinu,
- `pace_seconds_per_km` - tempo vzorku v sekundách na kilometr.

Po každém vložení vzorku se aktualizuje souhrn v tabulce `measurements`.

## Senzory

Aplikace preferuje `Sensor.TYPE_STEP_COUNTER`, pokud je dostupný a aplikace má oprávnění `ACTIVITY_RECOGNITION`. Intenzita se měří z `TYPE_LINEAR_ACCELERATION`, případně z `TYPE_ACCELEROMETER`.

Pokud krokoměr není dostupný, aplikace odhaduje kroky z výrazných pohybových špiček. Tento odhad je orientační, ale splňuje požadavek na relevantní pohybová data ze senzoru.

## GPS vzdálenost

`TrackingActivity` používá `LocationManager` s providery `GPS_PROVIDER` a `NETWORK_PROVIDER`. Při první použitelné poloze se uloží referenční bod. Každá další poloha s přesností do 50 metrů přidá vzdálenost od předchozí polohy, pokud přírůstek není nerealisticky velký. Přitom se průběžně aktualizuje celková vzdálenost na obrazovce i v notifikaci.

Při uložení vzorku se do SQLite uloží přírůstek vzdálenosti od minulého vzorku a poslední známá poloha. Pokud poloha není povolená nebo není k dispozici, aplikace stále ukládá kroky a intenzitu, ale vzdálenost zůstává 0.

Rychlost vzorku se počítá z přírůstku vzdálenosti a délky intervalu od předchozího uloženého vzorku. Tempo je stejná hodnota přepočtená na sekundy na kilometr.

## Persistenční chování

Měření se založí v databázi po otevření `TrackingActivity`. Vzorky se ukládají pravidelně v intervalu 5 sekund. UI časovač a GPS vzdálenost se aktualizují nezávisle, aby uživatel viděl plynule běžící čas a aktuální vzdálenost. Při ukončení se uloží poslední vzorek, měření se označí časem konce a znovu se přepočítají statistiky.

## Notifikace

Během měření `TrackingActivity` vytvoří průběžnou notifikaci `Aktivita probiha`. Na Androidu 8 a novějším se používá notification channel `active_measurement`. Notifikace se aktualizuje spolu s časovačem a zruší se po ukončení měření.

## Export

Export používá standardní Android `ACTION_SEND`. CSV se posílá jako text v `Intent.EXTRA_TEXT`, takže není potřeba `FileProvider` ani zápis do externího úložiště. CSV obsahuje souhrn měření a vzorky včetně rychlosti, tempa, vzdálenosti a GPS souřadnic.

## Mazání historie

Mazání probíhá přes `ActivityDatabase.deleteMeasurement()`. Metoda nejprve smaže vzorky z tabulky `samples` a potom souhrn z tabulky `measurements`, aby mazání fungovalo i bez spoléhání na SQLite foreign key cascade.

## Testování

Unit test `ActivityStatsCalculatorTest` ověřuje výpočet statistik, formát délky aktivity, formát vzdálenosti, rychlost, tempo a slovní popis intenzity. Test je bez Android UI závislostí.
