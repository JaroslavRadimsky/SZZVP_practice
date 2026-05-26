# Datove zdroje

## Cizinci v CR

Zdroj: Cesky statisticky urad, produkt CIZ002.

Pouzity endpoint:

```text
https://data.csu.gov.cz/opendata/sady/CIZ002/distribuce/csv
```

Vybrany endpoint obsahuje pohlavi, typ pobytu, statni obcanstvi, uzemi a cas. OLAP model obsahuje i dimenzi vekovych skupin s hodnotou `Celkem`, aby bylo schema pripraveno i pro detailnejsi zdroj s vekem.

## HDP na hlavu

Zdroj: World Bank, indikator `NY.GDP.PCAP.CD`.

Pouzity endpoint:

```text
https://api.worldbank.org/v2/country/all/indicator/NY.GDP.PCAP.CD?format=json&per_page=20000
```

## Mapovani zemi

Soubor `src/data_sources.py` obsahuje rucni mapovani nejcastejsich ceskych nazvu zemi na ISO3 kody World Bank. Radky bez mapovani zustavaji v databazi, ale nemaji pripojeny HDP udaj.

