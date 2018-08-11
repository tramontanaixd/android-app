package com.pierdr.pierluigidallarosa.myactivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.drafts.Draft_6455;

import java.util.Timer;
import java.util.TimerTask;

import ketai.net.KetaiNet;
import processing.android.CompatUtils;
import processing.android.PFragment;

//IMPORT WEBSOCKET


public class MainActivity extends AppCompatActivity implements WebsocketManager.WebsocketManagerListener {
    private Sketch sketch;
    private TextView ipAddressView;
    private VideoView videoView;

    private boolean isStarted=false;
    private FrameLayout frame;

    //OUTPUTS
    private CameraManager mCameraManager;
    private Vibrator vibrer;
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
            WebsocketManager wsm = new WebsocketManager(port, new Draft_6455());
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
        ipAddressView = findViewById(R.id.ipAddressView);
        checkIpAddress();

        //SETUP OUTPUTS
        vibrer = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //POLICY THREAD
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //INITIALIZE INPUTS
        System.out.print("tramontana started1");

    }

    private void checkIpAddress()
    {

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // this code will be executed after 2 seconds
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ipAddressView.setText(KetaiNet.getIP());
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
    private void startProcessingSketch()
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
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
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
                        final String message;
                        final private WebSocket socket;
            private OneShotTask(String message, WebSocket socket) {
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
                    int red,green,blue,alpha;
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
                else if(directive.equals("registerTouch") || directive.equals("registerTouchDrag"))
                {
                    boolean hasMultiTouch = false;
                    if(json.hasKey("multi"))
                    {
                        hasMultiTouch = json.getInt("multi") == 1;
                    }
                    if(!isStarted)
                    {
                        startProcessingSketch();
                        class myRunTmp implements Runnable {

                            private boolean multi;
                            private boolean drag;

                            private myRunTmp(boolean multi, boolean drag) {
                                this.multi = multi;
                                this.drag = drag;
                            }

                            public void run() {
                                sketch.startTouchListening(socket,multi,drag );
                            }
                        }

                        Handler handler = new Handler();
                        handler.postDelayed(new myRunTmp(hasMultiTouch, directive.equals("registerTouchDrag")), 500);
                    }
                    else
                    {
                        sketch.startTouchListening(socket,hasMultiTouch, directive.equals("registerTouchDrag"));
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
                    if(!isStarted) {
                        startProcessingSketch();
                        class myRunTmp implements Runnable {

                            private float f;

                            private myRunTmp(float f) {
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

    private void onMakeVibrate()
    {
        vibrer.vibrate(100);
    }
    private void onSetFlashLight(float value){

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    String mCameraId;
                    try {
                        mCameraId = mCameraManager.getCameraIdList()[0];
                        mCameraManager.setTorchMode(mCameraId, value > 0);
                        System.out.println("setting torchMode");
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.out.println("flash not available");
            }

    }

    private void onShowImage(String filename){
        sketch.showImage(filename);
    }
    private void onPlayVideo(String filename){
        if(sketch.isResourceLocal(filename))
        {
            //LOAD LOCAL FILE
            while(filename.contains("/"))
            {
                filename = filename.replace("/","");
            }

            startPlayingVideo(filename);
        }
        else
        {
            sketch.getMediaFromURL(filename);
        }
        sketch.changeState(Sketch.PLAY_VIDEO);
    }
    public void startPlayingVideo(String localVideoSrc)
    {
        try {
            videoView = new VideoView(this);
            frame.addView(videoView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            videoView.setVideoPath(getFilesDir() + "/" + localVideoSrc);
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(true);
                    videoView.start();
                }
            });
        }
        catch(Exception e)
        {
            e.printStackTrace(System.out);
        }


        System.out.println("playing video: "+localVideoSrc);

    }
    private void onSetBrightness(float brightness){

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);
    }
    private void onSetColor(int red, int green, int blue, int alpha)
    {
        System.out.println("starting sketch");
        startProcessingSketch();

        sketch.setColor(red,green,blue);

        onSetBrightness(alpha/(float)255);
    }





    public void onNewConnection(String newDevice){
        cm.addNewMessage("connection open with "+newDevice);
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

