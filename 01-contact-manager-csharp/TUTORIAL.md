# Tutorial - spravce kontaktu v C#

## Cil ulohy

Cilem je vytvorit konzolovou aplikaci pro spravu kontaktu. Aplikace musi umet zakladni operace CRUD, vyhledavani, ukladani dat do souboru a pouzit navrhove vzory Prototype, Command a Iterator. Dulezite je, aby aplikacni logika nebyla svazana jen s konzolovym vstupem, proto lze stejne tridy testovat pomoci xUnit testu.

Hotove reseni je jednoducha, ale kompletni ukazka objektoveho navrhu: kontakt je domenovy objekt, `ContactBook` drzi kolekci kontaktu, prikazy meni stav aplikace a `JsonContactStore` resi trvalou persistenci.

## Teoreticky zaklad

### Objektove programovani

Kontaktovy system je vhodny priklad objektove aplikace. Kazda dulezita vec v realnem svete ma v kodu vlastni typ:

- `Contact` reprezentuje jednu osobu nebo organizaci.
- `ContactBook` reprezentuje seznam kontaktu a operace nad nim.
- `IContactCommand` reprezentuje jednu zmenu, kterou lze provest a vratit zpet.
- `JsonContactStore` reprezentuje uloziste na disku.

Objektovy navrh snizuje zavislosti mezi castmi programu. Konzolove menu pouze vola metody domenovych trid, ale samo neresuje, jak se kontakt ulozi, aktualizuje nebo vyhleda.

### Prototype

Vzor Prototype slouzi k vytvareni kopii objektu bez toho, aby zbytek programu znal detaily konstruktoru. V tomto projektu ho reprezentuje rozhrani `IPrototype<T>` a metoda `Contact.Clone()`.

Smysl v teto uloze:

- lze vytvorit kopii existujiciho kontaktu,
- pri upravach neni nutne menit puvodni instanci okamzite,
- prikaz `UpdateContactCommand` si muze pamatovat predchozi stav pro `Undo`.

Typicka obhajoba: Prototype je vhodny tam, kde chceme kopirovat objekt se stejnymi hodnotami a nechceme znovu rucne skladat vsechny jeho vlastnosti.

### Command

Vzor Command zabali akci do objektu. Misto aby program primo volal `Add`, `Update` nebo `Delete`, vytvori prikaz:

- `AddContactCommand`
- `UpdateContactCommand`
- `DeleteContactCommand`

Kazdy prikaz ma metody `Execute()` a `Undo()`. Diky tomu lze po provedeni prikaz ulozit do historie a pozdeji ho vratit.

Princip historie je zasobnik: posledni provedena akce se vraci jako prvni. To odpovida beznemu chovani funkce Undo v aplikacich.

### Iterator

Vzor Iterator umoznuje prochazet kolekci bez znalosti vnitrni reprezentace. `ContactBook` implementuje `IEnumerable<Contact>`, a proto lze kontakty prochazet napr. pomoci `foreach`.

Vyhoda:

- vnejsi kod nemusi vedet, jestli jsou kontakty ulozene v `List<Contact>`, poli nebo jine kolekci,
- `ContactBook` si ponechava kontrolu nad tim, jak se data pridavaji, mazou a hledaji.

### JSON persistence

Data jsou ukladana do souboru `contacts.json`. JSON je vhodny pro jednoduchou ukazkovou aplikaci, protoze je citelny pro cloveka a .NET ma pripraveny serializer `System.Text.Json`.

Persistence je oddelena do `JsonContactStore`, aby se pripadne dala pozdeji vymenit za databazi bez velkeho zasahu do zbytku programu.

## Postup reseni

### 1. Navrhnout domenovy model

Nejprve je potreba urcit, jaka data kontakt obsahuje. Minimalni sada je:

- `Id` jako `Guid`, aby bylo mozne kontakt jednoznacne identifikovat,
- jmeno,
- e-mail,
- telefon,
- poznamka.

`Id` je lepsi nez samotne jmeno, protoze dve osoby mohou mit stejne jmeno.

### 2. Implementovat `Contact`

Trida `Contact` obsahuje vlastnosti kontaktu a metodu `Clone()`. Pri kopirovani zustava zachovane `Id`, protoze se jedna o kopii stejneho kontaktu. Kdyby se delalo "duplikovat jako novy kontakt", bylo by mozne vytvorit nove `Id`.

### 3. Implementovat `ContactBook`

`ContactBook` je hlavni trida pro praci s kolekci. Ma obsahovat metody:

- pridani kontaktu,
- aktualizace podle `Id`,
- smazani podle `Id`,
- nalezeni podle `Id`,
- vyhledavani podle textu,
- pruchod kolekci pres `IEnumerable<Contact>`.

V teto casti je dulezite validovat duplicity `Id` a nevracet primou kontrolu nad vnitrnim seznamem.

### 4. Pridat prikazy

Kazda zmena nad `ContactBook` se zabali do prikazu:

- `AddContactCommand` pri `Execute()` prida kontakt a pri `Undo()` ho smaze.
- `DeleteContactCommand` pri `Execute()` kontakt smaze a pri `Undo()` ho vrati.
- `UpdateContactCommand` si pred upravou ulozi starou kopii kontaktu a pri `Undo()` ji obnovi.

Tato cast je nejdulezitejsi pro splneni vzoru Command.

### 5. Pridat `CommandHistory`

Historie uchovava provedene prikazy. Pri provedeni prikazu se zavola `Execute()` a prikaz se ulozi do zasobniku. Pri undo se ze zasobniku vezme posledni prikaz a zavola se `Undo()`.

Pokud je zasobnik prazdny, aplikace ma uzivateli oznamit, ze neni co vratit.

### 6. Pridat JSON uloziste

`JsonContactStore` ma dve odpovednosti:

- nacist seznam kontaktu ze souboru,
- ulozit seznam kontaktu do souboru.

Pokud soubor neexistuje, aplikace zacina s prazdnym seznamem. Tato situace neni chyba, ale bezny prvni start programu.

### 7. Vytvorit konzolove menu

Konzolova cast ma byt jen tenka vrstva. Ma zobrazit moznosti, nacist vstup od uzivatele a zavolat prislusnou metodu nebo prikaz.

Doporucene volby menu:

1. vypsat kontakty,
2. pridat kontakt,
3. upravit kontakt,
4. smazat kontakt,
5. hledat,
6. vratit posledni akci,
7. ulozit a ukoncit.

### 8. Otestovat logiku

Testy maji pokryt hlavne aplikacni logiku:

- pridani kontaktu,
- vyhledavani,
- aktualizaci,
- smazani,
- undo pro vice typu prikazu,
- serializaci do JSON.

Konzolovy vstup se v teto ukazce netestuje, protoze nejvetsi riziko je v logice prace s kontakty.

## Vyvoj od nuly

Tato cast popisuje prakticky postup, jako kdyby projekt jeste neexistoval.

1. Vytvorit slozku a zakladni .NET projekty.

```powershell
mkdir 01-contact-manager-csharp
cd 01-contact-manager-csharp
dotnet new console -n ContactManager.App -o .\src\ContactManager.App
dotnet new xunit -n ContactManager.Tests -o .\tests\ContactManager.Tests
dotnet add .\tests\ContactManager.Tests\ContactManager.Tests.csproj reference .\src\ContactManager.App\ContactManager.App.csproj
```

2. Nejprve napsat minimalni model `Contact`.

V prvni verzi staci vlastnosti `Id`, `Name`, `Email`, `Phone` a `Note`. Hned zde se prida rozhrani `IPrototype<Contact>` a metoda `Clone()`. V teto fazi jeste neni potreba konzolove menu.

3. Pridat `ContactBook` a prvni test.

Implementuje se pridani kontaktu, vypsani kontaktu a hledani podle `Id`. Hned pote se napise test, ktery overi, ze pridany kontakt lze najit. Tim se overi zaklad domeny.

4. Doplnit vyhledavani.

Vyhledavani se udela nad jmenem, e-mailem a telefonem. Je vhodne resit ho case-insensitive, aby uzivatel nemusel presne dodrzovat velikost pismen.

5. Pridat Command pattern.

Nejprve vznikne rozhrani `IContactCommand`. Potom se postupne vytvori `AddContactCommand`, `UpdateContactCommand` a `DeleteContactCommand`. Po kazdem prikazu se napise test pro `Execute()` a `Undo()`.

6. Pridat `CommandHistory`.

Historie se implementuje az ve chvili, kdy jednotlive prikazy funguji samostatne. Pouzije se zasobnik, do ktereho se ukladaji provedene prikazy.

7. Pridat JSON persistenci.

Vytvori se `JsonContactStore`, ktery umi `Load()` a `Save()`. Test muze pouzit docasny soubor, aby se nepracovalo s realnymi daty uzivatele.

8. Teprve nakonec napsat konzolove menu.

Menu ma jen nacitat vstupy a volat existujici logiku. Pokud se pri tvorbe menu objevi potreba slozitejsi logiky, patri spise do `ContactBook`, prikazu nebo uloziste.

9. Projit rucni scenar.

Spustit aplikaci, pridat dva kontakty, jeden upravit, jeden smazat, vyzkouset undo, ulozit a znovu spustit aplikaci. Tim se overi i persistence.

10. Uklidit dokumentaci.

Do `README.md` patri rychle spusteni. Do `TUTORIAL.md` patri vysvetleni vzoru, postup vyvoje a obhajoba.

## Jak projekt spustit

Z adresare projektu:

```powershell
dotnet run --project .\src\ContactManager.App\ContactManager.App.csproj
```

Testy:

```powershell
dotnet test .\tests\ContactManager.Tests\ContactManager.Tests.csproj
```

Po spusteni aplikace vznikne soubor `contacts.json` ve spousteci slozce. Ten lze otevrit v editoru a zkontrolovat, ze se data ukladaji jako JSON pole kontaktu.

## Co umet vysvetlit u obhajoby

- Proc je `Guid` vhodny identifikator kontaktu.
- Jak funguje `Clone()` a proc patri ke vzoru Prototype.
- Proc ma prikaz metody `Execute()` a `Undo()`.
- Proc se pro undo pouziva zasobnik.
- Jak `IEnumerable<Contact>` skryva vnitrni ulozeni kontaktu.
- Proc je persistence oddelena od konzoloveho menu.

## Mozna rozsireni

- import a export kontaktu do CSV,
- vice ulozist, napr. SQLite,
- validace formatu e-mailu a telefonu,
- filtrovani podle skupin kontaktu,
- redo historie vedle undo historie.
