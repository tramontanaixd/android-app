package com.pierdr.tramontana.ui;

import com.pierdr.tramontana.model.Event;
import com.pierdr.tramontana.model.EventSink;

import processing.core.PApplet;
import processing.event.TouchEvent;

public class Sketch extends PApplet {
    private int bgRed = 255, bgGreen = 255, bgBlue = 255;

    private final static int TOUCH_INACTIVE = 0;
    private final static int TOUCH_LISTENING = 1;
    private final static int TOUCH_LISTENING_MULTI = 2;
    private final static int TOUCH_DRAG_LISTENING = 3;

    private int touchState = TOUCH_INACTIVE;
    private TouchEvent.Pointer lastEvents[];

    private final EventSink eventSink;

    public Sketch(EventSink eventSink) {
        this.eventSink = eventSink;
    }

    public void settings() {
        fullScreen();
    }

    public void draw() {
        background(bgRed, bgGreen, bgBlue);
    }

    public void setColor(int red, int green, int blue) {
        bgRed = red;
        bgGreen = green;
        bgBlue = blue;
    }

    public void startTouchListening(boolean multi, boolean drag) {
        if (multi && drag) {
            touchState = TOUCH_DRAG_LISTENING;
        } else if (multi) {
            touchState = TOUCH_LISTENING_MULTI;
        } else if (!drag) {
            touchState = TOUCH_LISTENING;
        }
    }

    public void stopTouchListening() {
        touchState = TOUCH_INACTIVE;
    }

    public void touchStarted() {
        lastEvents = touches;
        switch (touchState) {
            case TOUCH_LISTENING:
            case TOUCH_DRAG_LISTENING:

                if (touches.length > 0) {
                    eventSink.onEvent(new Event.TouchDown((int) touches[0].x, (int) touches[0].y));
                }

                break;
            case TOUCH_LISTENING_MULTI:
                // TODO add multi-touch listening
                //{\"m\":\"touched\",\"ts\":%@}
                break;

        }
    }

    public void touchMoved() {
        lastEvents = touches;
    }

    public void touchEnded() {
        switch (touchState) {
            case TOUCH_LISTENING:
            case TOUCH_DRAG_LISTENING:

                if (lastEvents.length > 0) {
                    eventSink.onEvent(new Event.Touched((int) lastEvents[0].x, (int) lastEvents[0].y));
                }

                break;
            case TOUCH_LISTENING_MULTI:
                break;
        }
    }
}
