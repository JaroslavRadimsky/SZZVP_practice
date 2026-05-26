# 10 - Relacni databaze a OLAP

Projekt propojuje otevrena data o cizincich v CR s HDP na hlavu podle World Bank. Vystupem je PostgreSQL databaze, SQL dotazy, agregace, grafy a navrh datoveho skladu.

## Datove zdroje

- Cizinci v CR: `https://data.csu.gov.cz/opendata/sady/CIZ002/distribuce/csv`
- GDP per capita: `https://api.worldbank.org/v2/country/all/indicator/NY.GDP.PCAP.CD?format=json&per_page=20000`

## Spusteni

```powershell
docker compose up -d
..\.venv\Scripts\python.exe .\src\load_postgres.py
..\.venv\Scripts\python.exe .\src\run_reports.py
```

Pokud spoustite z korene workspace, pouzijte:

```powershell
.\.venv\Scripts\python.exe .\10-db-01-relational-olap\src\load_postgres.py
.\.venv\Scripts\python.exe .\10-db-01-relational-olap\src\run_reports.py
```

## Overeni

```powershell
..\.venv\Scripts\python.exe .\tests\smoke.py
```

## Vystupy

- ER diagram: `docs/er-diagram.puml`
- OLAP/star schema: `docs/star-schema.puml`
- SQL schema: `sql/schema.sql`
- Analyticke dotazy: `sql/analytics.sql`
- CSV a PNG vystupy: `output/`
