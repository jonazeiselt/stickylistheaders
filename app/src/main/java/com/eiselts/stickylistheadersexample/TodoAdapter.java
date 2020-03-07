package com.eiselts.stickylistheadersexample;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.eiselts.stickylistheaders.StickyListHeadersAdapter;

import java.util.List;

/**
 * Created by Jonas Eiselt on 2020-02-23.
 */
class TodoAdapter extends BaseAdapter implements StickyListHeadersAdapter {

    private final List<Todo> todos;

    TodoAdapter(List<Todo> todos) {
        this.todos = todos;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        TodoHeaderView todoHeaderView;
        if (convertView == null) {
            todoHeaderView = new TodoHeaderView();
            convertView = todoHeaderView.inflate(parent);
            convertView.setTag(todoHeaderView);
        } else {
            todoHeaderView = (TodoHeaderView) convertView.getTag();
        }
        todoHeaderView.populateData(getItem(position).getDateString());
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        String dateString = todos.get(position).getDateString();
        return Long.parseLong(dateString.replaceAll("-", ""));
    }

    @Override
    public int getCount() {
        return todos.size();
    }

    @Override
    public Todo getItem(int position) {
        return position >= 0 && position < todos.size() ? todos.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TodoContentView todoContentView;
        if (convertView == null) {
            todoContentView = new TodoContentView();
            convertView = todoContentView.inflate(parent);
            convertView.setTag(todoContentView);
        } else {
            todoContentView = (TodoContentView) convertView.getTag();
        }
        todoContentView.populateData(getItem(position));
        return convertView;
    }
}
