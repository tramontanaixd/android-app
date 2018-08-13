package com.pierdr.pierluigidallarosa.myactivity;

import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
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

import java.util.Timer;
import java.util.TimerTask;

import ketai.net.KetaiNet;
import processing.android.CompatUtils;
import processing.android.PFragment;

public class MainActivity extends AppCompatActivity {
    // TODO run the sketch only while a WebSocket connection is active, and stop it when it disconnects
    private Sketch sketch;
    private TextView ipAddressView;
    private VideoView videoView;

    private boolean isStarted=false;
    private FrameLayout frame;

    private CameraManager mCameraManager;
    private ConsoleManager cm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.starting_view);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        cm = new ConsoleManager((TextView)findViewById(R.id.consoleLog));

        // TODO stop connections and server while the activity is paused
        (new Controller(this)).start();

        //CHECK IP ADDRESS
        ipAddressView = findViewById(R.id.ipAddressView);
        checkIpAddress();

        //POLICY THREAD
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        System.out.print("tramontana started1");

    }

    ConsoleManager getConsoleManager() {
        return cm;
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

    public void startTouchListening(final boolean multi, final boolean drag) {
        runWhenSketchIsReady(new Runnable() {
            @Override
            public void run() {
                // FIXME let the sketch obtain the current websocket
                sketch.startTouchListening(null, multi, drag);
            }
        });
    }

    public void stopTouchListening() {
        if (isStarted) {
            sketch.stopTouchListening();
        }
    }

    public void startDistanceSensing() {
        runWhenSketchIsReady(new Runnable() {
            @Override
            public void run() {
                // FIXME let the sketch obtain the current websocket
                sketch.startDistanceSensing(null);
            }
        });
    }

    public void stopDistanceSensing() {
        if (isStarted) {
            sketch.stopDistanceSensing();
        }
    }

    public void startAttitudeSensing(final float updateRate) {
        runWhenSketchIsReady(new Runnable() {
            @Override
            public void run() {
                // FIXME let the sketch obtain the current websocket
                sketch.startAttitudeSensing(updateRate, null);
            }
        });
    }

    public void stopAttitudeSensing() {
        if(isStarted) {
            sketch.stopAttitudeSensing();
        }
    }

    private void runWhenSketchIsReady(Runnable task) {
        if (isStarted) {
            task.run();
            return;
        }

        startProcessingSketch();

        Handler handler = new Handler();
        // TODO wait for some signal from the sketch instead of waiting
        handler.postDelayed(task, 500);
    }

    public void setFlashLight(float value){

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

    public void showImage(String filename){
        sketch.showImage(filename);
    }
    public void playVideo(String filename){
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
            // TODO try videoView.setClickable(false) to let the underlying sketch receive touch events.
        }
        catch(Exception e)
        {
            e.printStackTrace(System.out);
        }


        System.out.println("playing video: "+localVideoSrc);

    }
    public void setBrightness(float brightness){

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);
    }

    public void onSetColor(int red, int green, int blue, int alpha)
    {
        System.out.println("starting sketch");
        startProcessingSketch();

        sketch.setColor(red,green,blue);

        setBrightness(alpha/(float)255);
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

