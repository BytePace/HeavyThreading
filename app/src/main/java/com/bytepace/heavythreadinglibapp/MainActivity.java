package com.bytepace.heavythreadinglibapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.bytepace.heavythreading.HeavyThreading;

import java.util.ArrayList;
import java.util.Map;

import rx.Subscriber;

public class MainActivity extends AppCompatActivity {

    TextView text_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text_view = findViewById(R.id.text_view);
        text_view.setText("");
        int max = 200000;
        singleThreadSimpleCount(max);
    }

    public static String getThreadSignature() {
        Thread t = Thread.currentThread();
        long l = t.getId();
        String name = t.getName();
        long p = t.getPriority();
        String gname = t.getThreadGroup().getName();
        return (name + ":(id)" + l + ":(priority)" + p
                + ":(group)" + gname);
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

    private void singleThreadSimpleCount(final int to_max) {
        text_view.setText("Started single thread for " + to_max + " units");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                int simpleCount = 0;
                final long time = System.currentTimeMillis();
                for (int i = 1; i < to_max; i++) {
                    if (i % 100000 == 0) Log.d("whoa", i + "");
                    boolean result = checkSimple(i);
                    if (result) {
                        simpleCount++;
                    }
                }
                final int finalSimpleCount = simpleCount;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text_view.setText(text_view.getText() + "\nSINGLE THREAD   count = " + finalSimpleCount + "; time = " + (System.currentTimeMillis() - time));
                        multiThreadSimpleCount(to_max);
                    }
                });
            }
        });
        t.start();
    }

    private void multiThreadSimpleCount(final int to_max) {
        text_view.setText(text_view.getText() + "\nMultiple single thread for " + to_max + " units");
        final int[] simpleCount = {0};
        final long time = System.currentTimeMillis();
        HeavyThreading.load(null, new HeavyThreading.LoaderInterface<Integer, ArrayList<Integer>, Integer>() {
            @Override
            public void preThread(Subscriber<? super ArrayList<Integer>> subscriber, Map<String, Integer> incoming_settings) {
                ArrayList<Integer> batch = new ArrayList<>();
                for (int i = 1; i <= to_max; i++) {
                    if (i % 100000 == 0) Log.d("whoa", i + "");
                    batch.add(i);
                    if (batch.size() > to_max / 8) {
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
            public Integer onThread(ArrayList<Integer> batch, Map<String, Integer> incoming_settings) {
                Log.d("whoa", batch.size() + "");
                int batch_simple_count = 0;
                for (Integer i : batch) {
                    if (checkSimple(i)) {
                        batch_simple_count++;
                    }
                }
                return batch_simple_count;
            }

            @Override
            public void postThread(Integer batch_simple_count, Map<String, Integer> incoming_settings) {
                simpleCount[0] += batch_simple_count;
            }

            @Override
            public void doneThread() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text_view.setText(text_view.getText() + "\nMULTIPLE THREAD   count = " + simpleCount[0] + "; time = " + (System.currentTimeMillis() - time)+ "\ndone");
                    }
                });
            }
        });
    }
}
