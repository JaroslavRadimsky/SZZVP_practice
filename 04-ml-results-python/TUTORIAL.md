# Tutorial - sprava vysledku ML modelu v Pythonu

## Cil ulohy

Cilem je vytvorit desktopovou aplikaci v Pythonu, ktera eviduje experimenty strojoveho uceni, modely, jejich parametry a vysledky. Aplikace pouziva Tkinter pro graficke rozhrani a SQLite pro ulozeni dat.

Projekt ukazuje spojeni GUI aplikace, relacni databaze a jednoduche domenove logiky. Soucasne rozlisuje dva typy modelu: `RandomForest` a `SVC`, pricemz kazdy model musi mit alespon tri parametry.

## Teoreticky zaklad

### Tkinter

Tkinter je standardni knihovna Pythonu pro tvorbu desktopovych aplikaci. Program bezi v udalostni smycce:

- aplikace zobrazi okno,
- uzivatel klikne na tlacitko nebo vyplni pole,
- Tkinter zavola prislusnou metodu,
- metoda upravi data a obnovi zobrazeni.

V projektu je hlavni trida `MlResultsApp`, ktera dedi z `tk.Tk`.

### SQLite

SQLite je jednoducha relacni databaze ulozena v jednom souboru. Nevyzaduje samostatny server, proto je vhodna pro mensi desktopove aplikace.

V projektu se pouziva pro ulozeni:

- experimentu,
- modelu,
- parametru modelu ve formatu JSON,
- vysledne metriky.

Relacni pristup je vhodny, protoze jeden experiment muze mit vice modelu. To odpovida vztahu 1:N mezi tabulkami `experiments` a `models`.

### Repository pattern

`Repository` oddeluje databazove operace od GUI. Tkinter aplikace tak nemusi znat SQL dotazy. Pouze vola metody jako `add_experiment`, `add_model` nebo `list_models`.

Vyhody:

- databazova logika je na jednom miste,
- testy mohou pouzit docasnou databazi,
- GUI zustava jednodussi,
- pozdeji lze uloziste vymenit nebo rozsirit.

### Parametry modelu

Model strojoveho uceni je definovan typem a parametry. Napriklad:

- `RandomForest`: pocet stromu, maximalni hloubka, nahodny seed,
- `SVC`: kernel, regularizace `C`, hodnota `gamma`.

Ukladani parametru do JSON sloupce je prakticke, protoze ruzne typy modelu mohou mit ruzne parametry. V realne velke databazi by se dalo uvazovat i o samostatne tabulce parametru.

### Filtrovani a razeni

Aplikace umoznuje:

- filtrovat modely podle typu,
- radit je podle vysledku.

Tato cast ukazuje beznou praci s daty nad databazi i nad uzivatelskym rozhranim. Uzivatel tak rychle najde nejlepsi model v experimentu.

## Postup reseni

### 1. Navrhnout databazove schema

Nejprve je potreba urcit entity:

- experiment,
- modelovy vysledek.

Tabulka `experiments` obsahuje nazev, popis a datum vytvoreni. Tabulka `models` obsahuje odkaz na experiment, typ modelu, parametry a vysledek.

Dulezite je pouzit cizi klic `experiment_id`, aby model vzdy patril ke konkretnimu experimentu.

### 2. Vytvorit datove tridy

V `models.py` jsou tridy:

- `Experiment`,
- `ModelResult`.

Tyto tridy reprezentuji data v aplikaci. Jsou jednodussi na pouziti nez prace s radky databaze napric celym kodem.

### 3. Implementovat `Repository`

`Repository` ma zajistit:

- otevreni SQLite spojeni,
- vytvoreni schematu,
- vlozeni experimentu,
- vypsani experimentu,
- vlozeni modelu,
- vypsani modelu s filtrem a razenim,
- validaci parametru.

Validace modelu kontroluje, ze podporovane modely maji alespon tri parametry. Tim se plni pozadavek zadani.

### 4. Vytvorit GUI

V `app.py` se vytvori okno s castmi:

- formular pro experiment,
- formular pro model,
- vyber typu modelu,
- vstupy pro parametry,
- tabulka experimentu,
- tabulka modelu,
- filtr a razeni.

Tkinter widget `Treeview` je vhodny pro tabulkove zobrazeni dat.

### 5. Napojit GUI na repository

Po kliknuti na tlacitko "pridat experiment" aplikace:

1. precte hodnoty z poli,
2. vytvori `Experiment`,
3. zavola `Repository.add_experiment`,
4. obnovi seznam experimentu.

Podobny postup se pouzije pro model. Po ulozeni se obnovi tabulka modelu, aby uzivatel okamzite videl zmenu.

### 6. Resit ruzne parametry modelu

Pri zmene typu modelu se meni popisky parametru. To uzivateli napovi, jake hodnoty ma zadat. Program ale parametry stale uklada jako slovnik, ktery se v databazi serializuje do JSON.

### 7. Doplnit testy

Testy pouzivaji docasnou SQLite databazi. Overuji:

- pridani experimentu,
- pridani modelu,
- filtrovani a razeni,
- validaci poctu parametru.

Tento pristup je spolehlivejsi nez rucni kontrola klikani v GUI.

## Vyvoj od nuly

U teto aplikace je vhodne zacit databazi a repository, ne oknem. GUI se pak jen napoji na hotovou logiku.

1. Vytvorit strukturu projektu.

```powershell
mkdir 04-ml-results-python
cd 04-ml-results-python
mkdir src, tests, docs
New-Item .\src\models.py, .\src\repository.py, .\src\app.py, .\tests\test_repository.py, .\docs\database.md
```

2. Navrhnout schema.

Do dokumentace se nejprve zapise, ze budou tabulky `experiments` a `models`. U modelu bude cizi klic `experiment_id`, typ modelu, JSON parametry a ciselny vysledek.

3. Vytvorit datove tridy.

V `models.py` vzniknou `Experiment` a `ModelResult`. V teto fazi jeste neexistuje Tkinter. Cilem je mit datove objekty, ktere se budou predavat mezi GUI a repository.

4. Implementovat `Repository.init_schema()`.

Repository vytvori tabulky v SQLite. Hned se napise test, ktery vytvori docasnou databazi a zavola `init_schema()`.

5. Pridat metody pro experimenty.

Implementuje se `add_experiment()` a `list_experiments()`. Test overi, ze vlozeny experiment jde nacist zpet.

6. Pridat metody pro modely.

Implementuje se `add_model()` a `list_models()`. Parametry se serializuji do JSON. Test overi, ze parametry po nacteni zustanou zachovane.

7. Doplnit validaci modelu.

Repository odmita znamy model bez alespon tri parametru. Tato validace patri do aplikacni logiky, protoze chrani data bez ohledu na GUI.

8. Pridat filtrovani a razeni.

Nejdrive se filtrovani otestuje na repository. Teprve potom se prida ovladaci prvek do GUI.

9. Vytvorit Tkinter okno.

V `app.py` vznikne trida `MlResultsApp`. Nejprve se zobrazi jen seznam experimentu a formular pro pridani experimentu. Po overeni se prida formular modelu a tabulka vysledku.

10. Napojit udalosti.

Tlacitka volaji repository metody a po ulozeni obnovuji tabulky. Kdyz nastane chyba validace, zobrazi se uzivateli dialog.

11. Provest rucni scenar.

Vytvorit experiment, pridat `RandomForest` se tremi parametry, pridat `SVC`, filtrovat podle typu a seradit podle vysledku.

## Jak projekt spustit

```powershell
python .\src\app.py
```

Pokud neni systemovy Python v `PATH`, lze pouzit bundled runtime:

```powershell
& "C:\Users\pokej\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe" .\src\app.py
```

Testy:

```powershell
python -m unittest discover -s .\tests
```

Popis databaze je v `docs/database.md`.

## Co umet vysvetlit u obhajoby

- Proc je SQLite vhodna pro desktopovou aplikaci.
- Jak funguje vztah experiment 1:N model.
- Proc se parametry ukladaji jako JSON.
- Proc je databazova vrstva oddelena do `Repository`.
- Jak funguje filtrovani a razeni modelu.
- Jak by se aplikace rozsirila o dalsi typy modelu.

## Mozna rozsireni

- vice metrik pro jeden model,
- import a export experimentu,
- graf porovnani vysledku,
- realne spousteni trenovani modelu,
- uzivatelske ucty,
- pokrocile vyhledavani podle parametru.
