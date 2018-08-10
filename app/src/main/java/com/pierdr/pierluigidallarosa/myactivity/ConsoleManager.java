package com.pierdr.pierluigidallarosa.myactivity;

import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;


public class ConsoleManager {
    public TextView c1;

    public static final int NUM_CONSOLE_LINES = 3;

    public String[] messages;
    public ConsoleManager(TextView c1)
    {
        this.c1 = c1;

        messages = new String[3];
        for(int i = 0;i<3;i++)
        {
            messages[i]="";
        }
    }
    public void addNewMessage(String newMessage)
    {
        messages[2] = messages[1];
        messages[1] = messages[0];
        messages[0]=newMessage;
        c1.setText("-> "+messages[0]+"\n->"+messages[1]+"\n->"+messages[2]);
    }


}
