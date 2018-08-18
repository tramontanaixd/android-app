package com.pierdr.pierluigidallarosa.myactivity;

import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {
    private Sketch sketch;
    private TextView ipAddressView;
    private VideoView videoView;

    private boolean isStarted=false;
    private FrameLayout frame;

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

