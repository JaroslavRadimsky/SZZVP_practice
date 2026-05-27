# Uživatelská příručka

## Zahájení měření

1. Spusťte aplikaci `Activity Tracker`.
2. Povolte oprávnění pro rozpoznávání aktivity a polohu, pokud se systém zeptá.
3. Klepněte dole na `Zahajit aktivitu`.
4. Otevře se nová obrazovka měření přes celou aplikaci.
5. Pohybujte se s telefonem u sebe.

Aplikace každých 5 sekund ukládá vzorek s časem, počtem kroků nebo odhadem kroků, intenzitou pohybu, přírůstkem vzdálenosti z GPS, rychlostí v km/h a tempem na kilometr. Čas na obrazovce ale běží plynule po sekundách. Během měření je v systémové liště zobrazena notifikace `Aktivita probiha`.

## Ukončení měření

1. Na obrazovce měření klepněte na `Ukoncit a ulozit`.
2. Měření se uloží do SQLite databáze.
3. V seznamu `Ulozena mereni` se objeví nový záznam.

Pokud stisknete tlačítko zpět, aplikace se nejprve zeptá, zda chcete měření ukončit a uložit.

## Detail měření

Klepnutím na řádek v seznamu otevřete detail. Detail zobrazuje:

- čas začátku a konce,
- délku aktivity,
- celkový počet kroků,
- uraženou vzdálenost,
- průměrnou rychlost a tempo,
- průměrnou intenzitu,
- počet vzorků,
- graf s přepínačem metriky: intenzita, rychlost, tempo nebo vzdálenost.

Detail už nezobrazuje dlouhý textový seznam všech vzorků. Vzorky zůstávají dostupné v CSV exportu.

## Mazání měření

V historii klepněte na `Smazat` u vybraného měření a potvrďte dialog. Aplikace smaže souhrn i všechny uložené vzorky dané aktivity.

## Export CSV

V detailu klepněte na `Sdilet CSV`. Android nabídne aplikace, přes které lze CSV text sdílet nebo uložit. CSV obsahuje i vzorky s rychlostí, tempem, vzdáleností a GPS souřadnicemi.

## Poznámky k měření

Pokud telefon nemá hardwarový krokoměr, aplikace počítá orientační odhad kroků z akcelerometru. Pokud není povolena poloha nebo není dostupný GPS signál, vzdálenost zůstane nulová nebo se bude aktualizovat až po získání polohy. Intenzita je také orientační a slouží hlavně k porovnání průběhu aktivity v rámci jednoho měření.
