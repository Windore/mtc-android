package com.github.windore.mtca.ui.items;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ItemsViewModel extends ViewModel {
    private final MutableLiveData<ShownItem[]> shownItems;

    public ItemsViewModel() {
        shownItems = new MutableLiveData<>();
        shownItems.setValue(new ShownItem[]{});
    }

    public LiveData<ShownItem[]> getShownItems() {
        return shownItems;
    }

    public void setShownItems(ShownItem[] shownItems) {
        this.shownItems.setValue(shownItems);
    }
}
