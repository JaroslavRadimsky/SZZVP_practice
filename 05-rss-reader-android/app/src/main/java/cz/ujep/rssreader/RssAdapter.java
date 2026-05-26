package cz.ujep.rssreader;

import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RssAdapter extends RecyclerView.Adapter<RssAdapter.ViewHolder> {
    public interface ItemClickListener {
        void onItemClick(RssItem item);
    }

    private final List<RssItem> items = new ArrayList<>();
    private final ItemClickListener listener;

    public RssAdapter(ItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<RssItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView view = new TextView(parent.getContext());
        view.setTextSize(17);
        view.setPadding(18, 18, 18, 18);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RssItem item = items.get(position);
        holder.text.setText(item.title);
        holder.text.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView text;

        public ViewHolder(@NonNull TextView itemView) {
            super(itemView);
            this.text = itemView;
        }
    }
}

