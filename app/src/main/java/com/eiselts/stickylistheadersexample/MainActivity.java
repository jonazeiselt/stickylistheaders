package com.eiselts.stickylistheadersexample;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eiselts.stickylistheaders.StickyListHeadersListView;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TodoAdapter adapter = new TodoAdapter(Arrays.asList(
                new Todo("Do laundry", "2020-02-23"),
                new Todo("Do some Android programming", "2020-02-23"),
                new Todo("Eat dinner", "2020-02-23"),
                new Todo("Sleep", "2020-02-23"),
                new Todo("Wake up", "2020-02-24"),
                new Todo("Commute to work", "2020-02-24"),
                new Todo("Work", "2020-02-24"),
                new Todo("Eat lunch at work", "2020-02-24"),
                new Todo("Work", "2020-02-24"),
                new Todo("Commute home", "2020-02-24"),
                new Todo("Sleep", "2020-02-24"),
                new Todo("Wake up", "2020-02-25"),
                new Todo("Eat breakfast", "2020-02-25"),
                new Todo("Watch television", "2020-02-25"),
                new Todo("Do some Android programming", "2020-03-07"),
                new Todo("Order a pizza", "2020-03-07"),
                new Todo("Watch Melodifestivalen", "2020-03-07")
        ));

        swipeRefreshLayout = findViewById(R.id.swiperefreshlayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Refreshing...");
            doSomeWork();
        });

        StickyListHeadersListView listView = findViewById(R.id.stickylistheaderslistview);
        listView.setAdapter(adapter);
        listView.setOnScrollWhenTopReachedListener(() -> swipeRefreshLayout.setEnabled(true));

        listView.setOnHeaderClickListener((header, position, id) -> {
            Log.d(TAG, "Header click: position=" + position + ", id=" + id);
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(TAG, "Item{position=" + position + ", todo=" + adapter.getItem(position) + "}");
        });
    }

    private void doSomeWork() {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                runOnUiThread(() -> swipeRefreshLayout.setRefreshing(false));
            }
        }).start();
    }
}
