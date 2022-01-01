package com.github.windore.mtca.mtc;

import java.util.Optional;

public class Todo extends MtcItem {
    Todo(Mtc mtc) {
        super("Todo" , mtc);
    }

    @Override
    public Optional<String> removeSelf() {
        return mtc.removeItem(Mtc.ItemType.Todo, getId());
    }
}
