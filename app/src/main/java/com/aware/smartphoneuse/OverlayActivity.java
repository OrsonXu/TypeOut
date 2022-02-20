package com.aware.smartphoneuse;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.Html;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;


public class OverlayActivity extends AppCompatActivity {
    private boolean pass = false;
    private boolean skipButtonPressed = false;
    boolean DEBUG_Flag = false;
    private String TAG = Constants.TAG;
    private String OverlayType;
    private String TextOrAudio;
    private String curAppName;
    private String curPackageName;
    private String timeSpentTodayStr;
    private LinearLayout oView;
    private Bundle intervBundle;
    private String user_input_text;
    private String user_input_text2;
    private RecognizerDialog mDialog;
    private Activity this_activity;
    private Calendar c;
    private Date start_time;
    private long start_millis;
    String[] random_text = new String[6];
    Intent intent;
    private String currentPhase;
    private boolean clickDialog = false;
    private HashMap<String, String> mIatResults = new LinkedHashMap<String , String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overlay);

        intent = getIntent();
        currentPhase =  getCurrentPhase();
        OverlayType = intent.getStringExtra("OverlayType");
        TextOrAudio = intent.getStringExtra("TextOrAudio");
        curAppName = intent.getStringExtra("application_name");
        curPackageName = intent.getStringExtra("package_name");
        timeSpentTodayStr = intent.getStringExtra("time_today_to_timeout");
        intervBundle = intent.getExtras();
        user_input_text = "";
        user_input_text2 = "";
        c = Calendar.getInstance();
        start_time = c.getTime();
        start_millis = c.getTimeInMillis();
        if (! currentPhase.equals("RANDOM_TEXT")){
            random_text = getPersuasiveText(true);
        } else {
            random_text = new String[] {getMeaninglessText(),getMeaninglessText(),"0","0","0","0"} ;
        }

        if(curPackageName.equals(Constants.WeChatPackageName)){
            Constants.lastSNSTime[0] = System.currentTimeMillis();
        }
        Constants.enableWaitingPopUp[0] = false;
        AddLayoutView(OverlayType,TextOrAudio);
        LinearLayout ll = (LinearLayout) findViewById(R.id.overlayConstrain);
        oView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT));
        if(oView != null)
            ll.addView(oView);
        else
            Log.d(TAG, "OverlayActivity onCreate: oView = null");

        // for iflytek
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=be572664");
        this_activity = this;
    }

    protected void onStop() {
        super.onStop();
        if(!pass){
            returnHome("on_stop");
        }else if(!clickDialog && currentPhase.equals("MEANINGFUL_TEXT") && !skipButtonPressed){
            returnHome("on_dialog_home");
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void AddLayoutView(String type, final String TextOrAudio) {
        LayoutInflater li = (LayoutInflater) this.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        View overlayView = li.inflate(R.layout.linearlayout_overlay,null);
        // create display msgs
        String timeSpentStr = "";

        if(type.equals("start")) {
            Log.d(TAG, "openAppTodayStr "+timeSpentTodayStr);
            timeSpentStr = getResources().getString(R.string.tap_in_cn);
//            tvApp.setText(curAppName);
        }
        else {
            Log.d(TAG, "timeSpentTodayStr " + timeSpentTodayStr);
            timeSpentStr = getResources().getString(R.string.spend_time_cn);
        }

        oView = overlayView.findViewById(R.id.oView);
        oView.setBackgroundColor(Color.argb(240, 220, 220, 220));
        oView.setOrientation(LinearLayout.VERTICAL);
        oView.setGravity(Gravity.CENTER);

        // Text -- "You have already ..."
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setLayoutDirection(1);
        TextView tv = overlayView.findViewById(R.id.tv);
        tv.setText(timeSpentStr);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(14.0f);
        tv.setTextColor(Color.BLACK);
        tv.setLayoutParams(llp);

        // inView has the Icon and the a textView
        LinearLayout inView = overlayView.findViewById(R.id.inView);
        inView.setOrientation(LinearLayout.HORIZONTAL);
        inView.setGravity(Gravity.CENTER);


        if (DEBUG_Flag) {
            TextView tvApp = new TextView(this);
            if (type.equals("start")){
                tvApp.setText(curAppName);
            } else {
            long timeSpentToday = Long.valueOf(timeSpentTodayStr);
            long timeSpentTodayInSec = (timeSpentToday/ (long) 1000.0);
            long timeSpentTodayInMin = (timeSpentTodayInSec/(long) 60.0);
            long timeSpentTodayInHr = (timeSpentTodayInMin/ (long) 60.0);

            //String timeSpentStr = "You have spent " + Long.toString(timeSpentTodayInMin) + " min on this app today.";
                timeSpentStr = getResources().getString(R.string.spend_time_cn);
            tvApp.setText(curAppName);

//            String msg = "Try something else instead!";
            Log.d(TAG, "Timeout should display");
            //tvApp.setText("   "+ curAppName);
            if (timeSpentTodayInMin == 0) {
                tvApp.setText(Long.toString(timeSpentTodayInSec) + " secs on \n" + curAppName);
                //tvApp.setText(Long.toString(timeSpentTodayInMin) +" min " + Long.toString(timeSpentTodayInSec) + " secs on \n" + curAppName);
            }
            else if (timeSpentTodayInMin > 0 && timeSpentTodayInHr == 0){
                tvApp.setText(Long.toString(timeSpentTodayInMin) + " min on \n" + curAppName);
            }
            else {
                long timeSpentTodayInMinRemaining = timeSpentTodayInMin - (timeSpentTodayInHr*60);
                tvApp.setText(Long.toString(timeSpentTodayInHr) +" hr " + Long.toString(timeSpentTodayInMinRemaining) + " min on \n" + curAppName);
            }
            }
            // Getting Icon for current package And App Name to go next to it
            ImageView appIm = new ImageView(this);
            appIm.setMaxHeight(35);
            appIm.setMaxWidth(35);
            try {
                Drawable icon = getPackageManager().getApplicationIcon(curPackageName);
                appIm.setImageDrawable(icon);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, e.toString());
            }


            if (appIm.getDrawable() != null) {
                inView.addView(appIm);
            }
            tvApp.setGravity(Gravity.CENTER);
            tvApp.setTypeface(Typeface.DEFAULT_BOLD);
            tvApp.setTextSize(14.0f);
            tvApp.setTextColor(Color.BLACK);
            inView.addView(tvApp);
        }

        ImageView persuasive_icon = overlayView.findViewById(R.id.persuasive_icon);
        Log.d(TAG, "AddLayoutView: see if persuasive_icon is a valid view: "+persuasive_icon);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,400);
        lp.setMargins(20, 0, 20, 0);
        persuasive_icon.setLayoutParams(lp);
        persuasive_icon.setImageResource(R.mipmap.working_icon_transluscent);
        persuasive_icon.setVisibility(View.VISIBLE);

        // persuasive text
        final TextView instruction_text = overlayView.findViewById(R.id.instruction_text);
        final TextView middle_text = overlayView.findViewById(R.id.middle_text);
        middle_text.setTypeface(Typeface.DEFAULT_BOLD);
        middle_text.setText("");
        middle_text.setTextSize(4.0f);
        final TextView input_text = overlayView.findViewById(R.id.input_text);
        final TextView input_text2 = overlayView.findViewById(R.id.input_text2);
        final TextView hint_text = overlayView.findViewById(R.id.hint_text);
        hint_text.setText("");
        hint_text.setTextSize(4.0f);
        SpannableString persuasive_text = new SpannableString("11");
        SpannableString spannableString = new SpannableString("11");
        SpannableString spannableProvokeString = new SpannableString("");


        final EditText input_text_box = overlayView.findViewById(R.id.input_text_box);
        input_text_box.setSelection(0);
        final EditText input_text_box2 = overlayView.findViewById(R.id.input_text_box2);
        final Button input_audio_box = overlayView.findViewById(R.id.input_audio_box);


        if(currentPhase.equals("NO_INTERVENTION")) {
            //Do nothing for no intervention phase
        }else if(currentPhase.equals("POP_UP_LAYOUT")){
            instruction_text.setText("");
            spannableString = new SpannableString(random_text[0]);
            getResources().getColor(R.color.input_value);
            spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#8064A2"))
                    , Integer.parseInt(random_text[2]),Integer.parseInt(random_text[3]),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            persuasive_text = new SpannableString(random_text[1].substring(0,Integer.parseInt(random_text[4])) + "品味生活");


        } else if(currentPhase.equals("RANDOM_TEXT")){
            instruction_text.setText(getResources().getText(R.string.input_instruction_keep_cn));
            spannableString = new SpannableString(random_text[0]);
            persuasive_text = new SpannableString(random_text[1]);
        }else {
            instruction_text.setText(getResources().getText(R.string.input_instruction_need_cn));

            String middlesen = (String) getResources().getText(R.string.input_mid_cn);
            String value = random_text[0].substring(Integer.parseInt(random_text[2]),Integer.parseInt(random_text[3]));
            String middleSentence = middlesen + " " + value + getRandomText(Constants.thoughtProvokingList);
            spannableProvokeString = new SpannableString(middleSentence);
            spannableProvokeString.setSpan(new ForegroundColorSpan(Color.parseColor("#8064A2"))
                    , middlesen.length(), middlesen.length() + value.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            middle_text.setText(spannableProvokeString);
            middle_text.setTextSize(14.0f);
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    middle_text,12, 16, 1, TypedValue.COMPLEX_UNIT_DIP);

            hint_text.setText(getResources().getText(R.string.input_instruction_hint_cn));
            hint_text.setTextSize(14.0f);

            String inputText = random_text[0];
            spannableString = new SpannableString(inputText);
            spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#8064A2"))
                    , Integer.parseInt(random_text[2]),Integer.parseInt(random_text[3]),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            persuasive_text = new SpannableString(random_text[1]);
            persuasive_text.setSpan(new ForegroundColorSpan(Color.parseColor("#808080"))
                    , Integer.parseInt(random_text[4]),Integer.parseInt(random_text[5]),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        instruction_text.setTypeface(Typeface.DEFAULT_BOLD);
        instruction_text.setTextSize(14.0f);

        input_text.setText(spannableString);
        input_text.setTypeface(Typeface.DEFAULT_BOLD);
        input_text.setTextSize(16.0f);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                input_text,12, 20, 1, TypedValue.COMPLEX_UNIT_DIP);
        input_text.setTextColor(Color.parseColor("#6CAEA6"));
        input_text.setLayoutParams(llp);
        input_text2.setText(persuasive_text);
        input_text2.setTypeface(Typeface.DEFAULT_BOLD);
        input_text2.setTextSize(16.0f);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                input_text2,12, 20, 1, TypedValue.COMPLEX_UNIT_DIP);
        input_text2.setTextColor(Color.parseColor("#6CAEA6"));
        input_text2.setLayoutParams(llp);


        if(TextOrAudio.equals("text")) {
            // input_text_box
            input_text_box.setTextSize(14.0f);
            input_text_box.setGravity(Gravity.CENTER_VERTICAL);
            input_text_box.setTextColor(Color.parseColor("#525252"));
            input_text_box.setHintTextColor(Color.parseColor("#8b8b8b"));
            input_text_box.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(32, 5, 32, 5);
            input_text_box.setLayoutParams(params);
            input_text_box.setFocusable(true);
            input_text_box.setFocusableInTouchMode(true);
            input_text_box.requestFocus();
            input_text_box.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS | EditorInfo.TYPE_TEXT_VARIATION_FILTER);
            input_text_box2.setTextSize(14.0f);
            input_text_box2.setGravity(Gravity.CENTER_VERTICAL);
            input_text_box2.setTextColor(Color.parseColor("#525252"));
            input_text_box2.setHintTextColor(Color.parseColor("#8b8b8b"));
            input_text_box2.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            params2.setMargins(32, 5, 32, 5);
            input_text_box2.setLayoutParams(params2);
            input_text_box2.setFocusable(true);
            input_text_box2.setFocusableInTouchMode(true);
//            input_text_box2.requestFocus();
            input_text_box2.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS | EditorInfo.TYPE_TEXT_VARIATION_FILTER);
            // TODO:: do not know what to do with something works for others but not with me
            // final InputMethodManager imm = (InputMethodManager) input_text_box.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            input_text_box.setVisibility(View.VISIBLE);
            input_text_box2.setVisibility(View.VISIBLE);
            input_audio_box.setVisibility(View.INVISIBLE);
        }
        else if(TextOrAudio.equals("audio")) {
            // input_audio_box
            input_audio_box.setTextSize(12.0f);
            input_audio_box.setGravity(Gravity.CENTER_VERTICAL);
            input_audio_box.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            input_audio_box.setTextColor(Color.GRAY);
            input_audio_box.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            input_audio_box.setPadding(20,0,20,0);
            input_audio_box.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    initSpeech(this_activity);
                }
            });
            input_text_box.setVisibility(View.VISIBLE);
            input_text_box2.setVisibility(View.VISIBLE);
            input_audio_box.setVisibility(View.VISIBLE);
        }
        else {
            input_text_box.setVisibility(View.INVISIBLE);
            input_text_box2.setVisibility(View.INVISIBLE);
            input_audio_box.setVisibility(View.INVISIBLE);
        }

        input_text_box.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                user_input_text = String.valueOf(input_text_box.getText());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                Log.i(TAG, "before input");
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.i(TAG, "after input");
            }
        });

        input_text_box2.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                user_input_text2 = String.valueOf(input_text_box2.getText());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                Log.i(TAG, "before input");
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.i(TAG, "after input");
            }
        });

        // Create buttons to remove overlay
        LinearLayout buttonLayoutView = overlayView.findViewById(R.id.button_layout);
        buttonLayoutView.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayoutView.setGravity(Gravity.CENTER);
        Button submit = new Button(this);
        if (currentPhase.equals("MEANINGFUL_TEXT")) {
            submit.setText(getResources().getString(R.string.submit_cn_ours));
        } else {
            submit.setText(getResources().getString(R.string.submit_cn_controls));
        }
        submit.setTextSize(11.0f);
        submit.setTextColor(Color.BLACK);
        submit.setTag("submitBtn");
        submit.setLayoutParams(llp);
        submit.setPadding(20,0,20,0);
        submit.setBackgroundColor(Color.argb(255, 200, 200, 200));
        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(TextOrAudio.equals("text")) {
                    user_input_text = input_text_box.getText().toString();
                    user_input_text2 = input_text_box2.getText().toString();
                }
                else {
                    // TODO::
                }
                endTimeout("on_submit");
            }
        });
        submit.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)
                {
                    v.setBackgroundColor(Color.argb(255, 255, 255, 255));
                }
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                {
                    v.setBackgroundColor(Color.argb(255, 200, 200, 200));
                }
                return false;
            }
        });
        Button dismiss = new Button(this);
//        if (currentPhase.equals("MEANINGFUL_TEXT")) {
        dismiss.setText(getResources().getString(R.string.dismiss_cn_ours));
//        } else {
//            dismiss.setText(getResources().getString(R.string.dismiss_cn_controls));
//        }
        dismiss.setTextSize(11.0f);
        dismiss.setTextColor(Color.BLACK);
        dismiss.setTag("dismissBtn");
        dismiss.setLayoutParams(llp);
        dismiss.setPadding(20,0,20,0);
        dismiss.setBackgroundColor(Color.argb(255, 200, 200, 200));
        dismiss.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Constants.lastInputTime.put(curPackageName, (long) 0);
                Constants.lastInputRight.put(curPackageName, false); //Set the lastInputRight for judgement in Timeout about whether pop up.
                returnHome("on_dismiss");
            }
        });
        dismiss.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)
                {
                    v.setBackgroundColor(Color.argb(255, 255, 255, 255));
                }
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                {
                    v.setBackgroundColor(Color.argb(255, 200, 200, 200));
                }
                return false;
            }
        });
        TextView empty_space_textview = new TextView(this);
        empty_space_textview.setWidth(30);
        buttonLayoutView.addView(submit);
        buttonLayoutView.addView(empty_space_textview);
        buttonLayoutView.addView(dismiss);

        ImageButton skip = (ImageButton)overlayView.findViewById(R.id.skipButton);
        skip.setImageResource(R.mipmap.cross);
        skip.setVisibility(View.VISIBLE);
        skip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                    endTimeout("skip");
                }
        });
        skip.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
                {
                    return false;
                }
        });
        skip.setVisibility(View.VISIBLE);
    }

    /*
     * 创建一个listener
     */
    class MySynthesizerListener implements SynthesizerListener {
        @Override
        public void onSpeakBegin() {
            Toast toast = Toast.makeText(this_activity, "开始播放", Toast.LENGTH_SHORT);
            toast.show();
        }
        @Override
        public void onSpeakPaused() {
            Toast toast = Toast.makeText(this_activity, "暂停播放", Toast.LENGTH_SHORT);
            toast.show();
        }
        @Override
        public void onSpeakResumed() {
            Toast toast = Toast.makeText(this_activity, "继续播放", Toast.LENGTH_SHORT);
            toast.show();
        }
        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos , String info) {
            // 合成进度
        }
        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
        }
        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                Toast toast = Toast.makeText(this_activity, "播放完成", Toast.LENGTH_SHORT);
                toast.show();
            } else if (error != null ) {
                Toast toast = Toast.makeText(this_activity, error.getPlainDescription( true), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        @Override
        public void onEvent(int eventType, int arg1 , int arg2, Bundle obj) {
            //if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //     String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //     Log.d(TAG, "session id =" + sid);
            //}
        }
    }

    class MyInitListener implements InitListener {
        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {
                Toast toast = Toast.makeText(this_activity, "初始化失败", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    /**
     * 初始化语音识别
     */
    public void initSpeech(final Context context) {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{
                    android.Manifest.permission.RECORD_AUDIO},1);
        }else {
            mDialog = new RecognizerDialog(context, new MyInitListener());
            mDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            mDialog.setParameter(SpeechConstant.ACCENT, "mandarin");
            mDialog.setListener(new RecognizerDialogListener() {
                @Override
                public void onResult(RecognizerResult recognizerResult, boolean isLast) {
                    if (!isLast) {
                        // TODO:: not stable, sometimes get nothing
                        user_input_text = parseVoice(recognizerResult.getResultString());
                        Log.d(TAG, "onListenerResult: "+user_input_text);
                    }
                }
                @Override
                public void onError(SpeechError speechError) {

                }
            });
            mDialog.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==1&&grantResults[0]== PackageManager.PERMISSION_GRANTED){
            RecognizerDialog mDialog = new RecognizerDialog(this, new MyInitListener());
            mDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            mDialog.setParameter(SpeechConstant.ACCENT, "mandarin");
            mDialog.setListener(new RecognizerDialogListener() {
                @Override
                public void onResult(RecognizerResult recognizerResult, boolean isLast) {
                    if (!isLast) {
                        user_input_text = parseVoice(recognizerResult.getResultString());
                    }
                }
                @Override
                public void onError(SpeechError speechError) {

                }
            });
            mDialog.show();
        }else {
            Toast.makeText(this,"用户拒绝了权限",Toast.LENGTH_SHORT).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public String parseVoice(String resultString) {
        Gson gson = new Gson();
        Voice voiceBean = gson.fromJson(resultString, Voice.class);
        StringBuffer sb = new StringBuffer();
        ArrayList<Voice.WSBean> ws = voiceBean.ws;
        for (Voice.WSBean wsBean : ws) {
            String word = wsBean.cw.get(0).w;
            sb.append(word);
        }
        return sb.toString();
    }

    public class Voice {
        public ArrayList<WSBean> ws;
        public class WSBean {
            public ArrayList<CWBean> cw;
        }
        public class CWBean {
            public String w;
        }
    }

    private String getCurrentPhase() {
        String currentPhase = "NO_INTERVENTION";
        SharedPreferences phaseSettings = getApplicationContext().getSharedPreferences("phaseSetting", 0);
        String phaseVal = phaseSettings.getString("phase", "0");
        if((!phaseVal.equals("0"))){
            currentPhase = phaseVal;
        }
        if(!Constants.phases.contains(currentPhase))
            currentPhase = Constants.phases.get(0); // NO_INTERVENTION
        return currentPhase;
    }

    private void sendInterventionFailureData(){
        Log.d(TAG, "Ready to send intervention FAILED data");
        Intent intentToLog = new Intent(Constants.ACTION_LOG_INTERVENTION);
        intentToLog.putExtras(intervBundle);
        intentToLog.putExtra("dismiss_timestamp", "");
        intentToLog.putExtra("overlay_permitted", false);
        sendBroadcast(intentToLog);
        Log.d(TAG, "Sent failure broadcast");
        intervBundle = null;
    }

    private void sendInterventionData(String result, String temp_succeeded){
        if (oView!=null && intervBundle!=null) {
            c = Calendar.getInstance();
            long now_millis = c.getTimeInMillis();
            Date now = c.getTime();
            Log.d(TAG, "Ready to send intervention data");
            Intent intentToLog = new Intent(Constants.ACTION_LOG_INTERVENTION);
            intentToLog.putExtras(intervBundle);
            intentToLog.putExtra("interventionResult",result);
            intentToLog.putExtra("tempt_succeeded",temp_succeeded);
            intentToLog.putExtra("persuasive_text", random_text[0] + "&&&" + random_text[1] );
            String user_input = (TextOrAudio.equals("text") || TextOrAudio.equals("audio")) ? user_input_text + "&&&" + user_input_text2: "NO_TEXT_OR_AUDIO_INPUT";
            intentToLog.putExtra("user_input",user_input);
            intentToLog.putExtra("OverlayType",OverlayType);
            intentToLog.putExtra("start_overlay_timestamp", start_time.toString());
            intentToLog.putExtra("end_overlay_timestamp",now.toString());
            intentToLog.putExtra("dismiss_timestamp", Long.toString(now_millis-start_millis));
            intentToLog.putExtra("overlay_permitted", true);
            sendBroadcast(intentToLog);
            Log.d(TAG, "Sent broadcast");
        }
    }


    private void returnHome(String result_string){
        Log.d(TAG, "Dismiss button clicked");
        sendInterventionData(result_string,"return_home");
        Constants.lastInputRight.put(curPackageName, false);
        Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
        mHomeIntent.addCategory(Intent.CATEGORY_HOME);
        mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(mHomeIntent);
        intervBundle = null;
        this_activity.finish();
    }

    private void endTimeout(String result){
        pass = false;
        Log.d(TAG, "Submit and continue button clicked");
        user_input_text = user_input_text.toLowerCase();
        user_input_text2 = user_input_text2.toLowerCase();
        random_text[0] = random_text[0].toLowerCase();
        random_text[1] = random_text[1].toLowerCase();
        LayoutInflater li = (LayoutInflater) this.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(result.equals("skip")) {
            pass = true;
            skipButtonPressed = true;
            Toast toast = Toast.makeText(this_activity, getResources().getText(R.string.input_instruction_skip_cn), Toast.LENGTH_SHORT);
            toast.show();
        }
        else if(TextOrAudio.equals("text")) {
            if (currentPhase.equals("RANDOM_TEXT")) {
                pass = ((user_input_text.replace(" ", "").equals(random_text[0].replace(" ", ""))) &&
                        (user_input_text2.replace(" ", "").equals(random_text[1].replace(" ", ""))));
            } else if (currentPhase.equals("MEANINGFUL_TEXT")){
                if((user_input_text.replace(" ", "").equals(random_text[0].replace(" ", ""))) &&
                        (user_input_text2.length() > Integer.parseInt(random_text[4]) + 1) &&
                        (user_input_text2.substring(0,Integer.parseInt(random_text[4])).replace(" ", "").equals(
                                random_text[1].substring(0,Integer.parseInt(random_text[4])).replace(" ", "")))){
                    pass = true;
                }else if(Math.abs(user_input_text.length() - random_text[0].length()) > 1 ||
                        (user_input_text2.length() <= Integer.parseInt(random_text[4]) + 1)){
                    pass = false;
                }else {
                    if(user_input_text.length() != random_text[0].length()){
                        if(user_input_text.contains(random_text[0]) || random_text[0].contains(user_input_text)){
                            pass = true;
                        }else {
                            pass = false;
                        }
                    }else {
                        int count1 = 0;
                        int count2 = 0;
                        for(int i = 0; i < user_input_text.length(); i++){
                            if(user_input_text.charAt(i) != random_text[0].charAt(i)){
                                count1++;
                            }
                        }
                        for(int j = 0; j < Integer.parseInt(random_text[4]); j++){
                            if(user_input_text2.charAt(j) != random_text[1].charAt(j)){
                                count2++;
                            }
                        }
                        if(count1 > 1 || count2 > 1){
                            pass = false;
                        }else {
                            pass = true;
                        }
                    }
                }
            }

            Log.d(TAG, "endTimeout: pass="+pass+" user_input_text="+user_input_text +" getPersuasiveText0(): " + (random_text[0]));
            Log.d(TAG, "endTimeout: pass="+pass+" user_input_text2="+user_input_text2 +" getPersuasiveText1(): " + (random_text[1]));
        }
        else if(TextOrAudio.equals("audio")){
            pass = ((getSimilarityRatio(user_input_text,random_text[0]) >= 70.0));
            if (!pass && user_input_text.equals("")) {
                pass = true;
            }
            Log.d(TAG, "endTimeout: pass="+pass+" user_input_text="+user_input_text);
        }
        else if(TextOrAudio.equals("pop_up")) {
            pass = true;
            Log.d(TAG, "endTimeout: pass="+pass+" should be true for only pop_up");
        }

        if(pass) {
            PackageManager packageManager = getPackageManager();
            Log.d(TAG, "onClick for enter: clicked");
            if (!currentPhase.equals("MEANINGFUL_TEXT") || skipButtonPressed){
                Constants.lastInputTime.put(curPackageName, intent.getLongExtra("timestamp", 0));
                Constants.lastInputRight.put(curPackageName, pass); //Set the lastInputRight for judgement in Timeout about whether pop up.
                sendInterventionData(result + "_enter_app", "succeed");
                Intent intent = packageManager.getLaunchIntentForPackage(curAppName);
                if (intent != null) {
                    Log.d(TAG, "endTimeout: intent != null");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(intent);
                }
                OverlayActivity.this.finish();
                //moveTaskToBack(true);
            }
            else {
                // TODO:: timer and time elapsed
//            sendInterventionData(result,"succeed");
//            removeOverlay();
                AlertDialog.Builder builder = new AlertDialog.Builder(OverlayActivity.this);
                View alert_dialog_view = LayoutInflater.from(OverlayActivity.this).inflate(
                        R.layout.alert_dialog_enter_app, null);
                builder.setView(alert_dialog_view);

                String pBtnText = "";
                String nBtnText = "";
                if (currentPhase.equals("MEANINGFUL_TEXT")) {
                    nBtnText = (String) getResources().getText(R.string.input_instruction_enter_cn);// + "[" + getResources().getString(R.string.confirm_enter_cn) + "]";
                    pBtnText = (String) getResources().getText(R.string.input_instruction_exit_cn);// + "[" + getResources().getString(R.string.exit_app_cn) + "]";
                } else { // this is now useless
                    nBtnText = getResources().getString(R.string.confirm_enter_cn);
                    pBtnText = getResources().getString(R.string.exit_app_cn);
                }

            builder.setPositiveButton(pBtnText,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
//                                dialog.dismiss();
                            Constants.lastInputRight.put(curPackageName, false); //Set the lastInputRight for judgement in Timeout about whether pop up.
                            Log.d(TAG, "EXIT APP AFTER RIGHT INPUT");
                            clickDialog = true;
                            returnHome("on_dialog_exit");
                        }
                    }
                    );

//            builder.setNegativeButton(getResources().getString(R.string.exit_app_cn),
                builder.setNegativeButton(nBtnText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Constants.lastInputTime.put(curPackageName, intent.getLongExtra("timestamp", 0));
                        Constants.lastInputRight.put(curPackageName, pass); //Set the lastInputRight for judgement in Timeout about whether pop up.
                        clickDialog = true;
                        sendInterventionData(result + "_enter_app", "succeed");
                        Intent intent = packageManager.getLaunchIntentForPackage(curAppName);
                        Log.d(TAG, "INTENT?" + intent);
                        if (intent != null) {
                            Log.d(TAG, "endTimeout: intent != null");
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            startActivity(intent);
                        }
                        OverlayActivity.this.finish();
                        //moveTaskToBack(true);
                    }


                }
                );

                AlertDialog alert = builder.create();
                alert.show();
                Button negative_button = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
                Button positive_button = alert.getButton(DialogInterface.BUTTON_POSITIVE);
                negative_button.setTextColor(Color.GRAY);
                positive_button.setTextColor(Color.GRAY);

            }

        }
        else {
            Constants.lastInputRight.put(curPackageName, false); //Set the lastInputRight for judgement in Timeout about whether pop up.
            sendInterventionData(result,"failed");
            if(TextOrAudio.equals("audio")) {
                Toast toast = Toast.makeText(this_activity, getResources().getText(R.string.input_instruction_detect_cn)+
                        user_input_text+getResources().getText(R.string.input_instruction_wrong_cn), Toast.LENGTH_SHORT);
                toast.show();
            }
            else {
                if (currentPhase.equals("MEANINGFUL_TEXT") &&
                    (user_input_text2.length() >= Integer.parseInt(random_text[4])) &&
                    (user_input_text2.substring(0,Integer.parseInt(random_text[4])).replace(" ", "").equals(
                            random_text[1].substring(0,Integer.parseInt(random_text[4])).replace(" ", "")))
                    ) {
                    Toast toast = Toast.makeText(this_activity, getResources().getText(R.string.input_instruction_two_cn), Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(this_activity, getResources().getText(R.string.input_instruction_wrong_cn), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }
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

    private String getRandomText(ArrayList content){
        Random r = new Random();
        int randomNum = r.nextInt(content.size());
        return (String) content.get(randomNum);
    }

    private String getRandomText(String[] content){
        Random r = new Random();
        int randomNum = r.nextInt(content.length);
        return content[randomNum];
    }

    private ArrayList getValueContent(){
        ArrayList valueContent = new ArrayList();
        ArrayList valueStart = new ArrayList();
        ArrayList valueEnd = new ArrayList();

        for(int i = 1; i < Constants.userValue.length; i++) {
            if (Constants.userValue[i]) {
                for (int j = 0; j < Constants.interventionContentComponent_value[i].length; j++) {
                    valueContent.add(Constants.interventionContentComponent_value[i][j][0]);
//                    valueStart.add(Integer.parseInt(Constants.interventionContentComponent_value[i][j][1]));
//                    valueEnd.add(Integer.parseInt(Constants.interventionContentComponent_value[i][j][2]));
                    valueStart.add(Constants.interventionContentComponent_value[i][j][1]);
                    valueEnd.add(Constants.interventionContentComponent_value[i][j][2]);
                }
            }
        }
        Random r = new Random();
        int randomNum = r.nextInt(valueContent.size());
        ArrayList result = new ArrayList();
        result.add(valueContent.get(randomNum));
        result.add(valueStart.get(randomNum));
        result.add(valueEnd.get(randomNum));
        return result;
    }

    /*
    Get random meaningless sentences for input. Only used in phase RANDOM_TEXT
     */
    private String getMeaninglessText() {
        Random rand = new Random();
        //int len = rand.nextInt(5) + 4;
        String[] figures = new String[4];
        //boolean blank_insert_flag = false;
        String text = "";
        for (int i = 0; i < figures.length; i ++){
            int rand_idx = rand.nextInt(9) + 1;
            figures[i] = Constants.int2String.get(rand_idx);
//            text = text + Constants.int2String.get(rand_idx);
            /*if (i == len/2-1 & !blank_insert_flag){
                text += " ";
                blank_insert_flag = true;
            }*/
        }
        text = figures[0] + " " + figures[1] + " " + figures[2] + " " + figures[3];
//        text = figures[0] + "千" + figures[1] + "百" + figures[2] + "十" + figures[3];
//        String text = "";
//        text += getRandomText(Constants.MeaninglessWordList) + " ";
//        text += getRandomText(Constants.MeaninglessWordList) + " ";
//        text += getRandomText(Constants.MeaninglessWordList);
        return text;
    }

    /*
    * Get persuasive text, will set the persuasiveText array with following rules:
    * @return The Persuasive Text
    * @para newText Boolean Whether requesting a new Text
    * */
    private String[] getPersuasiveText(boolean newText) {
        // index 0 for value content, 1 for action content
        // 2 for value start index of value content, 3 for value end index of value content
        // 4 for value start index of action content, 5 for value end index of action content
        String[] result = new String[6];
        if(newText){
            Calendar c = Calendar.getInstance();
            int inTime = 0; //inTime 0 for work time, 1 for sleep time, 2 for all time
            if(Constants.WorkSleepTime[0] == 0 &&Constants.WorkSleepTime[1] == 0 &&Constants.WorkSleepTime[2] == 0 &&Constants.WorkSleepTime[3] == 0){
                Log.d(TAG, "No time recording, then always intervention");
                inTime = 2;
            }else if(c.get(Calendar.DAY_OF_WEEK) > 1 && c.get(Calendar.DAY_OF_WEEK) < 7){
                Log.d(TAG, "In Weekday");
                if(c.get(Calendar.HOUR_OF_DAY) >= Constants.WorkSleepTime[0] &&
                        c.get(Calendar.HOUR_OF_DAY) <= Constants.WorkSleepTime[2]){
                    Log.d(TAG, "In hours of Weekday Work Time");
                    if(c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[0]){
                        Log.d(TAG, "In same hour with the start hour");
                        if(c.get(Calendar.MINUTE) > Constants.WorkSleepTime[1]){
                            Log.d(TAG, "The minute is later than the start time, then in the intervention period");
                            inTime = 0;
                        }
                    }else if(c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[2]){
                        if(c.get(Calendar.MINUTE) < Constants.WorkSleepTime[3]){
                            Log.d(TAG, "The minute is earlier than the end time, then in the intervention period");
                            inTime = 0;
                        }
                    }else {
                        Log.d(TAG," Normal case, not at the hour of start or end");
                        inTime = 0;
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
                                inTime = 1;
                            }
                        } else if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[6]) {
                            if (c.get(Calendar.MINUTE) < Constants.WorkSleepTime[7]) {
                                Log.d(TAG, "The minute is earlier than the end time, then in the intervention period");
                                inTime = 1;
                            }
                        } else {
                            Log.d(TAG, "Normal case, not at the hour of start or end");
                            inTime = 1;
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
                                inTime = 1;
                            }
                        }else {
                            Log.d(TAG, "Normal case");
                            inTime = 1;
                        }
                    }else if(c.get(Calendar.HOUR_OF_DAY) <= Constants.WorkSleepTime[6]){
                        Log.d(TAG, "If current hour is earlier than the end hour");
                        if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[6]) {
                            Log.d(TAG, "In same hour with the end hour");
                            if (c.get(Calendar.MINUTE) < Constants.WorkSleepTime[5]) {
                                Log.d(TAG, "The minute is earlier than the end hour, then in the intervention period");
                                inTime = 1;
                            }
                        }else {
                            Log.d(TAG, "Normal case");
                            inTime = 1;
                        }
                    }
                }else if(Constants.WorkSleepTime[4] == Constants.WorkSleepTime[6]){
                    Log.d(TAG, "The start hour of sleep time is equal to the end time, we need to pick interval");
                    if(c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[4]){
                        Log.d(TAG, "If current hour is equal to the start hour and end hour(they are same)");
                        if (c.get(Calendar.MINUTE) > Constants.WorkSleepTime[5] && c.get(Calendar.MINUTE) < Constants.WorkSleepTime[7]) {
                            Log.d(TAG, "The minute is later than the start minute and earlier than the end minute, then in the intervention period");
                            inTime = 1;
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
                            inTime = 0;
                        }
                    }else if(c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[10]){
                        if(c.get(Calendar.MINUTE) < Constants.WorkSleepTime[11]){
                            Log.d(TAG, "The minute is earlier than the end time, then in the intervention period");
                            inTime = 0;
                        }
                    }else {
                        Log.d(TAG, "Normal case, not at the hour of start or end");
                        inTime = 0;
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
                                inTime = 1;
                            }
                        } else if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[14]) {
                            if (c.get(Calendar.MINUTE) < Constants.WorkSleepTime[15]) {
                                Log.d(TAG, "The minute is earlier than the end time, then in the intervention period");
                                inTime = 1;
                            }
                        } else {
                            Log.d(TAG, "Normal case, not at the hour of start or end");
                            inTime = 1;
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
                                inTime = 1;
                            }
                        }else {
                            Log.d(TAG, "Normal case");
                            inTime = 1;
                        }
                    }else if(c.get(Calendar.HOUR_OF_DAY) <= Constants.WorkSleepTime[14]){
                        Log.d(TAG, "If current hour is earlier than the end hour");
                        if (c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[14]) {
                            Log.d(TAG, "In same hour with the end hour");
                            if (c.get(Calendar.MINUTE) < Constants.WorkSleepTime[15]) {
                                Log.d(TAG, "The minute is earlier than the end hour, then in the intervention period");
                                inTime = 1;
                            }
                        }else {
                            Log.d(TAG, "Normal case");
                            inTime = 1;
                        }
                    }
                }else if(Constants.WorkSleepTime[12] == Constants.WorkSleepTime[14]){
                    Log.d(TAG, "The start hour of sleep time is equal to the end time, we need to pick interval");
                    if(c.get(Calendar.HOUR_OF_DAY) == Constants.WorkSleepTime[14]){
                        Log.d(TAG, "If current hour is equal to the start hour and end hour(they are same)");
                        if (c.get(Calendar.MINUTE) > Constants.WorkSleepTime[13] && c.get(Calendar.MINUTE) < Constants.WorkSleepTime[15]) {
                            Log.d(TAG, "The minute is later than the start minute and earlier than the end minute, then in the intervention period");
                            inTime = 1;
                        }
                    }
                }
            }else {
                inTime = -1;
            }

            ArrayList valueContent = getValueContent();
            result[0] = (String)valueContent.get(0);
            result[2] = (String)valueContent.get(1);
            result[3] = (String)valueContent.get(2);
            ArrayList actionContent = new ArrayList();
            if (inTime == 2) {
//                actionContent = new ArrayList<String[]>(java.util.Arrays.asList(Constants.interventionContentComponent_action_customize));
//                result[1] = getRandomText(actionContent);
                Random r = new Random();
                int randomNum = r.nextInt(Constants.interventionContentComponent_action_customize.length);
                String[] action = Constants.interventionContentComponent_action_customize[randomNum];
                result[1] = action[0];
                result[4] = action[1];
                result[5] = action[2];

            } else if(inTime == 0){
                actionContent = new ArrayList<String>(java.util.Arrays.asList(Constants.interventionContentComponent_action[0]));
                result[1] = getRandomText(actionContent);
            }else if(inTime == 1){
                actionContent = new ArrayList<String>(java.util.Arrays.asList(Constants.interventionContentComponent_action[1]));
                result[1] = getRandomText(actionContent);
            }else {
                result[1] = "ERROR";
                result[2] = 1 + "";
                result[3] = 2 + "";
            }
            return result;
        } else {
            return random_text;
        }
    }

    public static float getSimilarityRatio(String str, String target) {
        int d[][]; // Matrix
        int n = str.length();
        int m = target.length();
        int i; // for loop of str
        int j; // for loop of target
        char ch1; // char for str
        char ch2; // char for target
        int temp; // 0 or 1, record the same char
        if (n == 0 || m == 0) {
            return 0;
        }
        d = new int[n + 1][m + 1];
        for (i = 0; i <= n; i++) { // initialize
            d[i][0] = i;
        }
        for (j = 0; j <= m; j++) { // initialize
            d[0][j] = j;
        }
        for (i = 1; i <= n; i++) {
            ch1 = str.charAt(i - 1);
            // match target
            for (j = 1; j <= m; j++) {
                ch2 = target.charAt(j - 1);
                if (ch1 == ch2 || ch1 == ch2 + 32 || ch1 + 32 == ch2) {
                    temp = 0;
                } else {
                    temp = 1;
                }
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + temp);
            }
        }
        return (1 - (float) d[n][m] / Math.max(str.length(), target.length())) * 100F;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK ) {
            // TODO:: click once but pop up twice
            Toast toast = Toast.makeText(this_activity, getResources().getText(R.string.input_instruction_require_cn), Toast.LENGTH_SHORT);
            toast.show();
            return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendInterventionData("destroy","destroy");
//        removeOverlay();
    }
}
