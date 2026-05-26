# Tutorial - NoSQL databaze MongoDB

## Cil ulohy

Cilem je zpracovat stejna data jako v relacnim projektu, ale ulozit je do dokumentove databaze MongoDB. Projekt ma ukazat dokumentovy model, denormalizaci, idempotentni load, agregacni pipelines a vizualizace.

Data:

- cizinci v CR podle statniho obcanstvi z CSU,
- GDP per capita z World Bank.

Hlavni kolekce se jmenuje `country_year_foreigners`.

## Teoreticky zaklad

### NoSQL

NoSQL je skupina databazi, ktere nepouzivaji klasicky relacni model jako hlavni princip. MongoDB je dokumentova databaze. Uklada dokumenty podobne JSON objektum, realne ve formatu BSON.

Dokument muze obsahovat:

- jednoduche hodnoty,
- vnorene objekty,
- pole,
- pole vnorenenych objektu.

To je vhodne pro data, ktera se casto ctou jako jeden celek.

### Dokumentovy model

V projektu je jednotkou dokumentu dvojice stat a rok. Priklad:

```json
{
  "_id": "UKR-2024",
  "country": {
    "csu_name": "Ukrajina",
    "iso3": "UKR",
    "world_bank_name": "Ukraine"
  },
  "year": 2024,
  "gdp_per_capita_usd": 5342.1,
  "gdp_bucket": "nizke HDP",
  "observations": []
}
```

V poli `observations` jsou vnorena pozorovani podle pohlavi, veku, uzemi a typu pobytu.

### Denormalizace

Denormalizace znamena, ze se nektere udaje opakuji, aby se rychleji cetly. V relacni databazi by byly zeme, pohlavi a regiony v samostatnych tabulkach. V MongoDB jsou casto primo v dokumentu.

Vyhody:

- mene dotazu,
- zadne bezne `JOIN`,
- rychle cteni prehledovych dokumentu,
- pruzne schema.

Nevyhody:

- duplicity,
- slozitejsi udrzba konzistence,
- chybi cizi klice v relacnim smyslu.

### Upsert

Upsert je kombinace update a insert. Pokud dokument existuje, aktualizuje se. Pokud neexistuje, vlozi se.

Projekt pouziva `_id` ve tvaru `ISO3-rok`, napr. `UKR-2024`. Diky tomu lze loader spustit opakovane bez duplicit.

### Agregacni pipeline

MongoDB agregace se sklada z kroku. Casto pouzivane kroky:

- `$match` filtruje dokumenty,
- `$unwind` rozbali pole na jednotlive radky,
- `$group` seskupuje a pocita agregace,
- `$project` meni tvar vystupu,
- `$sort` radi vysledky,
- `$limit` omezuje pocet vysledku.

Pipeline je dokumentova obdoba analytickeho dotazu.

### Porovnani s relacnim resenim

PostgreSQL je silnejsi pro formalni model, integritu a ad-hoc dotazy pres vztahy. MongoDB je pohodlna, pokud typicky cteme cely dokument nebo predem zname dotazovaci tvary.

V teto uloze je rozdil dobre videt: relacni projekt pouziva dimenze a fakta, MongoDB projekt uklada zemi a rok jako jeden dokument s vnorenenymi pozorovanimi.

## Postup reseni

### 1. Spustit MongoDB

Projekt obsahuje `docker-compose.yml`, ktery spusti MongoDB na portu `27018`.

```powershell
docker compose up -d
```

Docker zajistuje reprodukovatelne prostredi.

### 2. Prevzit cisteni dat

`data_sources.py` pouziva stejny princip jako relacni projekt:

- cte CSU CSV,
- cte World Bank JSON,
- cisti datumy,
- filtruje radky k 31. 12.,
- mapuje pohlavi,
- mapuje staty na ISO3,
- cisti GDP hodnoty.

Stejne cisteni je dulezite, aby bylo mozne porovnat relacni a NoSQL vysledky.

### 3. Navrhnout dokument

Jednotkou dokumentu je stat a rok. Tato volba dava smysl, protoze reporty casto odpovidaji na otazky typu:

- ktere staty maji nejvice cizincu v danem roce,
- jak vypada rozdeleni podle GDP bucketu,
- jaky je rozpad podle pohlavi v poslednim roce.

Vsechna pozorovani pro stat a rok jsou v jednom poli `observations`.

### 4. Implementovat loader

`load_mongo.py` vytvori dokumenty takto:

1. seskupi vycistena data podle `country_name` a `year`,
2. pro kazdou skupinu dohleda ISO3 a GDP,
3. spocita `gdp_bucket`,
4. vytvori pole `observations`,
5. provede upsert do kolekce.

Loader vytvori indexy nad `year`, `country.iso3` a `gdp_bucket`, aby se casto pouzivane dotazy zrychlily.

### 5. Implementovat agregace

`run_aggregations.py` obsahuje pipelines:

- top staty podle poctu cizincu v poslednim roce,
- cizinci podle GDP bucketu,
- rozpad podle pohlavi.

Protoze pozorovani jsou v poli, pouziva se `$unwind`. Tim se kazde pozorovani z pole docasne chova jako samostatny vstup pro agregaci.

### 6. Generovat vystupy

Vysledky agregaci se ukladaji do `output/` jako CSV a PNG grafy. Pandas prevadi vysledek pipeline na tabulku a Matplotlib vytvari grafy.

### 7. Popsat vyhody a nevyhody

Soubor `docs/nosql-evaluation.md` shrnuje, kdy je dokumentovy model vyhodny a kde ma slabiny. Tato cast je dulezita pro teoreticke vysvetleni.

### 8. Spustit testy

Unit testy overuji transformace a GDP bucket. Smoke test overuje existenci dokumentu, funkcnost pipeline a vystupy.

## Vyvoj od nuly

NoSQL projekt je nejlepsi stavet vedle relacniho projektu, aby bylo videt porovnani stejneho problemu ve dvou modelech.

1. Vytvorit strukturu projektu.

```powershell
mkdir 11-db-02-nosql-mongodb
cd 11-db-02-nosql-mongodb
mkdir src, docs, tests, output
New-Item docker-compose.yml, README.md, TUTORIAL.md
New-Item .\src\data_sources.py, .\src\load_mongo.py, .\src\run_aggregations.py
New-Item .\docs\document-model.md, .\docs\nosql-evaluation.md
New-Item .\tests\test_transform.py, .\tests\smoke.py
```

2. Pripravit MongoDB v Dockeru.

`docker-compose.yml` nastavi MongoDB na portu `27018`. Po spusteni se overi, ze kontejner bezi.

```powershell
docker compose up -d
docker compose ps
```

3. Prevzit nebo vytvorit cisteni dat.

V `data_sources.py` se implementuje stejne cisteni jako v relacnim projektu. Je dulezite, aby rozdil mezi projekty nebyl v datech, ale v databazovem modelu.

4. Navrhnout dokument.

Do `docs/document-model.md` se nejdrive zapise ukazkovy JSON dokument. Tim se pred implementaci ujasni, co bude `_id`, co bude v objektu `country` a co bude v poli `observations`.

5. Implementovat `gdp_bucket()`.

Tato mala funkce se dobre testuje samostatne. Test overi nizke, stredni, vysoke a nezname HDP.

6. Implementovat `build_documents()`.

Funkce seskupi data podle zeme a roku a vytvori seznam dokumentu. V teto fazi se jeste nemusi zapisovat do MongoDB, staci zkontrolovat tvar dokumentu v pameti.

7. Implementovat loader.

`load_mongo.py` se pripoji k MongoDB, vytvori indexy a provede upsert dokumentu podle `_id`. Upsert zaridi, ze opakovane spusteni nevytvori duplicity.

8. Napsat prvni pipeline.

Nejdrive se napise pipeline pro top staty v poslednim roce. Obsahuje `$match`, `$unwind`, `$group`, `$project`, `$sort` a `$limit`.

9. Pridat dalsi agregace.

Po overeni prvni pipeline se doplni GDP bucket a rozpad podle pohlavi. Kazda pipeline se prevede na `DataFrame` a ulozi jako CSV.

10. Generovat grafy.

`run_aggregations.py` z CSV nebo DataFrame vytvori PNG grafy. Graf se generuje az po tom, co pipeline vraci spravna data.

11. Napsat hodnoceni NoSQL reseni.

Do `docs/nosql-evaluation.md` se popise, proc je dokumentovy model vhodny pro cteni prehledu a proc je slabsi pro referencni integritu.

12. Pridat smoke test.

Smoke test kontroluje, ze kolekce neni prazdna, pipeline vraci data a vznikl vystupni graf.

13. Finalni rucni scenar.

Spustit MongoDB, loader, agregace a smoke test. Potom zastavit kontejner pres `docker compose down`.

## Jak projekt spustit

Z adresare projektu:

```powershell
docker compose up -d
..\.venv\Scripts\python.exe .\src\load_mongo.py
..\.venv\Scripts\python.exe .\src\run_aggregations.py
..\.venv\Scripts\python.exe .\tests\smoke.py
```

Z korene workspace:

```powershell
.\.venv\Scripts\python.exe .\11-db-02-nosql-mongodb\src\load_mongo.py
.\.venv\Scripts\python.exe .\11-db-02-nosql-mongodb\src\run_aggregations.py
```

Po praci:

```powershell
docker compose down
```

## Co umet vysvetlit u obhajoby

- Co je dokumentova databaze.
- Proc je dokument navrzen jako stat a rok.
- Co znamena denormalizace.
- Proc se pouziva upsert.
- Jak funguje `$unwind`, `$group` a `$project`.
- Kdy je MongoDB vyhodnejsi nez relacni databaze.
- Kde ma NoSQL reseni slabiny oproti PostgreSQL.

## Mozna rozsireni

- pridat dalsi kolekci pro metadata zemi,
- vytvorit Atlas Charts nebo webovy dashboard,
- doplnit textove indexy,
- ulozit historii nahravani dat,
- pridat validacni schema kolekce,
- porovnat rychlost vybranych dotazu s PostgreSQL.
