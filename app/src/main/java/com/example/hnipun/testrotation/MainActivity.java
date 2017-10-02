package com.example.hnipun.testrotation;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
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

    public final String ACTION_LOCATION_BROADCAST = "ActivityBroadcast";
    //Recieve the probility broadcast here
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float EXTRA_MRT = intent.getFloatExtra("extra_MRT", 0);
            float EXTRA_Bicycle = intent.getFloatExtra("extra_Bicycle", 0);
            float EXTRA_Bus = intent.getFloatExtra("extra_Bus", 0);
            float EXTRA_Still = intent.getFloatExtra("extra_Still", 0);
            float EXTRA_Taxi = intent.getFloatExtra("extra_Taxi", 0);
            float EXTRA_Walking = intent.getFloatExtra("extra_Walking", 0);

            float[] all = {EXTRA_MRT, EXTRA_Bicycle, EXTRA_Bus, EXTRA_Still, EXTRA_Taxi, EXTRA_Walking};
            //Get the max probability
            float max = Integer.MIN_VALUE;
            int index = -20;
            for (int i = 0; i < all.length; i++) {
                if (all[i] > max) {
                    max = all[i];
                    index = i;
                }
            }
        }
    };


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
                    Log.d("Padmal", mode + " " + place);
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
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
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
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            mode = parent.getItemAtPosition(pos).toString();

            if (!mode.equals("Choose a Mode")) {
                if (check) {
                    Intent intent1 = new Intent(getBaseContext(), MyService.class);
                    stopService(intent1);
                }
                //Start the Service
                Intent intent2 = new Intent(getBaseContext(), MyService.class);
                intent2.putExtra("mode", mode);
                startService(intent2);
                check = true;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }
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
        //startService(new Intent(this, MyService.class));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(ACTION_LOCATION_BROADCAST));
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }
}
