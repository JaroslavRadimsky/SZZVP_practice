using System.Collections;
using System.Text.Json;

namespace ContactManager.App;

/// <summary>Shared contract for objects that can create their own copy.</summary>
public interface IPrototype<out T>
{
    T Clone();
}

/// <summary>Contact persisted by the application.</summary>
public sealed class Contact : IPrototype<Contact>
{
    public Contact()
    {
    }

    public Contact(Guid id, string name, string phone, string email, string note)
    {
        Id = id;
        Name = name;
        Phone = phone;
        Email = email;
        Note = note;
    }

    public Guid Id { get; set; }
    public string Name { get; set; } = "";
    public string Phone { get; set; } = "";
    public string Email { get; set; } = "";
    public string Note { get; set; } = "";

    public Contact Clone() => new(Guid.NewGuid(), $"{Name} (kopie)", Phone, Email, Note);
}

/// <summary>Collection with iterator support and search helpers.</summary>
public sealed class ContactBook : IEnumerable<Contact>
{
    private readonly List<Contact> _contacts = [];

    public IReadOnlyList<Contact> Items => _contacts;

    public void Add(Contact contact) => _contacts.Add(contact);

    public bool Remove(Guid id)
    {
        var index = _contacts.FindIndex(c => c.Id == id);
        if (index < 0) return false;
        _contacts.RemoveAt(index);
        return true;
    }

    public bool Replace(Contact contact)
    {
        var index = _contacts.FindIndex(c => c.Id == contact.Id);
        if (index < 0) return false;
        _contacts[index] = contact;
        return true;
    }

    public Contact? Find(Guid id) => _contacts.FirstOrDefault(c => c.Id == id);

    public IEnumerable<Contact> Search(string query)
    {
        return _contacts.Where(c =>
            c.Name.Contains(query, StringComparison.OrdinalIgnoreCase) ||
            c.Phone.Contains(query, StringComparison.OrdinalIgnoreCase) ||
            c.Email.Contains(query, StringComparison.OrdinalIgnoreCase));
    }

    public IEnumerator<Contact> GetEnumerator() => _contacts.GetEnumerator();

    IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
}

public interface IContactCommand
{
    string Name { get; }
    void Execute();
    void Undo();
}

public sealed class AddContactCommand(ContactBook book, Contact contact) : IContactCommand
{
    public string Name => "Pridani kontaktu";
    public void Execute() => book.Add(contact);
    public void Undo() => book.Remove(contact.Id);
}

public sealed class UpdateContactCommand(ContactBook book, Contact updated) : IContactCommand
{
    private Contact? _original;
    public string Name => "Uprava kontaktu";

    public void Execute()
    {
        _original = book.Find(updated.Id);
        if (_original is null || !book.Replace(updated))
        {
            throw new InvalidOperationException("Kontakt nebyl nalezen.");
        }
    }

    public void Undo()
    {
        if (_original is not null)
        {
            book.Replace(_original);
        }
    }
}

public sealed class DeleteContactCommand(ContactBook book, Guid id) : IContactCommand
{
    private Contact? _deleted;
    public string Name => "Smazani kontaktu";

    public void Execute()
    {
        _deleted = book.Find(id);
        if (_deleted is null || !book.Remove(id))
        {
            throw new InvalidOperationException("Kontakt nebyl nalezen.");
        }
    }

    public void Undo()
    {
        if (_deleted is not null)
        {
            book.Add(_deleted);
        }
    }
}

public sealed class CommandHistory
{
    private readonly Stack<IContactCommand> _history = new();

    public void Execute(IContactCommand command)
    {
        command.Execute();
        _history.Push(command);
    }

    public bool UndoLast()
    {
        if (_history.Count == 0) return false;
        _history.Pop().Undo();
        return true;
    }
}

public sealed class JsonContactStore(string path)
{
    private static readonly JsonSerializerOptions Options = new() { WriteIndented = true };

    public async Task<ContactBook> LoadAsync()
    {
        var book = new ContactBook();
        if (!File.Exists(path)) return book;
        var contacts = JsonSerializer.Deserialize<List<Contact>>(await File.ReadAllTextAsync(path), Options) ?? [];
        foreach (var contact in contacts)
        {
            book.Add(contact);
        }
        return book;
    }

    public Task SaveAsync(ContactBook book)
    {
        var json = JsonSerializer.Serialize(book.Items, Options);
        return File.WriteAllTextAsync(path, json);
    }
}

public static class Program
{
    public static async Task Main()
    {
        var store = new JsonContactStore("contacts.json");
        var book = await store.LoadAsync();
        var history = new CommandHistory();

        while (true)
        {
            Console.WriteLine();
            Console.WriteLine("Spravce kontaktu");
            Console.WriteLine("1 - Vypsat kontakty");
            Console.WriteLine("2 - Pridat kontakt");
            Console.WriteLine("3 - Vyhledat kontakt");
            Console.WriteLine("4 - Upravit kontakt");
            Console.WriteLine("5 - Smazat kontakt");
            Console.WriteLine("6 - Duplikovat kontakt");
            Console.WriteLine("7 - Vratit posledni zmenu");
            Console.WriteLine("0 - Ulozit a konec");
            Console.Write("Volba: ");

            try
            {
                switch (Console.ReadLine())
                {
                    case "1":
                        PrintContacts(book);
                        break;
                    case "2":
                        history.Execute(new AddContactCommand(book, ReadContact()));
                        break;
                    case "3":
                        Console.Write("Hledat: ");
                        PrintContacts(book.Search(Console.ReadLine() ?? ""));
                        break;
                    case "4":
                        var updated = ReadExistingContact(book);
                        if (updated is not null) history.Execute(new UpdateContactCommand(book, updated));
                        break;
                    case "5":
                        var deleteId = ReadContactId(book);
                        if (deleteId is not null) history.Execute(new DeleteContactCommand(book, deleteId.Value));
                        break;
                    case "6":
                        var cloneId = ReadContactId(book);
                        var contact = cloneId is null ? null : book.Find(cloneId.Value);
                        if (contact is not null) history.Execute(new AddContactCommand(book, contact.Clone()));
                        break;
                    case "7":
                        Console.WriteLine(history.UndoLast() ? "Zmena vracena." : "Neni co vratit.");
                        break;
                    case "0":
                        await store.SaveAsync(book);
                        return;
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Chyba: {ex.Message}");
            }
        }
    }

    private static Contact ReadContact() =>
        new(Guid.NewGuid(), ReadText("Jmeno"), ReadText("Telefon"), ReadText("E-mail"), ReadText("Poznamka"));

    private static Contact? ReadExistingContact(ContactBook book)
    {
        var id = ReadContactId(book);
        if (id is null) return null;
        var old = book.Find(id.Value);
        if (old is null) return null;
        return new Contact(
            old.Id,
            ReadText("Jmeno", old.Name),
            ReadText("Telefon", old.Phone),
            ReadText("E-mail", old.Email),
            ReadText("Poznamka", old.Note));
    }

    private static Guid? ReadContactId(ContactBook book)
    {
        PrintContacts(book);
        Console.Write("Zadejte ID kontaktu: ");
        return Guid.TryParse(Console.ReadLine(), out var id) ? id : null;
    }

    private static string ReadText(string label, string? current = null)
    {
        Console.Write(current is null ? $"{label}: " : $"{label} [{current}]: ");
        var value = Console.ReadLine();
        return string.IsNullOrWhiteSpace(value) && current is not null ? current : value?.Trim() ?? "";
    }

    private static void PrintContacts(IEnumerable<Contact> contacts)
    {
        foreach (var contact in contacts)
        {
            Console.WriteLine($"{contact.Id} | {contact.Name} | {contact.Phone} | {contact.Email} | {contact.Note}");
        }
    }
}
