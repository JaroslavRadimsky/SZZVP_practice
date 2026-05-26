package cz.ujep.rssreader;

import android.app.Activity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TextView;

import java.util.Date;

public class DetailActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ((TextView) findViewById(R.id.titleText)).setText(getIntent().getStringExtra("title"));
        ((TextView) findViewById(R.id.descriptionText)).setText(getIntent().getStringExtra("description"));
        ((TextView) findViewById(R.id.linkText)).setText(getIntent().getStringExtra("link"));
        long publishedAt = getIntent().getLongExtra("publishedAt", System.currentTimeMillis());
        ((TextView) findViewById(R.id.dateText)).setText(DateFormat.format("dd.MM.yyyy HH:mm", new Date(publishedAt)));
    }
}

