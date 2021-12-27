package com.github.windore.mtca.ui.items;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ItemsViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public ItemsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is items fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}