package com.aware.smartphoneuse;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityEventCompat;
import androidx.core.view.accessibility.AccessibilityManagerCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import com.aware.Applications;
import com.aware.Screen;
import com.aware.Aware;
import com.aware.Aware_Preferences;


import android.content.ContentResolver;
import android.os.Bundle;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.service.notification.StatusBarNotification;
import androidx.core.app.NotificationCompat;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import android.database.Cursor;
import android.database.DatabaseUtils;

import java.util.Queue;
import java.util.LinkedList;

import java.util.Random;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.pm.ApplicationInfo;
import android.widget.Toast;


public class Plugin extends Service {
    private String TAG = Constants.TAG;

    public static Plugin Plugin_instance;

    private String currentPhase = "NO_INTERVENTION"; // BASELINE OR RANDOMIZATION
    private long currentPhaseStartTime;
    private List<String> whitelistedApps = new ArrayList<String>();

    private int popUpPeroid = 880000;
    private int popUpLimit = 900000;
    private int popUpInterval = 300000; //Minimum Interval Between two pop-up, 300000ms = 5min
    private int SNSInterval = 10000; //Time for user to escape from Wechat SNS page, 10000ms = 10s
    private int currAppDurationPeriod = 10000; // repeat every 10 sec.
    Handler currAppDurationHandler = new Handler();
    Handler complianceChecks = new Handler();
    private Long lastComplianceCheck = null;
    private int complianceCheckTimer = 60*60*1000;
    Long complianceLastAppReceived = null;

    // Updated by runnable
    private boolean isAppUsageEnabled = false;
    private boolean prevIsAppUsageEnabled = false;
    private Long currAppDuration = null;

    // To exclude apps from calculations and from interventions
    List<String> excTimeoutApps = PluginConstants.getExcTimeoutApps();
    List<String> excCalcApps = PluginConstants.getExcCalcApps();
    // To update every time a new app is launched
    private ContentValues currAppData = new ContentValues(); // must be stored onForegroundChange, post calculations
    public static ComponentName currentComponentName = new ComponentName("blank","blank");
    private String launcherPackageName = null;
    private String editorPackageName = null;
    private String securePackageName = "com.miui.securitycenter"; //The secure package for MIUI
    private boolean currAppIsLauncher = false;
    private long currAppPrevTimeSpentToday = 0;

    private boolean LastOneIsEditorPackage = false;

    // Screen status variables
    private boolean screenOff = false;
    private boolean screenLock = false;

    // Timeout variables - we will keep post-debugging
    private boolean timeoutOn = false;
    List <ContentValues> timeoutsSoFar = new ArrayList<ContentValues>();
    Queue <Long> timesOfLastFewTimeOuts = new LinkedList<>();
    Long lastTimeoutForCurrApp = null;
    Long lastTimeoutDismissForCurrApp = null;

    // Timeout variables - mostly for debugging now
    private int timeoutRandInitThres = 0; // every 3 calls to run() using runnable
    private int timeoutRandRegThres = 3;
    private int timeoutRandInitSel = 3; // choose between 1 to timeoutRandInitThres calls for first timeout
    private int timeoutRunCount = 0;
    private boolean timeoutRandInitDone = false;
//    private boolean timeoutDoneForCurrApp = false;

    // for randomization with app summary
    ContentValues currAppSummary = new ContentValues();
    boolean useCurrAppSummary = false;
    List<Boolean> randCurrAppSummarySegs = new ArrayList<Boolean>();
    private int timeoutRunSeg1 = 0;
    private int timeoutRunSeg2 = 0;
    private int timeoutRunSeg3 = 0;
    private int timeoutRunSeg4 = 0;
    private int timeoutRunSeg5 = 0;
    boolean timeRight = false;
    private boolean firstEqual = true; // Judge whether the is first time for equal next and last package name

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // never used
    private boolean doesNotificationExist(int notification_id){
        boolean exists = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            StatusBarNotification[] notifications = mNotificationManager.getActiveNotifications();
            for (StatusBarNotification notification : notifications) {
                if (notification.getId() == notification_id) {
                    exists = true;
                    break;
                }
            }
        }
        return exists;
    }

    private void notifyPluginStart(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = Constants.SMARTPHONE_USE_PLUGIN_NOTIFICATION_CID;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Smartphone Use Service", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null, null);
            channel.enableLights(false);
            channel.enableVibration(false);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Smartphone Use")
                    .setContentText("Started App Usage Plugin")
                    .build();
            startForeground(Constants.SMARTPHONE_USE_PLUGIN_NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //AUTHORITY = Provider.getAuthority(this);
        Log.d(TAG, "Plugin: onCreate");
        Plugin_instance = this;
        launcherPackageName = getLauncherName();

        // Load correct phase!
        SharedPreferences phaseSettings = getApplicationContext().getSharedPreferences("phaseSetting", 0);
        String phaseVal = phaseSettings.getString("phase", "0");
        if((!phaseVal.equals("0"))){
            currentPhase = phaseVal;
        }
        if(!Constants.phases.contains(currentPhase))
            currentPhase = Constants.phases.get(0); // NO_INTERVENTION
        Log.d(TAG, "onCreate for Plugin: phase="+currentPhase);;

        loadWhitelist();
        notifyPluginStart();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int i = super.onStartCommand(intent, flags, startId);
        String intentAction = null;
        if (intent != null)
            intentAction = intent.getAction();
        if (intentAction == null)
            //return i;
            return START_STICKY;
        Log.d(Constants.TAG, "Plugin: onStartCommand " + intentAction);
        Aware.startAWARE(this);
        switch (intentAction) {
            case Constants.ACTION_FIRST_RUN_APPUSEPLUGIN:
                // Check if Applications Service is working
                onStartActions();
                notifyPluginStart();
                complianceChecksRunnable.run();
                break;
            case Constants.ACTION_ENSURE_PLUGIN_WORKING:
                onStartActions();
                notifyPluginStart();
//                if (!doesNotificationExist(Constants.SMARTPHONE_USE_PLUGIN_NOTIFICATION_ID)){
//                    notifyPluginStart();
//                }
                if ((lastComplianceCheck == null) || ((System.currentTimeMillis() - lastComplianceCheck) > (complianceCheckTimer + 5*60*1000))) {
                    complianceChecksRunnable.run();
                }
                break;
            default:
                //return i;
                return START_STICKY;
        }
        //return i;
        return START_STICKY;
    }
    private void onStartActions(){
        registerReceiver();
        attachAppListener();
        attachScreenListener();
    }

    // App Usage CODE

    private void resetIfNoPermissions(){
        currAppDuration = null;
        currAppData = new ContentValues();
        launcherPackageName = null;
        currAppIsLauncher = false;
        currAppPrevTimeSpentToday = 0;
    }
    private void logPermissionChange(Boolean prevPermit, Boolean currPermit){
        Calendar c = Calendar.getInstance();
        Date now = c.getTime();
        String device_id = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID);
        ContentValues cv = new ContentValues();
        cv.put("timestamp", System.currentTimeMillis());
        cv.put("timestamp_read", now.toString());
        cv.put("device_id", device_id);
        cv.put("prev_is_enabled", String.valueOf(prevPermit));
        cv.put("curr_is_enabled", String.valueOf(currPermit));
        Log.d(TAG+" to insert permit ", cv.toString());
        insertPermissionChange(cv);
    }
    public void insertPermissionChange(ContentValues cv) {
        try {
            //Log.d(TAG, Provider.Applications_Switch_Permit.CONTENT_URI.toString());
            getContentResolver().insert(Provider.Applications_Switch_Permit.CONTENT_URI, cv);
            Log.d(TAG, "Success insertion into Applications_Switch_Permit");
        } catch (Exception ex) {
            Log.e(TAG, "insert exception", ex);
            Log.e(TAG, "Failed insertion into Applications_Switch_Permit");
        }
    }

    // To update app duration
    Runnable appDurationRunnable = new Runnable() {
        public void run() {
            // Check for accessbility permissions
            isAppUsageEnabled = isAccessibilityEnabled(getApplicationContext());
            //Log.d(TAG + " isAppUsageEnabled", Boolean.toString(isAppUsageEnabled));
            if (!isAppUsageEnabled){
                resetIfNoPermissions();
            }
            if (isAppUsageEnabled != prevIsAppUsageEnabled){
                logPermissionChange(prevIsAppUsageEnabled, isAppUsageEnabled);
                checkAdditionalCompliance();
            }
            // Update current app duration every few seconds
            if (currAppData.size() > 0) {
                currAppDuration = System.currentTimeMillis() - (Long) currAppData.get("timestamp");
                Log.d(TAG + " CurrAppDuration", currAppDuration.toString());
                // Administer intervention
                administerIntervention();
            }
            prevIsAppUsageEnabled = isAppUsageEnabled;
            // Delay in updating app duration
            currAppDurationHandler.postDelayed(appDurationRunnable, currAppDurationPeriod); // update every few seconds
            waitingPopUp();
        }
    };

    protected void pseudo_onForeground(String nextPckg, long nextAppTimeStamp) {
        Log.i(TAG, "~~~pseudo_onForeground: nextPckg = "+nextPckg);
        complianceLastAppReceived = System.currentTimeMillis();
        removeNotificationsOnAppReceive();
//        String nextPckg = (String) data.get("package_name");
        Log.d(TAG, "~~~Switched to "+nextPckg);
        if (nextPckg.equals(PluginConstants.testApp)){
            notifySwitchToTestForDebug();
            Log.d(TAG, "~~~Test app switched ");
        }
        // Exclude excCalc Apps such as Smartphone Use first
        if (excCalcApps.contains(nextPckg)){
            return;
        }
        // Exclude keyboard
        if (editorPackageName == null){
            editorPackageName = getEditorName();
        }
        if (nextPckg.toLowerCase().contains(securePackageName.toLowerCase())){
            return; // Meaning do NOTHING if package name of app should be skipped from calculations
        }
        if (editorPackageName!=null && nextPckg.toLowerCase().contains(editorPackageName.toLowerCase())){
            return; // Meaning do NOTHING if package name of app should be skipped from calculations
        }
        if (editorPackageName!=null && nextPckg.toLowerCase().contains(launcherPackageName.toLowerCase())){
            return; // Meaning do NOTHING if package name of app should be skipped from calculations
        }
        // Exclude if nextApp == currApp due to orientation changes or keyboard in between
        if (currAppData.size() > 0 && nextPckg.equals((String) currAppData.get("package_name"))){
            if(firstEqual){
                Constants.lastEqualTime[0] = System.currentTimeMillis();
            }
            if((System.currentTimeMillis() - Constants.lastEqualTime[0]) < popUpInterval){
                return;
            }
            Constants.lastEqualTime[0] = System.currentTimeMillis();
        }else {
            firstEqual = false;
        }

        //Remove tracking duration for previous app
        if (currAppDuration != null) {
            currAppDurationHandler.removeCallbacks(appDurationRunnable);
        }
        if (currAppData.size() > 0) {
            long currAppTimeStamp = (long) currAppData.get("timestamp");
            long currAppTimeSpent = nextAppTimeStamp - currAppTimeStamp;
            long currAppTimeSpentToday = currAppTimeSpent + currAppPrevTimeSpentToday;
            boolean islauncher = currAppIsLauncher;
            ContentValues toInsertData = new ContentValues(currAppData);
            toInsertData.put("time_spent", Long.toString(currAppTimeSpent));
            toInsertData.put("time_spent_today", Long.toString(currAppTimeSpentToday));
            toInsertData.put("end_timestamp_day", new Date(nextAppTimeStamp).toString());
            toInsertData.put("end_timestamp", Long.toString(nextAppTimeStamp));
            toInsertData.put("is_launcher", String.valueOf(islauncher));
            insertDataIntoAppDiff(toInsertData); // insert data -- need to debug
            String currPckgName = (String) currAppData.get("package_name");
                updateAppSummary(currPckgName, currAppTimeSpent,currentPhase);
                if (!islauncher && !excCalcApps.contains(currPckgName)) {
                    updateAppSummary("OVERALL", currAppTimeSpent,currentPhase);
                }
            Log.d(TAG + " TO INSERT ", toInsertData.toString());
        }
        editorPackageName = getEditorName();
        // Log.d(TAG + " editorPackageName ", editorPackageName);
        launcherPackageName = getLauncherName();
        Log.d(TAG + " ~~~launcherPackageName ", launcherPackageName);
        currAppIsLauncher = launcherPackageName.equals(nextPckg);
        Log.d(TAG + " ~~~NEWLY LAUNCHED APP ", nextPckg);
        Intent startAppIntent = new Intent(Plugin.this, Timeout.class);
        startAppIntent.setAction(Constants.ACTION_START_APPLICATION);
        startAppIntent.putExtra("device_id", (String) currAppData.get("device_id"));
        startAppIntent.putExtra("timestamp", Calendar.getInstance().getTimeInMillis()); // timeout_timestamp as index to db
        startAppIntent.putExtra("timestamp_read", Calendar.getInstance().getTime().toString()); // timeout_timestamp as index to db
        startAppIntent.putExtra("application_name", (String) currAppData.get("application_name"));
        startAppIntent.putExtra("package_name", (String) currAppData.get("package_name"));
        startAppIntent.putExtra("is_system_app", String.valueOf((boolean) currAppData.get("is_system_app")));
        startAppIntent.putExtra("is_launcher", String.valueOf(currAppIsLauncher));
        long start_time = (long) currAppData.get("timestamp");
        startAppIntent.putExtra("start_timestamp", Long.toString(start_time));
        startAppIntent.putExtra("time_today_to_timeout", Long.toString(-1));
        startAppIntent.putExtra("phase", currentPhase);
        timeoutOn = true;
        String package_name = (String) currAppData.get("package_name");

        // if app in "white"listedApps, then we need to do intervention
        if(whitelistedApps.contains(package_name)) {
            Log.d(TAG, "administerIntervention: Timeout.class start 1");
            startService(startAppIntent);
        }

        //Log.d(TAG + " NEWLY LAUNCHED APP ", currAppData.toString());
        // Get time previously spent on the next app (app brought into foreground now)
        currAppPrevTimeSpentToday = getPrevRowTimeSpentToday(nextPckg, "WeChat");
        Log.d(TAG + " time spent today ", Long.toString(currAppPrevTimeSpentToday));
        // If Randomization mode, fetch app summary
        // TODO::what for???
        if (currentPhase.equals("NO_INTERVENTION")) {
            currAppSummary = getAppSummary(nextPckg);
            if (currAppSummary==null){
                currAppSummary = getAppSummary("OVERALL");
            }
            useCurrAppSummary = isAppSummaryUsable(currAppSummary);
            randCurrAppSummarySegs = new ArrayList<Boolean>();
            Random rand = new Random();
            randCurrAppSummarySegs.add(rand.nextInt(2)==0);
            randCurrAppSummarySegs.add(rand.nextInt(2)==0);
            randCurrAppSummarySegs.add(rand.nextInt(2)==0);
            randCurrAppSummarySegs.add(rand.nextInt(2)==0);
            randCurrAppSummarySegs.add(rand.nextInt(2)==0);
        }
        // Ensure that sync is enabled
        autoSyncPlugin();
        // All the computation - start tracking duration for this new app
        appDurationRunnable.run(); // update duration
    }


    protected void attachAppListener() {
        Log.d(TAG, "Plugin: attachAppListener");
        Applications.isAccessibilityServiceActive(getApplicationContext());
        Applications.setSensorObserver(new Applications.AWARESensorObserver() {
            @Override
            public void onForeground(ContentValues data) {
                Log.i(TAG, "onForeground: data = "+data);
                complianceLastAppReceived = System.currentTimeMillis();
                removeNotificationsOnAppReceive();
                String nextPckg = (String) data.get("package_name");
                Log.d(TAG, "Switched to "+nextPckg);
                String currAppPackageName = (String) currAppData.get("package_name");
                //Log.d(TAG, "USING CURR: " + currAppPackageName + " NEXT " + nextPckg + " USING TIME: " + System.currentTimeMillis());
                Constants.lastUsingTime.put(currAppPackageName, System.currentTimeMillis());
                if (nextPckg.equals(PluginConstants.testApp)){
                    notifySwitchToTestForDebug();
                    Log.d(TAG, "Test app switched ");
                }
                Log.d(TAG, "onForeground: here1");
                // Exclude excCalc Apps such as Smartphone Use first
                if (excCalcApps.contains(nextPckg)){
                    return;
                }
                Log.d(TAG, "onForeground: here2");
                // Exclude keyboard
                if (editorPackageName == null){
                    editorPackageName = getEditorName();
                }
                Log.d(TAG, "onForeground: here3");
                if (nextPckg.toLowerCase().contains(securePackageName.toLowerCase())){
                    Log.d(TAG, "onForeground: in is securePackage, LastOneIsEditorPackage = "+LastOneIsEditorPackage);
                    LastOneIsEditorPackage = true;
                    return; // Meaning do NOTHING if package name of app should be skipped from calculations
                }
                if (editorPackageName!=null && nextPckg.toLowerCase().contains(editorPackageName.toLowerCase())){
                    Log.d(TAG, "onForeground: in is editorPackage, LastOneIsEditorPackage = "+LastOneIsEditorPackage);
                    LastOneIsEditorPackage = true;
                    return; // Meaning do NOTHING if package name of app should be skipped from calculations
                }
                Log.d(TAG, "onForeground: here4");
                if (editorPackageName!=null && nextPckg.toLowerCase().contains(launcherPackageName.toLowerCase())){
                    Log.d(TAG, "onForeground: in is launcherPackageName, LastOneIsEditorPackage = "+LastOneIsEditorPackage);
                    if(!LastOneIsEditorPackage) {
                        currAppData = data;
                    }
                    return; // Meaning do NOTHING if package name of app should be skipped from calculations
                }
                Log.d(TAG, "onForeground: here5");
                // Exclude if nextApp == currApp due to orientation changes or keyboard in between
                if (currAppData.size() > 0 && nextPckg.equals((String) currAppData.get("package_name"))){
                    if(firstEqual){
                        Constants.lastEqualTime[0] = System.currentTimeMillis();
                    }
                    if((System.currentTimeMillis() - Constants.lastEqualTime[0]) < popUpInterval){
                        return;
                    }
                    Constants.lastEqualTime[0] = System.currentTimeMillis();
                }else {
                    firstEqual = false;
                }
                Log.d(TAG, "onForeground: here6");
                // End timeout when app is switched (does not end when keyboard is pulled out but that should never happen)
                if (timeoutOn){
                    Log.d(TAG, "Timeout sending END INTENT");
                    Log.d(TAG, nextPckg);
                    Log.d(TAG, (String) currAppData.get("package_name"));
                    Intent timeoutEndIntent = new Intent(Plugin.this, Timeout.class);
                    timeoutEndIntent.setAction(Constants.ACTION_END_INTERVENTION).putExtra("phase",currentPhase);
                    Log.d(TAG, "administerIntervention: Timeout.class start 2");
                    startService(timeoutEndIntent);
                }
                Log.d(TAG, "onForeground: here7");
                //Remove tracking duration for previous app
                if (currAppDuration != null) {
                    currAppDurationHandler.removeCallbacks(appDurationRunnable);
                }
                // Compute time spent on previous app
                long nextAppTimeStamp = (long) data.get("timestamp");
//                Log.d(TAG, "calling nextAppTimeStamp with "+nextAppTimeStamp);
                logAndResetTimeouts(nextAppTimeStamp); // reset interventions for the new app
                if (currAppData.size() > 0) {
                    long currAppTimeStamp = (long) currAppData.get("timestamp");
                    long currAppTimeSpent = nextAppTimeStamp - currAppTimeStamp;
                    long currAppTimeSpentToday = currAppTimeSpent + currAppPrevTimeSpentToday;
                    boolean islauncher = currAppIsLauncher;
                    ContentValues toInsertData = new ContentValues(currAppData);
                    toInsertData.put("time_spent", Long.toString(currAppTimeSpent));
                    toInsertData.put("time_spent_today", Long.toString(currAppTimeSpentToday));
                    toInsertData.put("end_timestamp_day", new Date(nextAppTimeStamp).toString());
                    toInsertData.put("end_timestamp", Long.toString(nextAppTimeStamp));
                    toInsertData.put("is_launcher", String.valueOf(islauncher));
                    insertDataIntoAppDiff(toInsertData); // insert data -- need to debug
                    String currPckgName = (String) currAppData.get("package_name");
//                    if (currentPhase.equals("NO_INTERVENTION")) { // update app summary only during baseline phase
                        updateAppSummary(currPckgName, currAppTimeSpent,currentPhase);
                        if (!islauncher && !excCalcApps.contains(currPckgName)) {
                            updateAppSummary("OVERALL", currAppTimeSpent,currentPhase);
                        }
//                    }
                    Log.d(TAG + " TO INSERT ", toInsertData.toString());
                }
                // Update variables for next app
                String tmp_system_str = data.getAsString("is_system_app");
                if((!tmp_system_str.equals("true")) || (!LastOneIsEditorPackage)) {
                    currAppData = data;
                    LastOneIsEditorPackage = false;
                }
                // Update launcher package name (it may have changed if the user installed new launcher
                editorPackageName = getEditorName();
                // Log.d(TAG + " editorPackageName ", editorPackageName);
                launcherPackageName = getLauncherName();
                Log.d(TAG + " launcherPackageName ", launcherPackageName);
                currAppIsLauncher = launcherPackageName.equals((String) data.get("package_name"));
                Log.d(TAG + " NEWLY LAUNCHED APP ", (String) data.get("package_name"));
                Intent startAppIntent = new Intent(Plugin.this, Timeout.class);
                startAppIntent.setAction(Constants.ACTION_START_APPLICATION);
                startAppIntent.putExtra("device_id", (String) currAppData.get("device_id"));
                startAppIntent.putExtra("timestamp", Calendar.getInstance().getTimeInMillis()); // timeout_timestamp as index to db
                startAppIntent.putExtra("timestamp_read", Calendar.getInstance().getTime().toString()); // timeout_timestamp as index to db
                startAppIntent.putExtra("application_name", (String) currAppData.get("application_name"));
                startAppIntent.putExtra("package_name", (String) currAppData.get("package_name"));
                startAppIntent.putExtra("is_system_app", String.valueOf((boolean) currAppData.get("is_system_app")));
                startAppIntent.putExtra("is_launcher", String.valueOf(currAppIsLauncher));
                long start_time = (long) currAppData.get("timestamp");
                startAppIntent.putExtra("start_timestamp", Long.toString(start_time));
                startAppIntent.putExtra("time_today_to_timeout", Long.toString(-1));
                startAppIntent.putExtra("phase", currentPhase);
                timeoutOn = true;
                String package_name = (String) currAppData.get("package_name");

                // if app in "white"listedApps, then we need to do intervention
                if(whitelistedApps.contains(package_name)) {
                    boolean found_flag = true;
                    if(package_name.equals(Constants.WeChatPackageName)) {

                        found_flag = false;
                        Constants.lastWechatEnterTime[0] = System.currentTimeMillis();
                    }
                    // other parts of WeChat excluded
                    if(found_flag) {
                        Log.d(TAG, "**onForeground: can start service");
                        Log.d(TAG, "administerIntervention: Timeout.class start 3");
                        startService(startAppIntent);
                    }
                }

                //Log.d(TAG + " NEWLY LAUNCHED APP ", currAppData.toString());
                // Get time previously spent on the next app (app brought into foreground now)
                currAppPrevTimeSpentToday = getPrevRowTimeSpentToday((String) data.get("package_name"), (String) data.get("application_name"));
                Log.d(TAG + " time spent today ", Long.toString(currAppPrevTimeSpentToday));

                // fetch app summary
                currAppSummary = getAppSummary((String) data.get("package_name"));
                if (currAppSummary==null){
                    currAppSummary = getAppSummary("OVERALL");
                }
                useCurrAppSummary = isAppSummaryUsable(currAppSummary);
                randCurrAppSummarySegs = new ArrayList<Boolean>();
                Random rand = new Random();
                randCurrAppSummarySegs.add(rand.nextInt(2)==0);
                randCurrAppSummarySegs.add(rand.nextInt(2)==0);
                randCurrAppSummarySegs.add(rand.nextInt(2)==0);
                randCurrAppSummarySegs.add(rand.nextInt(2)==0);
                randCurrAppSummarySegs.add(rand.nextInt(2)==0);

                // Ensure that sync is enabled
                autoSyncPlugin();
                // All the computation - start tracking duration for this new app
                appDurationRunnable.run(); // update duration
            }

            @Override
            public void onNotification(ContentValues data) {

            }

            @Override
            public void onCrash(ContentValues data) {

            }

            @Override
            public void onKeyboard(ContentValues data) {

            }

            @Override
            public void onBackground(ContentValues data) {

            }

            @Override
            public void onTouch(ContentValues data) {

            }
        });
    }


    private synchronized static boolean isAccessibilityEnabled(Context context) {
        boolean enabled = false;

        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);

        // Try to fetch active accessibility services directly from Android OS database instead of broken API...
        String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (settingValue != null) {
            if (settingValue.contains(context.getPackageName())) {
                enabled = true;
            }
        }
        if (!enabled) {
            try {
                List<AccessibilityServiceInfo> enabledServices = AccessibilityManagerCompat.getEnabledAccessibilityServiceList(accessibilityManager, AccessibilityEventCompat.TYPES_ALL_MASK);
                if (!enabledServices.isEmpty()) {
                    for (AccessibilityServiceInfo service : enabledServices) {
                        if (service.getId().contains(context.getPackageName())) {
                            enabled = true;
                            break;
                        }
                    }
                }
            } catch (NoSuchMethodError e) {
            }
        }
        if (!enabled) {
            try {
                List<AccessibilityServiceInfo> enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
                if (!enabledServices.isEmpty()) {
                    for (AccessibilityServiceInfo service : enabledServices) {
                        if (service.getId().contains(context.getPackageName())) {
                            enabled = true;
                            break;
                        }
                    }
                }
            } catch (NoSuchMethodError e) {
            }
        }

        //Keep the global setting up-to-date
        Aware.setSetting(context, Applications.STATUS_AWARE_ACCESSIBILITY, enabled, "com.aware.phone");

        return enabled;
    }
    public String getLauncherName() {
        PackageManager localPackageManager = getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        String str = localPackageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName;
        //Log.d(TAG + " Current launcher ", str);
        return str;
    }
    public String getEditorName(){
        String edStr = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        String[] edStrSep = edStr.split("/");
        String edName;
        if (edStrSep.length > 1){
            edName = edStrSep[0].trim();
        }
        else {
            edName = edStr;
        }
        Log.d(TAG, "EDNAME: " + edName);
        return edName;
    }

    private long getPrevRowTimeSpentToday(String pckg, String appName){
        //long startnow = android.os.SystemClock.uptimeMillis();
        Calendar c = Calendar.getInstance();
        long now = c.getTimeInMillis();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long midntTimestamp = c.getTimeInMillis();
        //Log.d(TAG, "midntTimestamp "+Long.toString(midntTimestamp));
        String whereCond = "timestamp > "+ Long.toString(midntTimestamp) + " AND package_name = '" + pckg+"' AND application_name = '" + appName + "'";
        // String whereCond = "timestamp > "+ Long.toString(midntTimestamp);
        Cursor prevAppDataCursor = getContentResolver().query(Provider.Applications_Diff.CONTENT_URI, null, whereCond, null, "timestamp DESC");
        ContentValues prevAppData = null;
        long prevRowTimeSpentToday = 0;
//        Log.v(TAG, DatabaseUtils.dumpCursorToString(prevAppDataCursor));
        if (prevAppDataCursor != null) {
            if (prevAppDataCursor.moveToFirst()) {
                prevAppData = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(prevAppDataCursor, prevAppData);
                String time_spent_today_str = (String) prevAppData.get("time_spent_today");
                if (time_spent_today_str != null && !time_spent_today_str.equals("") && !time_spent_today_str.equals("''")) {
                    prevRowTimeSpentToday = Long.valueOf(time_spent_today_str);
                }

            }
        }
        //long endnow = android.os.SystemClock.uptimeMillis();
        //Log.d(TAG, "Db query Execution time: " + (endnow - startnow)/1000.0 + " s");
        return prevRowTimeSpentToday;
    }
    public void insertDataIntoAppDiff(ContentValues cv) {
        try {
            //Log.d(TAG, Provider.Applications_Diff.CONTENT_URI.toString());
            getContentResolver().insert(Provider.Applications_Diff.CONTENT_URI, cv);
            //Log.d(TAG, "Success insertion into Applications_Diff");
        } catch (Exception ex) {
            Log.e(TAG, "insert exception", ex);
            Log.e(TAG, "Failed insertion into Applications_Diff");
        }
    }
    public ContentValues getAppSummary(String pckg){
        Log.d(TAG, "getAppSummary: pckg="+pckg+" see what happends next......");
        String placeHolderValueArr[] = {pckg};
        Cursor summaryCur = getContentResolver().query(Provider.Applications_Summary.CONTENT_URI, null, "package_name = ?", placeHolderValueArr, "timestamp DESC");
        Log.d(TAG, "getAppSummary: summaryCur="+summaryCur+" see what happends next......");
        ContentValues summaryVals = null;
        if (summaryCur != null) {
            if (summaryCur.moveToFirst()) {
                summaryVals = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(summaryCur, summaryVals);
                Log.d(TAG+" App Summary, GOT: ", summaryVals.toString());
            }
        }
        Log.d(TAG, "getAppSummary: returning="+summaryVals+" and end");
        return(summaryVals);
    }
    private Long stringToLong(String str){
        if (str != null && !str.equals("") && !str.equals("''")){
            return (Long.valueOf(str));
        }
        return null;
    }
    private Integer stringToInteger(String str){
        if (str != null && !str.equals("") && !str.equals("''")){
            return (Integer.valueOf(str));
        }
        return null;
    }
    private Long getMeanDur(Long sumDur, int countS){
        return (sumDur/countS);
    }
    public void updateAppSummary(String pckg, long time_spent, String currentPhase) {
        ContentValues appSummary = getAppSummary(pckg);
        String device_id = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID);
        if (appSummary!=null) { // update app summary if it makes sense
            long minDur = stringToLong(appSummary.getAsString("min_duration"));
            long maxDur = stringToLong(appSummary.getAsString("max_duration"));
            long sumDur = stringToLong(appSummary.getAsString("sum_duration"));
            int countS = stringToInteger(appSummary.getAsString("count_sessions"));
            if (time_spent < minDur){
                minDur = time_spent;
            }
            if (time_spent > maxDur){
                maxDur = time_spent;
            }
            sumDur = sumDur + time_spent;
            countS = countS + 1;
            ContentValues cvNew = new ContentValues();
            Calendar c = Calendar.getInstance();
            Date now = c.getTime();
            cvNew.put("timestamp_day", Calendar.getInstance().getTime().toString());
            cvNew.put("timestamp", System.currentTimeMillis());
            cvNew.put("timestamp_read", now.toString());
            cvNew.put("device_id", device_id);
            cvNew.put("package_name", pckg);
            cvNew.put("currentPhase", currentPhase);
            cvNew.put("min_duration", minDur);
            cvNew.put("max_duration", maxDur);
            cvNew.put("sum_duration", sumDur);
            cvNew.put("count_sessions", countS);
            try {
                String placeHolderValueArr[] = {pckg};
                getContentResolver().update(Provider.Applications_Summary.CONTENT_URI, cvNew, "package_name = ?", placeHolderValueArr);
                Log.d(TAG+" App Summary, UPDATE: ", cvNew.toString());
            } catch (Exception ex) {
                Log.e(TAG, "update exception", ex);
                Log.e(TAG, "Failed updation of Applications_Summary");
            }
        }
        else { // insert app summary
            ContentValues cvNew = new ContentValues();
            Calendar c = Calendar.getInstance();
            Date now = c.getTime();
            cvNew.put("timestamp_day", Calendar.getInstance().getTime().toString());
            cvNew.put("timestamp", System.currentTimeMillis());
            cvNew.put("timestamp_read", now.toString());
            cvNew.put("device_id", device_id);
            cvNew.put("package_name", pckg);
            cvNew.put("currentPhase", currentPhase);
            cvNew.put("min_duration", time_spent);
            cvNew.put("max_duration", time_spent);
            cvNew.put("sum_duration", time_spent);
            cvNew.put("count_sessions", 1);
            try {
                getContentResolver().insert(Provider.Applications_Summary.CONTENT_URI, cvNew);
                Log.d(TAG+" App Summary, INSERT: ", cvNew.toString());
            } catch (Exception ex) {
                Log.e(TAG, "insert exception", ex);
                Log.e(TAG, "Failed insertion into Applications_Summary");
            }
        }
    }

    // SCREEN STATUS CODE
    protected void attachScreenListener(){
        Log.d(TAG, "Plugin: attachScreenListener");
        Aware.startScreen(this);
        Screen.setSensorObserver(new Screen.AWARESensorObserver() {
            @Override
            public void onScreenOn() {
                screenOff = false;
                Log.d(TAG, "SCREEN ON");
            }

            @Override
            public void onScreenOff() {
                screenOff = true;
                Log.d(TAG, "SCREEN OFF");
            }

            @Override
            public void onScreenLocked() {
                screenLock = true;
                Log.d(TAG, "SCREEN LOCK");
            }

            @Override
            public void onScreenUnlocked() {
                screenLock = false;
                Log.d(TAG, "SCREEN UNLOCK");
            }
        });
    }

    private void waitingPopUp(){
            //If need to wait for the pop up time, then check whether current time near in the time need to pop up overlay.
            Calendar c = Calendar.getInstance();
            boolean inTime = false;
            boolean inclusionCheck = false;
            boolean popUp = false;
            if(Constants.WorkSleepTime[0] == 0 &&Constants.WorkSleepTime[1] == 0 &&Constants.WorkSleepTime[2] == 0 &&Constants.WorkSleepTime[3] == 0){
                //Do nothing
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

                    //Check Noon break
                    if(c.get(Calendar.HOUR_OF_DAY) >= Constants.WorkSleepTime[16] &&
                            c.get(Calendar.HOUR_OF_DAY) <= Constants.WorkSleepTime[18]){
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
            String currAppPackageName = (String) currAppData.get("package_name");
            inclusionCheck = !screenLock && !screenOff && whitelistedApps.contains(currAppPackageName);
            if(inTime){
                //Check Intime
                if(Constants.enableWaitingPopUp[0] && inclusionCheck){
                    //If intime and are waiting for pop up, then time right
                    popUp = true;
                }
            }else {
                //If not in time, then waiting for pop up
                Constants.enableWaitingPopUp[0] = true;
            }
            if(!Constants.enableWaitingPopUp[0]){
                //If not waiting for pop up, then time not right
                popUp = false;
            }
            // Pop up if re-enter Wechat SNS page more than 10 seconds
            if(currAppPackageName.equals(Constants.WeChatPackageName) &&
                    (System.currentTimeMillis() - Constants.lastWechatEnterTime[0]) > SNSInterval){
                String[] WeChatActivities;
                if(Constants.WithPyq[0] && Constants.WithMini[0]){
                    WeChatActivities = Constants.WeChatActivities_with_both;
                }else if(Constants.WithPyq[0]){
                    WeChatActivities = Constants.WeChatActivities_with_pyq;
                }else if(Constants.WithMini[0]){
                    WeChatActivities = Constants.WeChatActivities_with_mini;
                }else {
                    WeChatActivities = Constants.WeChatActivities_without_both;
                }
                for(String bad_activity:WeChatActivities) {
                    /*Log.d(TAG, "SNS BAD: " + bad_activity + " " + currentComponentName.flattenToShortString() + " " +
                            (bad_activity.equals(currentComponentName.flattenToShortString())));*/
                    if(bad_activity.equals(currentComponentName.flattenToShortString())) {
                        // If have not successfully entered the Wechat blacklisted page last time
                        // or have not entered yet, then give a pop up
                        if(Constants.lastInputRight.get(Constants.WeChatPackageName) == null){
                            popUp = true;
                            break;
                        }else if(!Constants.lastInputRight.get(Constants.WeChatPackageName)){
                            popUp = true;
                            break;
                        }
                    }
                }
            }
        //Toast.makeText(getApplicationContext(),"INTIME " + inTime,Toast.LENGTH_LONG + "INCLUSION " + inclusionCheck + " POPUP " + popUp).show();
            if(popUp){
                long timeSpentToday = currAppDuration + currAppPrevTimeSpentToday;
                String interv_policy = null;
                long timeSpentTodayInSec = (timeSpentToday/ (long) 1000.0);
                long now = c.getTimeInMillis();
                // Update Queue to prevent burden (i.e. the not more than X intervention in Y min limit)
                lastTimeoutForCurrApp = now;
                if (timesOfLastFewTimeOuts.size() >= 5) {
                    timesOfLastFewTimeOuts.remove();
                }
                timesOfLastFewTimeOuts.add(now);
                // Trigger Intervention
                Log.d(TAG + " Interv. Triggered ", currAppDuration.toString());
                Intent timeoutIntent = new Intent(Plugin.this, Timeout.class);
                timeoutIntent.setAction(Constants.ACTION_START_INTERVENTION);
                timeoutIntent.putExtra("device_id", (String) currAppData.get("device_id"));
                timeoutIntent.putExtra("timestamp", now); // timeout_timestamp as index to db
                timeoutIntent.putExtra("timestamp_read", c.getTime().toString()); // timeout_timestamp as index to db
                timeoutIntent.putExtra("application_name", (String) currAppData.get("application_name"));
                timeoutIntent.putExtra("package_name", (String) currAppData.get("package_name"));
                timeoutIntent.putExtra("is_system_app", String.valueOf((boolean) currAppData.get("is_system_app")));
                timeoutIntent.putExtra("is_launcher", String.valueOf(currAppIsLauncher));
                long start_time = (long) currAppData.get("timestamp");
                timeoutIntent.putExtra("start_timestamp", Long.toString(start_time));
                timeoutIntent.putExtra("time_to_timeout", currAppDuration.toString());
                timeoutIntent.putExtra("time_today_to_timeout", Long.toString(timeSpentToday));
                timeoutIntent.putExtra("interv_policy", interv_policy);
                timeoutIntent.putExtra("phase", currentPhase);
                timeoutOn = true;
                Log.d(TAG, "administerIntervention: Timeout.class start 4");
                startService(timeoutIntent);
            }
    }


    // INTERVENTIONS CODE
    private boolean isAppSummaryUsable(ContentValues appSummary){
        boolean toUse = false;
        if (appSummary!=null) {
            //long minDur = stringToLong(appSummary.getAsString("min_duration"));
            //long maxDur = stringToLong(appSummary.getAsString("max_duration"));
            long sumDur = stringToLong(appSummary.getAsString("sum_duration"));
            int countS = stringToInteger(appSummary.getAsString("count_sessions"));
            long meanDur = getMeanDur(sumDur, countS);
            //long segOne = meanDur - minDur;
            //long segTwo = maxDur - meanDur;
            //if ((segOne > 30 * 1000) && (segTwo > 30 * 1000)) {
            //    toUse = true;
            //}
            if (meanDur/2.0 > 30*1000){
                toUse = true;
            }
        }
        return toUse;
    }
    private void administerIntervention(){
        if (currentPhase.equals("BASELINE")){
            return;
        }
        // Administer intervention
        // Calculate important metrics
        String currAppPackageName = (String) currAppData.get("package_name");
        long timeSpentToday = currAppDuration + currAppPrevTimeSpentToday;
        long timeSpentTodayInSec = (timeSpentToday/ (long) 1000.0);
        timeoutRunCount = timeoutRunCount + 1; // updated every 10 secs
        // Check if time is right
        boolean isTest = false;
        String interv_policy = null;
        if (!timeoutOn && !screenLock && !screenOff && currAppPackageName.equalsIgnoreCase(PluginConstants.testApp)) { // Test app always sends timeouts every 10 secs
            timeRight = true;
            isTest = true;
            interv_policy = "TEST";
            Log.d(TAG + " TIMEOUT TEST ", Boolean.toString(timeRight));

        }
        else{
            // BURDEN CHECK
            boolean burdenOk = true;
            if (timesOfLastFewTimeOuts.size() >= 5 && (System.currentTimeMillis() - timesOfLastFewTimeOuts.element()) < 15*60*1000){
                burdenOk = false;
                Log.d(TAG, "Already issued 5 timeouts in 15 min");
            }
            boolean burdenOkCurrApp = true;
            if (lastTimeoutDismissForCurrApp != null && (System.currentTimeMillis() - lastTimeoutDismissForCurrApp) < 1.0*60*1000){
                burdenOkCurrApp = false;
                Log.d(TAG, "Already issued 1 timeout for this app in 1 min (dismiss time)");
            }
            if (lastTimeoutForCurrApp != null && (System.currentTimeMillis() - lastTimeoutForCurrApp) < 1.0*60*1000) {
                burdenOkCurrApp = false;
                Log.d(TAG, "Already issued 1 timeout for this app in 1 min (launch time)");
            }
            int probaDeno = 4;
            // RANDOMIZATION CHECK
            boolean inclusionCheck = !timeoutOn && !screenLock && !screenOff && whitelistedApps.contains(currAppPackageName) && timeSpentTodayInSec > 0 && burdenOkCurrApp && burdenOk && (!currAppIsLauncher) && !excTimeoutApps.contains(currAppPackageName);
            Log.d(TAG, "inclusion check "+inclusionCheck+", in Wl "+whitelistedApps.contains(currAppPackageName)+", current whitelist "+whitelistedApps.toString()+", current app "+currAppPackageName);

            boolean found_flag = true;
            if(currAppPackageName.equals(Constants.WeChatPackageName)) {
                found_flag = false;
                Log.d(TAG, "**administerIntervention: currentApp = "+currentComponentName.flattenToShortString());
                String[] WeChatActivities;
                if(Constants.WithPyq[0] && Constants.WithMini[0]){
                    WeChatActivities = Constants.WeChatActivities_with_both;
                }else if(Constants.WithPyq[0]){
                    WeChatActivities = Constants.WeChatActivities_with_pyq;
                }else if(Constants.WithMini[0]){
                    WeChatActivities = Constants.WeChatActivities_with_mini;
                }else {
                    WeChatActivities = Constants.WeChatActivities_without_both;
                }
                for(String bad_activity:WeChatActivities) {
                    if(bad_activity.equals(currentComponentName.flattenToShortString())) {
                        found_flag = true;
                        break;
                    }
                }
                Log.d(TAG, "**administerIntervention: found_flag = "+found_flag);
            }
            // other parts of WeChat excluded
            if(!found_flag) {
                Log.d(TAG, "**administerIntervention: just return");
                return;
            }
            if (inclusionCheck) {
                String appOrOverall = "NONE";
                String pckgUsed = "";
                if (currAppSummary!=null) {
                    pckgUsed = currAppSummary.getAsString("package_name");
                    if (pckgUsed != null && pckgUsed.equals("OVERALL")) {
                        appOrOverall = "OVERALL";
                    }
                    else{
                        appOrOverall = "APP";
                    }
                }
                Log.d(TAG, "LARGER: " + (currAppDuration % popUpLimit) + " " + ((currAppDuration % popUpLimit) > popUpPeroid));
                if((currAppDuration % popUpLimit) > popUpPeroid && Constants.enableUsingPopUp[0]){
                    //Check whethre reach the limit time
                    timeRight = true;
                }else {
                    timeRight = false;
                }
            } // timeright filter
        } // test if-else end
        Log.d(TAG, "administerIntervention: timeRight = "+timeRight);;
        if (timeRight) {
            Calendar c = Calendar.getInstance();
            long now = c.getTimeInMillis();
            // Update Queue to prevent burden (i.e. the not more than X intervention in Y min limit)
            if (!isTest) {
                lastTimeoutForCurrApp = now;
                if (timesOfLastFewTimeOuts.size() >= 5) {
                    timesOfLastFewTimeOuts.remove();
                }
                timesOfLastFewTimeOuts.add(now);
            }
            // Trigger Intervention
            Log.d(TAG + " Interv. Triggered ", currAppDuration.toString());
            Intent timeoutIntent = new Intent(Plugin.this, Timeout.class);
            timeoutIntent.setAction(Constants.ACTION_START_INTERVENTION);
            timeoutIntent.putExtra("device_id", (String) currAppData.get("device_id"));
            timeoutIntent.putExtra("timestamp", now); // timeout_timestamp as index to db
            timeoutIntent.putExtra("timestamp_read", c.getTime().toString()); // timeout_timestamp as index to db
            timeoutIntent.putExtra("application_name", (String) currAppData.get("application_name"));
            timeoutIntent.putExtra("package_name", (String) currAppData.get("package_name"));
            timeoutIntent.putExtra("is_system_app", String.valueOf((boolean) currAppData.get("is_system_app")));
            timeoutIntent.putExtra("is_launcher", String.valueOf(currAppIsLauncher));
            long start_time = (long) currAppData.get("timestamp");
            timeoutIntent.putExtra("start_timestamp", Long.toString(start_time));
            timeoutIntent.putExtra("time_to_timeout", currAppDuration.toString());
            timeoutIntent.putExtra("time_today_to_timeout", Long.toString(timeSpentToday));
            timeoutIntent.putExtra("interv_policy", interv_policy);
            timeoutIntent.putExtra("phase", currentPhase);
            timeoutOn = true;
            Log.d(TAG, "administerIntervention: Timeout.class start 4");
            startService(timeoutIntent);
//            doneTimeout(); // done timeout
        }
    }

    private final BroadcastReceiver pluginReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null && action.equals(Constants.ACTION_LOG_INTERVENTION)){
                timeoutOn = false;
                ContentValues interCV = new ContentValues();
                interCV.put("device_id", intent.getStringExtra("device_id"));
                interCV.put("timestamp", intent.getLongExtra("timestamp", 0));
                interCV.put("application_name", intent.getStringExtra("application_name"));
                String currAppPckg = intent.getStringExtra("package_name");
                interCV.put("package_name", currAppPckg);
//                interCV.put("is_system_app", String.valueOf(intent.getBooleanExtra("is_system_app", false)));
//                interCV.put("is_launcher", String.valueOf(intent.getBooleanExtra("is_launcher", false)));
                String is_system_app_str = String.valueOf(intent.getStringExtra("is_system_app"));
                if(is_system_app_str == null || is_system_app_str.length() == 0) {
                    interCV.put("is_system_app", "false");
                } else {
                    interCV.put("is_system_app", is_system_app_str);
                }
                interCV.put("is_system_app", String.valueOf(intent.getStringExtra("is_system_app")));
                String is_launcher_str = String.valueOf(intent.getStringExtra("is_launcher"));
                if(is_launcher_str == null || is_launcher_str.length() == 0) {
                    interCV.put("is_launcher", "false");
                } else {
                    interCV.put("is_launcher", is_launcher_str);
                }
                interCV.put("start_timestamp", intent.getStringExtra("start_timestamp"));
                interCV.put("time_to_timeout", intent.getStringExtra("time_to_timeout"));
                interCV.put("time_today_to_timeout", intent.getStringExtra("time_today_to_timeout"));
                String interventionResult = intent.getStringExtra("interventionResult");
                interCV.put("interventionResult", interventionResult);
                String tempt_succeeded = intent.getStringExtra("tempt_succeeded");
                interCV.put("tempt_succeeded", tempt_succeeded);
                String persuasive_text = intent.getStringExtra("persuasive_text");
                interCV.put("persuasive_text", persuasive_text);
                String user_input = intent.getStringExtra("user_input");
                interCV.put("user_input", user_input);
                String OverlayType = intent.getStringExtra("OverlayType");
                interCV.put("OverlayType", OverlayType);
                String start_overlay_timestamp = intent.getStringExtra("start_overlay_timestamp");
                interCV.put("start_overlay_timestamp", start_overlay_timestamp);
                String end_overlay_timestamp = intent.getStringExtra("end_overlay_timestamp");
                interCV.put("end_overlay_timestamp", end_overlay_timestamp);
                String dismiss_timestamp = intent.getStringExtra("dismiss_timestamp");
                interCV.put("dismiss_timestamp", dismiss_timestamp);
                interCV.put("interv_policy", intent.getStringExtra("interv_policy"));
                interCV.put("phase", intent.getStringExtra("phase"));
                boolean success = intent.getBooleanExtra("overlay_permitted", false);
                if (success && !currAppPckg.equals(PluginConstants.testApp)) {
                    lastTimeoutDismissForCurrApp = Long.valueOf(dismiss_timestamp);
                    Log.d(TAG, "dismiss/continue-using timeout at " + lastTimeoutDismissForCurrApp.toString());
                }
                insertDataIntoAppInterv(interCV, success);
                // concat successful interventions for interventions_interv_app_end table
                if (success){
                    timeoutsSoFar.add(interCV);
                }
            }
            else if (action != null && action.equals(Constants.ACTION_SWITCH_PHASE)){
                String newphase = intent.getStringExtra("newPhase");
                String switchType = intent.getStringExtra("switchType");
                switchPhase(newphase, switchType);
            }
            else if (action != null && action.equals(Constants.ACTION_WHITELIST_CHANGED)){
                Log.d(TAG, "Received Action ACTION_WHITELIST_CHANGED.");
                loadWhitelist();
            }
        }
    };
    private void registerReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_LOG_INTERVENTION);
        filter.addAction(Constants.ACTION_SWITCH_PHASE);
        filter.addAction(Constants.ACTION_WHITELIST_CHANGED);
        registerReceiver(pluginReceiver, filter);
    }
    private void switchPhase(String newphase, String switchType){
        if (currentPhase.equals(newphase)){
            Log.d(TAG, "Plugin phase is already " + currentPhase);
        }
        else{
            String device_id = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID);
            ContentValues cvNew = new ContentValues();
            long ts = System.currentTimeMillis();
            cvNew.put("timestamp", ts);
            cvNew.put("timestamp_read", Calendar.getInstance().getTime().toString());
            cvNew.put("device_id", device_id);
            cvNew.put("old_phase", currentPhase);
            cvNew.put("new_phase", newphase);
            cvNew.put("switch_type", switchType);
            currentPhase = newphase;
            currentPhaseStartTime = System.currentTimeMillis();
            insertDataIntoPhaseSwitch(cvNew);
            Log.d(TAG, "Plugin Phase Switched to " + currentPhase);
            // Add same to shared pref
            SharedPreferences settings = getSharedPreferences("switchPhaseInfo", 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("switched_to", newphase);
            editor.putLong("at_time", ts);
            editor.commit();
        }
    }
    private void insertDataIntoPhaseSwitch(ContentValues cv){
        try {
            getContentResolver().insert(Provider.Applications_Phases.CONTENT_URI, cv);
            Log.d(TAG, "Success insertion into Applications_Phase_Switch");
        } catch (Exception ex) {
            Log.e(TAG, "insert exception", ex);
            Log.e(TAG, "Failed insertion into Applications_Phase_Switch");
        }
    }
    private void resetRandTimeoutVars(){
        timeoutRandInitSel = 3; // choose between 1 to timeoutRandInitThres calls for first timeout
        timeoutRunCount = 0;
        timeoutRunSeg1 = 0;
        timeoutRunSeg2 = 0;
        timeoutRunSeg3 = 0;
        timeoutRunSeg4 = 0;
        timeoutRunSeg5 = 0;
        timeoutRandInitDone = false;
    }

    private void logAndResetTimeouts(long appEndTimestamp){
        // log Timeouts
        int count = 0;
        for(ContentValues cv : timeoutsSoFar) {
            int totalInterv = timeoutsSoFar.size();
            int beforeInterv = count;
            int afterInterv = timeoutsSoFar.size() - beforeInterv - 1;
            long intervTimestamp = cv.getAsLong("timestamp");
            long time_to_quit = appEndTimestamp - intervTimestamp;
            cv.put("end_timestamp", Long.toString(appEndTimestamp));
            cv.put("time_to_quit", Long.toString(time_to_quit));
            cv.put("total_timeouts", Integer.toString(totalInterv));
            cv.put("num_timeouts_before", Integer.toString(beforeInterv));
            cv.put("num_timeouts_after", Integer.toString(afterInterv));
            Log.d(TAG + " to insert interv end ", cv.toString());
            insertDataIntoAppIntervEnd(cv);
            count = count + 1;
        }
        // reset Timeouts
        timeoutsSoFar = new ArrayList<ContentValues>();
        lastTimeoutForCurrApp = null;
        lastTimeoutDismissForCurrApp = null;
        resetRandTimeoutVars();
//        timeoutDoneForCurrApp = false;
    }
    //    private void doneTimeout(){
//        timeoutDoneForCurrApp = true;
//    }

    public void insertDataIntoAppIntervEnd(ContentValues cv) {
        try {
            //Log.d(TAG, Provider.Applications_Interv_App_End.CONTENT_URI.toString());
            getContentResolver().insert(Provider.Applications_Interv_App_End.CONTENT_URI, cv);
            Log.d(TAG, "Success insertion into Applications_Interv_App_End");
        } catch (Exception ex) {
            Log.e(TAG, "insert exception", ex);
            Log.e(TAG, "Failed insertion into Applications_Interv_App_End");
        }
    }
    public void insertDataIntoAppInterv(ContentValues cv, boolean success) {
        if (success) {
            try {
                //Log.d(TAG, Provider.Applications_Interv.CONTENT_URI.toString());
                getContentResolver().insert(Provider.Applications_Interv.CONTENT_URI, cv);
                Log.d(TAG + " to insert INTERV", cv.toString());
                Log.d(TAG, "Success insertion into Applications_Interv");
            } catch (Exception ex) {
                Log.e(TAG, "insert exception", ex);
                Log.e(TAG, "Failed insertion into Applications_Interv");
            }
        }
        else {
            try {
                //Log.d(TAG, Provider.Applications_Interv_No_Permissions.CONTENT_URI.toString());
                getContentResolver().insert(Provider.Applications_Interv_No_Permissions.CONTENT_URI, cv);
                Log.d(TAG + " to insert INTERVNP", cv.toString());
                Log.d(TAG, "Success insertion into Applications_Interv_No_Permissions");
            } catch (Exception ex) {
                Log.e(TAG, "insert exception", ex);
                Log.e(TAG, "Failed insertion into Applications_Interv_No_Permissions");
            }
        }
    }
    private void loadWhitelist(){
        String whereCond = "current = 1 AND whitelist = 1";
        Cursor wlCursor = getContentResolver().query(Provider.Applications_Whitelist.CONTENT_URI, null, whereCond, null, null);
        if (wlCursor != null) {
            if (wlCursor.moveToFirst()) {
                whitelistedApps = new ArrayList<String>();
                do {
                    ContentValues wlRow = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(wlCursor, wlRow);
                    String package_name_to_wl = (String) wlRow.get("package_name");
                    whitelistedApps.add(package_name_to_wl);
                } while (wlCursor.moveToNext());
            }
            Log.d(TAG, "loadWhitelist: current whitelist = "+whitelistedApps);
            wlCursor.close();
        }
    }
    // SYNC CODE
    private void autoSyncPlugin(){
        //Enable our plugin's sync-adapter to upload the data to the server if part of a study
        boolean isSyncEnabled = Aware.isSyncEnabled(getApplicationContext(), Provider.getAuthority(getApplicationContext()));
        if (Aware.isStudy(getApplicationContext()) && !isSyncEnabled){
            ContentResolver.setIsSyncable(Aware.getAWAREAccount(getApplicationContext()), Provider.getAuthority(getApplicationContext()), 1);
            ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(getApplicationContext()), Provider.getAuthority(getApplicationContext()), true);
            ContentResolver.addPeriodicSync(
                    Aware.getAWAREAccount(getApplicationContext()),
                    Provider.getAuthority(getApplicationContext()),
                    Bundle.EMPTY,
                    Long.parseLong(Aware.getSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60
            );
        }
        boolean iSyncEnabled_after = Aware.isSyncEnabled(getApplicationContext(), Provider.getAuthority(getApplicationContext()));
        Log.d(TAG, "applications_diff Sync "+Boolean.toString(iSyncEnabled_after));
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    // ADDITIONAL COMPLIANCE CHECKS
    // To update app duration
    Runnable complianceChecksRunnable = new Runnable() {
        public void run() {
            int defaultTimer = complianceCheckTimer;
            checkAdditionalCompliance();
            // Delay in checking for compliance
            complianceChecks.postDelayed(complianceChecksRunnable, defaultTimer); // update every 30 minutes
        }
    };
    private void checkAdditionalCompliance(){
        Log.d(TAG, "Checking compliance (additional)");
        lastComplianceCheck = System.currentTimeMillis();
        int missingHrs = 12;
        int missingTime = missingHrs*60*60*1000;
        // for accessibility notification
        Applications.isAccessibilityServiceActive(getApplicationContext());
        // Does current status match with the last entry in the app switch table. If not, insert.
        Boolean isEnabled = isAccessibilityEnabled(getApplicationContext());
        Boolean isEnabledDb = getPrevAppSwitchVal();
        if (isEnabledDb == null || (isEnabled != isEnabledDb)){
            Log.d(TAG, "accessibility permits in Db != current permits");
            logPermissionChange(null, isEnabled);
        }
        prevIsAppUsageEnabled = isEnabled;
        // Update timestamp of last insertion
        if (Aware.isStudy(getApplicationContext())) {
            Long timestampToCompare;
            Long prevAppDiffTimestamp = getPrevAppDiffTimestamp();
            if (complianceLastAppReceived!=null && prevAppDiffTimestamp!=null && complianceLastAppReceived > prevAppDiffTimestamp){
                timestampToCompare = complianceLastAppReceived;
            }
            else{
                timestampToCompare = prevAppDiffTimestamp;
            }
            if (timestampToCompare == null || (System.currentTimeMillis() - timestampToCompare) >= missingTime) {
                Log.d(TAG, "No app usage for over " + Integer.toString(missingHrs) + " hours.");
                notifyNoAppDataForXHours(missingHrs, isEnabled);
            }
        }
    }
    private Boolean getPrevAppSwitchVal(){
        Boolean curr_is_enabled_db = null;
        Cursor prevAppSwitchCursor = getContentResolver().query(Provider.Applications_Switch_Permit.CONTENT_URI, null, null, null, "timestamp DESC");
        if (prevAppSwitchCursor != null) {
            if (prevAppSwitchCursor.moveToFirst()) {
                ContentValues prevAppSwitch = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(prevAppSwitchCursor, prevAppSwitch);
                String curr_is_enabled_db_str = (String) prevAppSwitch.get("curr_is_enabled");
                if (curr_is_enabled_db_str != null && !curr_is_enabled_db_str.equals("") && !curr_is_enabled_db_str.equals("''")) {
                    //curr_is_enabled_db = Boolean.valueOf(curr_is_enabled_db_str);
                    curr_is_enabled_db = curr_is_enabled_db_str.equals("1");
                    //Log.d(TAG, " "+curr_is_enabled_db_str);
                }
            }
        }
        return curr_is_enabled_db;
    }
    private Long getPrevAppDiffTimestamp(){
        Long prevAppDiffTimestamp = null;
        Cursor prevAppDiffCursor = getContentResolver().query(Provider.Applications_Diff.CONTENT_URI, null, null, null, "timestamp DESC");
        if (prevAppDiffCursor != null) {
            if (prevAppDiffCursor.moveToFirst()) {
                ContentValues prevAppDiff = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(prevAppDiffCursor, prevAppDiff);
                String prevAppDiffTimestampStr = (String) prevAppDiff.get("end_timestamp");
                //Log.d(TAG, "prevAppDiffTimestampStr "+prevAppDiffTimestampStr);
                if (prevAppDiffTimestampStr != null && !prevAppDiffTimestampStr.equals("") && !prevAppDiffTimestampStr.equals("''")) {
                    prevAppDiffTimestamp = Long.valueOf(prevAppDiffTimestampStr);
                    //Log.d(TAG, "prevAppDiffTimestamp "+Long.toString(prevAppDiffTimestamp));
                }
            }
        }
        return prevAppDiffTimestamp;
    }
    private void notifySwitchToTestForDebug(){
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String currentDateandTime = sdf.format(new Date());
        String CHANNEL_ID = Constants.SMARTPHONE_USE_SWITCH_DEBUG_CID;
        NotificationManager mgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Testing Plugin", NotificationManager.IMPORTANCE_HIGH);
            mgr.createNotificationChannel(channel);
        }
        Notification notification;
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Test At "+currentDateandTime)
                .setContentText("Test At "+currentDateandTime).build();
        mgr.notify(Constants.SMARTPHONE_USE_NO_APP_USAGE_X_HOURS_ID, notification);
    }

    private void notifyNoAppDataForXHours(int X, boolean isEnabled){
        String CHANNEL_ID = Constants.SMARTPHONE_USE_NO_APP_USAGE_X_HOURS_CID;
        NotificationManager mgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Smartphone Use No App Usage", NotificationManager.IMPORTANCE_HIGH);
            mgr.createNotificationChannel(channel);
        }
        Notification notification;
        if (isEnabled) {
            notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getResources().getString(R.string.not_use_app_cn) + X + getResources().getString(R.string.hour_cn))
                    .setContentText(getResources().getString(R.string.crash_title_cn)).build();
        }
        else{
            notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getResources().getString(R.string.not_use_app_cn) + X + getResources().getString(R.string.hour_cn))
                    .setContentText(getResources().getString(R.string.enable_aware_cn)).build();
        }
        mgr.notify(Constants.SMARTPHONE_USE_NO_APP_USAGE_X_HOURS_ID, notification);
    }
    private void removeNotificationsOnAppReceive(){
        NotificationManager mgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        List<Integer> rmList = Arrays.asList(Constants.SMARTPHONE_USE_CRASH_NOTIFICATION_ID, Constants.SMARTPHONE_USE_NO_APP_USAGE_X_HOURS_ID);
        for (Integer rmEl : rmList) {
            mgr.cancel(rmEl);
        }
    }
    // DESTROY
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(pluginReceiver);
        //Turn off the sync-adapter if part of a study
        if (Aware.isStudy(this) && (getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone") || getApplicationContext().getResources().getBoolean(R.bool.standalone))) {
            ContentResolver.removePeriodicSync(
                    Aware.getAWAREAccount(this),
                    Provider.getAuthority(this),
                    Bundle.EMPTY
            );
        }
        complianceChecks.removeCallbacks(complianceChecksRunnable);
    }
}