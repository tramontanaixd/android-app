package com.pierdr.tramontana.ui;

import com.pierdr.tramontana.model.EventSink;
import com.pierdr.tramontana.ui.touch.MultiDragTouchHandler;
import com.pierdr.tramontana.ui.touch.MultiTouchHandler;
import com.pierdr.tramontana.ui.touch.NoTouchHandler;
import com.pierdr.tramontana.ui.touch.SingleDragTouchHandler;
import com.pierdr.tramontana.ui.touch.SingleTouchHandler;
import com.pierdr.tramontana.ui.touch.TouchHandler;

import processing.core.PApplet;
import processing.event.TouchEvent;

public class Sketch extends PApplet {
    private int bgRed = 255, bgGreen = 255, bgBlue = 255;

    private final EventSink eventSink;
    private TouchHandler touchHandler = new NoTouchHandler();

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
        if (!multi && !drag) {
            touchHandler = new SingleTouchHandler(eventSink);
        } else if (multi && drag) {
            touchHandler = new MultiDragTouchHandler(eventSink);
        } else if (multi) {
            touchHandler = new MultiTouchHandler(eventSink);
        } else {
            touchHandler = new SingleDragTouchHandler(eventSink);
        }
    }

    public void stopTouchListening() {
        touchHandler = new NoTouchHandler();
    }

    public void touchStarted() {
        touchHandler.onTouchStart(touches);
    }

    public void touchMoved() {
        touchHandler.onTouchMove(touches);
    }

    public void touchEnded() {
        touchHandler.onTouchEnd(touches);
    }
}
