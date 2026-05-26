# Tutorial - studijni system v C#

## Cil ulohy

Cilem je vytvorit konzolovy studijni system, ktery pracuje s uzivateli, kurzy, zapisy studentu a znamkami. Projekt ma ukazat objektovy navrh a tri navrhove vzory: Factory Method, Observer a Strategy.

Vysledna aplikace obsahuje domenove typy `Student`, `Teacher`, `Admin`, `Course`, `Grade` a servisni vrstvu `StudySystemService`. Konzolove menu je pouze ovladaci vrstva nad touto logikou.

## Teoreticky zaklad

### Domenovy model

Domenovy model popisuje problemovou oblast. V teto uloze jsou zakladni pojmy:

- uzivatel systemu,
- student,
- ucitel,
- administrator,
- kurz,
- zapis studenta do kurzu,
- znamka,
- vypocet vysledku.

Tridy a zaznamy v kodu nejsou nahodne. Odpovidaji pojmum, ktere by pouzil i uzivatel studijniho systemu.

### Factory Method

Factory Method oddeluje vytvareni objektu od mista, kde se objekty pouzivaji. Misto primych volani konstruktoru lze pouzit tovarny:

- `StudentFactory`
- `TeacherFactory`
- `AdminFactory`
- `StandardCourseFactory`
- `WeightedCourseFactory`

Vyhoda je, ze klientsky kod nemusi znat detaily vytvareni. Pokud se pozdeji prida napr. externi student nebo online kurz, lze pridat novou tovarnu bez prepisovani cele aplikace.

### Observer

Observer resi situaci, kdy jeden objekt oznamuje zmeny vice zavislym objektum. V projektu je pozorovanym objektem `Course` a pozorovateli jsou studenti implementujici `ICourseObserver`.

Kdyz se v kurzu zmeni informace nebo pribude oznameni, kurz informuje zapsane studenty. Tato vazba je volnejsi nez primy zapis do konkretni promenne studenta.

Typicka veta k obhajobe: Observer je vhodny pro notifikace, protoze subjekt nemusi znat detaily vsech prijemcu, staci mu spolecne rozhrani.

### Strategy

Strategy umoznuje menit algoritmus za behu programu. V projektu existuji strategie:

- `AverageGradeStrategy` pro prosty prumer,
- `WeightedAverageGradeStrategy` pro vazeny prumer.

Kurz nemusi sam obsahovat vsechny varianty vypoctu. Ma jen referenci na `IGradeStrategy`, kterou pouzije pri vypoctu vysledku. Diky tomu je kod rozsiritelny o dalsi pravidla hodnoceni.

## Postup reseni

### 1. Urcit role systemu

Nejprve je potreba zvolit typy uzivatelu. V teto ukazce jsou tri:

- `Student` se zapisuje do kurzu a prijima notifikace,
- `Teacher` reprezentuje vyucujiciho,
- `Admin` reprezentuje spravce.

Spolecne vlastnosti jsou v abstraktnim typu `User`: identifikator, jmeno a e-mail.

### 2. Navrhnout factory tridy

Pro kazdou skupinu uzivatelu vznikne tovarna odvozena od `UserFactory`. Metoda tovarny vytvori konkretni typ uzivatele.

Stejny princip se pouzije pro kurzy. `StandardCourseFactory` vytvari kurz s jednoduchym prumerem a `WeightedCourseFactory` kurz s vazenym hodnocenim.

### 3. Navrhnout kurz

`Course` ma uchovavat:

- identifikator,
- nazev,
- ucitele,
- zapsane studenty,
- znamky studentu,
- strategii vypoctu vysledku.

Kurz je prirozene misto pro zapis studentu a vkladani znamek, protoze zna svoje studenty a pravidla hodnoceni.

### 4. Implementovat Observer

Student implementuje `ICourseObserver`. Kurz si pri zapisu studenta ulozi do seznamu pozorovatelu. Pri oznameni projde pozorovatele a zavola jejich notifikacni metodu.

V praxi se tak simuluje napr. zprava "zmenen termin zkousky" nebo "pridana nova znamka".

### 5. Implementovat Strategy

Rozhrani `IGradeStrategy` definuje vypocet z kolekce znamek. Konkretni strategie se lisi:

- prosty prumer secte hodnoty a vydeli poctem,
- vazeny prumer zohledni vahu kazde znamky.

Toto reseni je cistsi nez velky `switch` podle typu kurzu.

### 6. Pridat `StudySystemService`

Servisni trida poskytuje jednotne operace pro konzolove menu a testy:

- vytvoreni uzivatele,
- vytvoreni kurzu,
- zapis studenta,
- pridani znamky,
- vypocet vysledku,
- ziskani notifikaci.

Servisni vrstva drzi aplikacni pravidla pohromade a usnadnuje testovani.

### 7. Vytvorit konzolove menu

Konzolove menu ma nabidnout hlavni scenare:

1. vytvorit studenta nebo ucitele,
2. vytvorit kurz,
3. zapsat studenta do kurzu,
4. pridat znamku,
5. zobrazit vysledek,
6. poslat oznameni kurzu,
7. vypsat stav systemu.

Menu by nemelo obsahovat slozitou domenu. Ma pouze prevadet vstup od uzivatele na volani `StudySystemService`.

### 8. Napsat testy

Testy overuji:

- ze factory vytvari spravne typy,
- ze observer doruci studentovi oznameni,
- ze prumerna strategie pocita aritmeticky prumer,
- ze vazena strategie zohlednuje vahy,
- ze zapis studenta do kurzu funguje.

## Vyvoj od nuly

Tato cast popisuje prakticky postup vytvoreni projektu od prazdne slozky.

1. Zalozit projekty.

```powershell
mkdir 02-study-system-csharp
cd 02-study-system-csharp
dotnet new console -n StudySystem.App -o .\src\StudySystem.App
dotnet new xunit -n StudySystem.Tests -o .\tests\StudySystem.Tests
dotnet add .\tests\StudySystem.Tests\StudySystem.Tests.csproj reference .\src\StudySystem.App\StudySystem.App.csproj
```

2. Vytvorit zakladni typy uzivatelu.

Nejprve vznikne abstraktni `User` a konkretni typy `Student`, `Teacher`, `Admin`. V teto fazi se jeste neresi kurzy ani znamky. Cilem je mit jasne oddelene role.

3. Pridat Factory Method pro uzivatele.

Vytvori se `UserFactory` a tri konkretni factory tridy. Hned se napise test, ze `StudentFactory` vraci `Student`, `TeacherFactory` vraci `Teacher` atd.

4. Vytvorit `Course` bez navrhovych vzoru.

Prvni verze kurzu ma jen nazev, ucitele a seznam studentu. Implementuje se zapis studenta do kurzu a test, ze student je skutecne zapsany.

5. Doplnit Observer.

Student implementuje `ICourseObserver`. Kurz si udrzuje pozorovatele a umi jim poslat oznameni. Po implementaci se napise test, ze student obdrzi zpravu z kurzu.

6. Pridat znamky a Strategy.

Nejprve vznikne `Grade`. Potom rozhrani `IGradeStrategy` a strategie `AverageGradeStrategy`. Az pote se prida `WeightedAverageGradeStrategy`. Takto se lepe kontroluje, ze kazdy algoritmus funguje samostatne.

7. Pridat Factory Method pro kurzy.

`StandardCourseFactory` vytvori kurz s jednoduchym prumerem. `WeightedCourseFactory` vytvori kurz s vazenym prumerem. Test overi, ze stejna sada znamek muze dat rozdilny vysledek podle strategie.

8. Vytvorit `StudySystemService`.

Do servisni tridy se presune prace s kolekcemi uzivatelu a kurzu. Konzole pak nebude primo manipulovat se vsemi seznamy.

9. Dopsat konzolove menu.

Menu se pridava az po otestovani domeny. Ma obsahovat volby pro vytvoreni uzivatelu, kurzu, zapis, znamky, oznameni a vypis vysledku.

10. Provest rucni test scenare.

Vytvorit ucitele, studenta a kurz. Zapsat studenta, poslat oznameni, pridat znamky a vypocitat vysledek. Tento scenar by mel jit ukazat u obhajoby.

## Jak projekt spustit

```powershell
dotnet run --project .\src\StudySystem.App\StudySystem.App.csproj
```

Testy:

```powershell
dotnet test .\tests\StudySystem.Tests\StudySystem.Tests.csproj
```

## Co umet vysvetlit u obhajoby

- Rozdil mezi primym konstruktorem a Factory Method.
- Jak kurz informuje studenty pomoci Observeru.
- Proc je vypocet znamky oddeleny do Strategy.
- Proc je vhodne mit servisni vrstvu.
- Jak by se pridala nova strategie, napr. nejlepsi tri znamky.
- Co by bylo potreba doplnit pro realny system: autentizace, databaze, opravneni.

## Mozna rozsireni

- persistence do JSON nebo SQLite,
- role a prihlasovani,
- rozvrh hodin,
- predmety a studijni programy,
- vice typu hodnoceni,
- export vysledku do CSV.
