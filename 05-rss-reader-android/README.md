# 05 - Android RSS ctecka

Jednoducha Android aplikace v Jave se seznamem RSS zprav, detailem, SQLite cache a automatickou aktualizaci.

## Funkce

- Nacteni RSS kanalu z verejne URL, vychozi je `https://www.idnes.cz/rss`.
- Offline demo RSS kanal v assets pro testovani bez internetu v emulatoru.
- Seznamove zobrazeni zprav pomoci RecyclerView.
- Detail zpravy ve druhe aktivite.
- Persistentni ulozeni zprav do SQLite a offline zobrazeni relevantnich zprav.
- Periodicka aktualizace pres WorkManager.

## Build

Nastavte Java z Android Studia:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
```

S Gradle wrapperem v projektu:

```powershell
.\gradlew.bat test assembleDebug
```

S lokalnim Gradle z workspace:

```powershell
..\.tools\gradle\bin\gradle.bat test assembleDebug
```

Pokud mate Gradle v PATH:

```powershell
gradle test assembleDebug
```

## Dokumentace

UML class diagram vlastni logiky je v `docs/rss-classes.puml`, vyvojarska dokumentace je v `docs/developer.md`.

## Test v emulatoru bez internetu

Pokud emulator nema funkcni internet, spustte aplikaci a klepnete na `Nacist offline demo`.
Tato volba nacte `app/src/main/assets/sample_rss.xml`, ulozi zpravy do SQLite a umozni otestovat seznam, detail i cache.

Instalace aktualni APK do beziciho emulatoru:

```powershell
.\gradlew.bat assembleDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n cz.ujep.rssreader/.MainActivity
```

Pokud chcete zkusit opravit sit emulatoru, vypnete emulator a spustte ho s DNS:

```powershell
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd "Medium_Phone_API_36.1" -dns-server 8.8.8.8,1.1.1.1
```
