package com.wshoto.dabao.http;

import org.json.JSONException;

/**
 * @author user
 */
public interface SubscriberOnNextListener<T> {
    void onNext(T t) throws JSONException;
}