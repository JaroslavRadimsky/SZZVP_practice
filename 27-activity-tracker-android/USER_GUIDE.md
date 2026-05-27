# Uzivatelska prirucka

## Zahajeni mereni

1. Spustte aplikaci `Activity Tracker`.
2. Povolte opravneni pro rozpoznavani aktivity, pokud se system zepta.
3. Klepnete na `Zahajit`.
4. Pohybujte se s telefonem u sebe.

Aplikace zacne kazdych 5 sekund ukladat vzorek s casem, poctem kroku nebo odhadem kroku a intenzitou pohybu.

## Ukonceni mereni

1. Klepnete na `Ukoncit`.
2. Mereni se ulozi do SQLite databaze.
3. V seznamu `Ulozena mereni` se objevi novy zaznam.

## Detail mereni

Klepnutim na radek v seznamu otevrite detail. Detail zobrazuje:

- cas zacatku a konce,
- delku aktivity,
- celkovy pocet kroku,
- prumernou intenzitu,
- pocet vzorku,
- graf prubehu intenzity,
- textovy seznam vzorku.

## Export CSV

V detailu klepnete na `Sdilet CSV`. Android nabidne aplikace, pres ktere lze CSV text sdilet nebo ulozit.

## Poznamky k mereni

Pokud telefon nema hardwarovy krokomer, aplikace pocita orientacni odhad kroku z akcelerometru. Intenzita je take orientacni a slouzi hlavne k porovnani prubehu aktivity v ramci jednoho mereni.
