# Tutorial - PHP skladovy system se SQLite

## Cil ulohy

Cilem je vytvorit webovou aplikaci pro spravu skladu v cistem PHP. Aplikace pouziva SQLite databazi pres PDO, prihlaseni uzivatelu, role `admin` a `skladnik`, CRUD produktu, objednavky, upozorneni na nizky stav a JSON API.

Projekt obsahuje:

- `Database.php` pro pripojeni, migrace a vychozi uzivatele,
- `Auth.php` pro prihlaseni,
- `InventoryRepository.php` pro produkty a objednavky,
- `bootstrap.php` pro spolecne funkce,
- `public/index.php` pro webove rozhrani,
- `public/api/products.php` a `public/api/orders.php` pro JSON API.

## Teoreticky zaklad

### Relacni databaze

Relacni databaze uklada data do tabulek. V tomto projektu jsou hlavni tabulky:

- `users`,
- `products`,
- `orders`,
- `order_items`.

Vztahy mezi tabulkami jsou dulezite:

- jedna objednavka ma jednu nebo vice polozek,
- polozka objednavky odkazuje na produkt,
- objednavka ma stav.

SQLite je serverless databaze ulozena v souboru. Pro skolni a mensi webovou aplikaci je vhodna, protoze nevyzaduje instalaci databazoveho serveru.

### PDO

PDO je standardni PHP rozhrani pro praci s databazemi. Umoznuje pouzivat prepared statements, ktere oddeluji SQL dotaz od hodnot zadanych uzivatelem.

To je zakladni ochrana proti SQL injection. Misto skladani dotazu retezcem se pouzije:

```php
$stmt = $pdo->prepare('SELECT * FROM users WHERE username = ?');
$stmt->execute([$username]);
```

Databaze pak hodnotu bere jako data, ne jako cast SQL prikazu.

### Role a opravneni

Aplikace rozlisuje role:

- `admin` muze spravovat produkty,
- `skladnik` pracuje hlavne se skladem a objednavkami.

Role-based access control znamena, ze aplikace kontroluje, kdo smi provest danou akci. V projektu jsou k tomu pomocne funkce `require_login()` a `require_admin()`.

### Transakce

Vytvoreni objednavky meni vice veci najednou:

1. vlozi se objednavka,
2. vlozi se polozka objednavky,
3. snizi se skladove mnozstvi.

Tyto kroky musi probehnout jako jeden celek. Pokud by se treti krok nepovedl, nesmi zustat objednavka ulozena napul. Proto se pouziva transakce `beginTransaction()`, `commit()` a `rollBack()`.

### JSON API

JSON API umoznuje integraci s jinymi systemy. Aplikace poskytuje endpointy pro produkty a objednavky. Odpovedi jsou ve formatu JSON a pristup je chranen hlavickou `X-API-Key`.

API je oddelene od HTML rozhrani. Stejna repository vrstva ale muze slouzit obema castem.

## Postup reseni

### 1. Navrhnout databazove schema

Tabulka `products` obsahuje:

- `sku`,
- nazev,
- popis,
- mnozstvi,
- minimalni mnozstvi,
- cenu.

Tabulka `orders` obsahuje zakladni informace o objednavce a stav. Tabulka `order_items` uklada konkretni produkty v objednavce.

Cizi klice zajistuji, ze polozka objednavky odkazuje na existujici objednavku a produkt.

### 2. Implementovat databazovou tridu

`Database` vytvori PDO pripojeni, zapne vyhazovani vyjimek a spusti migrace. Migrace vytvori tabulky, pokud jeste neexistuji.

Po migraci se zalozi vychozi uzivatele:

- `admin` / `admin123`,
- `skladnik` / `skladnik123`.

Hesla jsou ulozena jako hash.

### 3. Implementovat prihlaseni

`Auth` nacte uzivatele podle jmena a overi heslo pomoci `password_verify()`. Po uspesnem prihlaseni se uzivatel ulozi do session.

Pri kazde chranene akci se kontroluje, zda je uzivatel prihlaseny.

### 4. Implementovat repository

`InventoryRepository` obsahuje databazove operace:

- `products()` pro seznam produktu,
- `addProduct()` pro pridani,
- `updateProduct()` pro upravu,
- `deleteProduct()` pro smazani,
- `createOrder()` pro vytvoreni objednavky,
- `orders()` pro seznam objednavek,
- `updateOrderStatus()` pro zmenu stavu.

Vsechny vstupy se predavaji pres prepared statements.

### 5. Resit nizky stav zasob

Dotaz na produkty pocita sloupec `low_stock` jako `quantity <= min_quantity`. UI pak muze tyto produkty zvyraznit.

Tento pristup je jednoduchy a jasny. Alternativou by byl SQL view nebo samostatny report.

### 6. Vytvorit objednavku v transakci

Pri vytvareni objednavky se nejprve zkontroluje dostupne mnozstvi. Pokud je dostatek zasob, vlozi se objednavka a snizi se stav skladu.

Pokud nastane chyba, transakce se vrati zpet. Tim se zabrani nekonzistentnimu stavu.

### 7. Vytvorit webove rozhrani

`public/index.php` zobrazuje:

- prihlasovaci formular,
- seznam produktu,
- formular pro produkt,
- upozorneni na nizke stavy,
- seznam objednavek,
- formular pro novou objednavku,
- zmenu stavu objednavky.

Vystup uzivatelskych hodnot se escapuje funkci `h()`.

### 8. Vytvorit JSON API

Endpointy:

- `GET /api/products.php`,
- `POST /api/products.php`,
- `GET /api/orders.php`,
- `POST /api/orders.php`.

API kontroluje hlavicku `X-API-Key: demo-key`. Pri odpovedi nastavuje `Content-Type: application/json`.

### 9. Napsat smoke test

`tests/smoke.php` overuje:

- migraci databaze,
- prihlaseni,
- praci s produkty,
- vytvoreni objednavky,
- snizeni skladoveho mnozstvi.

## Vyvoj od nuly

U skladoveho systemu je nejlepsi postupovat od databaze pres repository k webu a API.

1. Vytvorit strukturu projektu.

```powershell
mkdir 07-php-inventory-sqlite
cd 07-php-inventory-sqlite
mkdir public, public\api, src, data, tests
New-Item .\public\index.php, .\public\styles.css
New-Item .\public\api\products.php, .\public\api\orders.php
New-Item .\src\Database.php, .\src\Auth.php, .\src\InventoryRepository.php, .\src\bootstrap.php
New-Item .\tests\smoke.php
```

2. Navrhnout databazove tabulky.

Pred psanim PHP se sepise schema: `users`, `products`, `orders`, `order_items`. U produktu se rozhodne, ze nizky stav je `quantity <= min_quantity`.

3. Implementovat `Database`.

Trida vytvori PDO spojeni, zapne vyjimky, vytvori tabulky a zalozi vychozi uzivatele. Prvni smoke test overi, ze databazovy soubor vznikne a tabulky existuji.

4. Implementovat `Auth`.

Prida se login pres `password_verify()`. V `bootstrap.php` vzniknou helpery `current_user()`, `require_login()` a `require_admin()`.

5. Implementovat produkty v repository.

Postupne se pridaji metody `products()`, `addProduct()`, `updateProduct()` a `deleteProduct()`. Vsechny SQL prikazy maji pouzivat `prepare()` a `execute()`.

6. Implementovat objednavky.

`createOrder()` se dela v transakci. Nejdrive overi zasobu, potom vlozi objednavku, polozku a snizi mnozstvi produktu. Smoke test ma overit i snizeni skladu.

7. Vytvorit webove UI.

`public/index.php` nejdrive zobrazi login. Po prihlaseni se prida tabulka produktu, formular produktu a objednavky. Adminske akce se schovaji za `require_admin()`.

8. Pridat zmenu stavu objednavky.

Stavy se povoli jen z pevneho seznamu. To brani ulozeni nahodnych nebo neplatnych hodnot.

9. Vytvorit JSON API.

Endpointy `products.php` a `orders.php` maji podporovat `GET` a `POST`. Na zacatku kontroluji `X-API-Key`. Odpovedi vraceji JSON a HTTP status podle vysledku.

10. Provest rucni scenar.

Prihlasit se jako admin, zalozit produkt, prihlasit se jako skladnik, vytvorit objednavku, zmenit stav a zavolat API pres `curl`.

11. Zkontrolovat bezpecnost.

Projit, zda se nepouziva skladani SQL z uzivatelskych hodnot, zda se HTML escapuje a zda adminske akce nejdou bez role admin.

## Jak projekt spustit

```powershell
php -S localhost:8081 -t .\public
```

Potom otevrit:

```text
http://localhost:8081
```

Smoke test:

```powershell
php .\tests\smoke.php
```

Ukazka API:

```powershell
curl -H "X-API-Key: demo-key" http://localhost:8081/api/products.php
curl -H "X-API-Key: demo-key" http://localhost:8081/api/orders.php
```

## Co umet vysvetlit u obhajoby

- Jak tabulky `orders` a `order_items` modeluji objednavku.
- Proc je PDO s prepared statements ochrana proti SQL injection.
- Proc je pro vytvoreni objednavky potreba transakce.
- Jak se pocita nizky stav zasob.
- Jak funguje role `admin` a `skladnik`.
- Proc je JSON API uzitecne pro integraci.

## Mozna rozsireni

- vice polozek v jedne objednavce,
- historie pohybu skladu,
- export skladu do CSV,
- REST endpointy s hezcimi URL,
- expirace API klicu,
- detailnejsi prava roli.
