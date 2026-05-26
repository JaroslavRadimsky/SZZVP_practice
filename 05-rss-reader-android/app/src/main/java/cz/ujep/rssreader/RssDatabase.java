package cz.ujep.rssreader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class RssDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "rss_reader.db";
    private static final int DB_VERSION = 1;
    private static final long RELEVANT_AGE_MS = 7L * 24L * 60L * 60L * 1000L;

    public RssDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE rss_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "link TEXT NOT NULL UNIQUE," +
                "description TEXT NOT NULL," +
                "published_at INTEGER NOT NULL," +
                "fetched_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS rss_items");
        onCreate(db);
    }

    public void upsert(List<RssItem> items) {
        SQLiteDatabase db = getWritableDatabase();
        for (RssItem item : items) {
            ContentValues values = new ContentValues();
            values.put("title", item.title);
            values.put("link", item.link);
            values.put("description", item.description);
            values.put("published_at", item.publishedAt);
            values.put("fetched_at", item.fetchedAt);
            db.insertWithOnConflict("rss_items", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public List<RssItem> listRelevant() {
        long minDate = System.currentTimeMillis() - RELEVANT_AGE_MS;
        Cursor cursor = getReadableDatabase().query(
                "rss_items",
                null,
                "published_at >= ? OR fetched_at >= ?",
                new String[]{String.valueOf(minDate), String.valueOf(minDate)},
                null,
                null,
                "published_at DESC, fetched_at DESC"
        );
        try {
            List<RssItem> items = new ArrayList<>();
            while (cursor.moveToNext()) {
                items.add(new RssItem(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        cursor.getString(cursor.getColumnIndexOrThrow("link")),
                        cursor.getString(cursor.getColumnIndexOrThrow("description")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("published_at")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("fetched_at"))
                ));
            }
            return items;
        } finally {
            cursor.close();
        }
    }
}

