package com.example.hnipun.testrotation;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Spinner modeSpinner, placeSpinner;
    private EditText customPlace;

    private Button record;

    private String mode = null;
    private String place = "";

    boolean check = false, recording = false;

    private int modePos = 0;

    List<String> modeList = new ArrayList<>();
    List<String> placeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        customPlace = (EditText) findViewById(R.id.custom_place);
        customPlace.setVisibility(View.INVISIBLE);

        record = (Button) findViewById(R.id.record);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Fetch custom place
                if (customPlace.getVisibility() == View.VISIBLE) {
                    place = customPlace.getText().toString();
                }
                // Check if the recording is actual
                if (modePos != 0 && !recording) {
                    Intent recorder = new Intent(MainActivity.this, MyService.class);
                    mode = modeList.get(modePos);
                    recorder.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    recorder.putExtra("mode", mode);
                    recorder.putExtra("place", place);
                    recording = true;
                    record.setText("Stop Recording!");
                    Toast.makeText(getApplicationContext(), "Recording initiated!", Toast.LENGTH_SHORT).show();
                    startService(recorder);
                } else if (recording) {
                    Intent recorder = new Intent(MainActivity.this, MyService.class);
                    record.setText("Start Recording!");
                    recording = false;
                    Toast.makeText(getApplicationContext(), "Recording stopped!", Toast.LENGTH_SHORT).show();
                    stopService(recorder);
                }
            }
        });
        addListenerOnSpinnerItemSelection();
    }

    public void addListenerOnSpinnerItemSelection() {
        modeSpinner = (Spinner) findViewById(R.id.spinner1);
        modeList.add("Choose a Mode");
        modeList.add("Taxi Car");
        modeList.add("Bus");
        modeList.add("MRT");
        modeList.add("Bicycle");
        modeList.add("Walking");
        modeList.add("Running");
        modeList.add("Still");
        modeList.add("Staircase");
        modeList.add("Taking Lift");
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, modeList);
        modeSpinner.setAdapter(modeAdapter);
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                modePos = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {/**/}
        });

        placeSpinner = (Spinner) findViewById(R.id.spinner_place);
        placeList.add("Cinema");
        placeList.add("Cafe");
        placeList.add("Park");
        placeList.add("Garden");
        placeList.add("Hall");
        placeList.add("Other");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, placeList);
        placeSpinner.setAdapter(dataAdapter);
        placeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                place = placeList.get(i);
                if (placeList.size() == i + 1) {
                    customPlace.setVisibility(View.VISIBLE);
                } else {
                    customPlace.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {/**/}
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MyService.class);
        stopService(intent);
        Toast.makeText(this, "Thanks for using application!!", Toast.LENGTH_LONG).show();
        finish();
    }

    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "Application closed!", Toast.LENGTH_LONG).show();
    }
}
