package com.aware.smartphoneuse;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.os.Bundle;
import android.Manifest;
import android.view.View;
import android.content.Intent;

import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import android.content.IntentFilter;
import com.aware.ui.PermissionsHandler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.net.Uri;
import android.content.ContentResolver;
import android.app.ActivityManager;
import android.widget.TimePicker;
import android.widget.Toast;
import android.content.SharedPreferences;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

import android.util.Log;

import com.aware.Applications;
import com.aware.Locations;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.google.activity_recognition.Settings;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import com.github.anrwatchdog.ANRWatchDog;
import com.github.anrwatchdog.ANRError;



public class MainActivity extends AppCompatActivity {
    private String TAG = Constants.TAG;
    private String currentPhase = "NO_INTERVENTION";
    private long currentPhaseStartTime;
    Handler switchPhaseHandler = new Handler();

    private boolean PARTICIPANT_INSTALL = DebugConstants.PARTICIPANT_INSTALL;

    private Button join;
    private Button sync;

    private ArrayList<String> REQUIRED_PERMISSIONS;
    private JoinObserver joinObserver = new JoinObserver();

    Handler pluginWorkingHandler = new Handler();

    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(this));
        // Load correct phase!
        SharedPreferences phaseSettings = getApplicationContext().getSharedPreferences("phaseSetting", 0);
        String phaseVal = phaseSettings.getString("phase", "0");
        if((!phaseVal.equals("0"))){
            currentPhase = phaseVal;
        }
        if(!Constants.phases.contains(currentPhase))
            currentPhase = Constants.phases.get(0); // NO_INTERVENTION

        setContentView(R.layout.activity_main);
        // disable crash button for participants
        if (PARTICIPANT_INSTALL){
            Button simCrash = findViewById(R.id.crash);
            simCrash.setVisibility(View.INVISIBLE);
        }


        join = findViewById(R.id.join_study);
        sync = findViewById(R.id.sync_data);
        // To ensure the privacy of user, currently do not allow user to join into study, can be modified
        // to your own study link
        join.setVisibility(View.INVISIBLE);
        sync.setVisibility(View.INVISIBLE);
        phaseUIadjust(currentPhase);

        Button wlButton = (Button) findViewById(R.id.whitelistBtn);
        wlButton.setText(R.string.title_setting);
        wlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick for wlButton: clicked");
                final View wl_button_view = v;
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                // Load an xml layout file as a View object through LayoutInflater
                View alert_dialog_view = LayoutInflater.from(MainActivity.this).inflate(
                        R.layout.alert_dialog_whitelist_password_input, null);
                // Set the layout file defined by ourselves as the Content of the pop-up box
                builder.setView(alert_dialog_view);
                final EditText password = (EditText) alert_dialog_view
                        .findViewById(R.id.whitelist_password_text);
                builder.setPositiveButton(getResources().getText(R.string.confirm_time_cn),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Determine the content of the operation
                                String password_entered = password.getText().toString();
                                if(currentPhase.equals("NO_INTERVENTION") && password_entered.equals(Constants.WhiteListPwd1)) {
                                    launchSetup(wl_button_view);
                                }
                                else if(currentPhase.equals("MEANINGFUL_TEXT") && password_entered.equals(Constants.WhiteListPwd2)) {
                                    launchSetup(wl_button_view);
                                }
                                else if(currentPhase.equals("RANDOM_TEXT") && password_entered.equals(Constants.WhiteListPwd3)) {
                                    launchSetup(wl_button_view);
                                }
                                else if(currentPhase.equals("POP_UP_LAYOUT") && password_entered.equals(Constants.WhiteListPwd4)) {
                                    launchSetup(wl_button_view);
                                }
                                else if(currentPhase.equals("MEANINGFUL_AUDIO") && password_entered.equals(Constants.WhiteListPwd5)) {
                                    launchSetup(wl_button_view);
                                }
                                else {
                                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.wrong_password_cn), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                builder.setNegativeButton(getResources().getText(R.string.cancel_cn),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.dismiss();
                            }
                        });
                builder.show();
            }
        });

        //Since Android 5+ we need to check in runtime if the permissions were given, so we will check every time the user launches the main UI.
        REQUIRED_PERMISSIONS = new ArrayList<>();
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.GET_ACCOUNTS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SYNC_STATS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_PHONE_STATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_CALL_LOG);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SMS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            REQUIRED_PERMISSIONS.add(Manifest.permission.FOREGROUND_SERVICE);

        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Constants.consentForm[0]){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    // Load an xml layout file as a View object through LayoutInflater
                    View alert_dialog_view = LayoutInflater.from(MainActivity.this).inflate(
                            R.layout.alert_dialog_name_input, null);
                    // Set the layout file defined by ourselves as the Content of the pop-up box
                    builder.setView(alert_dialog_view);
                    final EditText username = (EditText) alert_dialog_view
                            .findViewById(R.id.edUsername);
                    builder.setPositiveButton(getResources().getText(R.string.confirm_time_cn),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Determine the content of the operation
                                    String user_name_entered = username.getText().toString();
                                    if(!user_name_entered.equals("")) {
                                        Toast toast=Toast.makeText(getApplicationContext(),getResources().getString(R.string.wait_cn),Toast.LENGTH_LONG);
                                        toast.show();

                                        Log.d(TAG, "joinStudy called!");

                                        // In fact, it is a link to the remote, and then you can read the remote settings for app settings
                                        // To let user join into the study, replace the link below and make it to not be a comment
                                        // Aware.joinStudy(getApplicationContext(), "https://big1.andrew.cmu.edu/");
                                        callSettings(user_name_entered);
                                    }
                                }
                            });
                    builder.show();
                }else {
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.fill_form_need_cn), Toast.LENGTH_LONG).show();
                }

            }
        });

        sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast toast=Toast.makeText(getApplicationContext(),"Running Sync!",Toast.LENGTH_SHORT);
                toast.show();
                Intent sync = new Intent(Aware.ACTION_AWARE_SYNC_DATA);
                sendBroadcast(sync);
                syncPluginNow(Provider.getAuthority(getApplicationContext()));
            }
        });
    }

    // initiate all the settings when "add to research"
    private void callSettings(String username){
        //Now let's set the settings we want for the study
        // TODO:: user_name2, which one to set
        Aware.setSetting(getApplicationContext(),Aware_Preferences.DEVICE_LABEL, username);
        // App
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_APPLICATIONS, true); //includes usage, and foreground
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_NOTIFICATIONS, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_CRASHES, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_APPLICATIONS, 30);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_KEYBOARD, 30);
        Log.d(TAG, "App settings done");

        // Activity Recognition settings
        Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, true);
//        Aware.setSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, 300);
        Log.d(TAG, "Activity recog settings done");

        // Google fused location
        Aware.setSetting(getApplicationContext(), com.aware.plugin.google.fused_location.Settings.STATUS_GOOGLE_FUSED_LOCATION, true);
        Aware.setSetting(getApplicationContext(), com.aware.plugin.google.fused_location.Settings.FREQUENCY_GOOGLE_FUSED_LOCATION, 300);
        Aware.setSetting(getApplicationContext(), com.aware.plugin.google.fused_location.Settings.MAX_FREQUENCY_GOOGLE_FUSED_LOCATION, 60);
        Aware.setSetting(getApplicationContext(), com.aware.plugin.google.fused_location.Settings.ACCURACY_GOOGLE_FUSED_LOCATION, 102);
        Aware.setSetting(getApplicationContext(), com.aware.plugin.google.fused_location.Settings.FALLBACK_LOCATION_TIMEOUT, 20);
        Aware.setSetting(getApplicationContext(), com.aware.plugin.google.fused_location.Settings.LOCATION_SENSITIVITY, 5);
        Log.d(TAG, "Fused loc settings done");

        // Battery
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_BATTERY, true);
        Log.d(TAG, "Battery settings done");

        // Communication
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_COMMUNICATION_EVENTS, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_CALLS, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_MESSAGES, true);
        Log.d(TAG, "Communication settings done");

        // Location
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_GPS, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_NETWORK, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_LOCATION_GPS, 180);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_LOCATION_NETWORK, 300);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.MIN_LOCATION_GPS_ACCURACY, 150);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.MIN_LOCATION_NETWORK_ACCURACY, 1500);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.LOCATION_EXPIRATION_TIME, 300);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_PASSIVE, false);
//        Aware.setSetting(getApplicationContext(), Aware_Preferences.LOCATION_SAVE_ALL, true);
        Log.d(TAG, "Location settings done");

        //Screen
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_SCREEN, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_TOUCH, true);
        Log.d(TAG, "Screen settings done");

        //Settings for data synching strategies
        Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_WIFI_ONLY, false); //only sync over wifi
        Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_WEBSERVICE, 30);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA, 0); // How frequently to clean old data? (0 = never, 1 = weekly, 2 = monthly, 3 = daily, 4 = always)
        Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_CHARGING, false);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SILENT, true); //don't show notifications of synching events
        Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_FALLBACK_NETWORK, 1); //after 1h without being able to use Wifi to sync, fallback to 3G for syncing.
        Aware.setSetting(getApplicationContext(), Aware_Preferences.REMIND_TO_CHARGE, true); //remind participants to charge their phone when reaching 15% of battery left
        Aware.setSetting(getApplicationContext(), Aware_Preferences.FOREGROUND_PRIORITY, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG, false);
        Log.d(TAG, "data sync settings done");

        //Plugins
        Aware.startPlugin(getApplicationContext(), "com.aware.plugin.google.activity_recognition");
        Log.d(TAG, "Activity recog plugin started");
        Aware.startPlugin(getApplicationContext(), "com.aware.plugin.google.fused_location");
        Log.d(TAG, "Fused loc plugin started");


        Aware.isBatteryOptimizationIgnored(getApplicationContext(), getPackageName());
        Applications.isAccessibilityServiceActive(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkSwitchPhaseRunnable.run();

        boolean permissions_ok = true;
        for (String p : REQUIRED_PERMISSIONS) { //loop to check all the required permissions.
            if (PermissionChecker.checkSelfPermission(this, p) != PermissionChecker.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        if (permissions_ok) {
            Log.d(TAG, "IS_CORE_RUNNING "+ Boolean.toString(Aware.IS_CORE_RUNNING));
            if (!Aware.IS_CORE_RUNNING) {
                Intent aware = new Intent(getApplicationContext(), Aware.class);
                startService(aware);

                Applications.isAccessibilityServiceActive(getApplicationContext());
                Aware.isBatteryOptimizationIgnored(getApplicationContext(), getPackageName());
            }
            boolean isLocServiceRunning = isMyServiceRunning(Locations.class);
            Log.d(TAG, "isLocServiceRunning "+ Boolean.toString(isLocServiceRunning));
            boolean isAppServiceRunning = isMyServiceRunning(Applications.class);
            Log.d(TAG, "isAppServiceRunning "+ Boolean.toString(isAppServiceRunning));
            if (Aware.isStudy(getApplicationContext())) {
                TextView welcome = findViewById(R.id.welcome);
                String device_id = Aware.getSetting(this, Aware_Preferences.DEVICE_ID);
                String user_name = Aware.getSetting(this,Aware_Preferences.DEVICE_LABEL);
                welcome.setText("Device ID: " + device_id + "\nUsername: " + user_name);

                join.setVisibility(View.INVISIBLE);
                sync.setVisibility(View.INVISIBLE);

            } else {
                IntentFilter joinFilter = new IntentFilter(Aware.ACTION_JOINED_STUDY);
                registerReceiver(joinObserver, joinFilter);

                //join.setVisibility(View.VISIBLE);
                join.setVisibility(View.INVISIBLE);
                sync.setVisibility(View.INVISIBLE);
            }
        }
        callSettings(Aware.getSetting(this,Aware_Preferences.DEVICE_LABEL));// without this, applications, etc stops working when server stops!
        askOverlayPermission();
        additionalIsStudyOperations();
        startPlugin();
        checkIfPluginIsRunning();
        pluginWorkingRunnable.run();
    }

    public void startPlugin(){
        Log.d(TAG, "inside startPlugin");
//         start Plugin whether or not user is enrolled in study
        if (!isMyServiceRunning(Plugin.class)) {
            Log.d(TAG, "Starting Plugin");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(this, Plugin.class).setAction(Constants.ACTION_FIRST_RUN_APPUSEPLUGIN).putExtra("phase",currentPhase));
            }
            else {
                startService(new Intent(this, Plugin.class).setAction(Constants.ACTION_FIRST_RUN_APPUSEPLUGIN).putExtra("phase",currentPhase));
            }
        }
    }

    private void additionalIsStudyOperations(){
        Log.d(TAG, "inside additionalIsStudyOperations");
        // this will run even if permissions are false!
        if (Aware.isStudy(getApplicationContext())) {
            join.setVisibility(View.INVISIBLE);
            sync.setVisibility(View.INVISIBLE);
        }
        else{
            join.setVisibility(View.INVISIBLE);
            sync.setVisibility(View.INVISIBLE);
        }
        activatePeriodicSyncForAwarePlugins();
    }

    private void askOverlayPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(getApplicationContext())) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            }
        }
    }
    private void checkPluginsSync(){
        String garAuthority = com.aware.plugin.google.activity_recognition.Google_AR_Provider.getAuthority(getApplicationContext());
        String fusedLocAuthority = com.aware.plugin.google.fused_location.Provider.getAuthority(getApplicationContext());
        Log.d(TAG, "garAuthority "+garAuthority);
        Log.d(TAG, "fusedLocAuthority "+fusedLocAuthority);
        boolean isSyncEnabled_gar = Aware.isSyncEnabled(getApplicationContext(), garAuthority);
        boolean isSyncEnabled_fusedLoc = Aware.isSyncEnabled(getApplicationContext(), fusedLocAuthority);
        Log.d(TAG, "isSyncEnabled_gar "+Boolean.toString(isSyncEnabled_gar));
        Log.d(TAG, "isSyncEnabled_fusedLoc "+Boolean.toString(isSyncEnabled_fusedLoc));
    }
    private void activatePeriodicSyncForAwarePlugins(){
        // activate periodic sync for GAR and Fused Loc
        Log.d(TAG, "inside activatePeriodicSyncForAwarePlugins");
        String garAuthority = com.aware.plugin.google.activity_recognition.Google_AR_Provider.getAuthority(getApplicationContext());
        String fusedLocAuthority = com.aware.plugin.google.fused_location.Provider.getAuthority(getApplicationContext());
        activatePeriodicSync(garAuthority);
        activatePeriodicSync(fusedLocAuthority);
    }
    private void activatePeriodicSync(String authority){
        Log.d(TAG, "inside activatePeriodicSync "+authority);
        boolean isSyncEnabled = Aware.isSyncEnabled(getApplicationContext(), authority);
        if (Aware.isStudy(getApplicationContext()) && !isSyncEnabled){
            ContentResolver.setIsSyncable(Aware.getAWAREAccount(getApplicationContext()), authority, 1);
            ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(getApplicationContext()), authority, true);
            ContentResolver.addPeriodicSync(
                    Aware.getAWAREAccount(getApplicationContext()),
                    authority,
                    Bundle.EMPTY,
                    Long.parseLong(Aware.getSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60
            );
        }
        boolean iSyncEnabled_after = Aware.isSyncEnabled(getApplicationContext(), authority);
        Log.d(TAG, authority+" Sync "+Boolean.toString(iSyncEnabled_after));
    }
    private void syncAllPluginsNow(){
        // Will sync GAR, Fused Loc, and our custom Plugin
        Log.d(TAG, "syncAllPluginsNow called");
        String garAuthority = com.aware.plugin.google.activity_recognition.Google_AR_Provider.getAuthority(getApplicationContext());
        String fusedLocAuthority = com.aware.plugin.google.fused_location.Provider.getAuthority(getApplicationContext());
        syncPluginNow(garAuthority);
        syncPluginNow(fusedLocAuthority);
        syncPluginNow(Provider.getAuthority(getApplicationContext()));

    }
    private void syncPluginNow(String authority){
        Log.d(TAG, "inside syncPluginNow");
        boolean isSyncEnabled = Aware.isSyncEnabled(getApplicationContext(), authority);
        if (Aware.isStudy(getApplicationContext()) && isSyncEnabled){
            Log.d(TAG, "syncPluginNow requesting sync for "+authority);
            Bundle syncB = new Bundle();
            syncB.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            syncB.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            ContentResolver.requestSync(Aware.getAWAREAccount(getApplicationContext()), authority, syncB);
        }
    }
    public void checkIfPluginIsRunning(){
        Log.d(TAG, "Checking if PLUGIN running");
        if (isMyServiceRunning(Plugin.class)) {
            Log.d(TAG, "PLUGIN still running");
        }
        else{
            Log.d(TAG, "PLUGIN not running");
        }
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
    private void notifyAfterCrash(){
        String CHANNEL_ID = Constants.SMARTPHONE_USE_CRASH_NOTIFICATION_CID;
        NotificationManager mgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Smartphone Use Post-Crash", NotificationManager.IMPORTANCE_HIGH);
            mgr.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getResources().getString(R.string.crash_title_cn))
                .setContentText(getResources().getString(R.string.crash_content_cn)).build();
        mgr.notify(Constants.SMARTPHONE_USE_CRASH_NOTIFICATION_ID, notification);
    }

    private void notifyBeforeDestroy(){
        String CHANNEL_ID = Constants.SMARTPHONE_USE_CRASH_NOTIFICATION_CID;
        NotificationManager mgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Smartphone Use Post-Destory", NotificationManager.IMPORTANCE_HIGH);
            mgr.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getResources().getString(R.string.kill_title_cn))
                .setContentText(getResources().getString(R.string.kill_content_cn)).build();
        mgr.notify(Constants.SMARTPHONE_USE_CRASH_NOTIFICATION_ID, notification);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notifyBeforeDestroy();
        Log.d(TAG, "MAINACTIVITY DESTROY");
//        pluginWorkingHandler.removeCallbacks(pluginWorkingRunnable);
    }
    public void forceCrash(View view) {
        throw new RuntimeException("This is a crash");
    }
    public void switchPhase(View view){
        EditText phasePwdBox   = (EditText)findViewById(R.id.phasepwd);
        String phasePwd = phasePwdBox.getText().toString();
        phasePwdBox.setText("");
        //five-phases::[NO_INTERVENTION,POP_UP_LAYOUT,MEANINGFUL_TEXT,RANDOM_TEXT,MEANINGFUL_AUDIO]
        if(phasePwd.equals(Constants.PhasePwd1)) {
            currentPhase = "NO_INTERVENTION";
            switchPhaseFunc(currentPhase, false);
        }
        else if(phasePwd.equals(Constants.PhasePwd2)) {
            currentPhase = "MEANINGFUL_TEXT";
            switchPhaseFunc(currentPhase, false);
        }
        else if(phasePwd.equals(Constants.PhasePwd3)) {
            currentPhase = "RANDOM_TEXT";
            switchPhaseFunc(currentPhase, false);
        }
        else if(phasePwd.equals(Constants.PhasePwd4)) {
            currentPhase = "POP_UP_LAYOUT";
            switchPhaseFunc(currentPhase, false);
        }
        else if(phasePwd.equals(Constants.PhasePwd5)) {
            currentPhase = "MEANINGFUL_AUDIO";
            switchPhaseFunc(currentPhase, false);
        }
        else {
            Toast toast=Toast.makeText(getApplicationContext(), getResources().getString(R.string.wrong_password_cn),Toast.LENGTH_LONG);
            toast.show();
        }
    }
    private String phaseUIadjust(String currentPhase){
        //Update view
        TextView phaseStatus = (TextView)findViewById(R.id.phaseStatus);
        String phaseTxt;
        if (currentPhase.equals("NO_INTERVENTION")) {
            phaseTxt = getResources().getString(R.string.running_cn) + " " + Constants.phases_cn.get(Constants.phases.indexOf(currentPhase)) + " " + getResources().getString(R.string.no_notifications_cn);
        }
        else{
            phaseTxt = getResources().getString(R.string.running_cn) + " " + Constants.phases_cn.get(Constants.phases.indexOf(currentPhase)) + " " + getResources().getString(R.string.have_notifications_cn);
        }
        phaseStatus.setText(phaseTxt);
        return (phaseTxt);
    }
    private void switchPhaseFunc(String currentPhase, boolean auto){
        currentPhaseStartTime = System.currentTimeMillis();
        String phaseTxt = phaseUIadjust(currentPhase);
        if (!auto) {
            Toast toast = Toast.makeText(getApplicationContext(), phaseTxt, Toast.LENGTH_LONG);
            toast.show();
        }
        // Broadcast
        Intent switchPhase = new Intent(Constants.ACTION_SWITCH_PHASE);
        switchPhase.putExtra("newPhase", currentPhase);
        if (auto) {
            switchPhase.putExtra("switchType", "auto");
        }
        else {
            switchPhase.putExtra("switchType", "manual");
        }
        sendBroadcast(switchPhase);
        // Set correct phase
        SharedPreferences settings = getSharedPreferences("phaseSetting", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("phase", currentPhase);
        editor.commit();
    }
//    private void logUserFabric(String device_id){
//        Crashlytics.setUserIdentifier(device_id);
//    }
    private class JoinObserver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Aware.ACTION_JOINED_STUDY)) {

                unregisterReceiver(joinObserver);

                finish();

                Intent relaunch = new Intent(context, MainActivity.class);
                startActivity(relaunch);
            }
        }
    }
    public void launchWhitelist(View view){
        Toast toast=Toast.makeText(getApplicationContext(),getResources().getString(R.string.wait_cn),Toast.LENGTH_SHORT);
        toast.show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent myIntent = new Intent(MainActivity.this, WhitelistActivity.class);
                myIntent.putExtra("phase", currentPhase); //Optional parameters
                MainActivity.this.startActivity(myIntent);
            }
        }, 2000);

    }

    public void launchSetup(View view){
        Toast toast=Toast.makeText(getApplicationContext(),getResources().getString(R.string.wait_cn),Toast.LENGTH_SHORT);
        toast.show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent myIntent = new Intent(MainActivity.this, SetupActivity.class);
                myIntent.putExtra("phase", currentPhase); //Optional parameters
                MainActivity.this.startActivity(myIntent);
            }
        }, 0);

    }
    Runnable checkSwitchPhaseRunnable = new Runnable() {
        public void run() {
            int oneHr = 60*60*1000;
            int defaultTimer = 6*oneHr; // 6 hours
            int timeLimit = 1*24*oneHr;

            SharedPreferences settings = getApplicationContext().getSharedPreferences("switchPhaseInfo", 0);
            String switched_to = settings.getString("switched_to", "0");
            if((!switched_to.equals("0"))){
                long at_time = settings.getLong("at_time", 0);
                if ((at_time != 0) && switched_to.equals("RANDOMIZATION_TRIAL") && ((System.currentTimeMillis() - at_time) > timeLimit)){
                    currentPhase = "RANDOMIZATION";
                    switchPhaseFunc("RANDOMIZATION", true);
                    Log.d(TAG, "Automatic switch RANDOMIZATION_TRIAL --> RANDOMIZATION");
                }
            }

            // Delay in checking for compliance
            switchPhaseHandler.postDelayed(checkSwitchPhaseRunnable, defaultTimer); // update every 30 minutes
        }
    };
    private void keepPluginWorking(){
        Log.d(TAG, "Keep Plugin Working");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, Plugin.class).setAction(Constants.ACTION_ENSURE_PLUGIN_WORKING).putExtra("phase",currentPhase));
//            moveTaskToBack(true);
        }
        else {
            startService(new Intent(this, Plugin.class).setAction(Constants.ACTION_ENSURE_PLUGIN_WORKING).putExtra("phase",currentPhase));
//            moveTaskToBack(true);
        }
    }
    Runnable pluginWorkingRunnable = new Runnable() {
        public void run() {
            Log.d(TAG, "Running pluginWorkingRunnable");
            if (isMyServiceRunning(Plugin.class)) {
                Log.d(TAG, "Calling keepPluginWorking()");
                keepPluginWorking();
            }
            pluginWorkingHandler.postDelayed(pluginWorkingRunnable, 10*60*1000);
        }
    };

    public void invoke_notification_setting(View view) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getApplicationContext().getPackageName());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", getApplicationContext().getPackageName());
            intent.putExtra("app_uid", getApplicationContext().getApplicationInfo().uid);
        } else {
            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
        }
        this.startActivity(intent);
    }

    public void invoke_fill_form(View view) {
        Log.d(TAG, "onClick for wlButton: clicked");
        final View wl_button_view = view;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        // 通过LayoutInflater来加载一个xml的布局文件作为一个View对象
        View alert_dialog_view = LayoutInflater.from(MainActivity.this).inflate(
                R.layout.alert_dialog_fill_form, null);
        // 设置我们自己定义的布局文件作为弹出框的Content
        builder.setView(alert_dialog_view);
        builder.setPositiveButton(getResources().getText(R.string.confirm_time_cn),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //确定操作的内容
                        Constants.consentForm[0] = true;
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.fill_form_done_cn), Toast.LENGTH_LONG).show();
                    }
                });
        builder.setNegativeButton(getResources().getText(R.string.refuse_cn),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Constants.consentForm[0] = false;
//                                dialog.dismiss();
                    }
                });
        builder.show();
    }

    public void ask_for_permission(View view) {
        self_permission_check(true);
    }

    private void self_permission_check(boolean wakeup_notification_flag) {
        String[] permissions = new String[] {
//                android.Manifest.permission.INTERNET,
//                android.Manifest.permission.VIBRATE,
//                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
                android.Manifest.permission.READ_PHONE_STATE,
//                android.Manifest.permission.ACCESS_FINE_LOCATION,
//                android.Manifest.permission.ACCESS_COARSE_LOCATION,
//                android.Manifest.permission.ACCESS_WIFI_STATE,
//                android.Manifest.permission.CHANGE_WIFI_STATE,
//                android.Manifest.permission.ACCESS_NETWORK_STATE,
//                android.Manifest.permission.CHANGE_NETWORK_STATE,
//                android.Manifest.permission.BLUETOOTH,
//                android.Manifest.permission.BLUETOOTH_ADMIN,
//                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_SYNC_SETTINGS,
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.READ_CALL_LOG,
                android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
                android.Manifest.permission.ACCESS_NOTIFICATION_POLICY,
//                android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
        };
        boolean every_thing_allowed = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "self_permission_check: permission="+permission);
                ActivityCompat.requestPermissions(this,new String[]{permission},1);
                every_thing_allowed = false;
                break;
            }
        }

        if(every_thing_allowed) {
            Toast.makeText(this,getResources().getText(R.string.permission_succeed_cn),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==1) {
            if(grantResults.length > 0 && !(grantResults[0]==PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this,getResources().getText(R.string.permission_failed_cn),Toast.LENGTH_LONG).show();
            }
            else {
                // recursively ask for all the permissions
                self_permission_check(false);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @SuppressLint("NewApi")
    public static boolean isNotificationEnabled(Context context) {

        AppOpsManager mAppOps =
                (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        ApplicationInfo appInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = appInfo.uid;
        Class appOpsClass = null;

        /* Context.APP_OPS_MANAGER */
        try {
            appOpsClass = Class.forName(AppOpsManager.class.getName());

            Method checkOpNoThrowMethod =
                    appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE, String.class);

            Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);
            int value = (Integer) opPostNotificationValue.get(Integer.class);

            return ((Integer) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) ==
                    AppOpsManager.MODE_ALLOWED);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
