# 27 - Android Activity Tracker

Jednoducha Android aplikace v Jave pro zaznam fyzicke aktivity behem dne. Projekt je pripraveny jako samostatny Android projekt kompatibilni s Android Studiem 4.0.1.

## Funkce

- Zahajeni a ukonceni mereni aktivity.
- Pravidelne ukladani vzorku kazdych 5 sekund.
- Ukladani casu vzorku, kroku nebo odhadu kroku a orientacni intenzity pohybu.
- Persistentni ulozeni dat do lokalni SQLite databaze.
- Seznam ulozenych mereni.
- Detail mereni se statistikami:
  - delka aktivity,
  - prumerna intenzita,
  - celkovy pocet kroku,
  - pocet ulozenych vzorku.
- Jednoduchy graf intenzity aktivity.
- Sdileni namerenych dat jako CSV text.
- Fallback: pokud zarizeni nema krokomer nebo chybi opravneni, aplikace odhaduje kroky z pohyboveho senzoru.

## Kompatibilita s Android Studiem 4.0.1

Projekt zamerne pouziva starsi a jednoduche nastaveni:

- Android Gradle Plugin `4.0.1`
- Gradle wrapper `6.1.1`
- Java 8
- `compileSdkVersion 29`
- bez `namespace` a bez modernich AndroidX zavislosti

V Android Studiu 4.0.1 otevrite primo slozku `27-activity-tracker-android`. Pokud Studio nabidne instalaci Android SDK Platform 29, potvrdit.

## Build

V prostredi s JDK 8 a Android SDK 29:

```powershell
.\gradlew.bat test assembleDebug
```

Gradle 6.1.1 nespoustejte pod novym JDK 17/21. Pro Android Studio 4.0.1 je spravna volba jeho starsi bundled JDK/JRE 8.

Pri pouziti Android Studia 4.0.1 obvykle staci:

1. `File -> Open`
2. vybrat slozku `27-activity-tracker-android`
3. pockat na Gradle sync
4. spustit konfiguraci `app`

## Spusteni v emulatoru nebo telefonu

APK po buildu:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Na Androidu 10 a novejsim aplikace pozada o opravneni `Activity recognition`, pokud je dostupny krokomer. Bez opravneni porad funguje odhad z pohyboveho senzoru.

## Dokumentace

- Technicka dokumentace: `docs/developer.md`
- UML class diagram: `docs/activity-classes.puml`
- Uzivatelska prirucka: `USER_GUIDE.md`
- Tutorial a postup reseni: `TUTORIAL.md`
