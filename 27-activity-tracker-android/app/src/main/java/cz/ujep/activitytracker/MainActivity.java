package cz.ujep.activitytracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private ActivityDatabase database;
    private MeasurementAdapter adapter;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = new ActivityDatabase(this);
        statusText = findViewById(R.id.statusText);
        Button startButton = findViewById(R.id.startButton);
        ListView measurementList = findViewById(R.id.measurementList);

        adapter = new MeasurementAdapter(this);
        measurementList.setAdapter(adapter);
        measurementList.setOnItemClickListener((parent, view, position, id) -> openDetail(adapter.getItem(position)));

        startButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, TrackingActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        adapter.setRecords(database.listMeasurements());
        statusText.setText(adapter.getCount() == 0
                ? "Zatim neni ulozene zadne mereni."
                : "Ulozenych mereni: " + adapter.getCount());
    }

    private void openDetail(ActivityRecord record) {
        Intent intent = new Intent(this, MeasurementDetailActivity.class);
        intent.putExtra("measurementId", record.id);
        startActivity(intent);
    }
}
