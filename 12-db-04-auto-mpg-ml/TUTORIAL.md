# Tutorial - Auto MPG a strojove uceni

## Cil ulohy

Cilem je zpracovat dataset Auto MPG z UCI a vytvorit reprodukovatelny protokol strojoveho uceni pro regresni predikci spotreby `mpg`. Projekt obsahuje testovatelny Python modul, Jupyter notebook, EDA, cisteni dat, trenovani modelu, ladeni hyperparametru a porovnani vysledku.

Predikovana hodnota:

- `mpg` - miles per gallon, tedy efektivita spotreby.

Pouzite modely:

- LinearRegression,
- Ridge,
- SVR,
- RandomForestRegressor,
- MLPRegressor.

## Teoreticky zaklad

### Regrese

Regrese je uloha strojoveho uceni, kde cilova promenna je cislo. Zde se predikuje `mpg`. Model se uci vztah mezi vstupnimi priznaky a spotrebou.

Vstupni priznaky:

- `cylinders`,
- `displacement`,
- `horsepower`,
- `weight`,
- `acceleration`,
- `model_year`,
- `origin`.

`car_name` se nepouziva jako primy vstup do modelu, protoze jde o textovy identifikator auta a v male ukazce by snadno vedl k preuceni.

### Cisteni dat

Dataset obsahuje chybejici hodnoty ve sloupci `horsepower` jako znak `?`. Ty je potreba prevest na chybejici hodnoty a doplnit vhodnou strategii, napr. medianem.

Sloupec `origin` je ciselny kod:

- 1 - USA,
- 2 - Europe,
- 3 - Japan.

Pro model je prehlednejsi prevod na kategorii a nasledne one-hot encoding.

### Train/test split

Data se deli na trenovaci a testovaci cast. Trenovaci cast slouzi k uceni modelu a testovaci cast k nezavislemu vyhodnoceni.

Bez oddeleneho testu by model mohl vypadat dobre jen proto, ze si zapamatoval trenovaci data.

### Preprocessing pipeline

Scikit-learn pipeline spojuje predzpracovani a model do jednoho celku. V projektu se pouziva:

- imputace numerickych hodnot medianem,
- standardizace numerickych priznaku,
- one-hot encoding kategorickeho `origin`,
- regresni model.

Pipeline chrani pred chybou, kdy by se preprocessing naucil i z testovacich dat.

### GridSearchCV

`GridSearchCV` zkousi kombinace hyperparametru a vyhodnocuje je krizovou validaci. Hyperparametr neni naucen primo z dat, ale nastavuje chovani modelu, napr.:

- `alpha` u Ridge,
- `C` a `gamma` u SVR,
- pocet stromu u RandomForest,
- velikost skryte vrstvy u MLP.

Krizova validace snizuje zavislost na jednom nahodnem rozdeleni trenovacich dat.

### Metriky

Projekt pouziva:

- MAE - prumerna absolutni chyba,
- RMSE - odmocnina prumerne kvadraticke chyby,
- R2 - podil vysvetlene variability.

MAE je snadno interpretovatelna v jednotkach `mpg`. RMSE vice tresta velke chyby. R2 ukazuje, jak dobre model vysvetluje variabilitu cile.

### PCA a feature importance

PCA je metoda redukce dimenze. Hleda nove osy, ktere vysvetluji co nejvice variability v numerickych priznacich. V projektu se uklada `pca_summary.csv`.

Permutation feature importance meri, jak se zhorsi model, kdyz se hodnoty jednoho priznaku nahodne promichaji. Pokud se model vyrazne zhorsi, je priznak dulezity.

### MLPRegressor

`MLPRegressor` je vicevrstva neuronova sit ze scikit-learn. V teto uloze slouzi jako lehka ukazka neuralniho modelu bez nutnosti pouzit Keras nebo PyTorch.

MLP je citlivejsi na skalu vstupu, proto je standardizace priznaku dulezita.

## Postup reseni

### 1. Nacist dataset

`load_auto_mpg()` stahuje data z UCI:

```text
https://archive.ics.uci.edu/ml/machine-learning-databases/auto-mpg/auto-mpg.data
```

Data se mohou ulozit do cache, aby dalsi spusteni nebylo zavisle na internetu.

### 2. Naparsovat raw format

Auto MPG data jsou v textovem formatu. Funkce `parse_auto_mpg()` musi rozdelit radky na sloupce a oddelit `car_name`, ktery je v uvozovkach.

Vysledkem je `DataFrame` s pojmenovanymi sloupci.

### 3. Vycistit data

`clean_auto_mpg()` provede:

- prevod `horsepower` na cislo,
- prevod `?` na chybejici hodnotu,
- mapovani `origin` na `USA`, `Europe`, `Japan`,
- kontrolu ciselnych typu,
- oddeleni `car_name`.

Tato cast je testovana, protoze chyby v cisteni by ovlivnily vsechny modely.

### 4. Provest EDA

EDA znamena exploratory data analysis. Cilem je porozumet datu pred trenovanim modelu.

Projekt generuje:

- `descriptive_statistics.csv`,
- `missing_values.csv`,
- `correlation_matrix.csv`,
- `mpg_distribution.png`,
- `correlation_matrix.png`.

Z EDA lze typicky zjistit, ze `weight` a `displacement` souvisi s `mpg` negativne: tezsi a objemnejsi auta mivaji nizsi efektivitu.

### 5. Rozdelit priznaky a cil

`split_features()` vraci:

- `X` jako vstupni priznaky,
- `y` jako cil `mpg`.

Numericke a kategoricke sloupce jsou predzpracovany odlisne.

### 6. Vytvorit preprocessing

`build_preprocessor()` vytvori `ColumnTransformer`:

- numericke sloupce: imputace medianem a standardizace,
- kategoricky sloupec: one-hot encoding.

Tento preprocessor se potom vlozi do pipeline pred model.

### 7. Definovat modely a hyperparametry

`model_spaces()` vraci modely a mrizky hyperparametru. V rychlem rezimu jsou mrizky mensi, v plnem rezimu se zkousi vice kombinaci.

Modely maji ruzne vlastnosti:

- LinearRegression je zakladni linearni baseline,
- Ridge pridava regularizaci,
- SVR umi nelinearni vztahy,
- RandomForest zachyti nelinearity a interakce,
- MLPRegressor reprezentuje neuronovou sit.

### 8. Vyhodnotit modely

`evaluate_models()` provede train/test split, pro kazdy model spusti `GridSearchCV`, predikuje na testu a ulozi metriky MAE, RMSE a R2.

Vysledky se ukladaji do `model_results.csv` a grafu `model_rmse_comparison.png`.

### 9. Vypocitat PCA a feature importance

`pca_summary()` ukazuje, kolik variability vysvetluji hlavni komponenty numerickych priznaku.

`permutation_feature_importance()` se spousti nad nejlepsim modelem a uklada tabulku i graf dulezitosti priznaku.

### 10. Vytvorit notebook

Notebook `notebooks/auto_mpg_report.ipynb` slouzi jako protokol. Ma byt spustitelny od zacatku do konce a obsahovat postup, vysledky a grafy.

Notebook neni nahrada za modul, ale prezentacni vrstva nad testovatelnym kodem.

### 11. Pridat testy

Testy overuji:

- cisteni dat,
- vyhodnoceni modelu,
- vytvoreni vystupu reportu.

Testy pouzivaji zjednoduseny rychly rezim, aby byly prakticky spustitelne.

## Vyvoj od nuly

ML projekt je vhodne stavet tak, aby stejna logika fungovala ve skriptu, testech i notebooku. Notebook ma byt prezentace, ne jedine misto s kodem.

1. Vytvorit strukturu projektu.

```powershell
mkdir 12-db-04-auto-mpg-ml
cd 12-db-04-auto-mpg-ml
mkdir src, tests, notebooks, output
New-Item .\src\auto_mpg_pipeline.py, .\tests\test_pipeline.py
New-Item .\notebooks\auto_mpg_report.ipynb, README.md, TUTORIAL.md
```

2. Pripravit Python prostredi.

V koreni workspace se pouzije `.venv` s knihovnami `pandas`, `numpy`, `matplotlib`, `scikit-learn` a `jupyter`. Pred dalsi praci se overi:

```powershell
..\.venv\Scripts\python.exe --version
```

3. Implementovat nacteni dat.

Nejdrive vznikne `load_auto_mpg()`, ktera stahuje raw data a umi pouzit cache. V teto fazi se jeste netrenuji modely.

4. Implementovat parser.

`parse_auto_mpg()` prevede textovy format na `DataFrame`. Je potreba spravne oddelit `car_name`, protoze obsahuje mezery a je v uvozovkach.

5. Implementovat cisteni a test.

`clean_auto_mpg()` prevadi `horsepower` na cislo, nahrazuje `?` chybejici hodnotou a mapuje `origin`. Prvni test overi prave tyto transformace.

6. Oddelit priznaky a cil.

`split_features()` vrati `X` a `y`. V teto fazi se rozhodne, ze `car_name` nebude vstupnim priznakem.

7. Vytvorit preprocessing.

`build_preprocessor()` nastavi imputaci, standardizaci a one-hot encoding. Tento krok se dela pred modely, aby vsechny modely dostaly stejna data.

8. Pridat baseline model.

Nejprve se spusti jen `LinearRegression`. Pokud baseline funguje a vraci metriky, teprve potom se pridavaji slozitejsi modely.

9. Pridat dalsi modely a GridSearchCV.

Do `model_spaces()` se postupne doplni Ridge, SVR, RandomForest a MLP. U kazdeho modelu se nastavi mala mrizka hyperparametru. Rychly rezim se hodi pro testy.

10. Implementovat vyhodnoceni.

`evaluate_models()` provede train/test split, krizovou validaci, predikci a vypocet MAE, RMSE a R2. Vysledek je tabulka, ne jen tisk do konzole.

11. Pridat EDA vystupy.

Do `generate_report_outputs()` se prida popisna statistika, missing values, korelace, distribuce `mpg` a korelacni matice. Grafy se ukladaji do `output/`.

12. Pridat PCA a feature importance.

Po fungujicich modelech se doplni `pca_summary()` a `permutation_feature_importance()`. Tyto casti pomahaji interpretovat data a model.

13. Vytvorit notebook.

Notebook importuje funkce z `auto_mpg_pipeline.py`, spousti je a zobrazuje vysledky. Nema duplikovat velke bloky kodu.

14. Dopsat testy.

Testy maji bezet rychle. Proto pouzivaji maly vzorek nebo `quick=True`. Kontroluji hlavne, ze pipeline nespadne a vytvori ocekavane vystupy.

15. Finalni reprodukce.

Spustit skript, testy a notebook pres `nbconvert`. Pokud vse projde, projekt je reprodukovatelny.

## Jak projekt spustit

Z korene workspace:

```powershell
.\.venv\Scripts\python.exe .\12-db-04-auto-mpg-ml\src\auto_mpg_pipeline.py
```

Z adresare projektu:

```powershell
..\.venv\Scripts\python.exe .\src\auto_mpg_pipeline.py
```

Notebook:

```powershell
..\.venv\Scripts\jupyter.exe nbconvert --to notebook --execute .\notebooks\auto_mpg_report.ipynb --output auto_mpg_report.executed.ipynb
```

Testy:

```powershell
..\.venv\Scripts\python.exe -m unittest discover -s .\tests
```

Vystupy vznikaji v `output/`.

## Co umet vysvetlit u obhajoby

- Proc je Auto MPG regresni uloha.
- Proc se `horsepower = ?` musi vycistit.
- Proc se `origin` koduje jako kategoricka promenna.
- Proc je potreba train/test split.
- Rozdil mezi MAE, RMSE a R2.
- Proc je preprocessing soucasti pipeline.
- Jak `GridSearchCV` vybira hyperparametry.
- Proc MLP potrebuje standardizovana data.
- K cemu slouzi PCA a permutation importance.

## Mozna rozsireni

- log transformace nekterych priznaku,
- robustni porovnani pres opakovanou krizovou validaci,
- ulozeni nejlepsiho modelu pomoci `joblib`,
- webove demo pro predikci `mpg`,
- vysvetleni modelu pomoci SHAP,
- porovnani s XGBoost nebo LightGBM.
