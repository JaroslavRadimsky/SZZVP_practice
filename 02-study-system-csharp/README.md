# 02 - Studijni system v C#

Konzolova aplikace simuluje zakladni studijni system: uzivatele, kurzy, zapisy studentu, zmeny kurzu a vypocet vysledku.

## Navrhove vzory

- Factory Method: `UserFactory` a `CourseFactory` vytvareji ruzne typy uctu a kurzu.
- Observer: studenti se prihlasuji jako pozorovatele kurzu a dostavaji oznameni o zmenach.
- Strategy: `AverageGradeStrategy` a `WeightedAverageGradeStrategy` meni algoritmus vypoctu vysledku.

## Spusteni

```powershell
dotnet run --project .\src\StudySystem.App\StudySystem.App.csproj
```

## Testy

```powershell
dotnet test .\tests\StudySystem.Tests\StudySystem.Tests.csproj
```

