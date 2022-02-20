package com.aware.smartphoneuse;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;


import android.util.Log;


public class Autostart extends BroadcastReceiver {
    private String TAG = Constants.TAG;
    public void onReceive(Context context, Intent arg1)
    {
//        Intent myintent = new Intent(context, MainActivity.class);
        Intent myintent = new Intent(context, Plugin.class);
        myintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(myintent);

        Log.i(TAG, "Plugin Autostarted!");
    }

}