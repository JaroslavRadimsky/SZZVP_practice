# 12 - Auto MPG: uvod do strojoveho uceni

Projekt resi regresi hodnoty `mpg` na datasetu Auto MPG z UCI. Obsahuje testovatelny Python modul i Jupyter notebook s protokolem.

## Datovy zdroj

- UCI Auto MPG raw data: `https://archive.ics.uci.edu/ml/machine-learning-databases/auto-mpg/auto-mpg.data`

## Spusteni reportu

Z korene workspace:

```powershell
.\.venv\Scripts\python.exe .\12-db-04-auto-mpg-ml\src\auto_mpg_pipeline.py
```

Z adresare projektu:

```powershell
..\.venv\Scripts\python.exe .\src\auto_mpg_pipeline.py
```

## Notebook

```powershell
..\.venv\Scripts\jupyter.exe nbconvert --to notebook --execute .\notebooks\auto_mpg_report.ipynb --output auto_mpg_report.executed.ipynb
```

## Testy

```powershell
..\.venv\Scripts\python.exe -m unittest discover -s .\tests
```

## Vystupy

Skript i notebook ukladaji tabulky a grafy do `output/`.
