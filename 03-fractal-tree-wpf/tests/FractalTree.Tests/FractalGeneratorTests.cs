using FractalTree.Core;
using Xunit;

namespace FractalTree.Tests;

public sealed class FractalGeneratorTests
{
    [Theory]
    [InlineData(0, 1)]
    [InlineData(1, 3)]
    [InlineData(2, 7)]
    public async Task GeneratorCreatesExpectedBranchCount(int iterations, int expected)
    {
        var generator = new FractalGenerator();

        var branches = await generator.GenerateAsync(new TreeParameters(iterations, 30, 0.7, "#176B47"));

        Assert.Equal(expected, branches.Count);
    }

    [Fact]
    public async Task StoreRoundTripsDocument()
    {
        var path = Path.Combine(Path.GetTempPath(), $"{Guid.NewGuid()}.json");
        var store = new JsonTreeStore();
        var document = new TreeDocument(new TreeParameters(1, 30, 0.7, "#000000"), [new Branch(0, 0, 0, -10, 1)]);

        await store.SaveAsync(path, document);
        var loaded = await store.LoadAsync(path);

        Assert.Equal(document.Parameters, loaded.Parameters);
        Assert.Single(loaded.Branches);
        File.Delete(path);
    }
}

