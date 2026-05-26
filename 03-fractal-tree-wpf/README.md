# 03 - Fraktalni strom ve WPF

Desktopova aplikace v C# WPF generuje fraktalni strom podle parametru uzivatele.

## Funkce

- Nastaveni poctu iteraci, uhlu, koeficientu zkracovani a barvy kresleni.
- Start, preruseni, pauza a pokracovani vypoctu.
- UI zustava responzivni diky async generovani a `CancellationToken`.
- Export a import stromu do JSON souboru.

## Spusteni

```powershell
dotnet run --project .\src\FractalTree.App\FractalTree.App.csproj
```

## Testy

```powershell
dotnet test .\tests\FractalTree.Tests\FractalTree.Tests.csproj
```

## Dokumentace

UML class diagram je v `docs/fractal-classes.puml`.

