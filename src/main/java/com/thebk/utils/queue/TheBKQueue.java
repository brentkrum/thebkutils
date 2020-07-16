package com.thebk.utils.queue;

import com.thebk.utils.rc.RCBoolean;

public interface TheBKQueue {
    boolean enqueue(Object o, RCBoolean committed);
    boolean enqueue(Object o);
    Object dequeue();
    Object peek();
    boolean isFull();
    boolean isEmpty();
}
