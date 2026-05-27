package cz.ujep.activitytracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class MeasurementDetailActivity extends Activity {
    private ActivityRecord record;
    private List<ActivitySample> samples;

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
        ((TextView) findViewById(R.id.samplesText)).setText(buildSamplesText());
        ((IntensityGraphView) findViewById(R.id.intensityGraph)).setSamples(samples);

        Button exportButton = findViewById(R.id.exportButton);
        exportButton.setOnClickListener(view -> shareCsv());
    }

    private String buildTitle() {
        return "Mereni " + DateFormat.format("dd.MM.yyyy HH:mm", record.startedAt);
    }

    private String buildSummary() {
        long end = record.getDisplayEndTime();
        String startText = DateFormat.format("dd.MM.yyyy HH:mm:ss", record.startedAt).toString();
        String endText = record.isFinished()
                ? DateFormat.format("dd.MM.yyyy HH:mm:ss", record.endedAt).toString()
                : "probiha";
        return String.format(
                Locale.US,
                "Zacatek: %s\nKonec: %s\nDelka aktivity: %s\nCelkovy pocet kroku: %d\nUrazena vzdalenost: %s\nPrumerna intenzita: %s\nPocet ulozenych vzorku: %d",
                startText,
                endText,
                ActivityStatsCalculator.formatDuration(end - record.startedAt),
                record.totalSteps,
                ActivityStatsCalculator.formatDistance(record.distanceMeters),
                ActivityStatsCalculator.formatIntensity(record.averageIntensity),
                record.sampleCount
        );
    }

    private String buildSamplesText() {
        if (samples.isEmpty()) {
            return "Zatim nejsou ulozene zadne vzorky.";
        }
        StringBuilder builder = new StringBuilder();
        for (ActivitySample sample : samples) {
            builder.append(DateFormat.format("HH:mm:ss", sample.measuredAt))
                    .append(" | kroky +")
                    .append(sample.steps)
                    .append(" | intenzita ")
                    .append(ActivityStatsCalculator.formatIntensity(sample.intensity))
                    .append(" | vzdalenost +")
                    .append(ActivityStatsCalculator.formatDistance(sample.distanceMeters));
            if (!Double.isNaN(sample.latitude) && !Double.isNaN(sample.longitude)) {
                builder.append(" | GPS ")
                        .append(String.format(Locale.US, "%.5f, %.5f", sample.latitude, sample.longitude));
            }
            builder
                    .append('\n');
        }
        return builder.toString();
    }

    private void shareCsv() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_SUBJECT, "activity_measurement_" + record.id + ".csv");
        intent.putExtra(Intent.EXTRA_TEXT, buildCsv());
        startActivity(Intent.createChooser(intent, "Sdilet CSV"));
    }

    private String buildCsv() {
        StringBuilder builder = new StringBuilder();
        builder.append("measurement_id;started_at;ended_at;duration;total_steps;distance_meters;average_intensity\n");
        builder.append(record.id)
                .append(';')
                .append(DateFormat.format("yyyy-MM-dd HH:mm:ss", record.startedAt))
                .append(';')
                .append(record.isFinished() ? DateFormat.format("yyyy-MM-dd HH:mm:ss", record.endedAt) : "")
                .append(';')
                .append(ActivityStatsCalculator.formatDuration(record.getDisplayEndTime() - record.startedAt))
                .append(';')
                .append(record.totalSteps)
                .append(';')
                .append(String.format(Locale.US, "%.2f", record.distanceMeters))
                .append(';')
                .append(String.format(Locale.US, "%.2f", record.averageIntensity))
                .append("\n\n");
        builder.append("sample_time;steps;intensity;distance_meters;latitude;longitude\n");
        for (ActivitySample sample : samples) {
            builder.append(DateFormat.format("yyyy-MM-dd HH:mm:ss", sample.measuredAt))
                    .append(';')
                    .append(sample.steps)
                    .append(';')
                    .append(String.format(Locale.US, "%.2f", sample.intensity))
                    .append(';')
                    .append(String.format(Locale.US, "%.2f", sample.distanceMeters))
                    .append(';')
                    .append(Double.isNaN(sample.latitude) ? "" : String.format(Locale.US, "%.6f", sample.latitude))
                    .append(';')
                    .append(Double.isNaN(sample.longitude) ? "" : String.format(Locale.US, "%.6f", sample.longitude))
                    .append('\n');
        }
        return builder.toString();
    }
}
