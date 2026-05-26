# 01 - Spravce kontaktu v C#

Konzolova aplikace pro ukladani, prohlizeni, upravu, mazani a vyhledavani kontaktu.

## Navrhove vzory

- Prototype: `Contact.Clone()` vytvari kopii kontaktu pro rychle duplikovani a bezpecne upravy.
- Command: `AddContactCommand`, `UpdateContactCommand` a `DeleteContactCommand` zapisuji zmeny a umi `Undo`.
- Iterator: `ContactBook` implementuje `IEnumerable<Contact>` pro pruchod kolekci kontaktu.

## Spusteni

```powershell
dotnet run --project .\src\ContactManager.App\ContactManager.App.csproj
```

Kontakty se ukladaji do `contacts.json` ve spousteci slozce.

## Testy

```powershell
dotnet test .\tests\ContactManager.Tests\ContactManager.Tests.csproj
```

