using System.Windows;
using System.Globalization;
using System.Windows.Media;
using System.Windows.Shapes;
using FractalTree.Core;
using Microsoft.Win32;

namespace FractalTree.App;

public partial class MainWindow : Window
{
    private readonly FractalGenerator _generator = new();
    private readonly JsonTreeStore _store = new();
    private readonly PauseToken _pauseToken = new();
    private readonly List<Branch> _branches = [];
    private CancellationTokenSource? _cancellation;
    private TreeParameters _lastParameters = new(9, 28, 0.72, "#176B47");

    public MainWindow()
    {
        InitializeComponent();
    }

    private async void Start_Click(object sender, RoutedEventArgs e)
    {
        _cancellation?.Cancel();
        _cancellation = new CancellationTokenSource();
        _pauseToken.Resume();
        _branches.Clear();
        TreeCanvas.Children.Clear();

        try
        {
            _lastParameters = ReadParameters();
            StatusText.Text = "Generuji...";
            var progress = new Progress<Branch>(branch =>
            {
                _branches.Add(branch);
                DrawBranch(branch, _lastParameters.Color);
                StatusText.Text = $"Vetvi: {_branches.Count}";
            });

            await _generator.GenerateAsync(_lastParameters, progress, _pauseToken, _cancellation.Token);
            StatusText.Text = $"Hotovo. Vetvi: {_branches.Count}";
        }
        catch (OperationCanceledException)
        {
            StatusText.Text = "Vypocet prerusen.";
        }
        catch (Exception ex)
        {
            StatusText.Text = ex.Message;
        }
    }

    private void Stop_Click(object sender, RoutedEventArgs e) => _cancellation?.Cancel();
    private void Pause_Click(object sender, RoutedEventArgs e) => _pauseToken.Pause();
    private void Resume_Click(object sender, RoutedEventArgs e) => _pauseToken.Resume();

    private async void Export_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new SaveFileDialog { Filter = "JSON strom|*.json", FileName = "fractal-tree.json" };
        if (dialog.ShowDialog(this) != true) return;
        await _store.SaveAsync(dialog.FileName, new TreeDocument(_lastParameters, _branches));
        StatusText.Text = "Export ulozen.";
    }

    private async void Import_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog { Filter = "JSON strom|*.json" };
        if (dialog.ShowDialog(this) != true) return;
        var document = await _store.LoadAsync(dialog.FileName);
        _lastParameters = document.Parameters;
        IterationsInput.Text = _lastParameters.Iterations.ToString();
        AngleInput.Text = _lastParameters.AngleDegrees.ToString();
        ShorteningInput.Text = _lastParameters.Shortening.ToString();
        ColorInput.Text = _lastParameters.Color;
        _branches.Clear();
        _branches.AddRange(document.Branches);
        Redraw();
        StatusText.Text = $"Importovano vetvi: {_branches.Count}";
    }

    private TreeParameters ReadParameters() =>
        new(
            int.Parse(IterationsInput.Text),
            double.Parse(AngleInput.Text, CultureInfo.InvariantCulture),
            double.Parse(ShorteningInput.Text, CultureInfo.InvariantCulture),
            ColorInput.Text.Trim());

    private void Redraw()
    {
        TreeCanvas.Children.Clear();
        foreach (var branch in _branches)
        {
            DrawBranch(branch, _lastParameters.Color);
        }
    }

    private void DrawBranch(Branch branch, string color)
    {
        var centerX = TreeCanvas.ActualWidth / 2;
        var baseline = TreeCanvas.ActualHeight - 30;
        var brush = new SolidColorBrush((Color)ColorConverter.ConvertFromString(color));
        var line = new Line
        {
            X1 = centerX + branch.X1,
            Y1 = baseline + branch.Y1,
            X2 = centerX + branch.X2,
            Y2 = baseline + branch.Y2,
            Stroke = brush,
            StrokeThickness = Math.Max(1, 7 - branch.Level * 0.45)
        };
        TreeCanvas.Children.Add(line);
    }
}
