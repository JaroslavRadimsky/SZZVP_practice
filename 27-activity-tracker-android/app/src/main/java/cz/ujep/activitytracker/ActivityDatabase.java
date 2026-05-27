package cz.ujep.activitytracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ActivityDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "activity_tracker.db";
    private static final int DB_VERSION = 2;

    public ActivityDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE measurements (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "started_at INTEGER NOT NULL," +
                "ended_at INTEGER," +
                "total_steps INTEGER NOT NULL DEFAULT 0," +
                "distance_meters REAL NOT NULL DEFAULT 0," +
                "average_intensity REAL NOT NULL DEFAULT 0," +
                "sample_count INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE samples (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "measurement_id INTEGER NOT NULL," +
                "measured_at INTEGER NOT NULL," +
                "steps INTEGER NOT NULL," +
                "intensity REAL NOT NULL," +
                "latitude REAL," +
                "longitude REAL," +
                "distance_meters REAL NOT NULL DEFAULT 0," +
                "FOREIGN KEY(measurement_id) REFERENCES measurements(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX idx_samples_measurement ON samples(measurement_id, measured_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE measurements ADD COLUMN distance_meters REAL NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE samples ADD COLUMN latitude REAL");
            db.execSQL("ALTER TABLE samples ADD COLUMN longitude REAL");
            db.execSQL("ALTER TABLE samples ADD COLUMN distance_meters REAL NOT NULL DEFAULT 0");
        }
    }

    public long startMeasurement(long startedAt) {
        ContentValues values = new ContentValues();
        values.put("started_at", startedAt);
        return getWritableDatabase().insert("measurements", null, values);
    }

    public void insertSample(long measurementId, long measuredAt, int steps, double intensity, double latitude, double longitude, double distanceMeters) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("measurement_id", measurementId);
        values.put("measured_at", measuredAt);
        values.put("steps", Math.max(0, steps));
        values.put("intensity", Math.max(0.0, intensity));
        if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
            values.put("latitude", latitude);
            values.put("longitude", longitude);
        }
        values.put("distance_meters", Math.max(0.0, distanceMeters));
        db.insert("samples", null, values);
        updateSummary(db, measurementId, 0L);
    }

    public void finishMeasurement(long measurementId, long endedAt) {
        updateSummary(getWritableDatabase(), measurementId, endedAt);
    }

    public List<ActivityRecord> listMeasurements() {
        Cursor cursor = getReadableDatabase().query(
                "measurements",
                null,
                null,
                null,
                null,
                null,
                "started_at DESC"
        );
        try {
            List<ActivityRecord> records = new ArrayList<>();
            while (cursor.moveToNext()) {
                records.add(readRecord(cursor));
            }
            return records;
        } finally {
            cursor.close();
        }
    }

    public ActivityRecord getMeasurement(long id) {
        Cursor cursor = getReadableDatabase().query(
                "measurements",
                null,
                "id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        );
        try {
            if (cursor.moveToFirst()) {
                return readRecord(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public List<ActivitySample> listSamples(long measurementId) {
        Cursor cursor = getReadableDatabase().query(
                "samples",
                null,
                "measurement_id = ?",
                new String[]{String.valueOf(measurementId)},
                null,
                null,
                "measured_at ASC"
        );
        try {
            List<ActivitySample> samples = new ArrayList<>();
            while (cursor.moveToNext()) {
                samples.add(new ActivitySample(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("measurement_id")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("measured_at")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("steps")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("intensity")),
                        readNullableDouble(cursor, "latitude", Double.NaN),
                        readNullableDouble(cursor, "longitude", Double.NaN),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("distance_meters"))
                ));
            }
            return samples;
        } finally {
            cursor.close();
        }
    }

    private void updateSummary(SQLiteDatabase db, long measurementId, long endedAt) {
        Cursor cursor = db.rawQuery(
                "SELECT COALESCE(SUM(steps), 0), COALESCE(SUM(distance_meters), 0), COALESCE(AVG(intensity), 0), COUNT(*) FROM samples WHERE measurement_id = ?",
                new String[]{String.valueOf(measurementId)}
        );
        try {
            if (cursor.moveToFirst()) {
                ContentValues values = new ContentValues();
                values.put("total_steps", cursor.getInt(0));
                values.put("distance_meters", cursor.getDouble(1));
                values.put("average_intensity", cursor.getDouble(2));
                values.put("sample_count", cursor.getInt(3));
                if (endedAt > 0L) {
                    values.put("ended_at", endedAt);
                }
                db.update("measurements", values, "id = ?", new String[]{String.valueOf(measurementId)});
            }
        } finally {
            cursor.close();
        }
    }

    private ActivityRecord readRecord(Cursor cursor) {
        int endedColumn = cursor.getColumnIndexOrThrow("ended_at");
        long endedAt = cursor.isNull(endedColumn) ? 0L : cursor.getLong(endedColumn);
        return new ActivityRecord(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("started_at")),
                endedAt,
                cursor.getInt(cursor.getColumnIndexOrThrow("total_steps")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("distance_meters")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("average_intensity")),
                cursor.getInt(cursor.getColumnIndexOrThrow("sample_count"))
        );
    }

    private double readNullableDouble(Cursor cursor, String column, double fallback) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? fallback : cursor.getDouble(index);
    }
}
