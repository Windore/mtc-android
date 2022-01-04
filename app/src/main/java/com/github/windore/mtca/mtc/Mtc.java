package com.github.windore.mtca.mtc;

import androidx.annotation.Nullable;

import com.github.windore.mtca.ui.items.ShownItem;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.stream.Collectors;

/**
 * An interface between mtc-rust and java.
 */
public class Mtc extends Observable {
    private static boolean isInitialised = false;

    /**
     * Constructs and returns a instance of Mtc if there has not been any instances yet constructed.
     * Throws an IllegalStateException if a Mtc has previously been constructed.
     * @return a Mtc instance
     */
    public static Mtc constructOnlyOnce() {
        if (isInitialised) {
            throw new IllegalStateException("Mtc has already been initialised once.");
        }
        isInitialised = true;
        return new Mtc();
    }

    private Mtc() {
        System.loadLibrary("rustmtca");
        nativeInit();
    }

    private static native void nativeInit();

    private static native long[] nativeGetTodos();
    private static native long[] nativeGetTodosForDate(long timestamp_secs);
    private static native long[] nativeGetTodosForWeekday(int weekday_n);
    private static native String nativeGetTodoString(long id);
    private static native void nativeAddTodo(String body, int weekday);
    private static native void nativeRemoveTodo(long id);

    private static native long[] nativeGetTasks();
    private static native long[] nativeGetTasksForDate(long timestamp_secs);
    private static native long[] nativeGetTasksForWeekday(int weekday_n);
    private static native String nativeGetTaskString(long id);
    private static native long nativeGetTaskDuration(long id);
    private static native void nativeAddTask(String body, int weekday, long duration);
    private static native void nativeRemoveTask(long id);

    private static native long[] nativeGetEvents();
    private static native long[] nativeGetEventsForDate(long timestamp_secs);
    private static native long[] nativeGetEventsForWeekday(int weekday_n);
    private static native String nativeGetEventString(long id);
    private static native void nativeAddEvent(String body, long timestamp_secs);
    private static native void nativeRemoveEvent(long id);

    /**
     * Returns all items of a given type sorted.
     * @param type the type of item to return.
     * @return all items of a given type sorted.
     */
    public List<MtcItem> getItems(MtcItem.ItemType type) {
        switch (type) {
            case Todo:
                return listFromArrayOfIds(nativeGetTodos(), MtcItem.ItemType.Todo);
            case Task:
                return listFromArrayOfIds(nativeGetTasks(), MtcItem.ItemType.Task);
            case Event:
                return listFromArrayOfIds(nativeGetEvents(), MtcItem.ItemType.Event);
        }
        return null;
    }

    /**
     * Returns all items of a given type which are for a given weekday sorted.
     * @param type the type of item to return
     * @param weekday the weekday
     * @return all items of a given type which are for a given weekday sorted.
     */
    public List<MtcItem> getItemsForWeekday(MtcItem.ItemType type, DayOfWeek weekday) {
        int n = weekday.getValue() - 1; // Rust uses values from 0-6 for weekdays unlike java
        switch (type) {
            case Todo:
                return listFromArrayOfIds(nativeGetTodosForWeekday(n), MtcItem.ItemType.Todo);
            case Task:
                return listFromArrayOfIds(nativeGetTasksForWeekday(n), MtcItem.ItemType.Task);
            case Event:
                return listFromArrayOfIds(nativeGetEventsForWeekday(n), MtcItem.ItemType.Event);
        }
        return null;
    }

    /**
     * Returns all items of a given type which are for a given date sorted.
     * @param type the type of item to return
     * @param date the date
     * @return all items of a given type which are for a given date sorted.
     */
    public List<MtcItem> getItemsForDate(MtcItem.ItemType type, Date date) {
        long timestamp_secs = date.getTime() / 1000;
        switch (type) {
            case Todo:
                return listFromArrayOfIds(nativeGetTodosForDate(timestamp_secs), MtcItem.ItemType.Todo);
            case Task:
                return listFromArrayOfIds(nativeGetTasksForDate(timestamp_secs), MtcItem.ItemType.Task);
            case Event:
                return listFromArrayOfIds(nativeGetEventsForDate(timestamp_secs), MtcItem.ItemType.Event);
        }
        return null;
    }

    /**
     * Maps all MtcItems in a given list to a list of ShownItems. This function exists for writing readable code.
     * Returns a new list containing the ShownItems.
     * @param list list of MtcItem to map to ShownItems.
     * @return a list of mapped MtcItems as shown items.
     */
    public List<ShownItem> listToShown(List<MtcItem> list) {
        return list.stream().map(ShownItem::new).collect(Collectors.toList());
    }

    private List<MtcItem> listFromArrayOfIds(long[] ids, MtcItem.ItemType type) {
        ArrayList<MtcItem> list = new ArrayList<>();
        for (long id: ids) {
            list.add(new MtcItem(type, id, this));
        }
        list.sort(Comparator.comparing(MtcItem::getString));
        return list;
    }

    /**
     * Creates a new Todo and adds it to the todo list.
     */
    public void newTodo(String body, @Nullable DayOfWeek weekday) {
        int n;
        if (weekday == null) {
            n = -1;
        } else {
            n = weekday.getValue() - 1;
        }
        nativeAddTodo(body, n);

        setChanged();
        notifyObservers();
    }

    /**
     * Creates a new Task and adds it to the task list.
     */
    public void newTask(String body, @Nullable DayOfWeek weekday, long duration) {
        int n;
        if (weekday == null) {
            n = -1;
        } else {
            n = weekday.getValue() - 1;
        }

        nativeAddTask(body, n, duration);

        setChanged();
        notifyObservers();
    }

    /**
     * Creates a new Event and adds it to the event list.
     */
    public void newEvent(String body, Date date) {
        long seconds = date.getTime() / 1000;

        nativeAddEvent(body, seconds);

        setChanged();
        notifyObservers();
    }

    String getString(MtcItem.ItemType type, long id) {
        switch (type) {
            case Todo:
                return nativeGetTodoString(id);
            case Task:
                return nativeGetTaskString(id);
            case Event:
                return nativeGetEventString(id);
        }
        return null;
    }

    void removeItem(MtcItem.ItemType type, long id) {
        switch (type) {
            case Todo:
                nativeRemoveTodo(id);
                break;
            case Task:
                nativeRemoveTask(id);
                break;
            case Event:
                nativeRemoveEvent(id);
                break;
        }

        setChanged();
        notifyObservers();
    }

    long getTaskDuration(long id) {
        return nativeGetTaskDuration(id);
    }
}
