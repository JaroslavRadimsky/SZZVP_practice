package cz.ujep.activitytracker;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MeasurementAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<ActivityRecord> records = new ArrayList<>();

    public MeasurementAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setRecords(List<ActivityRecord> newRecords) {
        records.clear();
        records.addAll(newRecords);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return records.size();
    }

    @Override
    public ActivityRecord getItem(int position) {
        return records.get(position);
    }

    @Override
    public long getItemId(int position) {
        return records.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = inflater.inflate(R.layout.list_item_measurement, parent, false);
        }

        ActivityRecord record = getItem(position);
        TextView title = row.findViewById(R.id.rowTitleText);
        TextView subtitle = row.findViewById(R.id.rowSubtitleText);

        String start = DateFormat.format("dd.MM.yyyy HH:mm", record.startedAt).toString();
        String end = record.isFinished()
                ? DateFormat.format("HH:mm", record.endedAt).toString()
                : "probiha";
        long duration = record.getDisplayEndTime() - record.startedAt;

        title.setText(String.format(Locale.US, "Mereni %s - %s", start, end));
        subtitle.setText(String.format(
                Locale.US,
                "Doba %s | kroky %d | %s | intenzita %s | vzorky %d",
                ActivityStatsCalculator.formatDuration(duration),
                record.totalSteps,
                ActivityStatsCalculator.formatDistance(record.distanceMeters),
                ActivityStatsCalculator.formatIntensity(record.averageIntensity),
                record.sampleCount
        ));
        return row;
    }
}
