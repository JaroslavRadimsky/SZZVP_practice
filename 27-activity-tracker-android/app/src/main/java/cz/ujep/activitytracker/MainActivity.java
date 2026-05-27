package cz.ujep.activitytracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Hlavní obrazovka aplikace se seznamem uložených měření.
 */
public class MainActivity extends Activity {
    private ActivityDatabase database;
    private MeasurementAdapter adapter;
    private TextView statusText;

    /**
     * Inicializuje historii měření, tlačítko pro start aktivity a klikání v seznamu.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = new ActivityDatabase(this);
        statusText = findViewById(R.id.statusText);
        Button startButton = findViewById(R.id.startButton);
        ListView measurementList = findViewById(R.id.measurementList);

        adapter = new MeasurementAdapter(this, this::confirmDelete);
        measurementList.setAdapter(adapter);
        measurementList.setOnItemClickListener((parent, view, position, id) -> openDetail(adapter.getItem(position)));

        startButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, TrackingActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Po návratu na hlavní obrazovku obnoví seznam měření z databáze.
     */
    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    /**
     * Načte měření z databáze do adapteru a aktualizuje stavový text.
     */
    private void refreshList() {
        adapter.setRecords(database.listMeasurements());
        statusText.setText(adapter.getCount() == 0
                ? "Zatim neni ulozene zadne mereni."
                : "Ulozenych mereni: " + adapter.getCount());
    }

    /**
     * Otevře detail vybraného měření.
     */
    private void openDetail(ActivityRecord record) {
        Intent intent = new Intent(this, MeasurementDetailActivity.class);
        intent.putExtra("measurementId", record.id);
        startActivity(intent);
    }

    /**
     * Zobrazí potvrzovací dialog a po souhlasu smaže měření z historie.
     */
    private void confirmDelete(ActivityRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("Smazat mereni")
                .setMessage("Opravdu chcete smazat toto mereni z historie?")
                .setPositiveButton("Smazat", (dialog, which) -> {
                    database.deleteMeasurement(record.id);
                    refreshList();
                })
                .setNegativeButton("Zrusit", null)
                .show();
    }
}
