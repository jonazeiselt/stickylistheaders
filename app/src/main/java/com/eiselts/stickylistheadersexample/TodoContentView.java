package com.eiselts.stickylistheadersexample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Jonas Eiselt on 2020-02-23.
 */
class TodoContentView {

    private TextView todoTextView;

    View inflate(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.todo_content, parent, false);
        todoTextView = view.findViewById(R.id.todo_content_textview);
        return view;
    }

    void populateData(Todo item) {
        if (todoTextView != null && item != null) {
            todoTextView.setText(item.getDescription());
        }
    }
}
