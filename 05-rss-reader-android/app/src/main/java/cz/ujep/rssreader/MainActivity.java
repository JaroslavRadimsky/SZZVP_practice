package cz.ujep.rssreader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private RssDatabase database;
    private RssAdapter adapter;
    private EditText feedUrl;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = new RssDatabase(this);
        feedUrl = findViewById(R.id.feedUrl);
        statusText = findViewById(R.id.statusText);
        Button loadButton = findViewById(R.id.loadButton);
        Button demoButton = findViewById(R.id.demoButton);
        RecyclerView recyclerView = findViewById(R.id.rssList);

        adapter = new RssAdapter(this::openDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadButton.setOnClickListener(view -> loadFeed());
        demoButton.setOnClickListener(view -> loadDemoFeed());
        scheduleRefresh();
        refreshFromDatabase();
    }

    private void loadFeed() {
        statusText.setText("Nacitam...");
        String url = feedUrl.getText().toString();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<RssItem> items = new RssFetcher().fetch(url);
                database.upsert(items);
                runOnUiThread(() -> {
                    statusText.setText("Nacteno zprav: " + items.size());
                    refreshFromDatabase();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> statusText.setText("Chyba: " + ex.getMessage()));
            }
        });
    }

    private void loadDemoFeed() {
        statusText.setText("Nacitam offline demo...");
        Executors.newSingleThreadExecutor().execute(() -> {
            try (InputStream stream = getAssets().open("sample_rss.xml")) {
                List<RssItem> items = new RssParser().parse(stream, System.currentTimeMillis());
                database.upsert(items);
                runOnUiThread(() -> {
                    statusText.setText("Nacteno demo zprav: " + items.size());
                    refreshFromDatabase();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> statusText.setText("Chyba demo dat: " + ex.getMessage()));
            }
        });
    }

    private void refreshFromDatabase() {
        adapter.setItems(database.listRelevant());
    }

    private void openDetail(RssItem item) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("title", item.title);
        intent.putExtra("description", item.description);
        intent.putExtra("link", item.link);
        intent.putExtra("publishedAt", item.publishedAt);
        startActivity(intent);
    }

    private void scheduleRefresh() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(RssWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "rss-refresh",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
    }
}
