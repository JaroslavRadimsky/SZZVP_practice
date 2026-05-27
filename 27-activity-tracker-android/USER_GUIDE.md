# Uzivatelska prirucka

## Zahajeni mereni

1. Spustte aplikaci `Activity Tracker`.
2. Povolte opravneni pro rozpoznavani aktivity a polohu, pokud se system zepta.
3. Klepnete dole na `Zahajit aktivitu`.
4. Otevre se nova obrazovka mereni pres celou aplikaci.
5. Pohybujte se s telefonem u sebe.

Aplikace kazdych 5 sekund uklada vzorek s casem, poctem kroku nebo odhadem kroku, intenzitou pohybu, prirustkem vzdalenosti z GPS, rychlosti v km/h a tempem na kilometr. Cas na obrazovce ale bezi plynule po sekundach. Behem mereni je v systemove liste zobrazena notifikace `Aktivita probiha`.

## Ukonceni mereni

1. Na obrazovce mereni klepnete na `Ukoncit a ulozit`.
2. Mereni se ulozi do SQLite databaze.
3. V seznamu `Ulozena mereni` se objevi novy zaznam.

Pokud stisknete tlacitko zpet, aplikace se nejprve zepta, zda chcete mereni ukoncit a ulozit.

## Detail mereni

Klepnutim na radek v seznamu otevrite detail. Detail zobrazuje:

- cas zacatku a konce,
- delku aktivity,
- celkovy pocet kroku,
- urazenou vzdalenost,
- prumernou rychlost a tempo,
- prumernou intenzitu,
- pocet vzorku,
- graf s prepinacem metriky: intenzita, rychlost, tempo nebo vzdalenost.

Detail uz nezobrazuje dlouhy textovy seznam vsech vzorku. Vzorky zustavaji dostupne v CSV exportu.

## Mazani mereni

V historii klepnete na `Smazat` u vybraneho mereni a potvrdte dialog. Aplikace smaze souhrn i vsechny ulozene vzorky dane aktivity.

## Export CSV

V detailu klepnete na `Sdilet CSV`. Android nabidne aplikace, pres ktere lze CSV text sdilet nebo ulozit. CSV obsahuje i vzorky s rychlosti, tempem, vzdalenosti a GPS souradnicemi.

## Poznamky k mereni

Pokud telefon nema hardwarovy krokomer, aplikace pocita orientacni odhad kroku z akcelerometru. Pokud neni povolena poloha nebo neni dostupny GPS signal, vzdalenost zustane nulova nebo se bude aktualizovat az po ziskani polohy. Intenzita je take orientacni a slouzi hlavne k porovnani prubehu aktivity v ramci jednoho mereni.
