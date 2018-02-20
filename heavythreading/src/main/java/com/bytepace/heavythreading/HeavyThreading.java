package com.bytepace.heavythreading;

import android.os.Handler;

import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Generic RxJava pattern that allows to emit some data and do some heavy calculations
 * in parallel calculation threads. After the computation data will be reduced to one thread.
 * Best used when data could be divided in 8-16 parts
 * Created by Viktor on 19.02.2018.
 */

public class HeavyThreading {

    /**
     * Interface used for callbacks in {@link HeavyThreading#load load} method
     *
     * @param <U> Incoming data map type
     * @param <V> Internal data type before heavy transformation
     * @param <T> Data type after heavy transformation
     */
    public interface LoaderInterface<U, V, T> {
        /**
         * Called on start of the sequence. Used to emit values, based on incoming_settings.
         *
         * @param subscriber        An RxJava subscriber. Use onNext(V) method to emit data.
         *                          Use onCompleted() method to signal that all data is emitted.
         * @param incoming_settings Map of incoming data to base the generation of data on.
         */
        void preThread(Subscriber<? super V> subscriber, Map<String, U> incoming_settings);

        /**
         * Called on start of heavy loading computation section. This method will be executed in
         * parralel threads, so be careful about deadlocks and race conditions.
         *
         * @param emitted_data      Data emitted by preThread.
         * @param incoming_settings Settings that emitted data base on.
         * @return Data after heavy computation
         */
        T onThread(V emitted_data, Map<String, U> incoming_settings);

        /**
         * Called when heavy computation is done for a next part of the emitted data.
         *
         * @param computed_data     Data, emitted by preThread and computed by onThread.
         * @param incoming_settings Settings that emitted/computed data base on.
         */
        void postThread(T computed_data, Map<String, U> incoming_settings);

        /**
         * Called when every piece of data is computed and postThread-processed.
         *
         * @param incoming_settings Settings that emitted data base on.
         */
        void doneThread(Map<String, U> incoming_settings);
    }


    /**
     * Generic RxJava method that allows to emit some data and do some heavy calculations
     * in parallel calculation threads.
     *
     * @param incoming_settings Map of parameters to use in threads
     * @param callback          LoaderInterface callback
     * @param <U>               Incoming data map type
     * @param <V>               Internal data type before heavy transformation
     * @param <T>               Data type after heavy transformation
     */
    public static <U, V, T> void load(final Map<String, U> incoming_settings, final LoaderInterface<U, V, T> callback) {
        final Handler handler = new Handler();
        Observable.create(new Observable.OnSubscribe<V>() {
            @Override
            public void call(Subscriber<? super V> subscriber) {
                callback.preThread(subscriber, incoming_settings);
            }
        }).subscribeOn(Schedulers.computation())
                .flatMap(new Func1<V, Observable<T>>() {
                    @Override
                    public Observable<T> call(V result) {
                        return Observable.just(result)
                                .subscribeOn(Schedulers.computation())
                                .map(new Func1<V, T>() {
                                    @Override
                                    public T call(V s) {
                                        return callback.onThread(s, incoming_settings);
                                    }
                                });
                    }
                })
                .observeOn(Schedulers.newThread())
                .subscribe(new Subscriber<T>() {
                    @Override
                    public void onCompleted() {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.doneThread(incoming_settings);
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(final T tResult) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.postThread(tResult, incoming_settings);
                            }
                        });

                    }
                });
    }
}