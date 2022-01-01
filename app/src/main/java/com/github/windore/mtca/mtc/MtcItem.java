package com.github.windore.mtca.mtc;

import java.util.Optional;

/**
 * A base class for different MtcItems.
 */
public abstract class MtcItem {
    private int id;
    private final String body;

    protected final Mtc mtc;

    protected MtcItem(String body, Mtc mtc) {
        this.body = body;
        this.mtc = mtc;
    }

    /**
     * Returns the body of the item.
     *
     * @return the body of the item.
     */
    public String getBody() {
        return body;
    }

    /**
     * Returns the id of the item.
     *
     * @return the id of the item.
     */
    public int getId() {
        return id;
    }

    // This needs to be package private so that the id can be correctly set by Mtc
    void setId(int id)  {
        this.id = id;
    }

    public abstract Optional<String> removeSelf();

    public Optional<Integer> getDuration() {
        return Optional.empty();
    }
}
