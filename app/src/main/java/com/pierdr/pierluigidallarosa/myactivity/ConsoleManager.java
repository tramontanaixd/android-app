package com.pierdr.pierluigidallarosa.myactivity;

import android.widget.TextView;


public class ConsoleManager {
    public TextView c1;

    private String[] messages;
    ConsoleManager(TextView c1)
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
        c1.setText(String.format("-> %s\n->%s\n->%s", messages[0], messages[1], messages[2]));
    }


}
