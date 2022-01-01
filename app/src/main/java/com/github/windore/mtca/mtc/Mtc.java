package com.github.windore.mtca.mtc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Optional;

/**
 * An interface between mtc-rust and java.
 */
public class Mtc extends Observable {
    private final ArrayList<Todo> todos = new ArrayList<>();
    private final ArrayList<Task> tasks = new ArrayList<>();
    private final ArrayList<Event> events = new ArrayList<>();

    private int idCounter = 0;

    /**
     * An enum containing all the possible types for MtcItems.
     */
    public enum ItemType {
        Todo,
        Task,
        Event
    }

    /**
     * Returns all todos as an unmodifiable list.
     *
     * @return all todos as an unmodifiable list.
     */
    public List<Todo> getTodos() {
        return Collections.unmodifiableList(todos);
    }

    /**
     * Returns all tasks as an unmodifiable list.
     *
     * @return all tasks as an unmodifiable list.
     */
    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    /**
     * Returns all events as an unmodifiable list.
     *
     * @return all events as an unmodifiable list.
     */
    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Creates a new Todo and adds it to the todo list.
     */
    public void newTodo() {
        Todo todo = new Todo(this);
        todo.setId(getNextTodoId());
        todos.add(todo);

        setChanged();
        notifyObservers();
    }

    /**
     * Creates a new Task and adds it to the task list.
     */
    public void newTask() {
        Task task = new Task(this);
        task.setId(getNextTaskId());
        tasks.add(task);

        setChanged();
        notifyObservers();
    }

    /**
     * Creates a new Event and adds it to the event list.
     */
    public void newEvent() {
        Event event = new Event(this);
        event.setId(getNextEventId());
        events.add(event);

        setChanged();
        notifyObservers();
    }

    /**
     * Marks an item as removed. Returns a String if something went wrong. Otherwise returns empty.
     *
     * @param type The type of an item to be removed
     * @param id   The id of an item to be removed
     * @return A String if removing an item failed with a message. Otherwise empty.
     */
    public Optional<String> removeItem(ItemType type, int id) {
        // Todos, Tasks or Events should generally only have a single item with a given id so the following works.
        switch (type) {
            case Todo:
                todos.removeIf(todo -> todo.getId() == id);
                break;
            case Task:
                tasks.removeIf(todo -> todo.getId() == id);
                break;
            case Event:
                events.removeIf(todo -> todo.getId() == id);
                break;
        }

        setChanged();
        notifyObservers();

        return Optional.empty();
    }

    private int getNextTodoId() {
        idCounter++;
        return idCounter;
    }

    private int getNextTaskId() {
        idCounter++;
        return idCounter;
    }

    private int getNextEventId() {
        idCounter++;
        return idCounter;
    }
}
