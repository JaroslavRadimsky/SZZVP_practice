# SZZVP - Aplikovana informatika

Workspace obsahuje samostatne projekty podle zadani v `SZZVP-SW.pdf` a `SZZVP-DB.pdf`.

## Projekty

1. `01-contact-manager-csharp` - konzolovy spravce kontaktu v C# s navrhovymi vzory Prototype, Command a Iterator.
2. `02-study-system-csharp` - konzolovy studijni system v C# s Factory Method, Observer a Strategy.
3. `03-fractal-tree-wpf` - WPF aplikace pro generovani fraktalniho stromu.
4. `04-ml-results-python` - Tkinter aplikace pro spravu vysledku ML modelu v SQLite.
5. `05-rss-reader-android` - Android RSS ctecka se seznamem, detailem, SQLite cache a periodickou aktualizaci.
6. `06-php-xml-todo` - PHP zapisnik ukolu s XML ulozistem, prihlasenim a XSD importem.
7. `07-php-inventory-sqlite` - PHP skladovy system s PDO SQLite a JSON API.
8. `08-hotel-reservation-design` - UML a HTML wireframy rezervacniho systemu.
9. `09-project-management-design` - UML a HTML wireframy systemu spravy projektu.
10. `10-db-01-relational-olap` - PostgreSQL projekt pro relacni model, ETL, SQL agregace, vizualizace a OLAP navrh.
11. `11-db-02-nosql-mongodb` - MongoDB projekt s dokumentovym modelem, agregacnimi pipelines a vizualizaci.
12. `12-db-04-auto-mpg-ml` - Python/Jupyter protokol pro Auto MPG regresi a porovnani ML modelu.
13. `27-activity-tracker-android` - Android aplikace pro zaznam fyzicke aktivity, SQLite, senzory, statistiky, graf a CSV export.

Kazdy projekt ma vlastni `README.md` s navodem ke spusteni a overeni a `TUTORIAL.md` s podrobnym postupem reseni, vyvojem od nuly a vysvetlenim teoretickych prvku.

## Python prostredi pro DB projekty

```powershell
.\.venv\Scripts\python.exe -m pip install -r .\requirements-db.txt
```
