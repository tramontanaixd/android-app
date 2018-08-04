package com.pierdr.pierluigidallarosa.myactivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import org.java_websocket.WebSocket;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


import processing.core.PApplet;

import ketai.sensors.*;
import processing.event.TouchEvent;
//import processing.core.PImage;
//import apwidgets.*;

public class Sketch extends PApplet {
    int bgRed=255,bgGreen=255,bgBlue=255;
    processing.core.PImage imageToDisplay;
//    APVideoView videoView;
//    APWidgetContainer videoContainer;

    /***
     STATES
     ***/
    public int state;

    static final int SHOW_IMAGE            = 1;
    static final int SHOW_BG               = 2;
    static final int PLAY_VIDEO            = 3;

    //SENSORS
    KetaiSensor sensor;

    //SOCKETS
    WebSocket attitudeSocket;
    WebSocket touchSocket;
    WebSocket distanceSocket;
    WebSocket magneticSocket;
    WebSocket orientationSocket;

    //HashMap<String,WebSocket> arraySockets;

    WebSocket arraySockets[] = {attitudeSocket,touchSocket,distanceSocket,magneticSocket,orientationSocket};

    final static int ATTITUDE       = 0;
    final static int TOUCH          = 1;
    final static int DISTANCE       = 2;
    final static int MAGNETIC       = 3;
    final static int ORIENTATION    = 4;

    final static int TOUCH_INACTIVE         = 0;
    final static int TOUCH_LISTENING        = 1;
    final static int TOUCH_LISTENING_MULTI  = 2;
    final static int TOUCH_DRAG_LISTENING   = 3;

    int touchState = TOUCH_INACTIVE;
    private TouchEvent.Pointer lastEvents[];

    public void settings() {
//        size(600, 600);
        fullScreen();

        //background(255);

    }

    public void setup() {
//        videoContainer = new APWidgetContainer(this);

        sensor = new KetaiSensor(this);
        //arraySockets = new HashMap <String,WebSocket>();
        // imageToDisplay = new processing.core.PImage(10,10,ARGB);

    }

    public void draw() {
        switch(state)
        {
            case SHOW_BG:
            {
                background(bgRed,bgGreen,bgBlue);
                break;
            }
            case SHOW_IMAGE:
            {
                background(bgRed,bgGreen,bgBlue);
                if(imageToDisplay!=null) {
                    image(imageToDisplay, 0, 0,width,height);
                }
                break;
            }
            case PLAY_VIDEO:
            {
                background(bgRed,bgGreen,bgBlue);

                break;
            }
        }

    }
    public void setColor(int red, int green, int blue)
    {
       bgRed    = red;
       bgGreen  = green;
       bgBlue   = blue;
       changeState(SHOW_BG);
    }

    public void showImage(String imageName) {
        if (isResourceLocal(imageName)) {
            //LOAD LOCAL FILE
            while (imageName.indexOf("/") != -1) {
                imageName = imageName.replace("/", "");
            }
            imageToDisplay = new processing.core.PImage(BitmapFactory.decodeFile(getContext().getFilesDir() + "/" + imageName));
        } else {
            //DOWNLOAD FROM THE INTERNET
            getBitmapFromURL(imageName);
        }
        changeState(SHOW_IMAGE);

    }
    public void playVideo(String mediaName)
    {

    }
    boolean isResourceLocal(String name)
    {
        while(name.indexOf("/")!=-1)
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
    public void startTouchListening(WebSocket touchSocket, boolean multi, boolean drag)
    {

        this.touchSocket = touchSocket;
        arraySockets[TOUCH] = touchSocket;

        if(multi && drag)
        {
            touchState = TOUCH_DRAG_LISTENING;
        }
        else if(multi && !drag)
        {
            touchState = TOUCH_LISTENING_MULTI;
        }
        else if(!multi && !drag)
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
                       touchSocket.send("{\"m\":\"touchedDown\",\"x\":\"" + touches[0].x + "\",\"y\":\"" + touches[0].y + "\"}");
                   }

               break;
           case TOUCH_LISTENING_MULTI:
           //{\"m\":\"touched\",\"ts\":%@}
               break;

       }
    }

    public void touchMoved() {
        lastEvents = touches;
        if(touchState==TOUCH_DRAG_LISTENING)
        {

        }
    }

    public void touchEnded() {
        switch (touchState){
            case TOUCH_LISTENING:
            case TOUCH_DRAG_LISTENING:

                if(lastEvents.length>0) {

                    touchSocket.send("{\"m\":\"touched\",\"x\":\"" + lastEvents[0].x + "\",\"y\":\"" + lastEvents[0].y + "\"}");
                }

                break;
            case TOUCH_LISTENING_MULTI:
                break;
        }
    }

    public void saveBitmapToFile(String filename,Bitmap bitmap)
    {
        FileOutputStream out = null;
        try {
            while(filename.indexOf("/")!=-1)
            {
                filename = filename.replace("/","");
            }
            System.out.println("saving image at:\n"+filename);
            out = new FileOutputStream(getContext().getFilesDir()+"/"+filename);

            bitmap.compress((filename.contains(".png"))?Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.JPEG, 80, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void getBitmapFromURL(String src) {

        class RetrieveImage extends AsyncTask<String, Void, Void> {

            private Exception exception;
            private Bitmap bitmapToLoad;
            private String src;
            protected Void doInBackground(String... urls) {
                try {

                    URL url = new URL(urls[0]);
                    src = urls[0];
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    bitmapToLoad = BitmapFactory.decodeStream(input);
                    imageToDisplay = new processing.core.PImage(bitmapToLoad);



                } catch (IOException e) {
                    // Log exception
                    System.out.println("error retrieving image:\n "+e);
                    return null;
                }
                return null;
            }
            protected void onPostExecute(Void feed) {

                saveBitmapToFile(src,bitmapToLoad);
            }
        }
        new RetrieveImage().execute(src);

    }
    void getMediaFromURL(final String mediaSrc)
    {
        class RetrieveMedia extends AsyncTask<String, Void, Void> {

            private Exception exception;
            private InputStream input;
            private String src;
            protected Void doInBackground(String... urls) {
                try {

                    URL url = new URL(urls[0]);
                    src = urls[0];
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    input = connection.getInputStream();

                    System.out.println("media loaded \n"+urls[0]);
                    //return myBitmap;
                } catch (IOException e) {
                    // Log exception
                    System.out.println("error retrieving media:\n "+e);
                    //return null;
                }
                return null;
            }

            protected void onPostExecute(Void feed) {
                //SAVE MEDIA TO FILE
                OutputStream out = null;
                String localMediaSrc = mediaSrc;
                System.out.println("starting to save media to disk");
                try {

                    while(localMediaSrc.indexOf("/")!=-1)
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
                        //startPlayingVideo(localMediaSrc);
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
    public void startAttitudeSensing(Float f, WebSocket attitudeSocket)
    {
        System.out.println("f:"+f+"and delay"+(int)(10000.0/f)+ "socket:\n"+attitudeSocket);
        this.attitudeSocket = attitudeSocket;
        arraySockets[ATTITUDE] = attitudeSocket;

        sensor.setDelayInterval((int)(1000/f));

        if(sensor.isAccelerometerAvailable()) {
            sensor.enableRotationVector();
            if(!sensor.isStarted())
            {
                sensor.start();
            }
        }
        else
        {
            System.out.println("rotation vector not available");
        }
    }
    public void stopAttitudeSensing()
    {
        sensor.disableRotationVector();
        if(sensor.isStarted())
        {
            if(!isStillSensing()) {
                sensor.stop();
            }
        }
    }
    public void onRotationVectorEvent(float x, float y, float z, long a, int b){
        if(attitudeSocket!=null) {
            attitudeSocket.send("{\"m\":\"a\",\"r\":\"" + x + "\",\"p\":\"" + y + "\",\"y\":\"" + z + "\"}");
        }
        else if(!isStillSensing())
        {
            System.out.println("stopping sensing from rotationVector");
            stopAttitudeSensing();
        }
    }

    //SENSING DISTANCE
    public void startDistanceSensing(WebSocket distanceSocket)
    {
        this.distanceSocket = distanceSocket;
        if(sensor.isProximityAvailable()) {
            System.out.println("distance available");
            sensor.enableProximity();
            if(!sensor.isStarted())
            {
                sensor.start();
            }
        }
        else
        {
            System.out.println("distance not available");
        }
    }
    public void stopDistanceSensing()
    {
        System.out.println("stopping distance sensing:"+ isStillSensing());
        for(int i=0;i<arraySockets.length;i++) {
            System.out.println(arraySockets[i]);
        }
        System.out.println(attitudeSocket);
        sensor.disableProximity();
        if(sensor.isStarted())
        {
            if(!isStillSensing()) {
                sensor.stop();
            }
        }
    }
    public void onProximityEvent(float d, long a, int b){
        if(distanceSocket!=null) {
            distanceSocket.send("{\"m\":\"distanceChanged\",\"proximity\":\""+d+"\"}");
        }
        else if (!isStillSensing()) {

                System.out.println("stopping sensing from proximity");
                stopDistanceSensing();

        }
    }

    ///GENERAL SENSING METHODS
    public boolean isStillSensing()
    {
        for(int i=0;i<arraySockets.length;i++) {
            if(arraySockets[i]!=null) {
                return true;
            }
        }
        return false;
    }
    public void lostConnection(WebSocket socket)
    {
        for(int i=0;i<arraySockets.length;i++) {
            if(arraySockets[i]!=null) {
                if (socket.equals(arraySockets[i])) {
                    arraySockets[i] = null;
                }
            }
        }
    }
//    public void onAccelerometerEvent(float x, float y, float z, long a, int b)
//    {
//        //System.out.println(x+","+y+","+z);
//        attitudeSocket.send("{\"m\":\"a\",\"r\":\""+x+"\",\"p\":\""+y+"\",\"y\":\""+z+"\"}");
//    }

}
