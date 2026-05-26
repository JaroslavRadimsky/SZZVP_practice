# Tutorial - navrh systemu spravy projektu

## Cil ulohy

Cilem je navrhnout system pro spravu projektu v malych tymech. Projekt obsahuje UML diagramy a staticke HTML wireframy. Nejde o implementaci backendu, ale o analyzu, navrh procesu a obrazovek.

Vystupy:

- use-case diagram,
- activity diagram pro praci s ukolem,
- state diagram zivotniho cyklu ukolu,
- wireframy domovske stranky, detailu projektu, pridani ukolu a kanban nastenky.

## Teoreticky zaklad

### Projektove rizeni

System spravy projektu pomaha tymu sledovat:

- projekty,
- ukoly,
- odpovedne osoby,
- terminy,
- priority,
- stav prace,
- komunikaci a prilohy.

U malych tymu je dulezite, aby system nebyl zbytecne slozity. Hlavni tok je zalozeni projektu, pridani ukolu, prirazeni clena a sledovani stavu.

### Use-case diagram

Use-case diagram popisuje, co mohou jednotlivi aktori v systemu delat. V projektu jsou aktori:

- `Clen tymu`,
- `Projektovy manazer`,
- `System exportu`.

Pripady uziti zahrnuji vytvoreni projektu, spravu clenu, pridani ukolu, aktualizaci stavu, komentare, prilohy, dashboard a reporty.

Use-case diagram je analyticky pohled. Nema resit databazove tabulky ani konkretni HTML formular.

### Activity diagram

Activity diagram popisuje tok cinnosti. V tomto projektu ukazuje postup prace s ukolem:

1. vyber projektu,
2. otevreni formulare ukolu,
3. vyplneni nazvu, popisu, priority a terminu,
4. prirazeni clena tymu,
5. ulozeni ukolu,
6. aktualizace stavu nebo komentaru,
7. zahrnuti do reportu.

Activity diagram je vhodny pro procesy s rozhodovanim a opakovanim.

### State diagram

State diagram popisuje zivotni cyklus jednoho objektu. V tomto projektu je objektem ukol.

Stavy:

- `Backlog`,
- `Todo`,
- `InProgress`,
- `Review`,
- `Done`,
- `Archived`.

Prechody mezi stavy odpovidaji udalostem, napr. "clen zacal praci" nebo "prijato".

### Kanban

Kanban je vizualni metoda rizeni prace. Ukoly jsou ve sloupcich podle stavu. Typicke sloupce jsou:

- Backlog,
- To do,
- In progress,
- Review,
- Done.

Kanban pomaha tymu rychle videt, co se dela, co ceka a kde se prace zasekava.

### Wireframe

Wireframe je hruby navrh obrazovky. Ukazuje strukturu, navigaci a obsah. V tomto projektu jsou wireframy jako staticke HTML soubory, aby slo navrh otevrit v prohlizeci a verzovat v Gitu.

## Postup reseni

### 1. Urcit hlavni cile systemu

Nejprve je potreba popsat, proc system existuje:

- tym potrebuje prehled o projektech,
- clenove potrebuji videt svoje ukoly,
- manazer potrebuje reporty,
- tym potrebuje komunikovat u ukolu,
- vystupy je potreba exportovat.

Z techto cilu vzniknou pripady uziti.

### 2. Urcit aktory

Aktori jsou role mimo system. V tomto navrhu:

- `Projektovy manazer` zaklada projekty, spravuje role a generuje reporty,
- `Clen tymu` pracuje s ukoly, komentari a prilohami,
- `System exportu` reprezentuje externi napojeni pro vystupy.

Rozliseni roli je dulezite pro budouci opravneni.

### 3. Vytvorit use-case diagram

Do hranice systemu se vlozi vsechny hlavni use-casy. Aktori se propoji s akcemi, ktere realne provadeji.

Vztah `include` se pouzije u reportu a exportu: generovani reportu zahrnuje export, pokud chce uzivatel soubor mimo system.

### 4. Popsat proces prace s ukolem

Activity diagram se soustredi na konkretni tok. Dulezite je ukazat rozhodnuti:

- je prirazeny clen?
- je potreba priloha?
- je ukol hotovy?

Diagram tak vysvetluje nejen idealni cestu, ale i bezne varianty.

### 5. Popsat zivotni cyklus ukolu

State diagram se soustredi na jeden ukol. Ukol zacina v `Backlog`, pote se muze dostat do `Todo`, `InProgress`, `Review`, `Done` a nakonec `Archived`.

Zpetny prechod z `Review` do `InProgress` je dulezity, protoze prace muze byt vratena k uprave.

### 6. Navrhnout wireframy

Wireframy pokryvaji hlavni scenare:

- domovska stranka s prehledem,
- detail projektu,
- pridani ukolu,
- kanban nastenka.

Pri navrhu obrazovek je vhodne myslet na to, co uzivatel potrebuje udelat nejcasteji: najit ukol, zmenit stav, pridat komentar a zkontrolovat postup.

### 7. Overit konzistenci navrhu

Po dokonceni je potreba zkontrolovat:

- zda kazdy stav ukolu z diagramu odpovida sloupci v kanbanu,
- zda use-casy maji podporu ve wireframech,
- zda activity diagram odpovida realnemu workflow,
- zda role davaji smysl pro opravneni.

## Vyvoj od nuly

U tohoto projektu je vyvojovy postup zamereny na analyzu a navrh. Cilem je pripravit podklady, podle kterych by slo pozdeji aplikaci implementovat.

1. Vytvorit strukturu projektu.

```powershell
mkdir 09-project-management-design
cd 09-project-management-design
mkdir uml, wireframes
New-Item .\uml\use-case.puml, .\uml\activity-task-flow.puml, .\uml\state-task.puml
New-Item .\wireframes\index.html, .\wireframes\styles.css
New-Item .\README.md, .\TUTORIAL.md
```

2. Sepsat hlavni potreby tymu.

Pred kreslenim diagramu se napise kratky seznam: tym potrebuje projekty, ukoly, clenstvi, stavy, komentare, prilohy a reporty.

3. Urcit role.

Rozdeli se `Projektovy manazer`, `Clen tymu` a externi `System exportu`. Toto rozdeleni pozdeji ovlivni opravneni.

4. Vytvorit use-case diagram.

Do `use-case.puml` se zapisi cile uzivatelu. Dulezite je zapisovat "Spravovat cleny tymu" nebo "Generovat report", ne technicke kroky typu "Kliknout na tlacitko".

5. Popsat proces pridani ukolu textem.

Pred activity diagramem se napise hlavni proces: vyber projektu, otevreni formulare, vyplneni udaju, prirazeni clena, ulozeni, aktualizace stavu.

6. Nakreslit activity diagram.

Do `activity-task-flow.puml` se prevede textovy proces. Pridaji se rozhodovaci body, napr. zda je prirazeny clen a zda je potreba priloha.

7. Navrhnout stavy ukolu.

Ukol ma projit stavy `Backlog`, `Todo`, `InProgress`, `Review`, `Done`, `Archived`. Pred kreslenim se urci, ktere prechody jsou povolene.

8. Nakreslit state diagram.

Do `state-task.puml` se zapisi stavy a prechody. Zpetny prechod z `Review` do `InProgress` je dulezity pro opravy.

9. Navrhnout wireframy.

Nejdrive se urci obrazovky: dashboard, detail projektu, pridani ukolu, kanban. Potom se v HTML vytvori staticky prototyp.

10. Propojit navrh.

Zkontroluje se, ze kanban sloupce odpovidaji stavum ukolu, ze formular pridani ukolu odpovida activity diagramu a ze dashboard podporuje use-case "Zobrazit dashboard".

11. Pripravit obhajobu.

Do tutorialu a README se doplni, co ktery diagram ukazuje a proc byl zvolen. U navrhove ulohy je schopnost vysvetlit diagramy stejne dulezita jako jejich existence.

## Jak projekt prohlednout

Wireframy:

```text
wireframes/index.html
```

UML soubory:

```text
uml/use-case.puml
uml/activity-task-flow.puml
uml/state-task.puml
```

## Co umet vysvetlit u obhajoby

- Rozdil mezi use-case, activity a state diagramem.
- Proc je ukol vhodny objekt pro stavovy diagram.
- Jak se kanban sloupce vztahuji ke stavum ukolu.
- Proc ma manazer jina opravneni nez clen tymu.
- Kde se v navrhu objevuji reporty a export.
- Proc jsou wireframy uzitecne pred implementaci.

## Mozna rozsireni

- Ganttuv diagram,
- casove odhady a vykazy prace,
- upozorneni na blizici se terminy,
- integrace s Git repozitarem,
- sprinty a backlog,
- auditni historie zmen ukolu.
