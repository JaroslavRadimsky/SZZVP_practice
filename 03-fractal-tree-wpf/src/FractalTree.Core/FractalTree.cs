using System.Text.Json;

namespace FractalTree.Core;

public sealed record TreeParameters(int Iterations, double AngleDegrees, double Shortening, string Color)
{
    public void Validate()
    {
        if (Iterations is < 0 or > 14) throw new ArgumentOutOfRangeException(nameof(Iterations), "Iterace musi byt 0 az 14.");
        if (AngleDegrees is < 0 or > 90) throw new ArgumentOutOfRangeException(nameof(AngleDegrees), "Uhel musi byt 0 az 90.");
        if (Shortening is <= 0 or >= 1) throw new ArgumentOutOfRangeException(nameof(Shortening), "Koeficient musi byt mezi 0 a 1.");
    }
}

public sealed record Branch(double X1, double Y1, double X2, double Y2, int Level);

public sealed record TreeDocument(TreeParameters Parameters, List<Branch> Branches);

public sealed class PauseToken
{
    private volatile bool _paused;

    public bool IsPaused => _paused;
    public void Pause() => _paused = true;
    public void Resume() => _paused = false;

    public async Task WaitWhilePausedAsync(CancellationToken cancellationToken)
    {
        while (_paused)
        {
            await Task.Delay(50, cancellationToken);
        }
    }
}

public sealed class FractalGenerator
{
    public async Task<List<Branch>> GenerateAsync(
        TreeParameters parameters,
        IProgress<Branch>? progress = null,
        PauseToken? pauseToken = null,
        CancellationToken cancellationToken = default)
    {
        parameters.Validate();
        var branches = new List<Branch>();
        await GenerateBranchAsync(
            branches,
            progress,
            pauseToken ?? new PauseToken(),
            parameters.Iterations,
            0,
            0,
            100,
            -90,
            1,
            parameters,
            cancellationToken);
        return branches;
    }

    private static async Task GenerateBranchAsync(
        List<Branch> branches,
        IProgress<Branch>? progress,
        PauseToken pauseToken,
        int remaining,
        double x,
        double y,
        double length,
        double directionDegrees,
        int level,
        TreeParameters parameters,
        CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        await pauseToken.WaitWhilePausedAsync(cancellationToken);

        var radians = Math.PI * directionDegrees / 180.0;
        var x2 = x + Math.Cos(radians) * length;
        var y2 = y + Math.Sin(radians) * length;
        var branch = new Branch(x, y, x2, y2, level);
        branches.Add(branch);
        progress?.Report(branch);

        await Task.Delay(8, cancellationToken);
        if (remaining <= 0) return;

        var nextLength = length * parameters.Shortening;
        await GenerateBranchAsync(branches, progress, pauseToken, remaining - 1, x2, y2, nextLength, directionDegrees - parameters.AngleDegrees, level + 1, parameters, cancellationToken);
        await GenerateBranchAsync(branches, progress, pauseToken, remaining - 1, x2, y2, nextLength, directionDegrees + parameters.AngleDegrees, level + 1, parameters, cancellationToken);
    }
}

public sealed class JsonTreeStore
{
    private static readonly JsonSerializerOptions Options = new() { WriteIndented = true };

    public Task SaveAsync(string path, TreeDocument document)
    {
        return File.WriteAllTextAsync(path, JsonSerializer.Serialize(document, Options));
    }

    public async Task<TreeDocument> LoadAsync(string path)
    {
        var document = JsonSerializer.Deserialize<TreeDocument>(await File.ReadAllTextAsync(path), Options);
        return document ?? throw new InvalidDataException("JSON neobsahuje strom.");
    }
}

