package com.pierdr.tramontana.ui.touch;

import com.pierdr.tramontana.model.Event;
import com.pierdr.tramontana.model.EventSink;

import processing.event.TouchEvent;

public class SingleDragTouchHandler implements TouchHandler {
    private final EventSink eventSink;
    private TouchEvent.Pointer lastTouch;

    public SingleDragTouchHandler(EventSink eventSink) {
        this.eventSink = eventSink;
    }

    @Override
    public void onTouchStart(TouchEvent.Pointer[] touches) {
        if (touches.length <= 0) {
            return;
        }
        eventSink.onEvent(new Event.TouchStart((int) touches[0].x, (int) touches[0].y));
        lastTouch = touches[0];
    }

    @Override
    public void onTouchMove(TouchEvent.Pointer[] touches) {
        if (touches.length <= 0) {
            return;
        }
        eventSink.onEvent(new Event.TouchDrag((int) touches[0].x, (int) touches[0].y));
        lastTouch = touches[0];
    }

    @Override
    public void onTouchEnd(TouchEvent.Pointer[] touches) {
        eventSink.onEvent(new Event.TouchEnd((int) lastTouch.x, (int) lastTouch.y));
    }
}
