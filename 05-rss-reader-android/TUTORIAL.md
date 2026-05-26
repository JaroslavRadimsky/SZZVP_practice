# Tutorial - Android RSS ctecka

## Cil ulohy

Cilem je vytvorit Android aplikaci v Jave, ktera nacte RSS kanal, zobrazi seznam zprav, umozni otevrit detail a ulozi zpravy do lokalni SQLite cache. Aplikace ma fungovat i offline a pravidelne aktualizovat data pomoci WorkManageru.

Projekt ukazuje typickou strukturu Android aplikace:

- `MainActivity` pro hlavni seznam,
- `DetailActivity` pro detail zpravy,
- `RecyclerView` pro efektivni seznam,
- `RssFetcher` a `RssParser` pro nacitani a parsovani RSS,
- `RssDatabase` pro SQLite cache,
- `RssWorker` pro periodickou aktualizaci.

## Teoreticky zaklad

### Android Activity

`Activity` reprezentuje jednu obrazovku aplikace. V projektu jsou dve:

- `MainActivity` zobrazuje seznam RSS zprav,
- `DetailActivity` zobrazuje detail vybrane zpravy.

Metoda `onCreate()` je zakladni startovni bod obrazovky. Zde se nastavuje layout, inicializuji prvky a spousti nacitani dat.

### Intent

`Intent` slouzi k prechodu mezi aktivitami a k predani dat. Pri kliknuti na zpravu `MainActivity` vytvori `Intent`, vlozi do nej titulek, odkaz a popis zpravy a spusti `DetailActivity`.

V mensi aplikaci je predani hodnot pres `Intent` jednoduche a prehledne. Ve vetsi aplikaci by se casto predavalo jen `id` a detail by se nacetl z databaze.

### RecyclerView

`RecyclerView` je moderni Android komponenta pro seznamy. Recykluje radky, takze je efektivni i pro delsi seznamy.

Projekt obsahuje:

- `RssAdapter`, ktery prevadi `RssItem` na radek v seznamu,
- `ViewHolder`, ktery drzi referenci na UI prvky radku,
- `ItemClickListener`, ktery predava kliknuti z adapteru do aktivity.

### RSS a XML parsovani

RSS je XML format pro publikovani novinek. Typicka zprava je v elementu `item` a obsahuje:

- `title`,
- `link`,
- `description`,
- `pubDate`.

`RssParser` cte XML proud a prevadi ho na seznam objektu `RssItem`. Test parseru pouziva lokalni XML fixture, aby nebyl zavisly na internetu.

### SQLite cache

Offline rezim vyzaduje lokalni ulozeni zprav. Android poskytuje SQLite pres `SQLiteOpenHelper`.

`RssDatabase` resi:

- vytvoreni tabulky,
- ulozeni nebo aktualizaci zprav,
- vypsani relevantnich zprav,
- filtrovani starsich zprav.

Cache je prakticka i pri beznem online rezimu, protoze aplikace muze okamzite zobrazit posledni ulozena data a az pote je obnovit ze site.

### WorkManager

WorkManager slouzi pro odlozenou a periodickou praci na pozadi. Je vhodny pro pravidelnou synchronizaci, protoze respektuje stav zarizeni a systemove limity Androidu.

`RssWorker` v projektu pravidelne stahuje vychozi RSS kanal a uklada zpravy do SQLite.

## Postup reseni

### 1. Pripravit Android projekt

Nejprve se vytvori Gradle projekt s Android pluginem. Aplikace je napsana v Jave, aby odpovidala pozadavkum zadani.

Dulezite casti:

- `settings.gradle`,
- korenovy `build.gradle`,
- `app/build.gradle`,
- `AndroidManifest.xml`.

Manifest musi obsahovat aktivitu seznamu, detailu a opravneni pro internet.

### 2. Vytvorit model `RssItem`

`RssItem` je jednoducha trida se zpravou. Obsahuje:

- `id`,
- `title`,
- `link`,
- `description`,
- `publishedAt`,
- `fetchedAt`.

Oddeleni modelu od UI pomaha udrzet kod citelny. Parser, databaze i adapter pracuji se stejnym typem.

### 3. Implementovat parser

`RssParser` ma nacist XML a najit vsechny elementy `item`. Z kazdeho itemu vytvori `RssItem`.

Pri implementaci je potreba osetrit:

- chybejici popis,
- prazdne datum,
- odlisne formaty datumu,
- fallback na cas stazeni.

Parser je dobra cast pro unit test, protoze nepotrebuje Android UI.

### 4. Implementovat stahovani

`RssFetcher` otevira URL a predava vstupni proud parseru. Sitove volani nesmi blokovat hlavni UI vlakno. Proto se v aplikaci spousti mimo hlavni vlakno.

Pokud se stazeni nepovede, aplikace stale muze zobrazit data z SQLite cache.

### 5. Implementovat SQLite cache

`RssDatabase` dedi z `SQLiteOpenHelper`. V `onCreate()` vytvori tabulku zprav. Metoda `upsert()` ulozi nove zpravy nebo aktualizuje existujici.

Pro offline zobrazeni se pouziva `listRelevant()`, ktera vraci ulozene relevantni zpravy. Relevance muze byt casove omezena, napr. na poslednich sedm dni.

### 6. Vytvorit hlavni obrazovku

`MainActivity` provede:

1. inicializaci `RecyclerView`,
2. inicializaci databaze,
3. nacteni poslednich zprav z databaze,
4. pokus o stazeni RSS kanalu,
5. ulozeni novych dat,
6. obnoveni adapteru.

Status text informuje, zda se data nacetla ze site nebo z cache.

### 7. Vytvorit detail zpravy

`DetailActivity` nacte hodnoty z `Intent` a zobrazi je v layoutu. Detail je oddelena aktivita, protoze zadani pozaduje vice aktivit a protoze uzivatelsky dava smysl mit samostatnou obrazovku pro cteni zpravy.

### 8. Pridat WorkManager

`MainActivity` pri startu naplanuje periodickou praci. `RssWorker` na pozadi:

- stahne vychozi RSS URL,
- naparsuje zpravy,
- ulozi je do SQLite,
- vrati `Result.success()` nebo `Result.retry()`.

### 9. Pridat testy

Test `RssParserTest` overuje, ze parser spravne precte lokalni RSS ukazku. To je stabilnejsi nez test proti zive webove strance.

## Vyvoj od nuly

Android projekt je vhodne stavet postupne: nejdrive sestavitelny prazdny projekt, potom model a parser, nakonec UI a prace na pozadi.

1. Zalozit strukturu Gradle projektu.

Vytvori se korenove soubory `settings.gradle`, `build.gradle`, `gradle.properties` a modul `app`. Pokud se pouzije Android Studio, lze zalozit prazdny Java projekt a potom upravit soubory podle zadani.

Minimalni struktura:

```text
05-rss-reader-android/
  settings.gradle
  build.gradle
  app/build.gradle
  app/src/main/AndroidManifest.xml
  app/src/main/java/cz/ujep/rssreader/
  app/src/main/res/layout/
```

2. Nastavit manifest.

Do `AndroidManifest.xml` se prida opravneni `INTERNET`, `MainActivity` a `DetailActivity`. `MainActivity` ma byt launcher activity.

3. Vytvorit model `RssItem`.

Nejprve se vytvori jednoducha Java trida bez Android zavislosti. To umozni parser a testy psat oddelene od UI.

4. Implementovat `RssParser`.

Parser se napise pred stahovanim ze site. Do testu se vlozi kratke RSS XML a overi se, ze vznikne seznam zprav. Tak se oddeli chyba parseru od chyby internetu.

5. Pridat `RssFetcher`.

Fetcher otevira URL a predava stream parseru. V teto fazi se jeste nemusi zobrazovat UI, staci vedet, ze fetcher vraci seznam `RssItem`.

6. Vytvorit SQLite cache.

`RssDatabase` vytvori tabulku zprav, metodu `upsert()` a metodu `listRelevant()`. Prvni rucni test muze ulozit nekolik polozek a zkontrolovat, ze se vrati ze seznamu.

7. Vytvorit layout hlavni obrazovky.

`activity_main.xml` obsahuje pole pro URL, tlacitko nacteni, status a `RecyclerView`. Nejdrive lze zobrazit staticky seznam, aby bylo jasne, ze adapter funguje.

8. Implementovat `RssAdapter`.

Adapter prevadi `RssItem` na radek. Po kliknuti vola listener v `MainActivity`. V teto fazi jeste detail muze zobrazit jen titulek.

9. Napojit `MainActivity`.

Aktivita pri startu nacte cache, po kliknuti stahuje RSS, ulozi vysledek do databaze a obnovi adapter. Sitove volani je potreba spustit mimo UI vlakno.

10. Vytvorit `DetailActivity`.

Do detailu se pres `Intent` predaji hodnoty vybrane zpravy. Layout `activity_detail.xml` zobrazi titulek, popis a odkaz.

11. Doplnit WorkManager.

Po funkcni rucni aktualizaci se prida `RssWorker`, ktery periodicky stahuje vychozi kanal. `MainActivity` naplanuje periodickou praci.

12. Pridat offline demo pro test bez internetu.

Do `app/src/main/assets/sample_rss.xml` se ulozi maly RSS soubor. V hlavni obrazovce se prida tlacitko `Nacist offline demo`, ktere XML nacte pres `getAssets().open(...)`, naparsuje ho pres `RssParser`, ulozi do SQLite a obnovi seznam. Tato cast je prakticka pri obhajobe, protoze ukazuje funkcni parser, RecyclerView, detail i cache bez zavislosti na siti.

13. Finalni kontrola.

Spustit `gradle test assembleDebug`, otevrit aplikaci v emulatoru nebo zarizeni, nahrat feed, vypnout internet a overit, ze se stale zobrazuji ulozene zpravy.

## Jak projekt sestavit

Nastaveni Javy z Android Studia:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
```

Build a testy pres Gradle wrapper v projektu:

```powershell
.\gradlew.bat test assembleDebug
```

Build a testy pres lokalni Gradle z workspace:

```powershell
..\.tools\gradle\bin\gradle.bat test assembleDebug
```

Pokud je Gradle v `PATH`:

```powershell
gradle test assembleDebug
```

## Test bez internetu

Pokud emulator nema funkcni sit, lze aplikaci otestovat tlacitkem `Nacist offline demo`. Demo data jsou v `app/src/main/assets/sample_rss.xml` a prochazi stejnym parserem i SQLite ulozistem jako online RSS.

Pro instalaci aktualni verze do beziciho emulatoru:

```powershell
.\gradlew.bat assembleDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n cz.ujep.rssreader/.MainActivity
```

Pokud chcete zkusit obnovit internet v emulatoru, vypnete emulator a spustte ho s explicitnim DNS:

```powershell
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd "Medium_Phone_API_36.1" -dns-server 8.8.8.8,1.1.1.1
```

## Co umet vysvetlit u obhajoby

- Proc jsou pouzite dve aktivity.
- Jak `RecyclerView` spolupracuje s adapterem.
- Proc se RSS parsuje jako XML.
- Jak funguje SQLite cache a offline zobrazeni.
- Proc se sitove volani nema delat na hlavnim vlakne.
- K cemu slouzi WorkManager.
- Jak test parseru snizuje zavislost na internetu.

## Mozna rozsireni

- vyber vice RSS zdroju,
- otevirani odkazu v prohlizeci,
- oznaceni prectenych zprav,
- vyhledavani ve zpravach,
- notifikace pri novych zpravach,
- mazani stare cache.
