# 06 - PHP XML zapisnik ukolu

Webova aplikace v PHP uklada uzivatele a ukoly do XML souboru.

## Funkce

- Registrace a prihlaseni uzivatelu.
- Hesla jsou ulozena pomoci `password_hash`.
- Pridani, editace, mazani a filtrovani ukolu.
- Kategorie a stavy ukolu.
- Import ukolu z XML souboru pres formular.
- XSD validace importovaneho ukolu.

## Spusteni

```powershell
php -S localhost:8080 -t .\public
```

Otevrit `http://localhost:8080`.

## Test

```powershell
php .\tests\smoke.php
```

