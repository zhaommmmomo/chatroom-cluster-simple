package com.zmm.chatroom.shareable;

import io.vertx.core.shareddata.Shareable;

/**
 * @author zmm
 * @date 2021/9/6 11:45
 */
public class CustomShareable<T> implements Shareable {
    private final T data;

    public CustomShareable(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }
}
