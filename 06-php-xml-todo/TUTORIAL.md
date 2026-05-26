# Tutorial - PHP XML zapisnik ukolu

## Cil ulohy

Cilem je vytvorit webovou aplikaci v cistem PHP, ktera umozni registraci, prihlaseni a spravu ukolu. Data se neukladaji do SQL databaze, ale do XML souboru. Import ukolu musi byt validovan podle XSD schematu.

Projekt obsahuje:

- `Auth.php` pro registraci a prihlaseni,
- `XmlStore.php` pro nacitani a ukladani XML dokumentu,
- `TaskRepository.php` pro CRUD ukolu,
- `public/index.php` jako webovy vstup,
- `schemas/task.xsd` pro validaci importu,
- `storage/users.xml` a `storage/tasks.xml` jako datove uloziste.

## Teoreticky zaklad

### PHP a server-side aplikace

PHP bezi na serveru. Prohlizec posle HTTP pozadavek, PHP skript ho zpracuje a vrati HTML odpoved. Formularove akce pouzivaji typicky metodu `POST`, zobrazeni stranek typicky `GET`.

V tomto projektu neni pouzit framework. Je tak videt zakladni princip:

- nacteni pozadavku,
- validace vstupu,
- prace s ulozistem,
- priprava HTML odpovedi.

### Session

Session slouzi k uchovani prihlaseneho uzivatele mezi HTTP pozadavky. Po uspesnem prihlaseni se do session ulozi `user_id` a uzivatelske jmeno. Dalsi pozadavky pak poznaji, kdo je prihlasen.

Bez session by byl kazdy HTTP pozadavek nezavisly a aplikace by si nepamatovala prihlaseni.

### Bezpecne ukladani hesel

Hesla se nikdy nemaji ukladat jako cisty text. PHP poskytuje:

- `password_hash()` pro vytvoreni hashe,
- `password_verify()` pro overeni hesla.

Hash je jednosmerny otisk hesla. Pokud nekdo ziska XML soubor uzivatelu, nema primo puvodni hesla.

### XML jako uloziste

XML je textovy strukturovany format. V teto uloze nahrazuje databazi. Vyhoda je citelnost a jednoducha kontrola obsahu, nevyhoda je horsi vykon a slozitejsi soubezny pristup pri vetsim mnozstvi dat.

Projekt pouziva `DOMDocument`, protoze umoznuje bezpecne pracovat s XML strukturou jako se stromem elementu. To je lepsi nez skladat XML pomoci retezcove konkatenace.

### XSD validace

XSD schema popisuje povolenou strukturu XML dokumentu. Importovany ukol musi odpovidat souboru `schemas/task.xsd`.

Schema vyzaduje:

- `title`,
- volitelny `description`,
- `category`,
- `status` s hodnotami `nezahajene`, `zahajene`, `dokoncene`,
- volitelny `dueDate`.

Validace chrani aplikaci pred importem neplatnych dat.

### CRUD

CRUD je zkratka pro zakladni operace nad daty:

- Create - vytvorit ukol,
- Read - zobrazit ukoly,
- Update - upravit ukol,
- Delete - smazat ukol.

V projektu tyto operace poskytuje `TaskRepository`.

## Postup reseni

### 1. Navrhnout XML soubory

Pro uzivatele vznikne `storage/users.xml`, pro ukoly `storage/tasks.xml`. Oddeleni uzivatelu a ukolu zjednodusuje praci s daty.

U ukolu je potreba ulozit:

- identifikator ukolu,
- identifikator vlastnika,
- nazev,
- popis,
- kategorii,
- stav,
- termin.

Identifikator vlastnika je dulezity, aby kazdy uzivatel videl jen svoje ukoly.

### 2. Implementovat `XmlStore`

`XmlStore` resi spolecne operace:

- pokud XML soubor existuje, nacte ho,
- pokud neexistuje, vytvori novy dokument s korenovym elementem,
- ulozi dokument na disk,
- pomaha cist a pridavat textove elementy.

Tato trida zabranuje duplicitam v kodu.

### 3. Implementovat autentizaci

`Auth` obsahuje:

- `register()` pro registraci noveho uzivatele,
- `login()` pro overeni hesla,
- `findByUsername()` pro kontrolu duplicity.

Pri registraci se heslo zahashuje. Pri prihlaseni se pouzije `password_verify()`.

### 4. Implementovat repository ukolu

`TaskRepository` poskytuje:

- `allForUser()` pro vypsani ukolu prihlaseneho uzivatele,
- `add()` pro pridani,
- `update()` pro upravu,
- `delete()` pro smazani,
- `importXml()` pro import s XSD validaci.

Metody vzdy pracuji s `userId`, aby uzivatel nemohl menit cizi ukoly.

### 5. Doplnit filtrovani

Filtrovani podle kategorie a stavu je vhodne resit pri vypisu ukolu. Repository muze vratit jen ty ukoly, ktere odpovidaji zadanemu filtru.

Tento pristup je jednoduchy a pro XML soubor dostatecny. U SQL databaze by se filtr obvykle prelozil do `WHERE` podminky.

### 6. Vytvorit webove rozhrani

`public/index.php` funguje jako jednoduchy front controller. Podle odeslane akce:

- registruje uzivatele,
- prihlasuje,
- odhlasuje,
- pridava ukol,
- upravuje ukol,
- maze ukol,
- importuje XML.

HTML vystup musi escapovat uzivatelsky obsah pomoci `htmlspecialchars`, aby se snizilo riziko XSS.

### 7. Pridat XSD import

Import ukolu probiha takto:

1. uzivatel nahraje XML soubor,
2. PHP ho nacte do `DOMDocument`,
3. dokument se zvaliduje podle `schemas/task.xsd`,
4. z validniho XML se vytvori novy ukol,
5. ukol se priradi prihlasenemu uzivateli.

Pokud validace selze, aplikace ukol neulozi.

### 8. Napsat smoke test

`tests/smoke.php` overuje zakladni scenare:

- registraci,
- login,
- CRUD ukolu,
- validaci importu,
- praci s XML.

Smoke test neni rozsahla testovaci sada, ale rychle potvrdi, ze hlavni funkcionalita funguje.

## Vyvoj od nuly

PHP projekt je vhodne stavet od uloziste a autentizace k webovemu rozhrani.

1. Vytvorit strukturu projektu.

```powershell
mkdir 06-php-xml-todo
cd 06-php-xml-todo
mkdir public, src, storage, schemas, tests
New-Item .\public\index.php, .\public\styles.css
New-Item .\src\XmlStore.php, .\src\Auth.php, .\src\TaskRepository.php
New-Item .\schemas\task.xsd, .\tests\smoke.php
```

2. Pripravit XML uloziste.

Do `storage/users.xml` se vlozi korenovy element pro uzivatele. Do `storage/tasks.xml` se vlozi korenovy element pro ukoly. Pokud soubory nevzniknou rucne, musi je umet zalozit `XmlStore`.

3. Implementovat `XmlStore`.

Nejdrive se resi jen nacteni a ulozeni XML. Tato trida je zaklad pro vse ostatni. Rychla kontrola je nacist prazdny dokument, pridat element a ulozit ho.

4. Implementovat registraci.

V `Auth.php` vznikne metoda `register()`. Kontroluje duplicitu jmena a uklada hash hesla pres `password_hash()`. V teto fazi jeste neni prihlasovaci formular, staci smoke test.

5. Implementovat login.

`login()` najde uzivatele podle jmena a overi heslo pomoci `password_verify()`. Test overi spravne i spatne heslo.

6. Navrhnout XSD.

Do `schemas/task.xsd` se zapise struktura jednoho importovaneho ukolu. Je lepsi udelat schema pred importem, aby bylo jasne, jaka data aplikace prijima.

7. Implementovat CRUD ukolu.

V `TaskRepository.php` se postupne pridaji metody `add()`, `allForUser()`, `update()` a `delete()`. Po kazde metode se doplni smoke test.

8. Pridat import XML.

`importXml()` nacte uploadovany soubor, zvaliduje ho pomoci XSD a teprve potom vytvori ukol. Pokud validace selze, data se neulozi.

9. Vytvorit webove rozhrani.

`public/index.php` nejdrive zobrazi registraci a login. Po prihlaseni se prida seznam ukolu a formular pro pridani. Az potom se doplni editace, mazani, filtry a import.

10. Dodat ochranu vystupu.

Vsechny hodnoty z XML, ktere se vypisuji do HTML, musi projit pres `htmlspecialchars`. Tato cast se kontroluje pri kazdem novem vypisu.

11. Provest rucni scenar.

Registrovat uzivatele, prihlasit se, pridat ukol, upravit stav, filtrovat, smazat ukol a importovat validni XML soubor.

## Jak projekt spustit

```powershell
php -S localhost:8080 -t .\public
```

Potom otevrit:

```text
http://localhost:8080
```

Smoke test:

```powershell
php .\tests\smoke.php
```

## Co umet vysvetlit u obhajoby

- Proc se hesla ukladaji pomoci `password_hash`.
- Jak session udrzuje prihlaseneho uzivatele.
- Jak `DOMDocument` pracuje s XML stromem.
- Co kontroluje XSD schema.
- Proc musi mit kazdy ukol `userId`.
- Jake jsou vyhody a nevyhody XML uloziste oproti SQL databazi.

## Mozna rozsireni

- zamykani XML souboru pri zapisu,
- export vsech ukolu uzivatele,
- validace formatu data,
- priority ukolu,
- sdilene ukoly mezi uzivateli,
- prechod na SQLite pri vetsim objemu dat.
