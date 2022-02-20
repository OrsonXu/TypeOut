package com.aware.smartphoneuse;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
//import android.support.design.widget.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.provider.CalendarContract;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import androidx.recyclerview.widget.GridLayoutManager;
import android.util.Log;
import android.view.ViewManager;
import android.widget.Button;
import android.content.pm.PackageManager;

import java.sql.Time;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.DisplayMetrics;
import android.content.pm.ResolveInfo;

import android.os.Build;

import com.aware.Aware;
import com.aware.Aware_Preferences;

public class WhitelistActivity extends AppCompatActivity implements WhitelistGridAdapter.ItemClickListener {
    private String currentPhase;
    private boolean currentPhaseSaveAllowed = false;
    private String TAG = Constants.TAG;
    private String introText;
    WhitelistGridAdapter adapter;
    private List<AppInfo> appInfoList;
    private List<AppInfo> appInfoList_start;
    private List<String> blackListApps;
    final int[] work_sleep_time = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
    final String[] work_sleep_time_string = {"00","00","00","00","00","00","00","00","00","00","00","00","00","00","00","00","00","00","00","00","00"};
    final static Boolean WithPyq[] = {true};
    final static Boolean WithMini[] = {true};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get current phase
        currentPhase = getIntent().getStringExtra("phase");
//        introText = "Interventions are disabled for the apps in yellow. Toggle to enable/ disable interventions. Click 'save' to save changes.";
//        introText = "请进行以下三步设置：\n1. 请您的设置工作时间和入睡时间。我们的干预将在合适的时候出现。\n2. 请选择您合适自己的价值观，我们的干预将根据您的选择提供个性化的内容。\n3. 请选择您需要干预的应用。颜色显示为灰色的应用将不被干预。点击每个应用可以激活/关闭干预。在完成设置后，点击'保存'。";
        introText = (String) getResources().getText(R.string.whitelist_intro_cn);
//        setTitle(R.string.title_activity_whitelist_save_cn);
        setTitle("TypeOut Setup");
        currentPhaseSaveAllowed = true;
        setContentView(R.layout.activity_whitelist);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button saveBtn = (Button) findViewById(R.id.savewl);
        saveBtn.setVisibility(View.VISIBLE);
//        ConstraintSet.Layout viewManager = (ConstraintSet.Layout) saveBtn.getParent();

        final TextView title_worksleeptime = (TextView) findViewById(R.id.time_setting_text);
        final TimePicker tp_time = (TimePicker) findViewById(R.id.tp_time);
        final TextView start_time_text = (TextView) findViewById(R.id.textViewStarttime);
        final TextView end_time_text = (TextView) findViewById(R.id.textViewEndtime);
        final TextView wkdywork_text = (TextView) findViewById(R.id.textView3);
        final TextView wkdysleep_text = (TextView) findViewById(R.id.textView5);
        final TextView wkendwork_text = (TextView) findViewById(R.id.textView6);
        final TextView wkendsleep_text = (TextView) findViewById(R.id.textView7);
        final Button btn_work_work = (Button) findViewById(R.id.btn_work_work);
        final Button btn_work_sleep = (Button) findViewById(R.id.btn_work_sleep);
        final Button btn_weekend_work = (Button) findViewById(R.id.btn_weekend_work);
        final Button btn_weekend_sleep = (Button) findViewById(R.id.btn_weekend_sleep);
        final Button btn_work_work2 = (Button) findViewById(R.id.btn_work_work2);
        final Button btn_work_sleep2 = (Button) findViewById(R.id.btn_work_sleep2);
        final Button btn_weekend_work2 = (Button) findViewById(R.id.btn_weekend_work2);
        final Button btn_weekend_sleep2 = (Button) findViewById(R.id.btn_weekend_sleep2);
        final TextView noon_text = (TextView) findViewById(R.id.textView8);
        final TextView noon_alert = (TextView) findViewById(R.id.textView9);
        final Button btn_noon = (Button) findViewById(R.id.btn_noon);
        final Button btn_noon2 = (Button) findViewById(R.id.btn_noon2);
        final Button btn_set_noon = (Button) findViewById(R.id.btn_set_noon);
        final Button btn_rsttime = (Button) findViewById(R.id.btn_rsttime);

        final TextView value_setting_text = (TextView) findViewById(R.id.value_setting_text);

        if (Constants.WorkSleepTimeFlag) {

            noon_alert.setVisibility(View.INVISIBLE);

            final int[] counter = {0};
            String whereCond = "current = 1";
            Cursor wlCursor = getContentResolver().query(Provider.Applications_Whitelist.CONTENT_URI, null, whereCond, null, null);

            if (wlCursor != null) {
                if (wlCursor.moveToFirst()) {
                    do {
                        ContentValues wlRow = new ContentValues();
                        DatabaseUtils.cursorRowToContentValues(wlCursor, wlRow);
                        Log.d(TAG, "WLROW: " + wlRow);
                        Log.d(TAG, "WLROWTIME: " + (String) wlRow.get("time"));
                        Log.d(TAG, "WLROWVALUE: " + (String) wlRow.get("value"));
                        String time[] = ((String) wlRow.get("time")).split(",");
                        String value[] = ((String) wlRow.get("value")).split(",");
                        for (int i = 0; i < Constants.WorkSleepTime.length; i++) {
                            Constants.WorkSleepTime[i] = Integer.parseInt(time[i]);
                        }
                        Log.d(TAG, "WLROWTIME1: " + Constants.WorkSleepTime.toString());
                        Log.d(TAG, "WLROWVALUE1: " + Constants.userValue.toString());
                    } while (wlCursor.moveToNext());
                }
            }

        for (int i = 0; i < work_sleep_time.length; i++) {
            work_sleep_time[i] = Constants.WorkSleepTime[i];
            setWorkSleepTimeString(i, Constants.WorkSleepTime[i]);
        }

        btn_work_work.setBackgroundColor(getResources().getColor(R.color.colorSel));
        btn_work_work.setText(" " + work_sleep_time_string[0] + " : " + work_sleep_time_string[1] + " ");
        btn_work_work2.setText(" " + work_sleep_time_string[2] + " : " + work_sleep_time_string[3] + " ");
        btn_work_sleep.setText(" " + work_sleep_time_string[4] + " : " + work_sleep_time_string[5] + " ");
        btn_work_sleep2.setText(" " + work_sleep_time_string[6] + " : " + work_sleep_time_string[7] + " ");
        btn_weekend_work.setText(" " + work_sleep_time_string[8] + " : " + work_sleep_time_string[9] + " ");
        btn_weekend_work2.setText(" " + work_sleep_time_string[10] + " : " + work_sleep_time_string[11] + " ");
        btn_weekend_sleep.setText(" " + work_sleep_time_string[12] + " : " + work_sleep_time_string[13] + " ");
        btn_weekend_sleep2.setText(" " + work_sleep_time_string[14] + " : " + work_sleep_time_string[15] + " ");
        btn_noon.setText(" " + work_sleep_time_string[16] + " : " + work_sleep_time_string[17] + " ");
        btn_noon2.setText(" " + work_sleep_time_string[18] + " : " + work_sleep_time_string[19] + " ");

        if (work_sleep_time[16] == 0 && work_sleep_time[17] == 0 && work_sleep_time[18] == 0 && work_sleep_time[19] == 0) {
            noon_text.setVisibility(View.INVISIBLE);
            btn_noon.setVisibility(View.INVISIBLE);
            btn_noon2.setVisibility(View.INVISIBLE);
        } else {
            noon_text.setVisibility(View.VISIBLE);
            btn_noon.setVisibility(View.VISIBLE);
            btn_noon2.setVisibility(View.VISIBLE);
        }

        tp_time.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                boolean set = false;
                switch (counter[0]) {
                    case 0:
                        if (work_sleep_time[2] >= tp_time.getHour()) {
                            if (work_sleep_time[2] > tp_time.getHour()) {
                                set = true;
                            } else if (work_sleep_time[2] == tp_time.getHour() && work_sleep_time[3] >= tp_time.getMinute()) {
                                set = true;
                            }
                            if (set) {
                                work_sleep_time[0] = tp_time.getHour();
                                work_sleep_time[1] = tp_time.getMinute();
                                setWorkSleepTimeString(0, tp_time.getHour());
                                setWorkSleepTimeString(1, tp_time.getMinute());
                                btn_work_work.setText(" " + work_sleep_time_string[0] + " : " + work_sleep_time_string[1] + " ");
                            }
                        }
                        break;
                    case 1:
                        if (work_sleep_time[0] <= tp_time.getHour()) {
                            if (work_sleep_time[0] < tp_time.getHour()) {
                                set = true;
                            } else if (work_sleep_time[0] == tp_time.getHour() && work_sleep_time[1] <= tp_time.getMinute()) {
                                set = true;
                            }
                            if (set) {
                                work_sleep_time[2] = tp_time.getHour();
                                work_sleep_time[3] = tp_time.getMinute();
                                setWorkSleepTimeString(2, tp_time.getHour());
                                setWorkSleepTimeString(3, tp_time.getMinute());
                                btn_work_work2.setText(" " + work_sleep_time_string[2] + " : " + work_sleep_time_string[3] + " ");
                            }
                        }
                        break;
                    case 2:
                        set = true;
                        if (tp_time.getHour() == work_sleep_time[6] && tp_time.getMinute() > work_sleep_time[7]) {
                            set = false;
                        }
                        if (set) {
                            work_sleep_time[4] = tp_time.getHour();
                            work_sleep_time[5] = tp_time.getMinute();
                            setWorkSleepTimeString(4, tp_time.getHour());
                            setWorkSleepTimeString(5, tp_time.getMinute());
                            btn_work_sleep.setText(" " + work_sleep_time_string[4] + " : " + work_sleep_time_string[5] + " ");
                        }
                        break;
                    case 3:
                        set = true;
                        if (tp_time.getHour() == work_sleep_time[4] && tp_time.getMinute() < work_sleep_time[5]) {
                            set = false;
                        }
                        if (set) {
                            work_sleep_time[6] = tp_time.getHour();
                            work_sleep_time[7] = tp_time.getMinute();
                            setWorkSleepTimeString(6, tp_time.getHour());
                            setWorkSleepTimeString(7, tp_time.getMinute());
                            btn_work_sleep2.setText(" " + work_sleep_time_string[6] + " : " + work_sleep_time_string[7] + " ");
                        }
                        break;
                    case 4:
                        if (work_sleep_time[10] >= tp_time.getHour()) {
                            if (work_sleep_time[10] > tp_time.getHour()) {
                                set = true;
                            } else if (work_sleep_time[10] == tp_time.getHour() && work_sleep_time[11] >= tp_time.getMinute()) {
                                set = true;
                            }
                            if (set) {
                                work_sleep_time[8] = tp_time.getHour();
                                work_sleep_time[9] = tp_time.getMinute();
                                setWorkSleepTimeString(8, tp_time.getHour());
                                setWorkSleepTimeString(9, tp_time.getMinute());
                                btn_weekend_work.setText(" " + work_sleep_time_string[8] + " : " + work_sleep_time_string[9] + " ");
                            }
                        }
                        break;
                    case 5:
                        if (work_sleep_time[8] <= tp_time.getHour()) {
                            if (work_sleep_time[8] < tp_time.getHour()) {
                                set = true;
                            } else if (work_sleep_time[8] == tp_time.getHour() && work_sleep_time[9] <= tp_time.getMinute()) {
                                set = true;
                            }
                            if (set) {
                                work_sleep_time[10] = tp_time.getHour();
                                work_sleep_time[11] = tp_time.getMinute();
                                setWorkSleepTimeString(10, tp_time.getHour());
                                setWorkSleepTimeString(11, tp_time.getMinute());
                                btn_weekend_work2.setText(" " + work_sleep_time_string[10] + " : " + work_sleep_time_string[11] + " ");
                            }
                        }
                        break;
                    case 6:
                        set = true;
                        if (tp_time.getHour() == work_sleep_time[14] && tp_time.getMinute() > work_sleep_time[15]) {
                            set = false;
                        }
                        if (set) {
                            work_sleep_time[12] = tp_time.getHour();
                            work_sleep_time[13] = tp_time.getMinute();
                            setWorkSleepTimeString(12, tp_time.getHour());
                            setWorkSleepTimeString(13, tp_time.getMinute());
                            btn_weekend_sleep.setText(" " + work_sleep_time_string[12] + " : " + work_sleep_time_string[13] + " ");
                        }
                        break;
                    case 7:
                        set = true;
                        if (tp_time.getHour() == work_sleep_time[12] && tp_time.getMinute() < work_sleep_time[13]) {
                            set = false;
                        }
                        if (set) {
                            work_sleep_time[14] = tp_time.getHour();
                            work_sleep_time[15] = tp_time.getMinute();
                            setWorkSleepTimeString(14, tp_time.getHour());
                            setWorkSleepTimeString(15, tp_time.getMinute());
                            btn_weekend_sleep2.setText(" " + work_sleep_time_string[14] + " : " + work_sleep_time_string[15] + " ");
                        }
                        break;
                    case 8:
                        if ((work_sleep_time[18] - tp_time.getHour()) >= 3 || tp_time.getHour() > work_sleep_time[18]) {
                            //Check whether the interval between start and end of noon break is larger than 3 hours
                            if (Math.abs(work_sleep_time[18] - tp_time.getHour()) == 3) {
                                //If the hour part is same for start and end
                                if (tp_time.getMinute() < work_sleep_time[18]) {
                                    //Then if start minute is smaller than the end minute, interval is larger than 3 hours
                                    //So set the end minute to equal to start minute
                                    work_sleep_time[19] = tp_time.getMinute();
                                    setWorkSleepTimeString(19, tp_time.getMinute());
                                    btn_noon2.setText(" " + work_sleep_time_string[18] + " : " + work_sleep_time_string[19] + " ");
                                }
                            } else {
                                //If start hour is larger than end hour
                                //Then set the end our to equal with 3 hours of interval
                                work_sleep_time[18] = tp_time.getHour() + 3;
                                work_sleep_time[19] = tp_time.getMinute();
                                setWorkSleepTimeString(18, tp_time.getHour() + 3);
                                setWorkSleepTimeString(19, tp_time.getMinute());
                                btn_noon2.setText(" " + work_sleep_time_string[18] + " : " + work_sleep_time_string[19] + " ");
                            }
                        }
                        work_sleep_time[16] = tp_time.getHour();
                        work_sleep_time[17] = tp_time.getMinute();
                        setWorkSleepTimeString(16, tp_time.getHour());
                        setWorkSleepTimeString(17, tp_time.getMinute());
                        btn_noon.setText(" " + work_sleep_time_string[16] + " : " + work_sleep_time_string[17] + " ");
                        break;
                    case 9:
                        if (work_sleep_time[16] <= tp_time.getHour()) {
                            if ((tp_time.getHour() - work_sleep_time[16]) <= 3 && work_sleep_time[16] != tp_time.getHour()) {
                                set = true;
                            } else if (work_sleep_time[16] == tp_time.getHour() && work_sleep_time[17] <= tp_time.getMinute()) {
                                set = true;
                            }
                            if (set) {
                                //noon_text.setText(getResources().getString(R.string.noon_text_cn));
                                work_sleep_time[18] = tp_time.getHour();
                                work_sleep_time[19] = tp_time.getMinute();
                                setWorkSleepTimeString(18, tp_time.getHour());
                                setWorkSleepTimeString(19, tp_time.getMinute());
                                btn_noon2.setText(" " + work_sleep_time_string[18] + " : " + work_sleep_time_string[19] + " ");
//                                noon_text.setText(getResources().getString(R.string.noon_text_cn));
                                noon_alert.setVisibility(View.INVISIBLE);
                            } else {
//                                noon_text.setText(getResources().getString(R.string.noon_alert_cn));
                                noon_alert.setVisibility(View.VISIBLE);
                            }
                        } else {
//                            noon_text.setText(getResources().getString(R.string.noon_alert_cn));
                            noon_alert.setVisibility(View.VISIBLE);
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        btn_set_noon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (noon_text.getVisibility() == View.INVISIBLE) {
                    btn_set_noon.setText(R.string.noon_set_hide_cn);
//                    noon_text.setText(R.string.noon_text_cn);
                    noon_text.setVisibility(View.VISIBLE);
                    btn_noon.setVisibility(View.VISIBLE);
                    btn_noon2.setVisibility(View.VISIBLE);
                } else {
                    btn_set_noon.setText(R.string.noon_set_cn);
                    noon_text.setVisibility(View.INVISIBLE);
                    btn_noon.setVisibility(View.INVISIBLE);
                    btn_noon2.setVisibility(View.INVISIBLE);

                    // TODO: check: add code to reset the noon sleep record
                    for (int i = 16; i < Constants.WorkSleepTime.length; i++) {
                        Constants.WorkSleepTime[i] = 0;
                        work_sleep_time[i] = 0;
                        setWorkSleepTimeString(i, 0);
                    }
                    btn_noon.setText(" " + work_sleep_time_string[16] + " : " + work_sleep_time_string[17] + " ");
                    btn_noon2.setText(" " + work_sleep_time_string[18] + " : " + work_sleep_time_string[19] + " ");
                }
            }
        });

        btn_rsttime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter[0] = 0;
                btn_work_work.setBackgroundColor(getResources().getColor(R.color.colorSel));
                btn_work_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                for (int i = 0; i < Constants.WorkSleepTime.length; i++) {
                    Constants.WorkSleepTime[i] = 0;
                    work_sleep_time[i] = 0;
                    setWorkSleepTimeString(i, 0);
                }
                btn_work_work.setBackgroundColor(getResources().getColor(R.color.colorSel));
                btn_work_work.setText(" " + work_sleep_time_string[0] + " : " + work_sleep_time_string[1] + " ");
                btn_work_work2.setText(" " + work_sleep_time_string[2] + " : " + work_sleep_time_string[3] + " ");
                btn_work_sleep.setText(" " + work_sleep_time_string[4] + " : " + work_sleep_time_string[5] + " ");
                btn_work_sleep2.setText(" " + work_sleep_time_string[6] + " : " + work_sleep_time_string[7] + " ");
                btn_weekend_work.setText(" " + work_sleep_time_string[8] + " : " + work_sleep_time_string[9] + " ");
                btn_weekend_work2.setText(" " + work_sleep_time_string[10] + " : " + work_sleep_time_string[11] + " ");
                btn_weekend_sleep.setText(" " + work_sleep_time_string[12] + " : " + work_sleep_time_string[13] + " ");
                btn_weekend_sleep2.setText(" " + work_sleep_time_string[14] + " : " + work_sleep_time_string[15] + " ");
                btn_noon.setText(" " + work_sleep_time_string[16] + " : " + work_sleep_time_string[17] + " ");
                btn_noon2.setText(" " + work_sleep_time_string[18] + " : " + work_sleep_time_string[19] + " ");

                if (noon_text.getVisibility() == View.VISIBLE) {
                    noon_text.setVisibility(View.INVISIBLE);
                    btn_noon.setVisibility(View.INVISIBLE);
                    btn_noon2.setVisibility(View.INVISIBLE);
                }
            }
        });
        btn_work_work.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter[0] = 0;
                btn_work_work.setBackgroundColor(getResources().getColor(R.color.colorSel));
                btn_work_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
            }
        });
        btn_work_work2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter[0] = 1;
                btn_work_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_work2.setBackgroundColor(getResources().getColor(R.color.colorSel));
                btn_work_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
            }
        });
        btn_work_sleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter[0] = 2;
                btn_work_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep.setBackgroundColor(getResources().getColor(R.color.colorSel));
                btn_weekend_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
            }
        });
        btn_work_sleep2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter[0] = 3;
                btn_work_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep2.setBackgroundColor(getResources().getColor(R.color.colorSel));
                btn_weekend_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
            }
        });
        btn_weekend_work.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter[0] = 4;
                btn_work_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work.setBackgroundColor(getResources().getColor(R.color.colorSel));
                btn_weekend_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
            }
        });
        btn_weekend_work2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter[0] = 5;
                btn_work_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work2.setBackgroundColor(getResources().getColor(R.color.colorSel));
                btn_weekend_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
            }
        });
        btn_weekend_sleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter[0] = 6;
                btn_work_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep.setBackgroundColor(getResources().getColor(R.color.colorSel));
                btn_work_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
            }
        });
        btn_weekend_sleep2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter[0] = 7;
                btn_work_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep2.setBackgroundColor(getResources().getColor(R.color.colorSel));
                btn_noon.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
            }
        });
        btn_noon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter[0] = 8;
                btn_work_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon.setBackgroundColor(getResources().getColor(R.color.colorSel));
                btn_noon2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
            }
        });
        btn_noon2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter[0] = 9;
                btn_work_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_work_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_work2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_weekend_sleep2.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon.setBackgroundColor(getResources().getColor(R.color.colorNoSel));
                btn_noon2.setBackgroundColor(getResources().getColor(R.color.colorSel));
            }
        });

        } else {
            title_worksleeptime.setVisibility(View.GONE);
            tp_time.setVisibility(View.GONE);
            start_time_text.setVisibility(View.GONE);
            end_time_text.setVisibility(View.GONE);
            wkdywork_text.setVisibility(View.GONE);
            wkdysleep_text.setVisibility(View.GONE);
            wkendwork_text.setVisibility(View.GONE);
            wkendsleep_text.setVisibility(View.GONE);
            btn_work_work.setVisibility(View.GONE);
            btn_work_sleep.setVisibility(View.GONE);
            btn_weekend_work.setVisibility(View.GONE);
            btn_weekend_sleep.setVisibility(View.GONE);
            btn_work_work2.setVisibility(View.GONE);
            btn_work_sleep2.setVisibility(View.GONE);
            btn_weekend_work2.setVisibility(View.GONE);
            btn_weekend_sleep2.setVisibility(View.GONE);
            noon_text.setVisibility(View.GONE);
            noon_alert.setVisibility(View.GONE);
            btn_noon.setVisibility(View.GONE);
            btn_noon2.setVisibility(View.GONE);
            btn_set_noon.setVisibility(View.GONE);
            btn_rsttime.setVisibility(View.GONE);


            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) value_setting_text.getLayoutParams();
            layoutParams.topToBottom = ConstraintLayout.LayoutParams.UNSET;
            layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        }

        final Switch wechat_switch = (Switch) findViewById(R.id.wechat_switch);
        final Switch wechat_switch2 = (Switch) findViewById(R.id.wechat_switch2);
        WithMini[0] = Constants.WithMini[0];
        WithPyq[0] = Constants.WithPyq[0];
        wechat_switch.setChecked(WithPyq[0]);
        wechat_switch2.setChecked(WithMini[0]);
        // 添加监听
        wechat_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    WithPyq[0] = true;
                }else {
                    WithPyq[0] = false;
                }
            }
        });
        wechat_switch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    WithMini[0] = true;
                }else {
                    WithMini[0] = false;
                }
            }
        });
     /*
        checkBoxes[0] for discipline value 自律
        checkBoxes[1] for health value 健康
        checkBoxes[2] for order value 有序
        checkBoxes[3] for persistence value 坚定
        checkBoxes[4] for responsible value 责任
        checkBoxes[5] for self-awareness value 自我意识
        checkBoxes[6] for self-care value 自我关心
     */
        final CheckBox[] checkBoxes = {(CheckBox)findViewById(R.id.checkBoxDiscipline),
                (CheckBox)findViewById(R.id.checkBoxHealth),
                (CheckBox)findViewById(R.id.checkBoxOrder),
                (CheckBox)findViewById(R.id.checkBoxPersistence),
                (CheckBox)findViewById(R.id.checkBoxResponsibility),
                (CheckBox)findViewById(R.id.checkBoxSelfAwareness),
                (CheckBox)findViewById(R.id.checkBoxSelfCare)};
        for(int i = 0; i < checkBoxes.length; i++){
            checkBoxes[i].setChecked(Constants.userValue[i + 1]);
            String text = Constants.value_setting_cn[1][i][0];
            SpannableString spannableString = new SpannableString(text);
            spannableString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD)
                    , Integer.parseInt(Constants.value_setting_cn[1][i][1]), Integer.parseInt(Constants.value_setting_cn[1][i][2]),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new RelativeSizeSpan(1.5f)
                    , Integer.parseInt(Constants.value_setting_cn[1][i][1]), Integer.parseInt(Constants.value_setting_cn[1][i][2]),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            checkBoxes[i].setText(spannableString);
        }

        blackListApps = Constants.BlackListApps;
        //InitializeBlackList();

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
////                        .setAction("Action", null).show();
//                AlertDialog alertDialog = new AlertDialog.Builder(WhitelistActivity.this).create();
////                alertDialog.setTitle("Information");
//                alertDialog.setTitle("说明");
//                alertDialog.setMessage(introText);
//                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.dismiss();
//                            }
//                        });
//                alertDialog.show();
//            }
//        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        // get Launchable installed apps
        List<String> excApps = PluginConstants.getExcTimeoutApps();
        PackageManager pm=getPackageManager();
        Intent main=new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launchables=pm.queryIntentActivities(main, 0);
        appInfoList = new ArrayList<AppInfo>();
        appInfoList_start = new ArrayList<AppInfo>();
        for (ResolveInfo rinfo : launchables) {
            String package_name = rinfo.activityInfo.packageName;
            String app_name;
            Drawable appIcon = null;
            try {
                app_name = (String) pm.getApplicationLabel(pm.getApplicationInfo(package_name, PackageManager.GET_META_DATA));
            }
            catch (PackageManager.NameNotFoundException e){
                app_name = package_name;
            }
            int app_cat = -1;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    app_cat = pm.getApplicationInfo(package_name, PackageManager.GET_META_DATA).category;
                } catch (PackageManager.NameNotFoundException e) {
                    app_cat = -1;
                }
            }
            try {
                appIcon = pm.getApplicationIcon(package_name);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (!excApps.contains(package_name) && !package_name.equals(PluginConstants.testApp)) {
                if (!containsPkg(appInfoList, package_name) && !containsPkg(appInfoList_start, package_name)) {
                    // fetch status if it exists
                    Boolean statLoaded = fetchWhitelistStatForPkg(package_name);
                    Boolean statToUse;
                    //Boolean inDefault = fetchInDefault(package_name);
                    Boolean inDefault = false;
                    if (statLoaded == null){
                        statToUse = false;
                        // @TODO - can insert into db here.
                    }
                    else{
                        statToUse = statLoaded;
                    }
                    Log.d(TAG, package_name+", statLoaded = "+statLoaded+", statLoaded = "+statLoaded);
                    AppInfo currAppInfo = new AppInfo(app_name, package_name, app_cat, statToUse, statLoaded, inDefault, appIcon);
                    Log.d(TAG, package_name + ", stat = "+currAppInfo.getStat()+ ", prevStat = "+currAppInfo.getPrevStat());
                    if(Constants.BlackListApps_start_pkg.contains(package_name)
                            || Constants.BlackListApps_start_name.contains(app_name)){
                        appInfoList_start.add(currAppInfo);
                    }else {
                        appInfoList.add(currAppInfo);
                    }
                }
            }
        }
        Collections.sort(appInfoList);
        appInfoList.addAll(0, appInfoList_start);


        // Figure out number of columns for Grid layout
        DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        double dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        double dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        double minDimens;
        if (dpWidth < dpHeight){
            minDimens = dpWidth;
        }
        else {
            minDimens = dpHeight;
        }
        double cellDimens = 205;
        int numCols = (int) Math.floor(minDimens/cellDimens);

        // get status of each app
//        appStatus = new ArrayList<Integer>(Collections.nCopies(appNamesList.size(), 0));

        // display Installed Apps info using RecyclerView
        RecyclerView recyclerView = findViewById(R.id.rvNumbers);
        recyclerView.setLayoutManager(new GridLayoutManager(this, numCols));
        adapter = new WhitelistGridAdapter(this, appInfoList, currentPhaseSaveAllowed);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(View view, int position, Boolean newstatus, Boolean unChangeable) {
        if(unChangeable){
            Toast.makeText(this, getResources().getString(R.string.default_blacklist_cn), Toast.LENGTH_LONG).show();
        }
        Log.i(TAG, "You clicked number " + appInfoList.get(position).getName() + ", pkg = "+ appInfoList.get(position).getPkg() +", status = "+ appInfoList.get(position).getStat() +", prevStatus = "+ appInfoList.get(position).getPrevStat());
    }

    private Boolean fetchInDefault(String pkg){
        Boolean curr_stat_bool = false;
        for (String package_name:blackListApps) {
            if(package_name.contains(pkg)) {
                curr_stat_bool = true;
                break;
            }
        }
        return curr_stat_bool;
    }
    private Boolean fetchWhitelistStatForPkg(String pkg){
        Boolean curr_stat_bool = null;
        // fetch last whitelist status
        String whereCond = "package_name = '" + pkg+"' AND current = 1";
        Cursor prevWlStatCursor = getContentResolver().query(Provider.Applications_Whitelist.CONTENT_URI, null, whereCond, null, "timestamp DESC");
        if (prevWlStatCursor != null) {
            Log.d(TAG, "#Rows fetched = "+prevWlStatCursor.getCount());
            if (prevWlStatCursor.moveToFirst()) {
                ContentValues prevWlStat = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(prevWlStatCursor, prevWlStat);
                String curr_stat_str = (String) prevWlStat.get("whitelist");
                if (curr_stat_str != null && !curr_stat_str.equals("") && !curr_stat_str.equals("''")) {
                    curr_stat_bool = curr_stat_str.equals("1");
                    Log.d(TAG, pkg+" got "+curr_stat_str+ ", bool "+curr_stat_bool);
                }
            }
        }
        //Fetch default blacklist Apps
        for (String package_name:blackListApps) {
            if(package_name.equals(pkg)) {
                curr_stat_bool = true;
                break;
            }
        }
        return curr_stat_bool;
    }

/*    private void setNoon(boolean visible){
        if(visible){
            noon_text.setVisibility(View.VISIBLE);
            btn_noon.setVisibility(View.VISIBLE);
            btn_noon2.setVisibility(View.VISIBLE);
        }else {
            noon_text.setVisibility(View.INVISIBLE);
            btn_noon.setVisibility(View.INVISIBLE);
            btn_noon2.setVisibility(View.INVISIBLE);
        }
    }*/

    public void InitializeBlackList() {
        for (String package_name:blackListApps) {
            Log.d(TAG, "InitializeBlackList: package_name = "+package_name);
            String whereCond = "package_name = '" + package_name +"' AND current = 1";
            Cursor prevWlStatCursor = getContentResolver().query(Provider.Applications_Whitelist.CONTENT_URI, null, whereCond, null, "timestamp DESC");
            if(prevWlStatCursor != null)
                Log.d(TAG, "InitializeBlackList: prevWlStatCursor = "+prevWlStatCursor);
            else
                Log.d(TAG, "InitializeBlackList: prevWlStatCursor = null");
            String device_id = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID);
            ContentValues cvNew;
            if (prevWlStatCursor != null) {
                // TODO::do nothing??
//                // update all previous rows for this app as "not current"
//                String whereCond_whitelist = "package_name = '" + package_name +"' AND current = 1 AND whitelist = 1";
//                Cursor prevWlStatCursor_whitelist = getContentResolver().query(Provider.Applications_Whitelist.CONTENT_URI, null, whereCond_whitelist, null, "timestamp DESC");
//                String placeHolderValueArr[] = {package_name};
//                cvNew = new ContentValues();
//                cvNew.put("timestamp", System.currentTimeMillis());
//                cvNew.put("timestamp_read", Calendar.getInstance().getTime().toString());
//                cvNew.put("current", false);
//                try {
//                    getContentResolver().update(Provider.Applications_Whitelist.CONTENT_URI, cvNew, "package_name = ?", placeHolderValueArr);
//                    Log.d(TAG+" App WL, UPDATE: ", cvNew.toString());
//                } catch (Exception ex) {
//                    Log.e(TAG, "update exception", ex);
//                    Log.e(TAG, "Failed updation of Applications_Whitelist");
//                }
            }
            else {
                // insert a new row for this app as current
                String WorkSleepTime = "";
                String Value = "";
                for(int i = 0; i < Constants.WorkSleepTime.length; i++){
                    WorkSleepTime += Constants.WorkSleepTime[i] + ",";
                }
                for(int j = 0; j < Constants.userValue.length; j++){
                    if(Constants.userValue[j]){
                        Value += Constants.values[j] + ",";
                    }
                }
                cvNew = new ContentValues();
                cvNew.put("timestamp", System.currentTimeMillis());
                cvNew.put("timestamp_read", Calendar.getInstance().getTime().toString());
                cvNew.put("device_id", device_id);
                cvNew.put("package_name", package_name);
                cvNew.put("whitelist", true);
                cvNew.put("current", true);
                cvNew.put("time", WorkSleepTime);
                cvNew.put("value", Value);
                cvNew.put("wechat", Constants.WithMini[0] + "," + Constants.WithPyq[0]);
                try{
                    getContentResolver().insert(Provider.Applications_Whitelist.CONTENT_URI, cvNew);
                    Log.d(TAG+" App WL, INSERT: ", cvNew.toString());
                } catch (Exception ex) {
                    Log.e(TAG, "insert exception", ex);
                    Log.e(TAG, "Failed insertion of Applications_Whitelist");
                }
            }
        }
        Intent logChange = new Intent(Constants.ACTION_WHITELIST_CHANGED);
        sendBroadcast(logChange);
    }

    public void saveWhitelist(View view){
        Log.d(TAG, "CHANGE CURRENTPHASE: " + currentPhase + " SAVE ALLOW: " + currentPhaseSaveAllowed);
        if (!Constants.phases.contains(currentPhase)){
            Toast toast=Toast.makeText(getApplicationContext(),"Unknown phase!",Toast.LENGTH_SHORT);
            toast.show();
        }
        else {
            Boolean anyChanges = false;
            if(WithPyq[0] != Constants.WithPyq[0] || WithMini[0] != Constants.WithMini[0]){
                anyChanges = true;
                Constants.WithPyq[0] = WithPyq[0];
                Constants.WithMini[0] = WithMini[0];
            }
            for(int i = 0; i < work_sleep_time.length; i++){
                if(Constants.WorkSleepTime[i] != work_sleep_time[i]){
                    anyChanges = true;
                }
                Constants.WorkSleepTime[i] = work_sleep_time[i];
                Log.d(TAG, "CHANGE WORKSLEEPTIME");
            }

            final CheckBox[] checkBoxes = {(CheckBox)findViewById(R.id.checkBoxDiscipline),
                    (CheckBox)findViewById(R.id.checkBoxHealth),
                    (CheckBox)findViewById(R.id.checkBoxOrder),
                    (CheckBox)findViewById(R.id.checkBoxPersistence),
                    (CheckBox)findViewById(R.id.checkBoxResponsibility),
                    (CheckBox)findViewById(R.id.checkBoxSelfAwareness),
                    (CheckBox)findViewById(R.id.checkBoxSelfCare)};
            for(int i = 0; i < checkBoxes.length; i++){
                if(checkBoxes[i].isChecked() != Constants.userValue[i + 1]){
                    anyChanges = true;
                    Constants.userValue[i + 1] = checkBoxes[i].isChecked();
                }
            }

            Toast toast1=Toast.makeText(getApplicationContext(),getResources().getString(R.string.saving_cn),Toast.LENGTH_SHORT);
            toast1.show();
            // If loaded status != current status
            // update all prev rows to "old"
            // insert 1 row per app
            // This is better because it would accurately represent the last saved screen.
            // App uninstallation and reinstallation can create boundary conditions otherwise.
            for (AppInfo ainfo : appInfoList) {
                String pkg = ainfo.getPkg();
                String WorkSleepTime = "";
                String Value = "";
                for(int i = 0; i < Constants.WorkSleepTime.length; i++){
                    WorkSleepTime += Constants.WorkSleepTime[i] + ",";
                }
                for(int j = 0; j < Constants.userValue.length; j++){
                    if(Constants.userValue[j]){
                        Value += Constants.values[j] + ",";
                    }
                }
                if (ainfo.getStat() != ainfo.getPrevStat()) {
                    anyChanges = true;
                }
                Log.d(TAG, "Saving pkg "+pkg+ ", stat = "+ainfo.getStat()+", prevStat = "+ainfo.getPrevStat());
                String device_id = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID);
                ContentValues cvNew;
                // update all previous rows for this app as "not current"
                String placeHolderValueArr[] = {pkg};
                cvNew = new ContentValues();
                cvNew.put("timestamp", System.currentTimeMillis());
                cvNew.put("timestamp_read", Calendar.getInstance().getTime().toString());
                cvNew.put("current", false);
                try {
                    getContentResolver().update(Provider.Applications_Whitelist.CONTENT_URI, cvNew, "package_name = ?", placeHolderValueArr);
                    Log.d(TAG+" App WL, UPDATE: ", cvNew.toString());
                } catch (Exception ex) {
                    Log.e(TAG, "update exception", ex);
                    Log.e(TAG, "Failed updation of Applications_Whitelist");
                }
                // insert a new row for this app as current
                cvNew = new ContentValues();
                cvNew.put("timestamp", System.currentTimeMillis());
                cvNew.put("timestamp_read", Calendar.getInstance().getTime().toString());
                cvNew.put("device_id", device_id);
                cvNew.put("package_name", pkg);
                cvNew.put("whitelist", ainfo.getStat());
                cvNew.put("current", true);
                cvNew.put("time", WorkSleepTime);
                cvNew.put("value", Value);
                cvNew.put("wechat", Constants.WithMini[0] + ", " + Constants.WithPyq[0]);
                try{
                    getContentResolver().insert(Provider.Applications_Whitelist.CONTENT_URI, cvNew);
                    Log.d(TAG+" App WL, INSERT: ", cvNew.toString());
                } catch (Exception ex) {
                    Log.e(TAG, "insert exception", ex);
                    Log.e(TAG, "Failed insertion of Applications_Whitelist");
                }
            }
            if (anyChanges) {
                Log.d(TAG, "Changes to WL. Sending Broadcast.");
                Toast toast2 = Toast.makeText(getApplicationContext(), getResources().getText(R.string.saved_cn), Toast.LENGTH_SHORT);
                toast2.show();
                Intent logChange = new Intent(Constants.ACTION_WHITELIST_CHANGED);
                sendBroadcast(logChange);
            }
            else {
                Toast toast2 = Toast.makeText(getApplicationContext(), getResources().getText(R.string.no_save_cn), Toast.LENGTH_SHORT);
                toast2.show();
            }
        }
        finish();
    }

    protected void onStop() {
        super.onStop();
        this.finish();
    }

    public static class AppInfo implements Comparable<AppInfo>{
        private String name;
        private String pkg;
        private Integer cat;
        private Boolean stat;
        private Boolean prevStat;
        private Boolean unChangeable;
        private Drawable icon;
        public AppInfo(String name, String pkg, Integer cat, Boolean stat, Boolean prevStat, Boolean unChangeable, Drawable icon){
            this.name = name;
            this.pkg = pkg;
            this.cat = cat;
            this.stat = stat;
            this.prevStat = prevStat;
            this.unChangeable = unChangeable;
            this.icon = icon;
        }
        public AppInfo(AppInfo oldInfo, Boolean newStat){
            this.name = oldInfo.getName();
            this.pkg = oldInfo.getPkg();
            this.cat = oldInfo.getCat();
            this.prevStat = oldInfo.getPrevStat();
            this.stat = newStat;
            this.unChangeable = oldInfo.getUnChangeable();
            this.icon = icon;
        }
        public String getName() {
            return name;
        }
        public String getPkg() {
            return pkg;
        }
        public Integer getCat() {
            return cat;
        }
        public Boolean getStat() {
            return stat;
        }
        public Boolean getPrevStat() {
            return prevStat;
        }
        public Boolean getUnChangeable() {
            return unChangeable;
        }
        public Drawable getIcon() {
            return icon;
        }
        @Override
        public int compareTo(AppInfo a) {
            if (getName() == null || a.getName() == null) {
                return 0;
            }
            return getName().compareTo(a.getName());
        }
    }
    public static boolean containsPkg(List<AppInfo> list, String pkg) {
        for (AppInfo object : list) {
            if (object.getPkg().equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    private void setWorkSleepTimeString(int index, int num){
        if(num < 10){
            this.work_sleep_time_string[index] = "0" + num;
        }else {
            this.work_sleep_time_string[index] = "" + num;
        }
    }
}
