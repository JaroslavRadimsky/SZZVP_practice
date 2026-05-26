# Tutorial - fraktalni strom ve WPF

## Cil ulohy

Cilem je vytvorit desktopovou aplikaci ve WPF, ktera vykresluje fraktalni strom podle parametru uzivatele. Aplikace ma umet spustit vypocet, pozastavit ho, pokracovat, zrusit ho a exportovat nebo importovat vysledek do JSON souboru.

Projekt je rozdelen na dve casti:

- `FractalTree.Core` obsahuje vypocet vetvi, parametry stromu, pauzu, zruseni a JSON uloziste,
- `FractalTree.App` obsahuje WPF okno, ovladaci prvky a vykresleni na `Canvas`.

Takove rozdeleni je dulezite, protoze vypocet lze testovat bez grafickeho rozhrani.

## Teoreticky zaklad

### Fraktal

Fraktal je geometricky tvar, ktery vznikne opakovanim jednoducheho pravidla. Casto vykazuje samopodobnost: mensi cast pripomina celek. Fraktalni strom je jednoduchy priklad:

1. nakresli se kmen,
2. na konci kmene se vytvori dve mensi vetve,
3. kazda nova vetev se znovu rozdeli,
4. proces se opakuje do zadane hloubky.

Parametry stromu:

- pocet iteraci urcuje hloubku rekurze,
- uhel urcuje rozevreni vetvi,
- zkraceni urcuje delku dalsi vetve oproti predchozi,
- barva urcuje vykresleni.

### Rekurze

Rekurze je postup, kdy metoda vola sama sebe. U fraktalniho stromu je rekurze prirozena, protoze kazda vetev vytvari dalsi vetve stejnym algoritmem.

Rekurzivni algoritmus musi mit ukoncovaci podminku. Zde je to dosazeni nulte urovne nebo maximalniho poctu iteraci. Bez ukoncovaci podminky by vypocet nikdy neskoncil.

### WPF a Canvas

WPF je desktopovy framework pro Windows aplikace. `Canvas` je plocha, na kterou lze umistovat graficke prvky pomoci souradnic. Pro strom se kazda vetev prevadi na caru mezi body `X1`, `Y1`, `X2`, `Y2`.

WPF pouziva udalostni model: uzivatel klikne na tlacitko a aplikace zareaguje metodou obsluhy udalosti.

### Asynchronni vypocet a zruseni

Dlouhy vypocet by mohl zablokovat uzivatelske rozhrani. Proto je generovani stromu reseno asynchronne. UI zustava responzivni a uzivatel muze kliknout na pauzu nebo zruseni.

`CancellationToken` je standardni .NET mechanismus pro kooperativni zruseni. Algoritmus v pravidelnych mistech kontroluje, zda nebylo zruseni vyzadano.

`PauseToken` je vlastni pomocna trida, ktera umoznuje pozastavit vypocet bez jeho definitivniho ukonceni.

### Serializace do JSON

Export stromu uklada parametry i vypoctene vetve do JSON dokumentu. Import naopak dokument nacte a strom znovu vykresli. JSON je vhodny, protoze je textovy, snadno kontrolovatelny a podporovany v .NET.

## Postup reseni

### 1. Oddelit vypocet od UI

Nejprve je potreba vytvorit knihovnu `FractalTree.Core`. Do ni patri:

- `TreeParameters`,
- `Branch`,
- `TreeDocument`,
- `FractalGenerator`,
- `PauseToken`,
- `JsonTreeStore`.

UI projekt na tyto typy pouze navazuje.

### 2. Definovat parametry stromu

`TreeParameters` obsahuje hodnoty zadane uzivatelem. Je vhodne pouzit nemenny typ, napr. `record`, protoze parametry popisuji jednu konfiguraci vypoctu.

Validace parametru je dulezita:

- iterace nesmi byt zaporne,
- zkraceni ma byt mezi 0 a 1,
- uhel ma mit smysluplny rozsah,
- barva musi byt pouzitelna pro vykresleni.

### 3. Definovat vetev

`Branch` reprezentuje jednu caru stromu. Obsahuje pocatecni bod, koncovy bod a uroven. Diky urovni lze pozdeji menit tloustku nebo barvu vetve podle hloubky.

### 4. Implementovat generator

`FractalGenerator` vytvori seznam vetvi. Zakladni algoritmus:

1. vytvori kmen,
2. prida ho do seznamu,
3. vypocita levy a pravy uhel,
4. zkrati delku,
5. zavola se znovu pro levou a pravou vetev.

Souradnice noveho bodu se pocitaji trigonometricky:

- `x2 = x1 + cos(angle) * length`,
- `y2 = y1 - sin(angle) * length`.

Ve WPF osa Y roste smerem dolu, proto se u vertikalniho smeru casto pouziva znamenko minus.

### 5. Doplnit pauzu a zruseni

Do rekurzivniho generatoru se pred zpracovanim dalsi vetve vlozi kontrola:

- pokud je pozadovano zruseni, vyhodi se ukonceni operace,
- pokud je vypocet pozastaven, generator ceka na pokracovani.

Dulezite je, aby pauza i zruseni byly kooperativni. Algoritmus musi tyto signaly sam kontrolovat.

### 6. Vytvorit WPF okno

V `MainWindow.xaml` se pripravi:

- vstupy pro iterace, uhel, zkraceni a barvu,
- tlacitka Start, Pause, Resume, Cancel, Export, Import,
- `Canvas` pro kresleni.

V `MainWindow.xaml.cs` se cte vstup, spousti generator a vysledek se prevadi na WPF cary.

### 7. Implementovat export a import

`JsonTreeStore` uklada `TreeDocument`, tedy parametry i seznam vetvi. Export je vhodny po uspesnem vypoctu. Import ma po nacteni dokumentu rovnou prekreslit plochu.

### 8. Testovat vypocet

Testy nevyzaduji WPF okno. Overuji se vlastnosti generatoru:

- pro n iteraci vznikne ocekavany pocet vetvi,
- zmena uhlu zmeni geometrii,
- zruseni vypoctu skutecne skonci,
- export a import zachova data.

## Vyvoj od nuly

Prakticky postup je lepsi stavet po vrstvach: nejdrive vypocet, potom UI.

1. Zalozit WPF projekt, core knihovnu a testy.

```powershell
mkdir 03-fractal-tree-wpf
cd 03-fractal-tree-wpf
dotnet new classlib -n FractalTree.Core -o .\src\FractalTree.Core
dotnet new wpf -n FractalTree.App -o .\src\FractalTree.App
dotnet new xunit -n FractalTree.Tests -o .\tests\FractalTree.Tests
dotnet add .\src\FractalTree.App\FractalTree.App.csproj reference .\src\FractalTree.Core\FractalTree.Core.csproj
dotnet add .\tests\FractalTree.Tests\FractalTree.Tests.csproj reference .\src\FractalTree.Core\FractalTree.Core.csproj
```

2. V `FractalTree.Core` vytvorit datove typy.

Nejprve vzniknou `TreeParameters`, `Branch` a `TreeDocument`. Tyto typy jsou jednoduche a lze je pouzit v testech i ve WPF.

3. Implementovat synchronni generator bez UI.

Prvni verze `FractalGenerator` muze byt synchronni a vracet seznam vetvi. Napise se test na pocet vetvi pro maly pocet iteraci. Pokud tato cast funguje, geometrie je spravne oddelena od okna.

4. Pridat validaci parametru.

Osetri se zaporne iterace, spatne zkraceni a nesmyslne hodnoty. Validace se testuje pred pridanou grafikou, protoze chyby se hledaji snadneji.

5. Doplnit `CancellationToken`.

Generator dostane parametr pro zruseni a v rekurzi ho pravidelne kontroluje. Test muze predat uz zruseny token a overit, ze vypocet skonci.

6. Doplnit `PauseToken`.

Pauza se pridava az po zruseni. `PauseToken` ma umet pozastavit a zase uvolnit cekajici vypocet.

7. Vytvorit zakladni WPF layout.

V `MainWindow.xaml` se prida `Canvas`, vstupy pro parametry a tlacitko Start. Nejdrive staci jen spustit vypocet a vykreslit hotovy strom.

8. Pridat asynchronni spusteni.

Obsluha tlacitka Start spusti generator asynchronne. UI musi zustat pouzitelne, proto se vypocet nesmi delat dlouho primo v hlavnim vlakne.

9. Doplnit Pause, Resume a Cancel.

Tlacitka se napoji na `PauseToken` a `CancellationTokenSource`. Po kazde zmene se rucne overi, ze aplikace nezamrzne.

10. Pridat export/import.

Az kdyz kresleni funguje, doplni se `JsonTreeStore`. Export ulozi `TreeDocument`, import ho nacte a znovu vykresli.

11. Dopsat UML a dokumentaci.

UML diagram se kresli podle skutecnych trid. README obsahuje rychle spusteni a tutorial vysvetluje vyvojovy postup.

## Jak projekt spustit

```powershell
dotnet run --project .\src\FractalTree.App\FractalTree.App.csproj
```

Testy:

```powershell
dotnet test .\tests\FractalTree.Tests\FractalTree.Tests.csproj
```

UML diagram trid je v `docs/fractal-classes.puml`.

## Co umet vysvetlit u obhajoby

- Co je fraktal a proc se pro strom hodi rekurze.
- Jak se z uhlu a delky pocita koncovy bod vetve.
- Proc je vypocet ve `FractalTree.Core` a ne primo ve WPF okne.
- Jak funguje `CancellationToken`.
- Rozdil mezi pauzou a zrusenim vypoctu.
- Proc je JSON vhodny pro export/import.

## Mozna rozsireni

- plynula animace rustu stromu,
- zmena tloustky vetve podle urovne,
- barevny gradient,
- export do PNG nebo SVG,
- nahodna odchylka uhlu pro prirozenejsi strom.
