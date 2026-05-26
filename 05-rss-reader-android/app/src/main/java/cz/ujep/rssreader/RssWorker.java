package cz.ujep.rssreader;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

public class RssWorker extends Worker {
    private static final String DEFAULT_URL = "https://www.idnes.cz/rss";

    public RssWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            List<RssItem> items = new RssFetcher().fetch(DEFAULT_URL);
            new RssDatabase(getApplicationContext()).upsert(items);
            return Result.success();
        } catch (Exception ex) {
            return Result.retry();
        }
    }
}

