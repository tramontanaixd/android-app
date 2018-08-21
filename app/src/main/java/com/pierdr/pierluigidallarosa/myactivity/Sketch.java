package com.pierdr.pierluigidallarosa.myactivity;

import android.os.AsyncTask;

import com.pierdr.tramontana.model.Event;
import com.pierdr.tramontana.model.EventSink;
import com.pierdr.tramontana.ui.UserReporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import ketai.sensors.KetaiSensor;
import processing.core.PApplet;
import processing.event.TouchEvent;

public class Sketch extends PApplet {
    private int bgRed=255,bgGreen=255,bgBlue=255;

    /***
     STATES
     ***/
    private int state;

    private static final int SHOW_IMAGE            = 1;
    private static final int SHOW_BG               = 2;
    static final int PLAY_VIDEO            = 3;

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
       changeState(SHOW_BG);
    }

    boolean isResourceLocal(String name)
    {
        while(name.contains("/"))
        {
            name = name.replace("/","");
        }
        File file = new File(getContext().getFilesDir()+"/"+name);
        System.out.println("Looking for resource:"+getContext().getFilesDir()+"/"+name);
        if(file.exists())
        {
            System.out.println("Local resource!");
            return true;
        }
        return false;
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

    void getMediaFromURL(final String mediaSrc)
    {
        class RetrieveMedia extends AsyncTask<String, Void, Void> {

            private InputStream input;
            protected Void doInBackground(String... urls) {
                try {

                    URL url = new URL(urls[0]);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    input = connection.getInputStream();

                    System.out.println("media loaded \n"+urls[0]);
                } catch (IOException e) {
                    // Log exception
                    System.out.println("error retrieving media:\n "+e);
                }
                return null;
            }

            protected void onPostExecute(Void feed) {
                //SAVE MEDIA TO FILE
                OutputStream out = null;
                String localMediaSrc = mediaSrc;
                System.out.println("starting to save media to disk");
                try {

                    while(localMediaSrc.contains("/"))
                    {
                        localMediaSrc = localMediaSrc.replace("/","");
                    }
                    System.out.println("saving media at:\n"+localMediaSrc);
                    out = new FileOutputStream(getContext().getFilesDir()+"/"+localMediaSrc);

                    byte[] buf = new byte[1024];
                    int len;
                    while((len=input.read(buf))>0){
                        out.write(buf,0,len);
                    }
                }
                catch (Exception e) {
                    System.out.println("error in saving media");
                    e.printStackTrace();
                }
                finally {
                    // Ensure that the InputStreams are closed even if there's an exception.
                    try {
                        if ( out != null ) {
                            out.close();
                        }
                        // If you want to close the "in" InputStream yourself then remove this
                        // from here but ensure that you close it yourself eventually.
                        input.close();

                        ((MainActivity)getActivity()).startPlayingVideo(localMediaSrc);
                    }
                    catch ( IOException e ) {
                        e.printStackTrace();
                    }
                }
            }
        }
        new RetrieveMedia().execute(mediaSrc);

    }
    void changeState(int newState)
    {
        if(newState!=state)
        {

            switch(state)
            {

                case SHOW_BG:
                {

                    break;
                }
                case SHOW_IMAGE:
                {

                    break;
                }
            }
            state = newState;
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
