package com.eiselts.stickylistheadersexample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Jonas Eiselt on 2020-02-23.
 */
class TodoHeaderView {

    private TextView todoTextView;

    View inflate(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.todo_header, parent, false);
        todoTextView = view.findViewById(R.id.todo_header_textview);
        return view;
    }

    void populateData(String value) {
        if (todoTextView != null) {
            todoTextView.setText(value);
        }
    }
}
