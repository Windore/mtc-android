package com.github.windore.mtca.mtc;

import java.util.Optional;

public class Task extends MtcItem {
    Task(Mtc mtc) {
        super( "Task", mtc);
    }

    @Override
    public Optional<String> removeSelf() {
        return mtc.removeItem(Mtc.ItemType.Task, getId());
    }

    @Override
    public Optional<Integer> getDuration() {
        return Optional.of(1);
    }
}
