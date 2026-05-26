# 04 - Sprava vysledku ML modelu

Tkinter aplikace uklada experimenty, modely a vysledky do SQLite databaze.

## Funkce

- Zalozeni experimentu se jmenem a textovym popisem.
- Zalozeni modelu typu `RandomForest` nebo `SVC`.
- Ulozeni nejmene tri parametru modelu.
- Prirazeni modelu do experimentu a ulozeni jednoho ciselneho vysledku.
- Filtrovani modelu podle typu a razeni podle vysledku.

## Spusteni

```powershell
python .\src\app.py
```

Pokud neni systemovy Python v PATH, lze pouzit bundled runtime Codexu:

```powershell
& "C:\Users\pokej\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe" .\src\app.py
```

## Testy

```powershell
python -m unittest discover -s .\tests
```

## Databaze

Schema je popsane v `docs/database.md`.

