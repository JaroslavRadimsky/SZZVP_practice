using ContactManager.App;
using Xunit;

namespace ContactManager.Tests;

public sealed class ContactManagerTests
{
    [Fact]
    public void PrototypeCreatesIndependentCopy()
    {
        var source = new Contact(Guid.NewGuid(), "Eva Novakova", "123", "eva@example.com", "VIP");

        var clone = source.Clone();

        Assert.NotEqual(source.Id, clone.Id);
        Assert.Contains("kopie", clone.Name, StringComparison.OrdinalIgnoreCase);
        Assert.Equal(source.Phone, clone.Phone);
    }

    [Fact]
    public void CommandHistoryCanUndoAdd()
    {
        var book = new ContactBook();
        var history = new CommandHistory();
        var contact = new Contact(Guid.NewGuid(), "Jan", "111", "jan@example.com", "");

        history.Execute(new AddContactCommand(book, contact));
        Assert.Single(book.Items);

        Assert.True(history.UndoLast());
        Assert.Empty(book.Items);
    }

    [Fact]
    public void SearchFindsContactByEmail()
    {
        var book = new ContactBook();
        book.Add(new Contact(Guid.NewGuid(), "Anna", "222", "anna@school.cz", ""));

        var result = book.Search("school").Single();

        Assert.Equal("Anna", result.Name);
    }
}
