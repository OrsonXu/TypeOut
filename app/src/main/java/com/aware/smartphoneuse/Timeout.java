package com.aware.smartphoneuse;
import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.content.Intent;
import android.graphics.PixelFormat;


import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.view.MotionEvent;
import android.view.View;
import android.view.Gravity;
import android.graphics.Typeface;

import android.graphics.drawable.Drawable;
import android.content.pm.PackageManager;
import android.widget.ImageView;

import android.annotation.SuppressLint;
import android.widget.Toast;


import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static androidx.core.app.ActivityCompat.startActivityForResult;

public class Timeout extends Service {
    private String TAG = Constants.TAG;
    private Intent intervIntent = null;
    LinearLayout oView;
    private String curAppName;
    private String curPackageName;
    private String timeSpentTodayStr;
    private String currentPhase;
    private int popUpInterval = 300000; //Minimum Interval Between two pop-up, 300000ms = 5min
    private int exitInterval = 120000; //Minimum Interval for pop up after user exit the app, 120000ms = 2min
    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        String intentAction = null;
        if (intent != null)
            intentAction = intent.getAction();
        if (intentAction == null)
            return START_STICKY;
        Log.d(TAG, "Timeout Service started");
        switch (intentAction) {
            case Constants.ACTION_START_APPLICATION:
                intervIntent = intent;
                triggerOverlay("start");
                break;
            case Constants.ACTION_START_INTERVENTION:
                intervIntent = intent;
                triggerOverlay("intervention");
                break;
            case Constants.ACTION_END_INTERVENTION:
                Log.d(TAG, "Timeout End RECEIVED");
                endTimeout();
                break;
            default:
                return START_STICKY;
        }
        return START_STICKY;
    }
    private void triggerOverlay(String type){
        // create overlay if permissions granted
        Log.d(TAG, "triggerOverlay: overlay type = "+type);
        boolean overlayPermitted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            overlayPermitted = false;
            if (android.provider.Settings.canDrawOverlays(this)) {
                overlayPermitted = true;
            }
        }
        if (overlayPermitted){
            // get info from intent
            curAppName = intervIntent.getStringExtra("application_name");
            curPackageName = intervIntent.getStringExtra("package_name");
            timeSpentTodayStr = intervIntent.getStringExtra("time_today_to_timeout");
            currentPhase = intervIntent.getStringExtra("phase");
            if(Constants.lastInputRight.get(curPackageName) == null){
                createOverlay(type);
                /*Toast.makeText(getApplicationContext(),"PACKAGE " + curPackageName + " Have " + Constants.lastInputRight.get(curPackageName) +
                        " NULL LAST RIGHT " ,Toast.LENGTH_LONG).show();*/
            }else if(!Constants.lastInputRight.get(curPackageName)) {
                createOverlay(type);
                /*Toast.makeText(getApplicationContext(),"PACKAGE " + curPackageName + " Have " + Constants.lastInputRight.get(curPackageName) +
                        " FALSE LAST RIGHT " ,Toast.LENGTH_LONG).show();*/
            }else if(Constants.lastInputTime.get(curPackageName) == null){
                createOverlay(type);
                /*Toast.makeText(getApplicationContext(),"PACKAGE " + curPackageName + " Have " + Constants.lastInputRight.get(curPackageName) +
                        " NULL LAST TIME " ,Toast.LENGTH_LONG).show();*/
            }else if ((System.currentTimeMillis() - Constants.lastInputTime.get(curPackageName)) > popUpInterval) {
                if(Constants.lastUsingTime.get(curPackageName) == null){
                    createOverlay(type);
                }else if((System.currentTimeMillis() - Constants.lastUsingTime.get(curPackageName)) > exitInterval){
                    createOverlay(type);
                }
                Log.d(TAG,"PACKAGE " + curPackageName + " Have " + Constants.lastUsingTime.get(curPackageName) +
                        " TIMEINTERVE " + (System.currentTimeMillis() - Constants.lastUsingTime.get(curPackageName) + "LAST USING TIME" +
                        Constants.lastUsingTime.get(curPackageName)));
                /*Toast.makeText(getApplicationContext(),"PACKAGE " + curPackageName + " Have " + Constants.lastInputRight.get(curPackageName) +
                        " TIMEINTERVE " + (System.currentTimeMillis() - Constants.lastInputTime.get(curPackageName) + "LAST TIME" +
                        Constants.lastInputTime.get(curPackageName)) ,Toast.LENGTH_LONG).show();*/
                //}
            }
        }
        else{
            Log.d(TAG, "No permission for overlay");
            sendInterventionFailureData();
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    private void createOverlay(String type){
        Log.i(TAG, "Timeout: will create overlay");

        Intent overlayIntent = new Intent(getApplicationContext(), OverlayActivity.class);
        overlayIntent.putExtra("OverlayType",type);
        Calendar c = Calendar.getInstance();
        boolean inTime = false;
        if(Constants.WorkSleepTime[0] == 0 &&Constants.WorkSleepTime[1] == 0 &&Constants.WorkSleepTime[2] == 0 &&Constants.WorkSleepTime[3] == 0){
            Log.d(TAG, "No time recording, then always have intervention");
            inTime = true;
        }else if(c.get(Calendar.DAY_OF_WEEK) > 1 && c.get(Calendar.DAY_OF_WEEK) < 7){
            Log.d(TAG, "In Weekday");
            if(c.get(Calendar.HOUR_OF_DAY) >= Constants.WorkSleepTime[0] &&
                    c.get(Calendar.HOUR_OF_DAY) <= Constants.WorkSleepTime[2]){
                Log.d(TAG, "In hours of Weekday Work Time");
                if(c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[0]){
                    Log.d(TAG, "In same hour with the start hour");
                    if(c.get(Calendar.MINUTE) > Constants.WorkSleepTime[1]){
                        Log.d(TAG, "The minute is later than the start time, then in the intervention period");
                        inTime = true;
                    }
                }else if(c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[2]){
                    if(c.get(Calendar.MINUTE) < Constants.WorkSleepTime[3]){
                        Log.d(TAG, "The minute is earlier than the end time, then in the intervention period");
                        inTime = true;
                    }
                }else {
                    Log.d(TAG," Normal case, not at the hour of start or end");
                    inTime = true;
                }

                if(c.get(Calendar.HOUR_OF_DAY) >= Constants.WorkSleepTime[16] &&
                        c.get(Calendar.HOUR_OF_DAY) <= Constants.WorkSleepTime[18]){
                    //Noon break check
                    Log.d(TAG, "In Noon Break of Weekday Work Time");

                    if (Constants.WorkSleepTime[16] == Constants.WorkSleepTime[18]){ // if the start and the end have the same hour
                        if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[16]){
                            Log.d(TAG, "In same hour with the noon break start & end hour");
                            if (c.get(Calendar.MINUTE) > Constants.WorkSleepTime[17] && c.get(Calendar.MINUTE) < Constants.WorkSleepTime[19]) {
                                Log.d(TAG, "The minute is later than the start time and earlier than the end time, then not in the intervention period");
                                inTime = false;
                            }
                        }
                    } else { // if the start and the end have different hours
                        if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[16]) {
                            Log.d(TAG, "In same hour with the noon break start hour");
                            if (c.get(Calendar.MINUTE) > Constants.WorkSleepTime[17]) {
                                Log.d(TAG, "The minute is later than the start time, then not in the intervention period");
                                inTime = false;
                            }
                        } else if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[18]) {
                            if (c.get(Calendar.MINUTE) < Constants.WorkSleepTime[19]) {
                                Log.d(TAG, "The minute is earlier than the end time, then not in the intervention period");
                                inTime = false;
                            }
                        } else {
                            Log.d(TAG, " Normal case, not at the hour of start or end for noon break");
                            inTime = false;
                        }
                    }
                }
            }else if(Constants.WorkSleepTime[4] < Constants.WorkSleepTime[6]){
                Log.d(TAG, "The start hour of sleep time is earlier than the end time, we need to pick the interval between them");
                if(c.get(Calendar.HOUR_OF_DAY) >= Constants.WorkSleepTime[4] &&
                        c.get(Calendar.HOUR_OF_DAY) <= Constants.WorkSleepTime[6]) {
                    Log.d(TAG, "In hours of Weekday Sleep Time");
                    if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[4]) {
                        Log.d(TAG, "In same hour with the start hour");
                        if (c.get(Calendar.MINUTE) > Constants.WorkSleepTime[5]) {
                            Log.d(TAG, "The minute is later than the start time, then in the intervention period");
                            inTime = true;
                        }
                    } else if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[6]) {
                        if (c.get(Calendar.MINUTE) < Constants.WorkSleepTime[7]) {
                            Log.d(TAG, "The minute is earlier than the end time, then in the intervention period");
                            inTime = true;
                        }
                    } else {
                        Log.d(TAG, "Normal case, not at the hour of start or end");
                        inTime = true;
                    }
                }
            }else if(Constants.WorkSleepTime[4] > Constants.WorkSleepTime[6]){
                Log.d(TAG, "The start hour of sleep time is later than the end time, we need to pick from start hour to 24 O'Clock, and from 24 O'Clock to end hour");
                if(c.get(Calendar.HOUR_OF_DAY) >= Constants.WorkSleepTime[4]){
                    Log.d(TAG, "If current hour is later than the start hour");
                    if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[4]) {
                        Log.d(TAG, "In same hour with the start hour");
                        if (c.get(Calendar.MINUTE) > Constants.WorkSleepTime[5]) {
                            Log.d(TAG, "The minute is later than the start hour, then in the intervention period");
                            inTime = true;
                        }
                    }else {
                        Log.d(TAG, "Normal case");
                        inTime = true;
                    }
                }else if(c.get(Calendar.HOUR_OF_DAY) <= Constants.WorkSleepTime[6]){
                    Log.d(TAG, "If current hour is earlier than the end hour");
                    if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[6]) {
                        Log.d(TAG, "In same hour with the end hour");
                        if (c.get(Calendar.MINUTE) < Constants.WorkSleepTime[5]) {
                            Log.d(TAG, "The minute is earlier than the end hour, then in the intervention period");
                            inTime = true;
                        }
                    }else {
                        Log.d(TAG, "Normal case");
                        inTime = true;
                    }
                }
            }else if(Constants.WorkSleepTime[4] == Constants.WorkSleepTime[6]){
                Log.d(TAG, "The start hour of sleep time is equal to the end time, we need to pick interval");
                if(c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[4]){
                    Log.d(TAG, "If current hour is equal to the start hour and end hour(they are same)");
                    if (c.get(Calendar.MINUTE) > Constants.WorkSleepTime[5] && c.get(Calendar.MINUTE) < Constants.WorkSleepTime[7]) {
                        Log.d(TAG, "The minute is later than the start minute and earlier than the end minute, then in the intervention period");
                        inTime = true;
                    }
                }
            }
        }else if(c.get(Calendar.DAY_OF_WEEK) == 1 || c.get(Calendar.DAY_OF_WEEK) == 7) {
            Log.d(TAG, "In Weekend");
            if(c.get(Calendar.HOUR_OF_DAY) >= Constants.WorkSleepTime[8] &&
                    c.get(Calendar.HOUR_OF_DAY) <= Constants.WorkSleepTime[10]){
                Log.d(TAG, "In hours of Weekend Work Time");
                if(c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[8]){
                    Log.d(TAG, "In same hour with the start hour");
                    if(c.get(Calendar.MINUTE) > Constants.WorkSleepTime[9]){
                        Log.d(TAG, "The minute is later than the start time, then in the intervention period");
                        inTime = true;
                    }
                }else if(c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[10]){
                    if(c.get(Calendar.MINUTE) < Constants.WorkSleepTime[11]){
                        Log.d(TAG, "The minute is earlier than the end time, then in the intervention period");
                        inTime = true;
                    }
                }else {
                    Log.d(TAG, "Normal case, not at the hour of start or end");
                    inTime = true;
                }

                if(c.get(Calendar.HOUR_OF_DAY) >= Constants.WorkSleepTime[16] &&
                        c.get(Calendar.HOUR_OF_DAY) <= Constants.WorkSleepTime[18]){
                    Log.d(TAG, "In Noon Break of Weekday Work Time");
                    if (Constants.WorkSleepTime[16] == Constants.WorkSleepTime[18]){ // if the start and the end have the same hour
                        if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[16]){
                            Log.d(TAG, "In same hour with the noon break start & end hour");
                            if (c.get(Calendar.MINUTE) > Constants.WorkSleepTime[17] && c.get(Calendar.MINUTE) < Constants.WorkSleepTime[19]) {
                                Log.d(TAG, "The minute is later than the start time and earlier than the end time, then not in the intervention period");
                                inTime = false;
                            }
                        }
                    } else { // if the start and the end have different hours
                        if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[16]) {
                            Log.d(TAG, "In same hour with the noon break start hour");
                            if (c.get(Calendar.MINUTE) > Constants.WorkSleepTime[17]) {
                                Log.d(TAG, "The minute is later than the start time, then not in the intervention period");
                                inTime = false;
                            }
                        } else if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[18]) {
                            if (c.get(Calendar.MINUTE) < Constants.WorkSleepTime[19]) {
                                Log.d(TAG, "The minute is earlier than the end time, then not in the intervention period");
                                inTime = false;
                            }
                        } else {
                            Log.d(TAG, " Normal case, not at the hour of start or end for noon break");
                            inTime = false;
                        }
                    }
                }
            }else if(Constants.WorkSleepTime[12] < Constants.WorkSleepTime[14]){
                Log.d(TAG, "The start hour of sleep time is earlier than the end time, we need to pick the interval between them");
                if(c.get(Calendar.HOUR_OF_DAY) >= Constants.WorkSleepTime[12] &&
                        c.get(Calendar.HOUR_OF_DAY) <= Constants.WorkSleepTime[14]) {
                    Log.d(TAG, "In hours of Weekday Sleep Time");
                    if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[12]) {
                        Log.d(TAG, "In same hour with the start hour");
                        if (c.get(Calendar.MINUTE) > Constants.WorkSleepTime[13]) {
                            Log.d(TAG, "The minute is later than the start time, then in the intervention period");
                            inTime = true;
                        }
                    } else if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[14]) {
                        if (c.get(Calendar.MINUTE) < Constants.WorkSleepTime[15]) {
                            Log.d(TAG, "The minute is earlier than the end time, then in the intervention period");
                            inTime = true;
                        }
                    } else {
                        Log.d(TAG, "Normal case, not at the hour of start or end");
                        inTime = true;
                    }
                }
            }else if(Constants.WorkSleepTime[12] > Constants.WorkSleepTime[14]){
                Log.d(TAG, "The start hour of sleep time is later than the end time, we need to pick from start hour to 24 O'Clock, and from 24 O'Clock to end hour");
                if(c.get(Calendar.HOUR_OF_DAY) >= Constants.WorkSleepTime[12]){
                    Log.d(TAG, "If current hour is later than the start hour");
                    if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[12]) {
                        Log.d(TAG, "In same hour with the start hour");
                        if (c.get(Calendar.MINUTE) > Constants.WorkSleepTime[13]) {
                            Log.d(TAG, "The minute is later than the start hour, then in the intervention period");
                            inTime = true;
                        }
                    }else {
                        Log.d(TAG, "Normal case");
                        inTime = true;
                    }
                }else if(c.get(Calendar.HOUR_OF_DAY) <= Constants.WorkSleepTime[14]){
                    Log.d(TAG, "If current hour is earlier than the end hour");
                    if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[14]) {
                        Log.d(TAG, "In same hour with the end hour");
                        if (c.get(Calendar.MINUTE) < Constants.WorkSleepTime[15]) {
                            Log.d(TAG, "The minute is earlier than the end hour, then in the intervention period");
                            inTime = true;
                        }
                    }else {
                        Log.d(TAG, "Normal case");
                        inTime = true;
                    }
                }
            }else if(Constants.WorkSleepTime[12] == Constants.WorkSleepTime[14]){
                Log.d(TAG, "The start hour of sleep time is equal to the end time, we need to pick interval");
                if(c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[14]){
                    Log.d(TAG, "If current hour is equal to the start hour and end hour(they are same)");
                    if (c.get(Calendar.MINUTE) > Constants.WorkSleepTime[13] && c.get(Calendar.MINUTE) < Constants.WorkSleepTime[15]) {
                        Log.d(TAG, "The minute is later than the start minute and earlier than the end minute, then in the intervention period");
                        inTime = true;
                    }
                }
            }
        }else {
            inTime = false;
        }
        if(inTime) {
            if(currentPhase.equals(Constants.phases.get(0))) {
                Log.d(TAG, "createOverlay in Timeout.java: phase no_intervention");
                return;
            }
            else if(currentPhase.equals(Constants.phases.get(1)))
                overlayIntent.putExtra("TextOrAudio", "pop_up");
            else if(currentPhase.equals(Constants.phases.get(2)) || currentPhase.equals(Constants.phases.get(3)))
                overlayIntent.putExtra("TextOrAudio", "text");
            else if(currentPhase.equals(Constants.phases.get(4)))
                overlayIntent.putExtra("TextOrAudio", "audio");
            else {
                Toast.makeText(getApplicationContext(),"error with the phase, no overlay",Toast.LENGTH_LONG).show();
                return;
            }
            overlayIntent.putExtra("application_name", curAppName);
            overlayIntent.putExtra("package_name", curPackageName);
            overlayIntent.putExtra("time_today_to_timeout", timeSpentTodayStr);
            overlayIntent.putExtras(intervIntent);

            overlayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(overlayIntent);
            Log.i(TAG, "Timeout Service start overlay activity for result");
        }else {
            Constants.enableWaitingPopUp[0] = true;
            return;
        }
    }


    private void sendInterventionFailureData(){
        Log.d(TAG, "Ready to send intervention FAILED data");
        Intent intentToLog = new Intent(Constants.ACTION_LOG_INTERVENTION);
        intentToLog.putExtras(intervIntent);
        intentToLog.putExtra("dismiss_timestamp", "");
        intentToLog.putExtra("overlay_permitted", false);
        sendBroadcast(intentToLog);
        Log.d(TAG, "Sent failure broadcast");
        intervIntent = null;
    }

    private void sendInterventionData(String result){
        if (oView!=null && intervIntent!=null) {
            Calendar c = Calendar.getInstance();
            long now = c.getTimeInMillis();
            Log.d(TAG, "Ready to send intervention data");
            Intent intentToLog = new Intent(Constants.ACTION_LOG_INTERVENTION);
            intentToLog.putExtras(intervIntent);
            intentToLog.putExtra("interventionResult",result);
            intentToLog.putExtra("dismiss_timestamp", Long.toString(now));
            intentToLog.putExtra("overlay_permitted", true);
            sendBroadcast(intentToLog);
            Log.d(TAG, "Sent broadcast");
            intervIntent = null;
        }
    }

    private void returnHome(){
        Log.d(TAG, "Dismiss button clicked");
        sendInterventionData("returnHome");
        removeOverlay();

        Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
        mHomeIntent.addCategory(Intent.CATEGORY_HOME);
        mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(mHomeIntent);
    }

    private void endTimeout(){
        Log.d(TAG, "End timeout when app is switched");
        sendInterventionData("app switched so endTimeout");
        removeOverlay();
    }
    private void removeOverlay(){
        if(oView!=null){
            Log.d(TAG, "Timeout should be removed");
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            try {
                wm.removeView(oView);
            }
            catch (java.lang.IllegalArgumentException e){
                Log.e(TAG, "IllegalArgumentException when calling removeView");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendInterventionData("destroy");
        removeOverlay();
    }
}