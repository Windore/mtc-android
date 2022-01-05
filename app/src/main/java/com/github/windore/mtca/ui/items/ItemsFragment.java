package com.github.windore.mtca.ui.items;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.windore.mtca.MtcApplication;
import com.github.windore.mtca.R;
import com.github.windore.mtca.databinding.FragmentItemsBinding;
import com.github.windore.mtca.mtc.Mtc;
import com.github.windore.mtca.mtc.MtcItem;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class ItemsFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private FragmentItemsBinding binding;
    private Mtc mtc;
    private ItemsViewModel itemsViewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mtc = ((MtcApplication) context.getApplicationContext()).getMtc();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Using a view model might be unnecessary since the spinner onSelected event happens
        // every time which requires re-getting shown items but it probably should do any harm either.
        itemsViewModel = new ViewModelProvider(this).get(ItemsViewModel.class);
        binding = FragmentItemsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Spinner spinner = binding.spinnerShownSelection;
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                view.getContext(),
                R.array.array_shown_selection,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        RecyclerView mtcItemsRView = binding.rviewShownItems;
        mtcItemsRView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        ItemsAdapter adapterItems = new ItemsAdapter(itemsViewModel.getShownItems().getValue());
        mtcItemsRView.setAdapter(adapterItems);

        // If the shown items change update ui.
        itemsViewModel.getShownItems().observe(getViewLifecycleOwner(), shownItems -> {
            ItemsAdapter newAdapterItems = new ItemsAdapter(shownItems);
            mtcItemsRView.swapAdapter(newAdapterItems, false);
        });

        // Observe the mtc item for changes since those might effect shown items.
        mtc.addObserver((observable, o) -> updateShownItems());

        binding.btnAddItem.setOnClickListener(view1 -> addNewMtcItem());
    }

    private void addNewMtcItem() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_type)
                .setItems(R.array.array_mtc_type, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            addNewTodo();
                            break;
                        case 1:
                            addNewTask();
                            break;
                        case 2:
                            addNewEvent();
                            break;
                    }
                    dialog.dismiss();
                })
                .create()
                .show();
    }

    private void addNewTodo() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View layout = inflater.inflate(R.layout.dialog_add_todo, null, false);
        Spinner weekdaySpinner = layout.findViewById(R.id.spinner_weekday);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.array_weekdays,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weekdaySpinner.setAdapter(adapter);

        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.add_new_todo)
                .setView(layout)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    AlertDialog dialog = (AlertDialog) dialogInterface;
                    EditText bodyET = dialog.findViewById(R.id.edit_text_body);
                    int selection = weekdaySpinner.getSelectedItemPosition();
                    DayOfWeek weekday = null;
                    if (selection != 0) {
                        weekday = DayOfWeek.of(weekdaySpinner.getSelectedItemPosition());
                    }
                    mtc.newTodo(bodyET.getText().toString(), weekday);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .create().
                show();
    }

    private void addNewTask() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View layout = inflater.inflate(R.layout.dialog_add_task, null, false);
        Spinner weekdaySpinner = layout.findViewById(R.id.spinner_weekday);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.array_weekdays,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weekdaySpinner.setAdapter(adapter);

        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.add_new_task)
                .setView(layout)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    AlertDialog dialog = (AlertDialog) dialogInterface;
                    EditText bodyET = dialog.findViewById(R.id.edit_text_body);
                    EditText durationET = dialog.findViewById(R.id.edit_text_duration);
                    int selection = weekdaySpinner.getSelectedItemPosition();
                    DayOfWeek weekday = null;
                    if (selection != 0) {
                        weekday = DayOfWeek.of(weekdaySpinner.getSelectedItemPosition());
                    }
                    String durationText = durationET.getText().toString();
                    // Removing all '-' chars rather than properly handling input is probably not the best
                    // however it is a very easy solution and isn't that terrible. A quick google search
                    // didn't give me any other results that worked.
                    durationText = durationText.replace("-", "");

                    // Empty equals 0
                    if (durationText.isEmpty()) {
                        durationText = "0";
                    }
                    long duration = Long.parseLong(durationText);
                    mtc.newTask(bodyET.getText().toString(), weekday, duration);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .create()
                .show();
    }

    private void addNewEvent() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_add_event, null, false);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_new_event)
                .setView(layout)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    AlertDialog dialog = (AlertDialog) dialogInterface;
                    EditText bodyET = dialog.findViewById(R.id.edit_text_body);
                    DatePicker dateDP = dialog.findViewById(R.id.date_picker_date);
                    Calendar cal = Calendar.getInstance();
                    cal.set(dateDP.getYear(), dateDP.getMonth(), dateDP.getDayOfMonth());
                    mtc.newEvent(bodyET.getText().toString(), cal.getTime());
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .create()
                .show();
    }

    private void updateShownItems() {
        // Binding may be null here since mtc can update when this fragment is hidden
        if (binding != null) {
            updateShownItems(binding.spinnerShownSelection.getSelectedItemPosition());
        }
    }

    private void updateShownItems(int selection) {
        ArrayList<ShownItem> items = new ArrayList<>();

        switch (selection) {
            case 0: // Today
            case 1: // Tomorrow
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(Date.from(Instant.now()));
                calendar.add(Calendar.DAY_OF_MONTH, selection); // Adds 0 if today 1 if tomorrow.
                Date date = calendar.getTime();

                items.add(new ShownItem("Todos"));
                items.addAll(mtc.listToShown(mtc.getItemsForDate(MtcItem.ItemType.Todo, date)));
                items.add(new ShownItem("Tasks"));
                items.addAll(mtc.listToShown(mtc.getItemsForDate(MtcItem.ItemType.Task, date)));
                items.add(new ShownItem("Events"));
                items.addAll(mtc.listToShown(mtc.getItemsForDate(MtcItem.ItemType.Event, date)));
                break;
            case 2: // Todos
            case 3: // Tasks
                MtcItem.ItemType type = MtcItem.ItemType.Todo;
                if (selection == 3) type = MtcItem.ItemType.Task;

                // Show each day individually

                // This array contains none as its first element and thus the for loop starts at 1.
                String[] weekdays = getResources().getStringArray(R.array.array_weekdays);
                for (int i = 1; i <= 7; i++) {
                    DayOfWeek weekday = DayOfWeek.of(i);
                    items.add(new ShownItem(weekdays[i]));
                    items.addAll(mtc.listToShown(mtc.getItemsForWeekday(type, weekday)));
                }
                break;
            case 4: // Events
                items.addAll(mtc.listToShown(mtc.getItems(MtcItem.ItemType.Event)));
                break;
            default: // Weekdays
                int weekday_n = selection - 4; // Weekdays are in order and there are 5 items before them so 4 is subtracted since monday is 0
                DayOfWeek weekday = DayOfWeek.of(weekday_n);
                items.add(new ShownItem("Todos"));
                items.addAll(mtc.listToShown(mtc.getItemsForWeekday(MtcItem.ItemType.Todo, weekday)));
                items.add(new ShownItem("Tasks"));
                items.addAll(mtc.listToShown(mtc.getItemsForWeekday(MtcItem.ItemType.Task, weekday)));
                items.add(new ShownItem("Events"));
                items.addAll(mtc.listToShown(mtc.getItemsForWeekday(MtcItem.ItemType.Event, weekday)));
                break;
        }


        // Always add two empty shown items to the end of the list to give space for the add new item btn
        items.add(new ShownItem(""));
        items.add(new ShownItem(""));
        itemsViewModel.setShownItems(items.toArray(new ShownItem[]{}));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        updateShownItems(i);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}