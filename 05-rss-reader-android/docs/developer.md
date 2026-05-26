# Vyvojarska dokumentace

## Architektura

Aplikace pouziva jednoduche vrstvy:

- `MainActivity` zobrazuje seznam zprav a spousti rucni aktualizaci.
- `DetailActivity` zobrazuje detail jedne zpravy.
- `RssFetcher` nacita RSS pres HTTP a predava XML parseru.
- `RssParser` prevadi RSS XML na objekty `RssItem`.
- `RssDatabase` uklada a nacita relevantni zpravy pres `SQLiteOpenHelper`.
- `RssWorker` provadi periodickou aktualizaci na pozadi pres WorkManager.

## Offline chovani

Seznam se pri startu nacita ze SQLite. Pokud selze sitove nacteni, zustanou dostupne posledni ulozene relevantni zpravy.

## Testovani

Unit test `RssParserTest` overuje parsovani RSS XML fixture mimo Android UI.

