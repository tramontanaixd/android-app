package com.pierdr.tramontana.ui.touch;

import processing.event.TouchEvent;

public interface TouchHandler {
    void onTouchStart(TouchEvent.Pointer[] touches);

    void onTouchMove(TouchEvent.Pointer[] touches);

    void onTouchEnd(TouchEvent.Pointer[] touches);
}
