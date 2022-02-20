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
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
//import android.support.design.widget.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.provider.CalendarContract;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
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
import android.widget.EditText;
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

public class SetupActivity extends AppCompatActivity {
    private String currentPhase;
    private int clicked = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get current phase
        currentPhase = getIntent().getStringExtra("phase");
        setContentView(R.layout.activity_setup);

        final TextView typeout_text = (TextView) findViewById(R.id.typeout_text);
        final TextView useless_text = (TextView) findViewById(R.id.useless_text);
        Button setupBtn = (Button) findViewById(R.id.setup_button);
        setupBtn.setVisibility(View.VISIBLE);

        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View setup_button_view = v;
                launchWhitelist(setup_button_view);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(clicked > 1){
            SetupActivity.this.finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        clicked = clicked + 1;
        Log.d("MATAG", "result: " + clicked);
    }

    public void launchWhitelist(View view){
        Toast toast=Toast.makeText(getApplicationContext(),getResources().getString(R.string.wait_cn),Toast.LENGTH_SHORT);
        toast.show();
        clicked = 0;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent myIntent = new Intent(SetupActivity.this, WhitelistActivity.class);
                myIntent.putExtra("phase", currentPhase); //Optional parameters
                SetupActivity.this.startActivity(myIntent);
            }
        }, 2000);
        //SetupActivity.this.finish();
    }
}
