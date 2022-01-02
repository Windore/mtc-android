package com.github.windore.mtca.mtc;

import java.util.Optional;

/**
 * A class that represents a MtcItem. It can be a todo, task or an event.
 */
public class MtcItem {
    /**
     * An enum containing all the possible types for MtcItems.
     */
    public enum ItemType {
        Todo,
        Task,
        Event
    }

    private final long id;
    private final ItemType type;
    private final Mtc mtc;

    MtcItem(ItemType type, long id, Mtc mtc) {
        this.type = type;
        this.id = id;
        this.mtc = mtc;
    }

    /**
     * Returns the formatted body of the item.
     *
     * @return the formatted body of the item.
     */
    public String getString() {
        return mtc.getString(type, id);
    }

    /**
     * Returns the id of the item.
     *
     * @return the id of the item.
     */
    public long getId() {
        return id;
    }

    /**
     * Removes the item.
     */
    public void remove() {
        mtc.removeItem(type, id);
    }

    /**
     * If the item is a task returns a duration for the task. Otherwise returns empty.
     * @return a duration for a task. For events and todos empty.
     */
    public Optional<Long> getDuration() {
        if (type == ItemType.Task) {
            return Optional.of(mtc.getTaskDuration(id));
        }
        return Optional.empty();
    }
}
