package com.pierdr.pierluigidallarosa.myactivity;

import com.pierdr.tramontana.model.Event;
import com.pierdr.tramontana.model.EventSink;
import com.pierdr.tramontana.ui.UserReporter;

import ketai.sensors.KetaiSensor;
import processing.core.PApplet;
import processing.event.TouchEvent;

public class Sketch extends PApplet {
    private int bgRed=255,bgGreen=255,bgBlue=255;

    //SENSORS
    private KetaiSensor sensor;

    private boolean sensingDistance;
    private boolean sensingAttitude;

    private final static int TOUCH_INACTIVE         = 0;
    private final static int TOUCH_LISTENING        = 1;
    private final static int TOUCH_LISTENING_MULTI  = 2;
    private final static int TOUCH_DRAG_LISTENING   = 3;

    private int touchState = TOUCH_INACTIVE;
    private TouchEvent.Pointer lastEvents[];

    private final EventSink eventSink;
    private final UserReporter userReporter;

    public Sketch(EventSink eventSink, UserReporter userReporter) {
        this.eventSink = eventSink;
        this.userReporter = userReporter;
    }

    public void settings() {
        fullScreen();
    }

    public void setup() {
        sensor = new KetaiSensor(this);
        sensor.enableProximity();
        sensor.enableRotationVector();
        sensor.start();
    }

    public void draw() {
        background(bgRed, bgGreen, bgBlue);
    }
    public void setColor(int red, int green, int blue)
    {
       bgRed    = red;
       bgGreen  = green;
       bgBlue   = blue;
    }

    public void startTouchListening(boolean multi, boolean drag)
    {
        if(multi && drag)
        {
            touchState = TOUCH_DRAG_LISTENING;
        }
        else if(multi)
        {
            touchState = TOUCH_LISTENING_MULTI;
        }
        else if(!drag)
        {
            touchState = TOUCH_LISTENING;
        }
    }
    public void stopTouchListening(){
        touchState = TOUCH_INACTIVE;
    }

    public void touchStarted() {
        lastEvents = touches;
       switch (touchState){
           case TOUCH_LISTENING:
               case TOUCH_DRAG_LISTENING:

                   if(touches.length>0) {
                       eventSink.onEvent(new Event.TouchDown((int) touches[0].x, (int) touches[0].y));
                   }

               break;
           case TOUCH_LISTENING_MULTI:
           //{\"m\":\"touched\",\"ts\":%@}
               break;

       }
    }

    public void touchMoved() {
        lastEvents = touches;
    }

    public void touchEnded() {
        switch (touchState){
            case TOUCH_LISTENING:
            case TOUCH_DRAG_LISTENING:

                if(lastEvents.length>0) {
                    eventSink.onEvent(new Event.Touched((int) lastEvents[0].x, (int) lastEvents[0].y));
                }

                break;
            case TOUCH_LISTENING_MULTI:
                break;
        }
    }

    //SENSORS
    public void startAttitudeSensing(Float frameRate) {
        if (!sensor.isAccelerometerAvailable()) {
            userReporter.showWarning("rotation vector not available");
            return;
        }

        sensor.setDelayInterval((int) (1000 / frameRate));
        sensingAttitude = true;
    }

    public void stopAttitudeSensing()
    {
        sensingAttitude = false;
    }

    @SuppressWarnings("unused") // called by KetaiSensor via reflection
    public void onRotationVectorEvent(float x, float y, float z, long a, int b) {
        if (!sensingAttitude) {
            return;
        }
        eventSink.onEvent(new Event.Attitude(x, y, z));
    }

    //SENSING DISTANCE
    public void startDistanceSensing() {
        if (!sensor.isProximityAvailable()) {
            userReporter.showWarning("distance sensor not available!");
            return;
        }

        sensingDistance = true;
    }
    public void stopDistanceSensing()
    {
        sensingDistance = false;
    }

    @SuppressWarnings("unused") // called by KetaiSensor via reflection
    public void onProximityEvent(float d, long a, int b){
        if (!sensingDistance) {
            return;
        }
        eventSink.onEvent(new Event.Distance(d));
    }
}
