package com.github.windore.mtca.ui.sync;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.windore.mtca.MtcApplication;
import com.github.windore.mtca.R;
import com.github.windore.mtca.databinding.FragmentSyncBinding;
import com.github.windore.mtca.mtc.Mtc;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class SyncFragment extends Fragment {

    private FragmentSyncBinding binding;
    private Mtc mtc;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mtc = ((MtcApplication) context.getApplicationContext()).getMtc();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSyncBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        EditText usernameET = binding.editTextUsername;
        EditText addressET = binding.editTextServerAddress;
        EditText pathET = binding.editTextServerPath;

        Context context = getContext();
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences("sync", Context.MODE_PRIVATE);
            String username = preferences.getString("username", null);
            String address = preferences.getString("address", null);
            String path = preferences.getString("path", null);

            if (username != null) {
                usernameET.setText(username);
            }
            if (address != null) {
                addressET.setText(address);
            }
            if (path != null) {
                pathET.setText(path);
            }
        }

        usernameET.addTextChangedListener(new SyncTextWatcher("username"));
        addressET.addTextChangedListener(new SyncTextWatcher("address"));
        pathET.addTextChangedListener(new SyncTextWatcher("path"));

        binding.buttonSync.setOnClickListener(view -> {
                    LayoutInflater layoutInflater = requireActivity().getLayoutInflater();
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.text_password_inp)
                            .setView(layoutInflater.inflate(R.layout.dialog_get_password, null, false))
                            .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                            .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                                EditText passwordET = ((AlertDialog) dialogInterface).findViewById(R.id.edit_text_password);
                                sync(
                                        view.getContext(),
                                        usernameET.getText().toString(),
                                        addressET.getText().toString(),
                                        pathET.getText().toString(),
                                        passwordET.getText().toString()
                                );
                                dialogInterface.dismiss();
                            })
                            .create()
                            .show();
                }
        );

        return root;
    }

    @SuppressWarnings("deprecation")
    private void sync(Context context, String username, String address, String path, String password) {
        // ProgressDialogs are deprecated but are in my opinion a perfect fit for this task
        // and for that reason a ProgressDialog is used here. You could probably do the syncing in
        // the background but then you would have to account for adding items during syncing and
        // that is really not something I want to do. So a ProgressDialog is used here since I don't
        // want to just recreate it.
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle("Syncing");
        dialog.setMessage("Wait for the sync to finish.");
        dialog.setCancelable(false);
        dialog.show();

        Supplier<String> syncTask = () -> mtc.sync(username, address, path, password);
        CompletableFuture<String> task = CompletableFuture.supplyAsync(syncTask);

        // Is this safe?
        task.thenAccept(error -> requireActivity().runOnUiThread(() -> {
            dialog.dismiss();
            String title = getString(R.string.text_sync_failed);
            if (error == null) {
                title = getString(R.string.text_sync_successful);
            }

            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(error)
                    .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                    .create()
                    .show();
        }));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class SyncTextWatcher implements TextWatcher {

        String key;

        public SyncTextWatcher(String key) {
            this.key = key;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            Context context = getContext();
            if (context != null) {
                SharedPreferences preferences = context.getSharedPreferences("sync", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, editable.toString());
                editor.apply();
            }
        }
    }
}