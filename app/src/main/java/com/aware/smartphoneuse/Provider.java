package com.aware.smartphoneuse;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.Nullable;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;



public class Provider extends ContentProvider {
    public static String AUTHORITY = "com.aware.smartphoneuse.provider.applicationsdiff";
    public static final int DATABASE_VERSION = 1;
    public static String DATABASE_NAME = "plugin_applications_diff.db";
    public static String TAG = Constants.TAG;

    //Add here your database table names, as many as you need
    public static final String[] DATABASE_TABLES = {
            "applications_diff",
            "applications_interv",
            "applications_interv_no_permit",
            "applications_interv_app_end",
            "applications_switch_permit",
            "applications_summary",
            "applications_whitelist",
            "applications_phases"
    };
    private static final int APPDIFF = 1;
    private static final int APPDIFF_ID = 2;
    private static final int APPINTERV = 3;
    private static final int APPINTERV_ID = 4;
    private static final int APPINTERVNOPERMIT = 5;
    private static final int APPINTERVNOPERMIT_ID = 6;
    private static final int APPINTERVAPPEND = 7;
    private static final int APPINTERVAPPEND_ID = 8;
    private static final int APPPERMIT = 9;
    private static final int APPPERMIT_ID = 10;
    private static final int APPSUMMARY = 11;
    private static final int APPSUMMARY_ID = 12;
    private static final int APPWL = 13;
    private static final int APPWL_ID = 14;
    private static final int APPPHASES = 15;
    private static final int APPPHASES_ID = 16;
    // These are the columns that we need to sync data, don't change this!
    public interface AWAREColumns extends BaseColumns {
        String _ID = "_id";
        String TIMESTAMP = "timestamp";
        String DEVICE_ID = "device_id";
    }
    public static final class Applications_Diff implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/applications_diff");
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_diff"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_diff"; //modify me
        static final String PACKAGE_NAME = "package_name";
        static final String APP_NAME = "application_name";
        static final String IS_SYS_APP = "is_system_app";
        static final String END_TIMESTAMP_DAY = "end_timestamp_day";
        static final String END_TIMESTAMP = "end_timestamp";
        static final String TIME_SPENT = "time_spent";
        static final String TIME_SPENT_TODAY = "time_spent_today";
        static final String IS_LAUNCHER = "is_launcher";
    }
    public static final class Applications_Interv implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/applications_interv");
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_interv"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_interv"; //modify me
        static final String PACKAGE_NAME = "package_name";
        static final String APP_NAME = "application_name";
        static final String IS_SYS_APP = "is_system_app";
        static final String IS_LAUNCHER = "is_launcher";
        static final String START_TIMESTAMP = "start_timestamp";
        static final String TIME_TO_TIMEOUT = "time_to_timeout";
        static final String TIME_TODAY_TO_TIMEOUT = "time_today_to_timeout";
        static final String INTERV_RESULT = "interventionResult";
        static final String TEMPT_SUCCEEDED = "tempt_succeeded";
        static final String PERSUASIVE_TEXT = "persuasive_text";
        static final String USER_INPUT = "user_input";
        static final String OVERLAY_TYPE = "OverlayType";
        static final String START_OVERLAY_TIMESTAMP = "start_overlay_timestamp";
        static final String END_OVERLAY_TIMESTAMP = "end_overlay_timestamp";
        static final String DISMISS_TIMESTAMP = "dismiss_timestamp";
        static final String INTERV_POLICY = "interv_policy";
        static final String PHASE = "phase";
    }
    public static final class Applications_Interv_No_Permissions implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/applications_interv_no_permit");
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_interv_no_permit"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_interv_no_permit"; //modify me
        static final String PACKAGE_NAME = "package_name";
        static final String APP_NAME = "application_name";
        static final String IS_SYS_APP = "is_system_app";
        static final String IS_LAUNCHER = "is_launcher";
        static final String START_TIMESTAMP = "start_timestamp";
        static final String TIME_TO_TIMEOUT = "time_to_timeout";
        static final String TIME_TODAY_TO_TIMEOUT = "time_today_to_timeout";
//        static final String INTERV_RESULT = "interventionResult";
//        static final String OVERLAY_TYPE = "OverlayType";
//        static final String START_OVERLAY_TIMESTAMP = "start_overlay_timestamp";
//        static final String END_OVERLAY_TIMESTAMP = "end_overlay_timestamp";
        static final String DISMISS_TIMESTAMP = "dismiss_timestamp";
        static final String INTERV_POLICY = "interv_policy";
        static final String PHASE = "phase";
    }
    public static final class Applications_Interv_App_End implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/applications_interv_app_end");
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_interv_app_end"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_interv_app_end"; //modify me
        static final String PACKAGE_NAME = "package_name";
        static final String APP_NAME = "application_name";
        static final String IS_SYS_APP = "is_system_app";
        static final String IS_LAUNCHER = "is_launcher";
        static final String START_TIMESTAMP = "start_timestamp";
        static final String TIME_TO_TIMEOUT = "time_to_timeout";
        static final String TIME_TODAY_TO_TIMEOUT = "time_today_to_timeout";
        static final String INTERV_RESULT = "interventionResult";
        static final String TEMPT_SUCCEEDED = "tempt_succeeded";
        static final String PERSUASIVE_TEXT = "persuasive_text";
        static final String USER_INPUT = "user_input";
        static final String OVERLAY_TYPE = "OverlayType";
        static final String START_OVERLAY_TIMESTAMP = "start_overlay_timestamp";
        static final String END_OVERLAY_TIMESTAMP = "end_overlay_timestamp";
        static final String DISMISS_TIMESTAMP = "dismiss_timestamp";
        static final String END_TIMESTAMP = "end_timestamp";
        static final String TIME_TO_QUIT = "time_to_quit";
        static final String TOTAL_TIMEOUTS = "total_timeouts";
        static final String NUM_TIMEOUTS_BEFORE = "num_timeouts_before";
        static final String NUM_TIMEOUTS_AFTER = "num_timeouts_after";
        static final String INTERV_POLICY = "interv_policy";
        static final String PHASE = "phase";
    }
    public static final class Applications_Switch_Permit implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/applications_switch_permit");
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_switch_permit"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_switch_permit"; //modify me
        static final String TIMESTAMP_READ = "timestamp_read";
        static final String PREV_IS_ENABLED = "prev_is_enabled";
        static final String CURR_IS_ENABLED = "curr_is_enabled";
    }
    public static final class Applications_Summary implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/applications_summary");
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_summary"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_summary"; //modify me
        static final String TIMESTAMP_DAY = "timestamp_day";
        static final String TIMESTAMP_READ = "timestamp_read";
        static final String PACKAGE_NAME = "package_name";
        static final String CURRENT_PHASE = "currentPhase";
        static final String MIN_DURATION = "min_duration";
        static final String MAX_DURATION = "max_duration";
        static final String SUM_DURATION = "sum_duration";
        static final String COUNT_SESSIONS = "count_sessions";
    }
    public static final class Applications_Whitelist implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/applications_whitelist");
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_whitelist"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_whitelist"; //modify me
        static final String TIMESTAMP_READ = "timestamp_read";
        static final String PACKAGE_NAME = "package_name";
        static final String WHITELIST = "whitelist";
        static final String CURRENT = "current";
        static final String TIME = "time";
        static final String VALUE = "value";
        static final String WECHAT = "wechat";
    }
    public static final class Applications_Phases implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/applications_phases");
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_phases"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.smartphoneuse.provider.applications_phases"; //modify me
        static final String TIMESTAMP_READ = "timestamp_read";
        static final String OLD_PHASE = "old_phase";
        static final String NEW_PHASE = "new_phase";
        static final String SWITCH_TYPE = "switch_type";
    }
    //Define each database table fields
    public static final String[] TABLES_FIELDS = {
            Applications_Diff._ID + " integer primary key autoincrement," +
                    Applications_Diff.TIMESTAMP + " real default 0," +
                    Applications_Diff.DEVICE_ID + " text default ''," +
                    Applications_Diff.PACKAGE_NAME + " text default ''," +
                    Applications_Diff.APP_NAME + " text default ''," +
                    Applications_Diff.IS_SYS_APP + " text default ''," +
                    Applications_Diff.END_TIMESTAMP_DAY + " text default ''," +
                    Applications_Diff.END_TIMESTAMP + " text default ''," +
                    Applications_Diff.TIME_SPENT + " text default ''," +
                    Applications_Diff.TIME_SPENT_TODAY + " text default ''," +
                    Applications_Diff.IS_LAUNCHER + " text default ''",
            Applications_Interv._ID + " integer primary key autoincrement," +
                    Applications_Interv.TIMESTAMP + " real default 0," +
                    Applications_Interv.DEVICE_ID + " text default ''," +
                    Applications_Interv.PACKAGE_NAME + " text default ''," +
                    Applications_Interv.APP_NAME + " text default ''," +
                    Applications_Interv.IS_SYS_APP + " text default ''," +
                    Applications_Interv.IS_LAUNCHER + " text default '',"  +
                    Applications_Interv.START_TIMESTAMP + " text default ''," +
                    Applications_Interv.TIME_TO_TIMEOUT + " text default ''," +
                    Applications_Interv.TIME_TODAY_TO_TIMEOUT + " text default ''," +
                    Applications_Interv.INTERV_RESULT + " text default ''," +
                    Applications_Interv.TEMPT_SUCCEEDED + " text default ''," +
                    Applications_Interv.PERSUASIVE_TEXT + " text default ''," +
                    Applications_Interv.USER_INPUT + " text default ''," +
                    Applications_Interv.OVERLAY_TYPE + " text default ''," +
                    Applications_Interv.START_OVERLAY_TIMESTAMP + " text default ''," +
                    Applications_Interv.END_OVERLAY_TIMESTAMP + " text default ''," +
                    Applications_Interv.DISMISS_TIMESTAMP + " text default ''," +
                    Applications_Interv.INTERV_POLICY + " text default ''," +
                    Applications_Interv.PHASE + " text default ''",
            Applications_Interv_No_Permissions._ID + " integer primary key autoincrement," +
                    Applications_Interv_No_Permissions.TIMESTAMP + " real default 0," +
                    Applications_Interv_No_Permissions.DEVICE_ID + " text default ''," +
                    Applications_Interv_No_Permissions.PACKAGE_NAME + " text default ''," +
                    Applications_Interv_No_Permissions.APP_NAME + " text default ''," +
                    Applications_Interv_No_Permissions.IS_SYS_APP + " text default ''," +
                    Applications_Interv_No_Permissions.IS_LAUNCHER + " text default '',"  +
                    Applications_Interv_No_Permissions.START_TIMESTAMP + " text default ''," +
                    Applications_Interv_No_Permissions.TIME_TO_TIMEOUT + " text default ''," +
                    Applications_Interv_No_Permissions.TIME_TODAY_TO_TIMEOUT + " text default ''," +
                    Applications_Interv.DISMISS_TIMESTAMP + " text default ''," +
                    Applications_Interv.INTERV_POLICY + " text default ''," +
                    Applications_Interv.PHASE + " text default ''",
            Applications_Interv_App_End._ID + " integer primary key autoincrement," +
                    Applications_Interv_App_End.TIMESTAMP + " real default 0," +
                    Applications_Interv_App_End.DEVICE_ID + " text default ''," +
                    Applications_Interv_App_End.PACKAGE_NAME + " text default ''," +
                    Applications_Interv_App_End.APP_NAME + " text default ''," +
                    Applications_Interv_App_End.IS_SYS_APP + " text default ''," +
                    Applications_Interv_App_End.IS_LAUNCHER + " text default '',"  +
                    Applications_Interv_App_End.START_TIMESTAMP + " text default ''," +
                    Applications_Interv_App_End.TIME_TO_TIMEOUT + " text default ''," +
                    Applications_Interv_App_End.TIME_TODAY_TO_TIMEOUT + " text default ''," +
                    Applications_Interv_App_End.INTERV_RESULT + " text default ''," +
                    Applications_Interv_App_End.TEMPT_SUCCEEDED + " text default ''," +
                    Applications_Interv_App_End.PERSUASIVE_TEXT + " text default ''," +
                    Applications_Interv_App_End.USER_INPUT + " text default ''," +
                    Applications_Interv_App_End.OVERLAY_TYPE + " text default ''," +
                    Applications_Interv_App_End.START_OVERLAY_TIMESTAMP + " text default ''," +
                    Applications_Interv_App_End.END_OVERLAY_TIMESTAMP + " text default ''," +
                    Applications_Interv_App_End.DISMISS_TIMESTAMP + " text default ''," +
                    Applications_Interv_App_End.END_TIMESTAMP + " text default ''," +
                    Applications_Interv_App_End.TIME_TO_QUIT + " text default ''," +
                    Applications_Interv_App_End.TOTAL_TIMEOUTS + " text default ''," +
                    Applications_Interv_App_End.NUM_TIMEOUTS_BEFORE + " text default ''," +
                    Applications_Interv_App_End.NUM_TIMEOUTS_AFTER + " text default ''," +
                    Applications_Interv_App_End.INTERV_POLICY + " text default ''," +
                    Applications_Interv_App_End.PHASE + " text default ''",
            Applications_Switch_Permit._ID + " integer primary key autoincrement," +
                    Applications_Switch_Permit.TIMESTAMP + " real default 0," +
                    Applications_Switch_Permit.TIMESTAMP_READ + " text default ''," +
                    Applications_Switch_Permit.DEVICE_ID + " text default ''," +
                    Applications_Switch_Permit.PREV_IS_ENABLED + " text default ''," +
                    Applications_Switch_Permit.CURR_IS_ENABLED + " text default ''" ,
            Applications_Summary._ID + " integer primary key autoincrement," +
                    Applications_Summary.TIMESTAMP_DAY + " test default ''," +
                    Applications_Summary.TIMESTAMP + " real default 0," +
                    Applications_Summary.TIMESTAMP_READ + " text default ''," +
                    Applications_Summary.DEVICE_ID + " text default ''," +
                    Applications_Summary.PACKAGE_NAME + " text default ''," +
                    Applications_Summary.CURRENT_PHASE + " text default ''," +
                    Applications_Summary.MIN_DURATION + " text default ''," +
                    Applications_Summary.MAX_DURATION + " text default ''," +
                    Applications_Summary.SUM_DURATION + " text default ''," +
                    Applications_Summary.COUNT_SESSIONS + " text default ''",
            Applications_Whitelist._ID + " integer primary key autoincrement," +
                    Applications_Whitelist.TIMESTAMP + " real default 0," +
                    Applications_Whitelist.TIMESTAMP_READ + " text default ''," +
                    Applications_Whitelist.DEVICE_ID + " text default ''," +
                    Applications_Whitelist.PACKAGE_NAME + " text default ''," +
                    Applications_Whitelist.WHITELIST + " text default ''," +
                    Applications_Whitelist.CURRENT + " text default ''," +
                    Applications_Whitelist.TIME + " text default ''," +
                    Applications_Whitelist.VALUE + " text default ''," +
                    Applications_Whitelist.WECHAT + " text default ''",
            Applications_Phases._ID + " integer primary key autoincrement," +
                    Applications_Phases.TIMESTAMP + " real default 0," +
                    Applications_Phases.TIMESTAMP_READ + " text default ''," +
                    Applications_Phases.DEVICE_ID + " text default ''," +
                    Applications_Phases.OLD_PHASE + " text default ''," +
                    Applications_Phases.NEW_PHASE + " text default ''," +
                    Applications_Phases.SWITCH_TYPE + " text default ''",
    };

    //Helper variables for ContentProvider - DO NOT CHANGE
    private UriMatcher sUriMatcher = null;
    private DatabaseHelper dbHelper = null;
    private static SQLiteDatabase database = null;
    private boolean initialiseDatabase() {
        //Log.d(TAG, "Initialise DB called");
        if (dbHelper == null)
            dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if (database == null) {
            database = dbHelper.getWritableDatabase();
        }
        return (database != null && dbHelper != null);
    }
    //For each table, create a hashmap needed for database queries
    private HashMap<String, String> ApplicationsDiffHash = null;
    private HashMap<String, String> ApplicationsIntervHash = null;
    private HashMap<String, String> ApplicationsIntervNoPermitHash = null;
    private HashMap<String, String> ApplicationsIntervAppEndHash = null;
    private HashMap<String, String> ApplicationsSwitchPermitHash = null;
    private HashMap<String, String> ApplicationsSummaryHash = null;
    private HashMap<String, String> ApplicationsWhitelistHash = null;
    private HashMap<String, String> ApplicationsPhasesHash = null;

    /**
     * Returns the provider authority that is dynamic
     * @return
     */
    public static String getAuthority(Context context) {
        AUTHORITY = context.getPackageName() + ".provider.applicationsdiff";
        return AUTHORITY;
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "ProviderOnCreate");
        //This is a hack to allow providers to be reusable in any application/plugin by making the authority dynamic using the package name of the parent app
        AUTHORITY = getContext().getPackageName() + ".provider.applicationsdiff";

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        //For each table, add indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], APPDIFF);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", APPDIFF_ID);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1], APPINTERV);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1] + "/#", APPINTERV_ID);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[2], APPINTERVNOPERMIT);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[2] + "/#", APPINTERVNOPERMIT_ID);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[3], APPINTERVAPPEND);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[3] + "/#", APPINTERVAPPEND_ID);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[4], APPPERMIT);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[4] + "/#", APPPERMIT_ID);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[5], APPSUMMARY);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[5] + "/#", APPSUMMARY_ID);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[6], APPWL);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[6] + "/#", APPWL_ID);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[7], APPPHASES);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[7] + "/#", APPPHASES_ID);

        //Create each table hashmap so Android knows how to insert data to the database. Put ALL table fields.
        ApplicationsDiffHash = new HashMap<>();
        ApplicationsDiffHash.put(Applications_Diff._ID, Applications_Diff._ID);
        ApplicationsDiffHash.put(Applications_Diff.TIMESTAMP, Applications_Diff.TIMESTAMP);
        ApplicationsDiffHash.put(Applications_Diff.DEVICE_ID, Applications_Diff.DEVICE_ID);
        ApplicationsDiffHash.put(Applications_Diff.PACKAGE_NAME, Applications_Diff.PACKAGE_NAME);
        ApplicationsDiffHash.put(Applications_Diff.APP_NAME, Applications_Diff.APP_NAME);
        ApplicationsDiffHash.put(Applications_Diff.IS_SYS_APP, Applications_Diff.IS_SYS_APP);
        ApplicationsDiffHash.put(Applications_Diff.END_TIMESTAMP_DAY, Applications_Diff.END_TIMESTAMP_DAY);
        ApplicationsDiffHash.put(Applications_Diff.END_TIMESTAMP, Applications_Diff.END_TIMESTAMP);
        ApplicationsDiffHash.put(Applications_Diff.TIME_SPENT, Applications_Diff.TIME_SPENT);
        ApplicationsDiffHash.put(Applications_Diff.TIME_SPENT_TODAY, Applications_Diff.TIME_SPENT_TODAY);
        ApplicationsDiffHash.put(Applications_Diff.IS_LAUNCHER, Applications_Diff.IS_LAUNCHER);

        ApplicationsIntervHash = new HashMap<>();
        ApplicationsIntervHash.put(Applications_Interv._ID, Applications_Interv._ID);
        ApplicationsIntervHash.put(Applications_Interv.TIMESTAMP, Applications_Interv.TIMESTAMP);
        ApplicationsIntervHash.put(Applications_Interv.DEVICE_ID, Applications_Interv.DEVICE_ID);
        ApplicationsIntervHash.put(Applications_Interv.PACKAGE_NAME, Applications_Interv.PACKAGE_NAME);
        ApplicationsIntervHash.put(Applications_Interv.APP_NAME, Applications_Interv.APP_NAME);
        ApplicationsIntervHash.put(Applications_Interv.IS_SYS_APP, Applications_Interv.IS_SYS_APP);
        ApplicationsIntervHash.put(Applications_Interv.IS_LAUNCHER, Applications_Interv.IS_LAUNCHER);
        ApplicationsIntervHash.put(Applications_Interv.START_TIMESTAMP, Applications_Interv.START_TIMESTAMP);
        ApplicationsIntervHash.put(Applications_Interv.TIME_TO_TIMEOUT, Applications_Interv.TIME_TO_TIMEOUT);
        ApplicationsIntervHash.put(Applications_Interv.TIME_TODAY_TO_TIMEOUT, Applications_Interv.TIME_TODAY_TO_TIMEOUT);
        ApplicationsIntervHash.put(Applications_Interv.INTERV_RESULT, Applications_Interv.INTERV_RESULT);
        ApplicationsIntervHash.put(Applications_Interv.TEMPT_SUCCEEDED, Applications_Interv.TEMPT_SUCCEEDED);
        ApplicationsIntervHash.put(Applications_Interv.PERSUASIVE_TEXT, Applications_Interv.PERSUASIVE_TEXT);
        ApplicationsIntervHash.put(Applications_Interv.USER_INPUT, Applications_Interv.USER_INPUT);
        ApplicationsIntervHash.put(Applications_Interv.OVERLAY_TYPE, Applications_Interv.OVERLAY_TYPE);
        ApplicationsIntervHash.put(Applications_Interv.START_OVERLAY_TIMESTAMP, Applications_Interv.START_OVERLAY_TIMESTAMP);
        ApplicationsIntervHash.put(Applications_Interv.END_OVERLAY_TIMESTAMP, Applications_Interv.END_OVERLAY_TIMESTAMP);
        ApplicationsIntervHash.put(Applications_Interv.DISMISS_TIMESTAMP, Applications_Interv.DISMISS_TIMESTAMP);
        ApplicationsIntervHash.put(Applications_Interv.INTERV_POLICY, Applications_Interv.INTERV_POLICY);
        ApplicationsIntervHash.put(Applications_Interv.PHASE, Applications_Interv.PHASE);

        ApplicationsIntervNoPermitHash = new HashMap<>();
        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions._ID, Applications_Interv_No_Permissions._ID);
        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.TIMESTAMP, Applications_Interv_No_Permissions.TIMESTAMP);
        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.DEVICE_ID, Applications_Interv_No_Permissions.DEVICE_ID);
        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.PACKAGE_NAME, Applications_Interv_No_Permissions.PACKAGE_NAME);
        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.APP_NAME, Applications_Interv_No_Permissions.APP_NAME);
        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.IS_SYS_APP, Applications_Interv_No_Permissions.IS_SYS_APP);
        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.IS_LAUNCHER, Applications_Interv_No_Permissions.IS_LAUNCHER);
        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.START_TIMESTAMP, Applications_Interv_No_Permissions.START_TIMESTAMP);
        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.TIME_TO_TIMEOUT, Applications_Interv_No_Permissions.TIME_TO_TIMEOUT);
        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.TIME_TODAY_TO_TIMEOUT, Applications_Interv_No_Permissions.TIME_TODAY_TO_TIMEOUT);
//        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.INTERV_RESULT, Applications_Interv_No_Permissions.INTERV_RESULT);
//        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.OVERLAY_TYPE, Applications_Interv_No_Permissions.OVERLAY_TYPE);
//        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.START_OVERLAY_TIMESTAMP, Applications_Interv_No_Permissions.START_OVERLAY_TIMESTAMP);
//        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.END_OVERLAY_TIMESTAMP, Applications_Interv_No_Permissions.END_OVERLAY_TIMESTAMP);
        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.DISMISS_TIMESTAMP, Applications_Interv_No_Permissions.DISMISS_TIMESTAMP);
        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.INTERV_POLICY, Applications_Interv_No_Permissions.INTERV_POLICY);
        ApplicationsIntervNoPermitHash.put(Applications_Interv_No_Permissions.PHASE, Applications_Interv_No_Permissions.PHASE);

        ApplicationsIntervAppEndHash = new HashMap<>();
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End._ID, Applications_Interv_App_End._ID);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.TIMESTAMP, Applications_Interv_App_End.TIMESTAMP);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.DEVICE_ID, Applications_Interv_App_End.DEVICE_ID);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.PACKAGE_NAME, Applications_Interv_App_End.PACKAGE_NAME);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.APP_NAME, Applications_Interv_App_End.APP_NAME);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.IS_SYS_APP, Applications_Interv_App_End.IS_SYS_APP);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.IS_LAUNCHER, Applications_Interv_App_End.IS_LAUNCHER);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.START_TIMESTAMP, Applications_Interv_App_End.START_TIMESTAMP);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.TIME_TO_TIMEOUT, Applications_Interv_App_End.TIME_TO_TIMEOUT);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.TIME_TODAY_TO_TIMEOUT, Applications_Interv_App_End.TIME_TODAY_TO_TIMEOUT);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.INTERV_RESULT, Applications_Interv_App_End.INTERV_RESULT);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.TEMPT_SUCCEEDED, Applications_Interv_App_End.TEMPT_SUCCEEDED);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.PERSUASIVE_TEXT, Applications_Interv_App_End.PERSUASIVE_TEXT);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.USER_INPUT, Applications_Interv_App_End.USER_INPUT);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.OVERLAY_TYPE, Applications_Interv_App_End.OVERLAY_TYPE);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.START_OVERLAY_TIMESTAMP, Applications_Interv_App_End.START_OVERLAY_TIMESTAMP);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.END_OVERLAY_TIMESTAMP, Applications_Interv_App_End.END_OVERLAY_TIMESTAMP);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.DISMISS_TIMESTAMP, Applications_Interv_App_End.DISMISS_TIMESTAMP);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.END_TIMESTAMP, Applications_Interv_App_End.END_TIMESTAMP);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.TIME_TO_QUIT, Applications_Interv_App_End.TIME_TO_QUIT);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.TOTAL_TIMEOUTS, Applications_Interv_App_End.TOTAL_TIMEOUTS);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.NUM_TIMEOUTS_BEFORE, Applications_Interv_App_End.NUM_TIMEOUTS_BEFORE);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.NUM_TIMEOUTS_AFTER, Applications_Interv_App_End.NUM_TIMEOUTS_AFTER);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.INTERV_POLICY, Applications_Interv_App_End.INTERV_POLICY);
        ApplicationsIntervAppEndHash.put(Applications_Interv_App_End.PHASE, Applications_Interv_App_End.PHASE);

        ApplicationsSwitchPermitHash = new HashMap<>();
        ApplicationsSwitchPermitHash.put(Applications_Switch_Permit._ID, Applications_Switch_Permit._ID);
        ApplicationsSwitchPermitHash.put(Applications_Switch_Permit.TIMESTAMP, Applications_Switch_Permit.TIMESTAMP);
        ApplicationsSwitchPermitHash.put(Applications_Switch_Permit.TIMESTAMP_READ, Applications_Switch_Permit.TIMESTAMP_READ);
        ApplicationsSwitchPermitHash.put(Applications_Switch_Permit.DEVICE_ID, Applications_Switch_Permit.DEVICE_ID);
        ApplicationsSwitchPermitHash.put(Applications_Switch_Permit.PREV_IS_ENABLED, Applications_Switch_Permit.PREV_IS_ENABLED);
        ApplicationsSwitchPermitHash.put(Applications_Switch_Permit.CURR_IS_ENABLED, Applications_Switch_Permit.CURR_IS_ENABLED);

        ApplicationsSummaryHash = new HashMap<>();
        ApplicationsSummaryHash.put(Applications_Summary._ID, Applications_Summary._ID);
        ApplicationsSummaryHash.put(Applications_Summary.TIMESTAMP_DAY, Applications_Summary.TIMESTAMP_DAY);
        ApplicationsSummaryHash.put(Applications_Summary.TIMESTAMP, Applications_Summary.TIMESTAMP);
        ApplicationsSummaryHash.put(Applications_Summary.TIMESTAMP_READ, Applications_Summary.TIMESTAMP_READ);
        ApplicationsSummaryHash.put(Applications_Summary.DEVICE_ID, Applications_Summary.DEVICE_ID);
        ApplicationsSummaryHash.put(Applications_Summary.PACKAGE_NAME, Applications_Summary.PACKAGE_NAME);
        ApplicationsSummaryHash.put(Applications_Summary.CURRENT_PHASE, Applications_Summary.CURRENT_PHASE);
        ApplicationsSummaryHash.put(Applications_Summary.MIN_DURATION, Applications_Summary.MIN_DURATION);
        ApplicationsSummaryHash.put(Applications_Summary.MAX_DURATION, Applications_Summary.MAX_DURATION);
        ApplicationsSummaryHash.put(Applications_Summary.SUM_DURATION, Applications_Summary.SUM_DURATION);
        ApplicationsSummaryHash.put(Applications_Summary.COUNT_SESSIONS, Applications_Summary.COUNT_SESSIONS);

        ApplicationsWhitelistHash = new HashMap<>();
        ApplicationsWhitelistHash.put(Applications_Whitelist._ID, Applications_Whitelist._ID);
        ApplicationsWhitelistHash.put(Applications_Whitelist.TIMESTAMP, Applications_Whitelist.TIMESTAMP);
        ApplicationsWhitelistHash.put(Applications_Whitelist.TIMESTAMP_READ, Applications_Whitelist.TIMESTAMP_READ);
        ApplicationsWhitelistHash.put(Applications_Whitelist.DEVICE_ID, Applications_Whitelist.DEVICE_ID);
        ApplicationsWhitelistHash.put(Applications_Whitelist.PACKAGE_NAME, Applications_Whitelist.PACKAGE_NAME);
        ApplicationsWhitelistHash.put(Applications_Whitelist.WHITELIST, Applications_Whitelist.WHITELIST);
        ApplicationsWhitelistHash.put(Applications_Whitelist.CURRENT, Applications_Whitelist.CURRENT);
        ApplicationsWhitelistHash.put(Applications_Whitelist.TIME, Applications_Whitelist.TIME);
        ApplicationsWhitelistHash.put(Applications_Whitelist.VALUE, Applications_Whitelist.VALUE);
        ApplicationsWhitelistHash.put(Applications_Whitelist.WECHAT, Applications_Whitelist.WECHAT);

        ApplicationsPhasesHash = new HashMap<>();
        ApplicationsPhasesHash.put(Applications_Phases._ID, Applications_Phases._ID);
        ApplicationsPhasesHash.put(Applications_Phases.TIMESTAMP, Applications_Phases.TIMESTAMP);
        ApplicationsPhasesHash.put(Applications_Phases.TIMESTAMP_READ, Applications_Phases.TIMESTAMP_READ);
        ApplicationsPhasesHash.put(Applications_Phases.DEVICE_ID, Applications_Phases.DEVICE_ID);
        ApplicationsPhasesHash.put(Applications_Phases.OLD_PHASE, Applications_Phases.OLD_PHASE);
        ApplicationsPhasesHash.put(Applications_Phases.NEW_PHASE, Applications_Phases.NEW_PHASE);
        ApplicationsPhasesHash.put(Applications_Phases.SWITCH_TYPE, Applications_Phases.SWITCH_TYPE);

        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case, increasing the index accordingly
            case APPDIFF:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            case APPINTERV:
                count = database.delete(DATABASE_TABLES[1], selection, selectionArgs);
                break;
            case APPINTERVNOPERMIT:
                count = database.delete(DATABASE_TABLES[2], selection, selectionArgs);
                break;
            case APPINTERVAPPEND:
                count = database.delete(DATABASE_TABLES[3], selection, selectionArgs);
                break;
            case APPPERMIT:
                count = database.delete(DATABASE_TABLES[4], selection, selectionArgs);
                break;
            case APPSUMMARY:
                count = database.delete(DATABASE_TABLES[5], selection, selectionArgs);
                break;
            case APPWL:
                count = database.delete(DATABASE_TABLES[6], selection, selectionArgs);
                break;
            case APPPHASES:
                count = database.delete(DATABASE_TABLES[7], selection, selectionArgs);
                break;
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        //Log.d(TAG, "insert called");

        boolean initsuccess = initialiseDatabase();
        //Log.d(TAG + " INIT SUCCESS ", Boolean.toString(initsuccess));

        ContentValues values = (initialValues != null) ? new ContentValues(initialValues) : new ContentValues();

        database.beginTransaction();

        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case APPDIFF:
                long _id = database.insert(DATABASE_TABLES[0], Applications_Diff.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Applications_Diff.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            case APPINTERV:
                long interv_id = database.insert(DATABASE_TABLES[1], Applications_Interv.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (interv_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Applications_Interv.CONTENT_URI, interv_id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            case APPINTERVNOPERMIT:
                long intervnopermit_id = database.insert(DATABASE_TABLES[2], Applications_Interv_No_Permissions.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (intervnopermit_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Applications_Interv_No_Permissions.CONTENT_URI, intervnopermit_id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            case APPINTERVAPPEND:
                long intervappend_id = database.insert(DATABASE_TABLES[3], Applications_Interv_App_End.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (intervappend_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Applications_Interv_App_End.CONTENT_URI, intervappend_id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            case APPPERMIT:
                long permit_id = database.insert(DATABASE_TABLES[4], Applications_Switch_Permit.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (permit_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Applications_Switch_Permit.CONTENT_URI, permit_id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            case APPSUMMARY:
                long summary_id = database.insert(DATABASE_TABLES[5], Applications_Summary.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (summary_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Applications_Summary.CONTENT_URI, summary_id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            case APPWL:
                long wl_id = database.insert(DATABASE_TABLES[6], Applications_Whitelist.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (wl_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Applications_Whitelist.CONTENT_URI, wl_id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            case APPPHASES:
                long ph_id = database.insert(DATABASE_TABLES[7], Applications_Phases.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (ph_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Applications_Whitelist.CONTENT_URI, ph_id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {

            //Add all tables' DIR entries, with the right table index
            case APPDIFF:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(ApplicationsDiffHash); //the hashmap of the table
                break;
            case APPINTERV:
                qb.setTables(DATABASE_TABLES[1]);
                qb.setProjectionMap(ApplicationsIntervHash); //the hashmap of the table
                break;
            case APPINTERVNOPERMIT:
                qb.setTables(DATABASE_TABLES[2]);
                qb.setProjectionMap(ApplicationsIntervNoPermitHash); //the hashmap of the table
                break;
            case APPINTERVAPPEND:
                qb.setTables(DATABASE_TABLES[3]);
                qb.setProjectionMap(ApplicationsIntervAppEndHash); //the hashmap of the table
                break;
            case APPPERMIT:
                qb.setTables(DATABASE_TABLES[4]);
                qb.setProjectionMap(ApplicationsSwitchPermitHash); //the hashmap of the table
                break;
            case APPSUMMARY:
                qb.setTables(DATABASE_TABLES[5]);
                qb.setProjectionMap(ApplicationsSummaryHash); //the hashmap of the table
                break;
            case APPWL:
                qb.setTables(DATABASE_TABLES[6]);
                qb.setProjectionMap(ApplicationsWhitelistHash); //the hashmap of the table
                break;
            case APPPHASES:
                qb.setTables(DATABASE_TABLES[7]);
                qb.setProjectionMap(ApplicationsPhasesHash); //the hashmap of the table
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        //Don't change me
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }
    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            //Add each table indexes DIR and ITEM
            case APPDIFF:
                return Applications_Diff.CONTENT_TYPE;
            case APPDIFF_ID:
                return Applications_Diff.CONTENT_ITEM_TYPE;
            case APPINTERV:
                return Applications_Interv.CONTENT_TYPE;
            case APPINTERV_ID:
                return Applications_Interv.CONTENT_ITEM_TYPE;
            case APPINTERVNOPERMIT:
                return Applications_Interv_No_Permissions.CONTENT_TYPE;
            case APPINTERVNOPERMIT_ID:
                return Applications_Interv_No_Permissions.CONTENT_ITEM_TYPE;
            case APPINTERVAPPEND:
                return Applications_Interv_App_End.CONTENT_TYPE;
            case APPINTERVAPPEND_ID:
                return Applications_Interv_App_End.CONTENT_ITEM_TYPE;
            case APPPERMIT:
                return Applications_Switch_Permit.CONTENT_TYPE;
            case APPPERMIT_ID:
                return Applications_Switch_Permit.CONTENT_ITEM_TYPE;
            case APPSUMMARY:
                return Applications_Summary.CONTENT_TYPE;
            case APPSUMMARY_ID:
                return Applications_Summary.CONTENT_ITEM_TYPE;
            case APPWL:
                return Applications_Whitelist.CONTENT_TYPE;
            case APPWL_ID:
                return Applications_Whitelist.CONTENT_ITEM_TYPE;
            case APPPHASES:
                return Applications_Phases.CONTENT_TYPE;
            case APPPHASES_ID:
                return Applications_Phases.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case APPDIFF:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;
            case APPINTERV:
                count = database.update(DATABASE_TABLES[1], values, selection, selectionArgs);
                break;
            case APPINTERVNOPERMIT:
                count = database.update(DATABASE_TABLES[2], values, selection, selectionArgs);
                break;
            case APPINTERVAPPEND:
                count = database.update(DATABASE_TABLES[3], values, selection, selectionArgs);
                break;
            case APPPERMIT:
                count = database.update(DATABASE_TABLES[4], values, selection, selectionArgs);
                break;
            case APPSUMMARY:
                count = database.update(DATABASE_TABLES[5], values, selection, selectionArgs);
                break;
            case APPWL:
                count = database.update(DATABASE_TABLES[6], values, selection, selectionArgs);
                break;
            case APPPHASES:
                count = database.update(DATABASE_TABLES[7], values, selection, selectionArgs);
                break;
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

}