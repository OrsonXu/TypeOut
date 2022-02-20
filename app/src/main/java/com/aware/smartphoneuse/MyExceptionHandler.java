package com.aware.smartphoneuse;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Process;

import com.aware.Applications;
import com.crashlytics.android.Crashlytics;

import android.app.IntentService;

public class MyExceptionHandler implements Thread.UncaughtExceptionHandler {
    private String TAG = Constants.TAG;

    private Activity activity;
    public MyExceptionHandler(Activity a) {
        activity = a;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Exception except = new Exception(ex);
        try {
            Crashlytics.logException(except);
        } catch (Exception e2) {
            Log.d(TAG, "uncaughtException: Crashlytics not initialized, cannot send logs.");
        }

        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra("crash", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(activity.getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager mgr = (AlarmManager) activity.getBaseContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);
        //activity.finish();
        //System.exit(2);
        Process.killProcess(Process.myPid());
        System.exit(0);
    }
}
