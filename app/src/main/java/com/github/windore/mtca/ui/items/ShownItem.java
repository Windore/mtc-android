package com.github.windore.mtca.ui.items;

import com.github.windore.mtca.mtc.MtcItem;

public class ShownItem {
    private final String header;
    private final MtcItem item;

    public ShownItem(MtcItem item) {
        this.header = null;
        this.item = item;
    }

    public ShownItem(String header) {
        this.header = header;
        this.item = null;
    }

    public boolean isHeader() {
        return header != null;
    }

    public String getHeader() {
        return header;
    }

    public MtcItem getItem() {
        return item;
    }
}
