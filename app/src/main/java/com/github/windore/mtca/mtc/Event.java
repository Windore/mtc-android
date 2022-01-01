package com.github.windore.mtca.mtc;

import java.util.Optional;

public class Event extends MtcItem {

    Event(Mtc mtc) {
        super("Event", mtc);
    }

    @Override
    public Optional<String> removeSelf() {
        return mtc.removeItem(Mtc.ItemType.Event, getId());
    }

}
