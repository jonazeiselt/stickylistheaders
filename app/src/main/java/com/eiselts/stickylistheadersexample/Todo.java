package com.eiselts.stickylistheadersexample;

import androidx.annotation.NonNull;

/**
 * Created by Jonas Eiselt on 2020-02-23.
 */
class Todo {

    private final String description;
    private final String dateString;

    Todo(String description, String dateString) {
        this.description = description;
        this.dateString = dateString;
    }

    public String getDescription() {
        return description;
    }

    public String getDateString() {
        return dateString;
    }

    @NonNull
    @Override
    public String toString() {
        return "Todo{description='" + description + "', dateString='" + dateString + "'}";
    }
}
