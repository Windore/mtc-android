package com.github.windore.mtca.ui.sync;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.github.windore.mtca.databinding.FragmentSyncBinding;

public class SyncFragment extends Fragment {

    private SyncViewModel syncViewModel;
    private FragmentSyncBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        syncViewModel =
                new ViewModelProvider(this).get(SyncViewModel.class);

        binding = FragmentSyncBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textSync;
        syncViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}