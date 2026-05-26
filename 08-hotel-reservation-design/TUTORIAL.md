# Tutorial - navrh rezervacniho systemu pro hotely

## Cil ulohy

Cilem je zpracovat analyzu a navrh rezervacniho systemu pro male hotely a penziony. Projekt neimplementuje backend, ale dokumentuje reseni pomoci UML diagramu a statickych HTML wireframu.

Vystupy:

- use-case diagram,
- sekvencni diagram rezervace,
- tridni diagram domeny,
- wireframy hlavni stranky, rezervace, profilu a zpetne vazby.

## Teoreticky zaklad

### Analyza systemu

Analyza odpovida na otazku, co ma system delat. Nesoustredi se jeste na konkretni technologii. U rezervacniho systemu je potreba popsat:

- kdo system pouziva,
- jake akce provadi,
- jaka data system spravuje,
- jake jsou hlavni procesy.

### Use-case diagram

Use-case diagram zachycuje aktory a pripady uziti. Aktor je role mimo system, ktera se systemem komunikuje.

V projektu jsou aktori:

- `Host`,
- `Recepcni`,
- `Platebni brana`.

Pripady uziti popisuji napr. vyhledani pokoje, vytvoreni rezervace, zaplaceni rezervace nebo napsani recenze.

Vztah `include` znamena, ze jeden pripad uziti povinne obsahuje druhy. Napriklad vytvoreni rezervace zahrnuje platbu.

### Sekvencni diagram

Sekvencni diagram popisuje komunikaci objektu v case. Hodi se pro konkretni scenar, napr. vytvoreni rezervace.

Typicky ukazuje:

- uzivatele,
- webove rozhrani,
- rezervacni sluzbu,
- dostupnost pokoju,
- platebni branu,
- potvrzeni rezervace.

Hlavni hodnota sekvencniho diagramu je v tom, ze ukaze poradi kroku a odpovednosti jednotlivych casti.

### Tridni diagram

Tridni diagram popisuje statickou strukturu domeny. V projektu jsou hlavni tridy:

- `User`,
- `Accommodation`,
- `Room`,
- `Reservation`,
- `Payment`,
- `Review`.

Vazby ukazuji, ze ubytovani ma pokoje, uzivatel ma rezervace a rezervace ma platbu.

### Wireframe

Wireframe je jednoduchy navrh obrazovky. Nejde primarne o graficky design, ale o rozlozeni prvku, tok uzivatele a obsah stranky.

Wireframy jsou v HTML a CSS, aby byly snadno otevritelne v prohlizeci a verzovatelne v Gitu.

## Postup reseni

### 1. Ziskat pozadavky

Nejprve se sepisi hlavni potreby systemu:

- host chce najit volny pokoj,
- host chce vytvorit a zaplatit rezervaci,
- host chce spravovat profil,
- host chce napsat recenzi,
- recepcni chce spravovat dostupnost a reagovat na recenze.

Z techto pozadavku vzniknou pripady uziti.

### 2. Urcit aktory

Aktori nejsou konkretni osoby, ale role. Jeden clovek muze mit vice roli, ale v diagramu se roli popisuje samostatne.

V tomto systemu:

- `Host` pouziva verejnou cast a svoje rezervace,
- `Recepcni` spravuje provozni cast,
- `Platebni brana` je externi system.

### 3. Vytvorit use-case diagram

Do diagramu se vlozi hranice systemu a do ni pripady uziti. Aktori se propoji s pripady, kterych se ucastni.

Dulezite je nepouzivat use-case diagram jako seznam tlacitek. Ma popisovat cile uzivatele, napr. "Vytvorit rezervaci", ne "Kliknout na tlacitko".

### 4. Popsat scenar rezervace

Pro sekvencni diagram se zvoli hlavni scenar:

1. host zada datum a pocet hostu,
2. system vyhleda dostupne pokoje,
3. host vybere pokoj,
4. system spocita cenu,
5. host potvrdi rezervaci,
6. system vyzve platebni branu,
7. po uspesne platbe se rezervace potvrdi.

Alternativni scenare mohou byt nedostupny pokoj nebo neuspesna platba.

### 5. Navrhnout domenove tridy

Tridy se odvozuji z podstatnych jmen v zadani:

- uzivatel,
- ubytovani,
- pokoj,
- rezervace,
- platba,
- recenze.

Kazda trida ma mit jen odpovednosti, ktere k ni patri. Napriklad `Reservation` zna datum, stav a cenu, ale nema spravovat obsah recenze.

### 6. Navrhnout wireframy

Wireframy maji pokryt hlavni tok:

- hlavni stranka s vyhledavanim,
- rezervacni formular,
- profil uzivatele,
- zpetna vazba.

U wireframu je dulezite ukazat, jak se uzivatel dostane k hlavnim akcim. Texty a rozlozeni maji byt srozumitelne, ne nutne graficky finalni.

### 7. Overit konzistenci

Po vytvoreni diagramu a wireframu je potreba zkontrolovat:

- zda kazdy hlavni use-case ma obrazovku nebo proces,
- zda sekvencni diagram pouziva tridy z tridniho diagramu,
- zda wireframy odpovidaji pozadavkum,
- zda nejsou v diagramu zbytecne technicke detaily.

## Vyvoj od nuly

U navrhove ulohy se "vyvoj" nerovna programovani aplikace, ale postupne tvorbe analyzy, diagramu a prototypu.

1. Vytvorit strukturu projektu.

```powershell
mkdir 08-hotel-reservation-design
cd 08-hotel-reservation-design
mkdir uml, wireframes
New-Item .\uml\use-case.puml, .\uml\sequence-reservation.puml, .\uml\class-diagram.puml
New-Item .\wireframes\index.html, .\wireframes\styles.css
New-Item .\README.md, .\TUTORIAL.md
```

2. Sepsat slovni zadani vlastnimi slovy.

Pred diagramy je vhodne napsat, kdo system pouziva a jake problemy resi. Napriklad host hleda pokoj a recepcni spravuje dostupnost.

3. Vytvorit seznam aktoru a use-casu.

Nejdrive se sepise tabulka aktor -> cil. Az potom se kresli PlantUML diagram. Tim se zabrani tomu, aby diagram obsahoval nahodne funkce.

4. Nakreslit use-case diagram.

Do `use-case.puml` se vlozi hranice systemu, aktori a pripady uziti. Po prvni verzi se zkontroluje, zda se nepopisuji technicke kroky misto uzivatelskych cilu.

5. Popsat hlavni scenar rezervace.

Pred sekvencnim diagramem se textove napise hlavni uspechovy scenar: vyhledani, vyber, vytvoreni rezervace, platba, potvrzeni.

6. Nakreslit sekvencni diagram.

Do `sequence-reservation.puml` se prevede textovy scenar. Dulezite je ukazat poradi volani a externi platebni branu.

7. Vybrat domenove tridy.

Z textu a use-casu se vyberou podstatna jmena: `User`, `Accommodation`, `Room`, `Reservation`, `Payment`, `Review`. Ke kazde tride se doplni hlavni atributy.

8. Nakreslit tridni diagram.

Do `class-diagram.puml` se vlozi tridy a vazby. Po dokonceni se zkontroluje, ze rezervace je navazana na pokoj i uzivatele.

9. Navrhnout wireframy.

Nejdrive se nakresli hruba struktura na papir nebo v textu. Potom se v HTML vytvori hlavni obrazovky: home, rezervace, profil a zpetna vazba.

10. Sjednotit slovnik.

Nazvy ve wireframech, diagramech a README musi znamenat to same. Pokud je v diagramu `Reservation`, ve wireframu by se nemelo nahle mluvit o "objednavce pokoje" bez vysvetleni.

11. Provest kontrolu proti zadani.

Projit, zda projekt obsahuje vsechny pozadovane diagramy a obrazovky. Potom otevrit `wireframes/index.html` a zkontrolovat, ze navigace mezi castmi dava smysl.

## Jak projekt prohlednout

Wireframy otevrit v prohlizeci:

```text
wireframes/index.html
```

PlantUML soubory jsou:

```text
uml/use-case.puml
uml/sequence-reservation.puml
uml/class-diagram.puml
```

Pokud je dostupny PlantUML, lze diagramy vyrenderovat do PNG. Jinak jsou `.puml` soubory stale citelna textova dokumentace.

## Co umet vysvetlit u obhajoby

- Rozdil mezi aktorem a uzivatelem.
- Proc je platba v use-case diagramu jako externi aktor.
- Co znamena vztah `include`.
- Proc je pro rezervaci vhodny sekvencni diagram.
- Jak souvisi `Room`, `Reservation` a `Payment`.
- Proc jsou wireframy vhodne pred samotnou implementaci.

## Mozna rozsireni

- administrace cenovych sezon,
- storno podminky,
- slevove kupony,
- vic jazyku a men,
- pokojove balicky,
- notifikace e-mailem nebo SMS.
