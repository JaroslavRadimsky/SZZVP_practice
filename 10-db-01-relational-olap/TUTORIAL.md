# Tutorial - relacni databaze a OLAP v PostgreSQL

## Cil ulohy

Cilem je zpracovat otevrena data do relacni databaze PostgreSQL, navrhnout schema, provest ETL, napsat analyticke SQL dotazy, vytvorit agregace, grafy a OLAP/star schema navrh.

Projekt propojuje dva datove zdroje:

- Cizinci v CR podle statniho obcanstvi z CSU,
- GDP per capita z World Bank.

Vystupem je databaze, SQL skripty, PlantUML diagramy a vygenerovane CSV/PNG reporty.

## Teoreticky zaklad

### Relacni model

Relacni databaze uklada data do tabulek. Tabulky maji sloupce, radky, primarni klice a cizi klice. Relacni model je vhodny, kdyz chceme:

- konzistenci dat,
- jednoznacne vztahy,
- dotazy pres vice tabulek,
- transakce,
- kontrolu integritnich omezeni.

V projektu jsou dimenzni tabulky `countries`, `sexes`, `age_groups`, `regions` a faktove tabulky `foreigner_counts`, `gdp_per_capita`.

### Normalizace

Normalizace omezuje duplicity. Misto aby se v kazdem radku faktove tabulky opakoval plny nazev statu, pohlavi a uzemi, ulozi se tyto hodnoty do dimenzi a faktova tabulka obsahuje cizi klice.

Vyhody:

- mensi duplicita,
- snazsi aktualizace nazvu,
- lepsi referencni integrita.

Nevyhodou muze byt potreba `JOIN` pri analytickych dotazech.

### ETL

ETL znamena Extract, Transform, Load:

- Extract - stazeni dat ze zdroju,
- Transform - cisteni a prevod do jednotne podoby,
- Load - nahrani do databaze.

V projektu tuto logiku zajistuji `data_sources.py` a `load_postgres.py`.

### Transakce

Transakce zajistuje, ze se skupina databazovych operaci provede cela, nebo vubec. Pri ETL je to dulezite, protoze nechceme databazi nahrat jen napul.

Pokud nahravani selze, transakce se vrati zpet a databaze nezustane v nekonzistentnim stavu.

### Trigger

Trigger je databazova reakce na udalost, napr. `INSERT` nebo `UPDATE`. V projektu trigger `trg_reject_negative_foreigner_count` vola funkci `reject_negative_foreigner_count()` a brani ulozeni zaporneho poctu cizincu.

Trigger je vhodny, kdyz pravidlo patri primo do databaze a ma platit bez ohledu na aplikaci, ktera data zapisuje.

### Analyticke funkce

PostgreSQL podporuje window functions, napr. `RANK() OVER (...)`. Ty umoznuji pocitat poradi, prubezne soucty nebo prumery nad sadou radku, aniz by se radky sloucily jako pri beznem `GROUP BY`.

Projekt pouziva `RANK()` pro poradi zemi podle poctu cizincu.

### OLAP a star schema

OLAP je zamerene na analyzu dat, ne na bezne transakcni operace. Typicky se pouziva faktova tabulka a dimenze.

Faktova tabulka obsahuje meritelne hodnoty:

- pocet cizincu,
- GDP per capita.

Dimenze popisuji kontext:

- stat,
- rok,
- pohlavi,
- uzemi,
- vekova skupina.

Star schema ma uprostred faktovou tabulku a kolem ni dimenze. Je prehledne pro reporting a business intelligence.

## Postup reseni

### 1. Pripravit databazi

Projekt obsahuje `docker-compose.yml`, ktery spusti PostgreSQL 16 na portu `15432`. Docker je vhodny, protoze databaze ma reprodukovatelne nastaveni a neni nutne rucne instalovat server.

Spusteni:

```powershell
docker compose up -d
```

### 2. Navrhnout schema

V `sql/schema.sql` se nejprve mazou stare tabulky, potom se vytvari:

- `countries`,
- `sexes`,
- `age_groups`,
- `regions`,
- `gdp_per_capita`,
- `foreigner_counts`,
- `country_year_summary`.

`foreigner_counts` je faktova tabulka pro pocty cizincu. `gdp_per_capita` je druha faktova tabulka navazana na zemi a rok. `country_year_summary` je agregacni tabulka pro rychle reporty.

### 3. Ziskat data

`data_sources.py` stahuje:

- CSU CSV z `https://data.csu.gov.cz/opendata/sady/CIZ002/distribuce/csv`,
- World Bank JSON z `https://api.worldbank.org/v2/country/all/indicator/NY.GDP.PCAP.CD?format=json&per_page=20000`.

Pokud jsou dostupne lokalni kopie v `data/raw/`, lze je pouzit jako cache.

### 4. Vycistit data

U CSU dat se vybira:

- uzemi `Cesko`,
- typ pobytu `Celkem`,
- radky k 31. 12.,
- rok z datumoveho sloupce,
- pocet jako cele cislo,
- pohlavi mapovane na `TOTAL`, `M`, `F`,
- ISO3 kod zeme, pokud existuje mapovani.

U World Bank dat se vybiraji platne ISO3 kody, rok a hodnota GDP per capita.

### 5. Nahrat dimenze

ETL nejprve nahraje dimenze. Diky tomu lze ve faktovych tabulkach pouzivat cizi klice.

Typicky postup:

1. vlozit zeme,
2. vlozit pohlavi,
3. vlozit vekove skupiny,
4. vlozit regiony,
5. ulozit mapovani prirozenych klicu na databazova `id`.

### 6. Nahrat fakta

Po dimenzich se nahravaji:

- `foreigner_counts`,
- `gdp_per_capita`.

Vkladani pouziva `ON CONFLICT`, aby bylo idempotentni. To znamena, ze ETL lze spustit opakovane bez vytvareni duplicit.

### 7. Vytvorit agregace

Funkce `refresh_country_year_summary()` prepocita souhrnnou tabulku podle zeme a roku. Zaroven vytvori GDP bucket:

- `nezname HDP`,
- `nizke HDP`,
- `stredni HDP`,
- `vysoke HDP`.

Agregace zrychluje opakovane reporty, protoze se nemusi pokazde prochazet detailni fakta.

### 8. Napsat analyticke dotazy

`sql/analytics.sql` obsahuje ukazky:

- `JOIN` mezi fakty a dimenzemi,
- `GROUP BY` pro soucty,
- `ORDER BY` pro razeni,
- `RANK()` pro poradi,
- rekurzivni dotaz jako ukazku hierarchie OLAP modelu.

Tyto dotazy jsou vhodne i pro obhajobu, protoze ukazuji ruzne prvky SQL.

### 9. Generovat reporty

`run_reports.py` spusti dotazy, ulozi CSV a vytvori PNG grafy pomoci Pandas a Matplotlib. Vystupy vznikaji ve slozce `output/`.

Reporty prevadi databazova data do podoby, kterou lze snadno prohlednout nebo vlozit do dokumentace.

### 10. Zkontrolovat testy a smoke check

Unit testy overuji transformaci dat. Smoke test kontroluje, ze databaze obsahuje tabulky, data, agregace a aspon jeden graf.

## Vyvoj od nuly

U databazoveho projektu je prakticky postup: prostredi, data, schema, ETL, dotazy, vystupy, testy.

1. Vytvorit strukturu projektu.

```powershell
mkdir 10-db-01-relational-olap
cd 10-db-01-relational-olap
mkdir src, sql, docs, tests, output
New-Item docker-compose.yml, README.md, TUTORIAL.md
New-Item .\src\data_sources.py, .\src\load_postgres.py, .\src\run_reports.py
New-Item .\sql\schema.sql, .\sql\analytics.sql
New-Item .\docs\data-sources.md, .\docs\er-diagram.puml, .\docs\star-schema.puml
New-Item .\tests\test_transform.py, .\tests\smoke.py
```

2. Pripravit Python prostredi.

V koreni workspace se vytvori nebo pouzije `.venv` a nainstaluji se knihovny `pandas`, `psycopg`, `matplotlib` a testovaci nastroje. Pred psanim ETL se overi, ze Python jde spustit.

3. Napsat `docker-compose.yml`.

Nejdrive se pripravi PostgreSQL kontejner s portem `15432`, uzivatelem, heslem a databazi. Potom se overi:

```powershell
docker compose up -d
docker compose ps
```

4. Navrhnout relacni schema na papire.

Pred SQL skriptem se rozhodne, co jsou dimenze a co fakta. Dimenze jsou `countries`, `sexes`, `age_groups`, `regions`; fakta jsou `foreigner_counts` a `gdp_per_capita`.

5. Napsat `schema.sql`.

Nejprve tabulky bez triggeru, potom cizi klice a indexy. Az kdyz zaklad funguje, doplni se trigger pro zaporne hodnoty a funkce `refresh_country_year_summary()`.

6. Implementovat stahovani a cisteni dat.

V `data_sources.py` se nejdrive napise nacteni CSU CSV a test cisteni maleho vzorku. Potom se prida World Bank JSON a mapovani zemi na ISO3.

7. Napsat test transformace.

Test ma overit, ze se ponechava spravny radek k 31. 12., ze se pohlavi mapuje na kod a ze Ukrajina dostane ISO3 `UKR`. Testy se pisou pred plnym loadem, protoze jsou rychle.

8. Implementovat ETL load.

`load_postgres.py` provede schema, nahraje dimenze, ulozi mapovani id a potom nahraje fakta. Load se udela v transakci a s `ON CONFLICT`, aby sel spustit opakovane.

9. Napsat analyticke dotazy.

Do `analytics.sql` se pridaji ukazky `JOIN`, `GROUP BY`, `ORDER BY`, window function a rekurzivni dotaz. Kazdy dotaz by mel odpovidat konkretni analyticke otazce.

10. Implementovat reporty.

`run_reports.py` spusti vybrane dotazy, ulozi CSV a vykresli grafy do `output/`. Po kazdem grafu se overi, ze soubor skutecne vznikl.

11. Nakreslit diagramy.

ER diagram vychazi ze skutecneho SQL schematu. Star schema diagram ukazuje analyticky pohled, ktery muze byt mirne odlisny od fyzickeho schematu.

12. Pridat smoke test.

Smoke test se spousti po ETL a reportech. Kontroluje existenci tabulek, pocet radku, agregaci a aspon jeden PNG graf.

13. Finalni rucni scenar.

Spustit Docker, ETL, reporty a smoke test od zacatku. Potom zastavit kontejnery pres `docker compose down`.

## Jak projekt spustit

Z adresare projektu:

```powershell
docker compose up -d
..\.venv\Scripts\python.exe .\src\load_postgres.py
..\.venv\Scripts\python.exe .\src\run_reports.py
..\.venv\Scripts\python.exe .\tests\smoke.py
```

Z korene workspace:

```powershell
.\.venv\Scripts\python.exe .\10-db-01-relational-olap\src\load_postgres.py
.\.venv\Scripts\python.exe .\10-db-01-relational-olap\src\run_reports.py
```

Po praci lze databazi zastavit:

```powershell
docker compose down
```

## Co umet vysvetlit u obhajoby

- Rozdil mezi dimenzi a faktem.
- Proc se pouzivaji cizi klice.
- Jak ETL meni surova data na databazove tabulky.
- Proc je `ON CONFLICT` vhodny pro opakovatelny load.
- Jak trigger brani zapornym hodnotam.
- Jak funguje `RANK() OVER`.
- Proc je star schema vhodne pro OLAP.

## Mozna rozsireni

- pridat casovou dimenzi `date_dim`,
- detailnejsi vekove skupiny, pokud budou dostupne ve zdroji,
- materialized view misto agregacni tabulky,
- dashboard v Power BI nebo Metabase,
- dalsi ekonomicke indikatory,
- planovane automaticke ETL.
