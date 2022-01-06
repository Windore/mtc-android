package com.github.windore.mtca.ui.items;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.windore.mtca.R;
import com.github.windore.mtca.mtc.MtcItem;

public class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ViewHolder> {
    private final ShownItem[] shownItems;

    public ItemsAdapter(ShownItem[] shownItems) {
        this.shownItems = shownItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_shown_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShownItem item = shownItems[position];
        if (item.isHeader()) {
            holder.setAsHeader(item.getHeader());
        } else {
            holder.setMtcItem(item.getItem());
        }
    }

    @Override
    public int getItemCount() {
        return shownItems.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageButton removeBtn;
        private final ImageButton doTaskBtn;
        private final TextView tv;
        private MtcItem item;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Get the remove button and set correct removing functionality
            ImageButton removeBtn = itemView.findViewById(R.id.remove_item_btn);
            removeBtn.setOnClickListener(view -> {
                if (item == null) return;

                new AlertDialog.Builder(itemView.getContext())
                        .setMessage(R.string.remove_confirmation)
                        .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                            item.remove();
                            dialogInterface.dismiss();
                        })
                        .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                        .create()
                        .show();
            });

            // Set do task button functionality
            ImageButton doTaskBtn = itemView.findViewById(R.id.do_task_btn);
            doTaskBtn.setOnClickListener(view1 -> {
                // If the item doesn't have a duration it cannot have do task functionality.
                if (item == null || !item.getDuration().isPresent()) return;

                if (TaskTimerService.isTimerRunning()) {
                    new AlertDialog.Builder(view1.getContext())
                            .setMessage(R.string.timer_already_running)
                            .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                            .create()
                            .show();
                    return;
                }

                Intent serviceIntent = new Intent(view1.getContext(), TaskTimerService.class);
                serviceIntent.putExtra("name", item.getString());
                serviceIntent.putExtra("duration", item.getDuration().get());
                view1.getContext().startForegroundService(serviceIntent);
            });

            this.removeBtn = removeBtn;
            this.doTaskBtn = doTaskBtn;
            this.tv = itemView.findViewById(R.id.item_tv);
        }

        /**
         * Set the ViewHolder to contain a MtcItem.
         *
         * @param item item for the ViewHolder to contain.
         */
        public void setMtcItem(MtcItem item) {
            this.item = item;
            removeBtn.setVisibility(View.VISIBLE);
            // If no duration is set, then the item doesn't have do task functionality.
            if (item.getDuration().isPresent()) {
                doTaskBtn.setVisibility(View.VISIBLE);
            } else {
                doTaskBtn.setVisibility(View.GONE);
            }
            tv.setTextSize(16);
            tv.setTypeface(Typeface.DEFAULT);
            tv.setText(item.getString());
        }

        /**
         * Set the ViewHolder to be a header.
         *
         * @param header the content of the header.
         */
        public void setAsHeader(String header) {
            item = null;
            doTaskBtn.setVisibility(View.GONE);
            removeBtn.setVisibility(View.GONE);
            tv.setTextSize(16);
            tv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            tv.setText(header);
        }
    }

}
