# 07 - PHP skladovy system se SQLite

Webova aplikace pro spravu skladu v cistem PHP. Data jsou ulozena v relacni SQLite databazi pres PDO.

## Funkce

- Prihlaseni uzivatelu a role `admin` / `skladnik`.
- CRUD produktu, skladove mnozstvi a upozorneni na nizky stav.
- Vytvareni objednavek a sledovani stavu.
- Ochrana proti SQL injection pres prepared statements.
- JSON API pro produkty a objednavky.

## Vychozi ucty

- `admin` / `admin123`
- `skladnik` / `skladnik123`

## Spusteni

```powershell
php -S localhost:8081 -t .\public
```

Otevrit `http://localhost:8081`.

## API

API vyzaduje header `X-API-Key: demo-key`.

```powershell
curl -H "X-API-Key: demo-key" http://localhost:8081/api/products.php
curl -H "X-API-Key: demo-key" http://localhost:8081/api/orders.php
```

## Test

```powershell
php .\tests\smoke.php
```

