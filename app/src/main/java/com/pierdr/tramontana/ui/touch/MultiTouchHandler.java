package com.pierdr.tramontana.ui.touch;

import com.pierdr.tramontana.model.Event;
import com.pierdr.tramontana.model.EventSink;
import com.pierdr.tramontana.model.TouchPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import processing.event.TouchEvent;

public class MultiTouchHandler implements TouchHandler {
    private final EventSink eventSink;
    private List<TouchEvent.Pointer> lastTouches;

    public MultiTouchHandler(EventSink eventSink) {
        this.eventSink = eventSink;
    }

    @Override
    public void onTouchStart(TouchEvent.Pointer[] touches) {
        if (touches.length <= 0) {
            return;
        }
        List<TouchPoint> touchPoints = new ArrayList<>();
        for (TouchEvent.Pointer touch : touches) {
            touchPoints.add(new TouchPoint((int) touch.x, (int) touch.y));
        }
        eventSink.onEvent(new Event.MultiTouchStart(touchPoints));
        lastTouches = Arrays.asList(touches);
    }

    @Override
    public void onTouchMove(TouchEvent.Pointer[] touches) { }

    @Override
    public void onTouchEnd(TouchEvent.Pointer[] touches) {
        List<TouchPoint> touchPoints = new ArrayList<>();
        for (TouchEvent.Pointer touch : lastTouches) {
            touchPoints.add(new TouchPoint((int) touch.x, (int) touch.y));
        }
        eventSink.onEvent(new Event.MultiTouchEnd(touchPoints));
        lastTouches = Arrays.asList(touches);
    }
}
