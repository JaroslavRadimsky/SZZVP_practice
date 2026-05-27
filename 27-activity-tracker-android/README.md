# 27 - Android Activity Tracker

Jednoduchá Android aplikace v Javě pro záznam fyzické aktivity během dne. Projekt je připravený jako samostatný Android projekt kompatibilní s Android Studiem 4.0.1.

## Funkce

- Zahájení a ukončení měření aktivity.
- Tlačítko pro zahájení aktivity je na spodní hraně hlavní obrazovky.
- Samotné měření probíhá na samostatné celoplošné obrazovce.
- Pravidelné ukládání vzorků každých 5 sekund.
- Čas měření se na obrazovce aktualizuje každou sekundu.
- Ukládání času vzorku, kroků nebo odhadu kroků a orientační intenzity pohybu.
- Sledování uražené vzdálenosti pomocí GPS a uložení posledních souřadnic vzorku.
- Výpočet rychlosti v km/h a tempa ve formátu minuty:sekundy na kilometr pro každý vzorek.
- Průběžná notifikace v systémové liště po celou dobu probíhající aktivity.
- Upozornění po 30 sekundách bez pohybu s dotazem, zda uživatel nechce aktivitu ukončit.
- Persistentní uložení dat do lokální SQLite databáze.
- Seznam uložených měření.
- Mazání měření z historie.
- Detail měření se statistikami:
  - délka aktivity,
  - průměrná intenzita,
  - celkový počet kroků,
  - uražená vzdálenost,
  - průměrná rychlost a tempo,
  - počet uložených vzorků.
- Přepínatelný graf intenzity, rychlosti, tempa a vzdálenosti.
- Sdílení naměřených dat jako CSV text.
- Fallback: pokud zařízení nemá krokoměr nebo chybí oprávnění, aplikace odhaduje kroky z pohybového senzoru.

## Kompatibilita s Android Studiem 4.0.1

Projekt záměrně používá starší a jednoduché nastavení:

- Android Gradle Plugin `4.0.1`
- Gradle wrapper `6.1.1`
- Java 8
- `compileSdkVersion 29`
- bez `namespace` a bez moderních AndroidX závislostí

V Android Studiu 4.0.1 otevřete přímo složku `27-activity-tracker-android`. Pokud Studio nabídne instalaci Android SDK Platform 29, potvrďte ji.

## Build

V prostředí s JDK 8 a Android SDK 29:

```powershell
.\gradlew.bat test assembleDebug
```

Gradle 6.1.1 nespouštějte pod novým JDK 17/21. Pro Android Studio 4.0.1 je správná volba jeho starší bundled JDK/JRE 8.

Při použití Android Studia 4.0.1 obvykle stačí:

1. `File -> Open`
2. vybrat složku `27-activity-tracker-android`
3. počkat na Gradle sync
4. spustit konfiguraci `app`

## Spuštění v emulátoru nebo telefonu

APK po buildu:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Na Androidu 10 a novějším aplikace požádá o oprávnění `Activity recognition`, pokud je dostupný krokoměr. Pro GPS vzdálenost požádá o přístup k poloze. Bez těchto oprávnění pořád funguje odhad z pohybového senzoru, jen bez přesného krokoměru nebo GPS vzdálenosti.

## Dokumentace

- Technická dokumentace: `docs/developer.md`
- UML class diagram: `docs/activity-classes.puml`
- Uživatelská příručka: `USER_GUIDE.md`
- Tutorial a postup řešení: `TUTORIAL.md`
