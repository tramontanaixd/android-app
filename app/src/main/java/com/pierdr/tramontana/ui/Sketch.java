package com.pierdr.tramontana.ui;

import com.pierdr.tramontana.model.EventSink;
import com.pierdr.tramontana.ui.touch.MultiDragTouchHandler;
import com.pierdr.tramontana.ui.touch.MultiTouchHandler;
import com.pierdr.tramontana.ui.touch.NoTouchHandler;
import com.pierdr.tramontana.ui.touch.SingleDragTouchHandler;
import com.pierdr.tramontana.ui.touch.SingleTouchHandler;
import com.pierdr.tramontana.ui.touch.TouchHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import processing.core.PApplet;

public class Sketch extends PApplet {
    private int backgroundColor = color(255);

    private final EventSink eventSink;
    private TouchHandler touchHandler = new NoTouchHandler();
    private Set<Runnable> preDrawTasks = new HashSet<>();

    public Sketch(EventSink eventSink) {
        this.eventSink = eventSink;
    }

    public void settings() {
        fullScreen();
    }

    public void draw() {
        for (Runnable preDrawTask : new ArrayList<>(preDrawTasks)) {
            preDrawTask.run();
        }

        background(backgroundColor);
    }

    public void setColor(int red, int green, int blue) {
        backgroundColor = color(red, green, blue);
    }

    public void transitionColors(int fromRed, int fromGreen, int fromBlue, int toRed, int toGreen, int toBlue, int duration) {
        clearPendingBackgroundTransitionTasks();
        preDrawTasks.add(new BackgroundTransitionTask(color(fromRed, fromGreen, fromBlue), color(toRed, toGreen, toBlue), duration));
    }

    private void clearPendingBackgroundTransitionTasks() {
        for (Iterator<Runnable> preDrawTaskIterator = preDrawTasks.iterator(); preDrawTaskIterator.hasNext(); ) {
            Runnable preDrawTask = preDrawTaskIterator.next();
            if (preDrawTask instanceof BackgroundTransitionTask) {
                preDrawTaskIterator.remove();
            }
        }
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

    private class BackgroundTransitionTask implements Runnable {
        private final long startTime;
        private final int fromColor;
        private final int toColor;
        private final int duration;

        BackgroundTransitionTask(int fromColor, int toColor, int duration) {
            this.fromColor = fromColor;
            this.toColor = toColor;
            this.duration = duration;
            startTime = millis();
        }

        @Override
        public void run() {
            float timeAlpha = map(millis(), startTime, startTime + duration, 0, 1);
            float colorAlpha = constrain(timeAlpha, 0, 1);
            backgroundColor = lerpColor(fromColor, toColor, colorAlpha);

            if (timeAlpha > 1) {
                preDrawTasks.remove(this);
            }
        }
    }
}
