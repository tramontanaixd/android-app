package com.pierdr.pierluigidallarosa.myactivity;

import com.pierdr.pierluigidallarosa.*;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.os.Vibrator;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;


import ketai.camera.KetaiCamera;
import ketai.net.KetaiNet;
import processing.android.PFragment;
import processing.android.CompatUtils;
import processing.core.*;

//IMPORT WEBSOCKET

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;

import org.java_websocket.drafts.Draft_6455;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

//import static processing.core.PGraphics.R;


public class MainActivity extends AppCompatActivity implements WebsocketManager.websocketManagerListener {
    private Sketch sketch;
    private TextView ipAddressView;
    private String ipAddress;
    public WebsocketManager wsm;
    private VideoView videoView;

    public boolean isStarted=false;
    FrameLayout frame;

    //INPUTS
    private ArrayList<WebSocket> arrayAttitude;
    private ArrayList<WebSocket> arrayOrientation;
    private ArrayList<WebSocket> arrayTouch;

    private WebSocket attitudeSocket;
    private WebSocket orientationSocket;
    private WebSocket touchSocket;


    //OUTPUTS
    private CameraManager mCameraManager;
    public Vibrator vibrer;
    //CONSOLE
    private ConsoleManager cm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.starting_view);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ///CONSOLE
        cm = new ConsoleManager((TextView)findViewById(R.id.consoleLog));

        //START WEBSOCKETS
        WebSocketImpl.DEBUG = false;
        int port = 9092;
        try {
            wsm = new WebsocketManager(port, new Draft_6455());
            wsm.addAListener(this);

            wsm.setConnectionLostTimeout(0);
            wsm.start();

            cm.addNewMessage("server started");
        } catch (Exception e) {
            Toast toastTmp = Toast.makeText(getApplicationContext(),"Coudln't start the Websocket Server.",Toast.LENGTH_SHORT);
            toastTmp.show();
            cm.addNewMessage("Error in starting the websocket server.");
            System.out.println("Error in starting the websocket manager.");
        }

        //CHECK IP ADDRESS
        ipAddressView = (TextView) findViewById(R.id.ipAddressView);
        checkIpAddress();
        ipAddress = "--";

        //SETUP OUTPUTS
        vibrer = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //POLICY THREAD
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //INITIALIZE INPUTS
        arrayAttitude       = new ArrayList <WebSocket>();
        arrayOrientation    = new ArrayList <WebSocket>();
        arrayTouch          = new ArrayList <WebSocket>();

        System.out.printf("tramontana started1");

       // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    public void checkIpAddress()
    {

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // this code will be executed after 2 seconds
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ipAddressView.setText(KetaiNet.getIP());
                        ipAddress = KetaiNet.getIP();
                    }
                });
                checkIpAddress();
            }
        }, 2000);



    }
    public void onButtonPressed(View v)
    {
        Toast toastTmp = Toast.makeText(getApplicationContext(),"Restart Tramontana to see the info panel.",Toast.LENGTH_SHORT);
        toastTmp.show();
        startProcessingSketch();
    }
    public void startProcessingSketch()
    {
        if(!isStarted) {
            //START PROCESSING
            sketch = new Sketch();

            //SETUP VIEW FULLSCREEN
            frame = new FrameLayout(this);
            frame.setKeepScreenOn(true);
            frame.setId(CompatUtils.getUniqueViewId());
            setContentView(frame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            PFragment fragment = new PFragment(sketch);
            fragment.setView(frame, this);
            isStarted = true;
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        if (sketch != null) {
            sketch.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (sketch != null) {
            sketch.onNewIntent(intent);
        }
    }
    ///WEBSOCKET LISTENER
    public void onNewMessage(String message, WebSocket socket){
        System.out.println("received a message");
        System.out.println(message);
        class OneShotTask implements Runnable {
                        String message;
                        WebSocket socket;
            OneShotTask(String message, WebSocket socket) {
                this.message  = message;
                this.socket   = socket;
            }
            public void run() {
                processing.data.JSONObject json = processing.data.JSONObject.parse(message);

                String directive = json.getString("m");

                cm.addNewMessage("received msg: "+directive);

                if(directive.equals("makeVibrate"))
                {
                    onMakeVibrate();
                }
                else if(directive.equals("setColor"))
                {
                    int red=0,green=0,blue=0,alpha=0;
                    System.out.println(json.getString("r").contains("."));
                    if(json.getString("r").contains("."))
                    {
                        red      = (int)processing.core.PApplet.map(json.getFloat("r"),(float)0.0,(float)1.0,0,255);
                        green    = (int)processing.core.PApplet.map(json.getFloat("g"),(float)0.0,(float)1.0,0,255);
                        blue     = (int)processing.core.PApplet.map(json.getFloat("b"),(float)0.0,(float)1.0,0,255);
                        alpha    = (int)processing.core.PApplet.map(json.getFloat("a"),(float)0.0,(float)1.0,0,255);
                    }
                    else {
                        red     = json.getInt("r");
                        green   = json.getInt("g");
                        blue    = json.getInt("b");
                        alpha   = json.getInt("a");
                    }
                    onSetColor(red,green,blue,alpha);
                }
                else if(directive.equals("setBrightness")) {
                    onSetBrightness(json.getFloat("b"));
                }
                else if(directive.equals("setLED")) {
                    //startProcessingSketch();
                    onSetFlashLight(json.getFloat("in"));
                }
                else if(directive.equals("showImage"))
                {
                    onShowImage(json.getString("url"));
                }
                else if(directive.equals("playVideo"))
                {
                    onPlayVideo(json.getString("url"));
                }
                else if(directive.equals("playAudio"))
                {

                }
                else if(directive.equals("registerTouch") || directive.equals("registerTouchDrag"))
                {
                    boolean hasMultiTouch = false;
                    if(json.hasKey("multi"))
                    {
                        hasMultiTouch = (json.getInt("multi")==1)?true:false;
                    }
                    if(!isStarted)
                    {
                        startProcessingSketch();
                        class myRunTmp implements Runnable {

                            private boolean multi;
                            private boolean drag;

                            public myRunTmp(boolean multi, boolean drag) {
                                this.multi = multi;
                                this.drag = drag;
                            }

                            public void run() {
                                sketch.startTouchListening(socket,multi,drag );
                            }
                        }

                        Handler handler = new Handler();
                        handler.postDelayed(new myRunTmp(hasMultiTouch,directive.equals("registerTouchDrag")?true:false), 500);
                    }
                    else
                    {
                        sketch.startTouchListening(socket,hasMultiTouch,directive.equals("registerTouchDrag")?true:false );
                    }
                }

                else if(directive.equals("releaseTouch"))
                {
                    if(isStarted) {
                        sketch.stopTouchListening();
                    }
                }
                else if(directive.equals("registerDistance"))
                {
                    if(!isStarted)
                    {
                        startProcessingSketch();
                        class myRunTmp implements Runnable {

                            public void run() {
                                sketch.startDistanceSensing(socket);
                            }
                        }

                        Handler handler = new Handler();
                        handler.postDelayed(new myRunTmp(), 1000);

                    }
                    else
                    {
                        sketch.startDistanceSensing(socket);
                    }
                }
                else if(directive.equals("releaseDistance"))
                {
                    if(isStarted) {
                        sketch.stopDistanceSensing();
                    }
                }
                else if(directive.equals("registerAttitude"))
                {
//                    if(!socket.equals(attitudeSocket))
//                    {
//                        attitudeSocket = socket;
//                    }
                    if(!isStarted) {
                        startProcessingSketch();
                        class myRunTmp implements Runnable {

                            private float f;

                            public myRunTmp(float f) {
                                this.f = f;
                            }

                            public void run() {
                                sketch.startAttitudeSensing(f, socket);
                            }
                        }

                        Handler handler = new Handler();
                        handler.postDelayed(new myRunTmp(json.getFloat("f")), 1000);
                    }
                    else
                    {
                        sketch.startAttitudeSensing(json.getFloat("f"), socket);
                    }
                }
                else if(directive.equals("releaseAttitude"))
                {
                    //attitudeSocket = null;
                    if(isStarted) {
                        sketch.stopAttitudeSensing();
                    }
                }
            }
        }

        runOnUiThread(new OneShotTask(message,socket));


    }

    public void onMakeVibrate()
    {
        vibrer.vibrate(100);
    }
    public void onPlayAudio(String filename){

    }
    public void onSetFlashLight(float value){

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    String mCameraId = "";
                    try {
                        mCameraId = mCameraManager.getCameraIdList()[0];
                        mCameraManager.setTorchMode(mCameraId, (value>0)?true:false);
                        System.out.println("setting torchMode");
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                else
                {

                }
            } catch (Exception e) {
                System.out.println("flash not available");
            }

    }

    public void onShowImage(String filename){
        sketch.showImage(filename);
    }
    public void onPlayVideo(String filename){
        if(sketch.isResourceLocal(filename))
        {
            //LOAD LOCAL FILE
            while(filename.indexOf("/")!=-1)
            {
                filename = filename.replace("/","");
            }

            startPlayingVideo(filename);
        }
        else
        {
            sketch.getMediaFromURL(filename);
        }
        sketch.changeState(sketch.PLAY_VIDEO);
    }
    public void startPlayingVideo(String localVideoSrc)
    {
        try {
            videoView = (VideoView) new VideoView(this);
            frame.addView(videoView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
           // Uri video = Uri.parse(getFilesDir() + "/" + localVideoSrc);
            videoView.setVideoPath(getFilesDir() + "/" + localVideoSrc);
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(true);
                    videoView.start();
                }
            });
//            //MediaPlayer mp = new MediaPlayer();
//            MediaPlayer mp = MediaPlayer.create( this, Uri.parse(getFilesDir() + "/" + localVideoSrc));
//            // mp.setDataSource(getContext().getFilesDir() + "/" + localVideoSrc);
//            mp.prepare();
//            mp.start();
        }
        catch(Exception e)
        {
            System.out.println(e);
        }


        System.out.println("playing video: "+localVideoSrc);

    }
    public void onSetBrightness(float brightness){

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);
    }
    public void onSetColor(int red, int green, int blue, int alpha)
    {
        System.out.println("starting sketch");
        startProcessingSketch();

        sketch.setColor(red,green,blue);

        onSetBrightness(alpha/(float)255);
    }





    public void onNewConnection(String newDevice){
        cm.addNewMessage("connection open with "+newDevice);
    }
    public void onLostConnection(WebSocket socket){
        sketch.lostConnection(socket);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

// Checks the orientation of the screen for landscape and portrait
        Log.d("myTag", (newConfig.orientation+""));

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }
}

