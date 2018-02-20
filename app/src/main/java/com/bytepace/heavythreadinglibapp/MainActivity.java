package com.bytepace.heavythreadinglibapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.bytepace.heavythreading.HeavyThreading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import rx.Subscriber;

public class MainActivity extends AppCompatActivity {

    TextView text_view;
    private static final String PARAM_MAX = "max";
    private static final String PARAM_DIVISION = "division";
    private static final String PARAM_START_TIME = "start_time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text_view = findViewById(R.id.text_view);
        text_view.setText("");
        final int max = 50000;
        singleThreadSimpleCount(max, new Runnable() {
            @Override
            public void run() {
                multiThreadSimpleCount(max);
            }
        });
    }

    private boolean checkSimple(int number) {
        int sqrt = (int) Math.floor(Math.sqrt(number));
        for (int i = 2; i <= sqrt; i++) {
            Double small = (double) number / (double) i - Math.floor(number / i);
            if (small < 0.00000005d) {
                return false;
            }
        }
        return true;
    }

    private void singleThreadSimpleCount(final int to_max, final Runnable r) {
        text_view.setText(String.format(getString(R.string.started_single), to_max));
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                int simpleCount = 0;
                final long time = System.currentTimeMillis();
                for (int i = 1; i < to_max; i++) {
                    boolean result = checkSimple(i);
                    if (result) {
                        simpleCount++;
                    }
                }
                final int finalSimpleCount = simpleCount;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text_view.setText(String.format(getString(R.string.ended_single), text_view.getText(), finalSimpleCount, System.currentTimeMillis() - time));
                        r.run();
                    }
                });
            }
        });
        t.start();
    }

    private void multiThreadSimpleCount(int to_max) {
        text_view.setText(String.format(getString(R.string.started_multiple), text_view.getText(), to_max));
        final int[] simpleCount = {0};
        long time = System.currentTimeMillis();
        Map<String, Long> params = new HashMap<>();
        params.put(PARAM_MAX, (long) to_max);
        params.put(PARAM_START_TIME, time);
        params.put(PARAM_DIVISION, 8L);
        HeavyThreading.load(params, new HeavyThreading.LoaderInterface<Long, ArrayList<Integer>, Integer>() {
            @Override
            public void preThread(Subscriber<? super ArrayList<Integer>> subscriber, Map<String, Long> incoming_settings) {
                ArrayList<Integer> batch = new ArrayList<>();
                for (int i = 1; i <= incoming_settings.get(PARAM_MAX); i++) {
                    batch.add(i);
                    if (batch.size() > incoming_settings.get(PARAM_MAX) / incoming_settings.get(PARAM_DIVISION)) {
                        subscriber.onNext(batch);
                        batch = new ArrayList<>();
                    }
                }
                if (batch.size() > 0) {
                    subscriber.onNext(batch);
                }
                subscriber.onCompleted();
            }

            @Override
            public Integer onThread(ArrayList<Integer> batch, Map<String, Long> incoming_settings) {
                int batch_simple_count = 0;
                for (Integer i : batch) {
                    if (checkSimple(i)) {
                        batch_simple_count++;
                    }
                }
                return batch_simple_count;
            }

            @Override
            public void postThread(Integer batch_simple_count, Map<String, Long> incoming_settings) {
                simpleCount[0] += batch_simple_count;
            }

            @Override
            public void doneThread(Map<String, Long> incoming_settings) {
                long time = System.currentTimeMillis() - incoming_settings.get(PARAM_START_TIME);
                text_view.setText(String.format(getString(R.string.ended_multiple), text_view.getText(), simpleCount[0], time));
            }
        });
    }
}
