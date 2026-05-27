package cz.ujep.activitytracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * Obrazovka detailu jednoho měření se souhrnem, grafem a CSV exportem.
 */
public class MeasurementDetailActivity extends Activity {
    private ActivityRecord record;
    private List<ActivitySample> samples;
    private IntensityGraphView graphView;
    private TextView graphStatsText;

    /**
     * Načte měření podle id z intentu a připraví souhrn, graf a export.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        long measurementId = getIntent().getLongExtra("measurementId", -1L);
        ActivityDatabase database = new ActivityDatabase(this);
        record = database.getMeasurement(measurementId);
        if (record == null) {
            finish();
            return;
        }
        samples = database.listSamples(record.id);

        ((TextView) findViewById(R.id.detailTitleText)).setText(buildTitle());
        ((TextView) findViewById(R.id.detailSummaryText)).setText(buildSummary());
        graphView = findViewById(R.id.intensityGraph);
        graphStatsText = findViewById(R.id.graphStatsText);
        graphView.setSamples(samples);
        setupMetricSpinner();

        Button exportButton = findViewById(R.id.exportButton);
        exportButton.setOnClickListener(view -> shareCsv());
    }

    /**
     * Sestaví titulek detailu podle času začátku měření.
     */
    private String buildTitle() {
        return "Mereni " + DateFormat.format("dd.MM.yyyy HH:mm", record.startedAt);
    }

    /**
     * Sestaví textový souhrn základních statistik měření.
     */
    private String buildSummary() {
        long end = record.getDisplayEndTime();
        String startText = DateFormat.format("dd.MM.yyyy HH:mm:ss", record.startedAt).toString();
        String endText = record.isFinished()
                ? DateFormat.format("dd.MM.yyyy HH:mm:ss", record.endedAt).toString()
                : "probiha";
        long duration = end - record.startedAt;
        double averageSpeed = ActivityStatsCalculator.calculateSpeedKmh(record.distanceMeters, duration);
        double averagePace = ActivityStatsCalculator.calculatePaceSecondsPerKm(record.distanceMeters, duration);
        return String.format(
                Locale.US,
                "Zacatek: %s\nKonec: %s\nDelka aktivity: %s\nCelkovy pocet kroku: %d\nUrazena vzdalenost: %s\nPrumerna rychlost: %s\nPrumerne tempo: %s\nPrumerna intenzita: %s\nPocet ulozenych vzorku: %d",
                startText,
                endText,
                ActivityStatsCalculator.formatDuration(duration),
                record.totalSteps,
                ActivityStatsCalculator.formatDistance(record.distanceMeters),
                ActivityStatsCalculator.formatSpeed(averageSpeed),
                ActivityStatsCalculator.formatPace(averagePace),
                ActivityStatsCalculator.formatIntensity(record.averageIntensity),
                record.sampleCount
        );
    }

    /**
     * Připraví rozbalovací výběr metriky pro graf.
     */
    private void setupMetricSpinner() {
        String[] labels = new String[]{"Intenzita", "Rychlost", "Tempo", "Vzdalenost"};
        Spinner spinner = findViewById(R.id.graphMetricSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            /**
             * Po výběru metriky překreslí graf a aktualizuje text pod grafem.
             */
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                graphView.setMetric(position);
                graphStatsText.setText(buildGraphStats(position));
            }

            /**
             * Povinný callback Spinneru; aplikace žádnou speciální akci nepotřebuje.
             */
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        graphView.setMetric(IntensityGraphView.METRIC_INTENSITY);
        graphStatsText.setText(buildGraphStats(IntensityGraphView.METRIC_INTENSITY));
    }

    /**
     * Vypočítá doprovodný text ke grafu pro vybranou metriku.
     */
    private String buildGraphStats(int metric) {
        if (samples.isEmpty()) {
            return "Graf nema ulozena data.";
        }
        double max = 0.0;
        double sum = 0.0;
        int count = 0;
        double cumulativeDistance = 0.0;
        for (ActivitySample sample : samples) {
            cumulativeDistance += sample.distanceMeters;
            double value = graphValue(sample, metric, cumulativeDistance);
            if (value > max) {
                max = value;
            }
            if (value > 0.0) {
                sum += value;
                count++;
            }
        }
        double average = count == 0 ? 0.0 : sum / count;
        if (metric == IntensityGraphView.METRIC_SPEED) {
            return "Maximum: " + ActivityStatsCalculator.formatSpeed(max) + " | prumer vzorku: " + ActivityStatsCalculator.formatSpeed(average);
        }
        if (metric == IntensityGraphView.METRIC_PACE) {
            return "Nejvyssi tempo: " + ActivityStatsCalculator.formatPace(max) + " | prumer vzorku: " + ActivityStatsCalculator.formatPace(average);
        }
        if (metric == IntensityGraphView.METRIC_DISTANCE) {
            return "Celkova vzdalenost: " + ActivityStatsCalculator.formatDistance(cumulativeDistance);
        }
        return "Maximum: " + ActivityStatsCalculator.formatIntensity(max) + " | prumer vzorku: " + ActivityStatsCalculator.formatIntensity(average);
    }

    /**
     * Vrátí hodnotu vzorku odpovídající vybrané metrice grafu.
     */
    private double graphValue(ActivitySample sample, int metric, double cumulativeDistance) {
        if (metric == IntensityGraphView.METRIC_SPEED) {
            return sample.speedKmh;
        }
        if (metric == IntensityGraphView.METRIC_PACE) {
            return sample.paceSecondsPerKm;
        }
        if (metric == IntensityGraphView.METRIC_DISTANCE) {
            return cumulativeDistance;
        }
        return sample.intensity;
    }

    /**
     * Otevře systémové sdílení a předá do něj CSV text.
     */
    private void shareCsv() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_SUBJECT, "activity_measurement_" + record.id + ".csv");
        intent.putExtra(Intent.EXTRA_TEXT, buildCsv());
        startActivity(Intent.createChooser(intent, "Sdilet CSV"));
    }

    /**
     * Sestaví CSV export se souhrnem a všemi vzorky měření.
     */
    private String buildCsv() {
        StringBuilder builder = new StringBuilder();
        builder.append("measurement_id;started_at;ended_at;duration;total_steps;distance_meters;average_speed_kmh;average_pace;average_intensity\n");
        long duration = record.getDisplayEndTime() - record.startedAt;
        builder.append(record.id)
                .append(';')
                .append(DateFormat.format("yyyy-MM-dd HH:mm:ss", record.startedAt))
                .append(';')
                .append(record.isFinished() ? DateFormat.format("yyyy-MM-dd HH:mm:ss", record.endedAt) : "")
                .append(';')
                .append(ActivityStatsCalculator.formatDuration(duration))
                .append(';')
                .append(record.totalSteps)
                .append(';')
                .append(String.format(Locale.US, "%.2f", record.distanceMeters))
                .append(';')
                .append(String.format(Locale.US, "%.2f", ActivityStatsCalculator.calculateSpeedKmh(record.distanceMeters, duration)))
                .append(';')
                .append(ActivityStatsCalculator.formatPace(ActivityStatsCalculator.calculatePaceSecondsPerKm(record.distanceMeters, duration)))
                .append(';')
                .append(String.format(Locale.US, "%.2f", record.averageIntensity))
                .append("\n\n");
        builder.append("sample_time;steps;intensity;distance_meters;speed_kmh;pace;latitude;longitude\n");
        for (ActivitySample sample : samples) {
            builder.append(DateFormat.format("yyyy-MM-dd HH:mm:ss", sample.measuredAt))
                    .append(';')
                    .append(sample.steps)
                    .append(';')
                    .append(String.format(Locale.US, "%.2f", sample.intensity))
                    .append(';')
                    .append(String.format(Locale.US, "%.2f", sample.distanceMeters))
                    .append(';')
                    .append(String.format(Locale.US, "%.2f", sample.speedKmh))
                    .append(';')
                    .append(ActivityStatsCalculator.formatPace(sample.paceSecondsPerKm))
                    .append(';')
                    .append(Double.isNaN(sample.latitude) ? "" : String.format(Locale.US, "%.6f", sample.latitude))
                    .append(';')
                    .append(Double.isNaN(sample.longitude) ? "" : String.format(Locale.US, "%.6f", sample.longitude))
                    .append('\n');
        }
        return builder.toString();
    }
}
