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

/**
 * Adapter pro zobrazení uložených měření v ListView.
 */
public class MeasurementAdapter extends BaseAdapter {
    /**
     * Callback pro tlačítko smazání na řádku měření.
     */
    public interface DeleteClickListener {
        /**
         * Předá aktivitu, kterou chce uživatel smazat.
         */
        void onDeleteClick(ActivityRecord record);
    }

    private final LayoutInflater inflater;
    private final List<ActivityRecord> records = new ArrayList<>();
    private final DeleteClickListener deleteClickListener;

    /**
     * Připraví adapter a uloží callback pro mazání měření.
     */
    public MeasurementAdapter(Context context, DeleteClickListener deleteClickListener) {
        this.inflater = LayoutInflater.from(context);
        this.deleteClickListener = deleteClickListener;
    }

    /**
     * Nahradí aktuální data novým seznamem měření.
     */
    public void setRecords(List<ActivityRecord> newRecords) {
        records.clear();
        records.addAll(newRecords);
        notifyDataSetChanged();
    }

    /**
     * Vrací počet řádků v seznamu.
     */
    @Override
    public int getCount() {
        return records.size();
    }

    /**
     * Vrací měření na dané pozici.
     */
    @Override
    public ActivityRecord getItem(int position) {
        return records.get(position);
    }

    /**
     * Vrací stabilní id řádku podle id měření v databázi.
     */
    @Override
    public long getItemId(int position) {
        return records.get(position).id;
    }

    /**
     * Vytvoří nebo znovu použije řádek seznamu a vyplní ho daty měření.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = inflater.inflate(R.layout.list_item_measurement, parent, false);
        }

        ActivityRecord record = getItem(position);
        TextView title = row.findViewById(R.id.rowTitleText);
        TextView subtitle = row.findViewById(R.id.rowSubtitleText);
        row.findViewById(R.id.deleteButton).setOnClickListener(view -> deleteClickListener.onDeleteClick(record));

        String start = DateFormat.format("dd.MM.yyyy HH:mm", record.startedAt).toString();
        String end = record.isFinished()
                ? DateFormat.format("HH:mm", record.endedAt).toString()
                : "probiha";
        long duration = record.getDisplayEndTime() - record.startedAt;

        title.setText(String.format(Locale.US, "Mereni %s - %s", start, end));
        subtitle.setText(String.format(
                Locale.US,
                "Doba %s | kroky %d | %s | intenzita %s",
                ActivityStatsCalculator.formatDuration(duration),
                record.totalSteps,
                ActivityStatsCalculator.formatDistance(record.distanceMeters),
                ActivityStatsCalculator.formatIntensity(record.averageIntensity)
        ));
        return row;
    }
}
