# 11 - NoSQL databaze MongoDB

Projekt uklada propojena data o cizincich v CR a HDP na hlavu do dokumentove databaze MongoDB. Cilem je ukazat denormalizaci, agregacni pipelines a vizualizaci bez relacnich JOINu.

## Datove zdroje

- Cizinci v CR: `https://data.csu.gov.cz/opendata/sady/CIZ002/distribuce/csv`
- GDP per capita: `https://api.worldbank.org/v2/country/all/indicator/NY.GDP.PCAP.CD?format=json&per_page=20000`

## Spusteni

```powershell
docker compose up -d
..\.venv\Scripts\python.exe .\src\load_mongo.py
..\.venv\Scripts\python.exe .\src\run_aggregations.py
```

Z korene workspace:

```powershell
.\.venv\Scripts\python.exe .\11-db-02-nosql-mongodb\src\load_mongo.py
.\.venv\Scripts\python.exe .\11-db-02-nosql-mongodb\src\run_aggregations.py
```

## Overeni

```powershell
..\.venv\Scripts\python.exe .\tests\smoke.py
```

## Vystupy

- Dokumentovy model: `docs/document-model.md`
- Vyhody/nevyhody NoSQL reseni: `docs/nosql-evaluation.md`
- Agregacni CSV a PNG grafy: `output/`
