package com.example.hnipun.testrotation;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

//import for tensorflow
import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

/**
 * Created by hnipun on 7/28/2017.
 */

public class MyService extends Service implements SensorEventListener, LocationListener {
    // GPS
    private LocationManager locationManager;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 0;
    // Sensor initialization
    private SensorManager mSensorManager;
    private Sensor ASensor, LSensor, PSensor;
    // Logger for cell scan
    private Logger logger;
    //array list for samples
    ArrayList<String> Rawaccel = new ArrayList<>();
    ArrayList<String> noise = new ArrayList<>();
    ArrayList<String> Light = new ArrayList<>();
    ArrayList<String> Proximity = new ArrayList<>();
    ArrayList<String> GPS = new ArrayList<>();
    ArrayList<String> WiFi = new ArrayList<>();
    ArrayList<String> listOfPSCs = new ArrayList<>();

    // Random number for .text id
    String fileNameSuffix;

    //For audio recording
    private static final int sampleRate = 8000; //8000
    private AudioRecord audio;
    private int bufferSize;
    private static final int SAMPLE_DELAY = 30;

    // Threads
    private Thread thread;
    private Thread Wthread;

    private WifiManager wifiManager;
    private TelephonyManager tm;

    private static final int WRITE_DELAY = 10000; //8000
    String mode = null;

    private String TAG = "Debug";
    private String label = "";
    // For save accel values
    private final int N_SAMPLES = 128;
    private List <Double> input_signal = new ArrayList<>();
    //For low pass and high pass filter
    private boolean check = true;
    double prevX =0;
    double prevY =0;
    double prevZ =0;
    double alpha = 0.992;
    private int NodeCount = 0;

    @Override
    public void onCreate() {
        // Initialize sensors
        logger = new Logger(this);
        fileNameSuffix = String.valueOf(System.currentTimeMillis());

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        ASensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        LSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        PSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        // Initialize location
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.PASSIVE_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    this);
        } catch (Exception ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        }

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    this);
        } catch (Exception ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        }

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    this);
        } catch (Exception ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        }
        // Intialize audio
        try {
            bufferSize = AudioRecord
                    .getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
        } catch (Exception e) {
            android.util.Log.e("TrackingFlow", "Exception", e);
        }

        setupWiFi();

        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    private void startLogging() {

        try {
            JSONObject cellScan = new JSONObject();
            HashMap<String, String> cellIDwithPSC = logger.getCellIDs();
            GsmCellLocation location = (GsmCellLocation) tm.getCellLocation();

            final int cid = location.getCid();
            final int lac = location.getLac();

            final String networkOperator = tm.getNetworkOperator();
            final int mcc = Integer.parseInt(networkOperator.substring(0, 3));
            final int mnc = Integer.parseInt(networkOperator.substring(3));
            int psc = Math.abs(location.getPsc());
            cellScan.put("Connected CID", cid);
            cellScan.put("Connected PSC", psc);
            cellScan.put("Connected LAC", lac);
            cellScan.put("Connected MCC", mcc);
            cellScan.put("Connected MNC", mnc);

            // Update local store of PSCs with Cell IDs if a new CID is encountered
            if (!cellIDwithPSC.containsKey(String.valueOf(psc))) {
                logger.setCellIDs(String.valueOf(psc) + "," + String.valueOf(cid));
            }
            JSONArray neighbours = new JSONArray();
            JSONArray allCells = new JSONArray();
            for (NeighboringCellInfo i : tm.getNeighboringCellInfo()) {
                if (cellIDwithPSC.containsKey(String.valueOf(Math.abs(i.getPsc())))) {
                    JSONObject object = new JSONObject();
                    object.put("CID", cellIDwithPSC.get(String.valueOf(i.getPsc())));
                    object.put("PSC", i.getPsc());
                    object.put("RSSI",  i.getRssi());
                    object.put("LAC", i.getLac());
                    neighbours.put(object);
                }
                JSONObject object = new JSONObject();
                object.put("PSC", i.getPsc());
                object.put("RSSI",  i.getRssi());
                object.put("LAC", i.getLac());
                allCells.put(object);
            }
            cellScan.put("Actual Scan", neighbours);
            cellScan.put("All Neighbours", allCells);
            listOfPSCs.add(cellScan.toString());
            Log.d("Padmal", cellScan.toString());
        } catch (Exception e) {
            listOfPSCs.add("No cellular data available");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get data about place and mode
        try {
            mode = intent.getStringExtra("mode");
            label = intent.getStringExtra("place");

            Toast.makeText(this, "Mode Selected : " + mode + " at " + label, Toast.LENGTH_SHORT).show();

            mSensorManager.registerListener(this, ASensor, 20000);
            mSensorManager.registerListener(this, LSensor, 20000);
            mSensorManager.registerListener(this, PSensor, 20000);

            audio = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            audio.startRecording();

            thread = new Thread(new Runnable() {
                public void run() {
                    while (thread != null && !thread.isInterrupted()) {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
                        // Make the thread sleep for a the approximate sampling time
                        try {
                            Thread.sleep(SAMPLE_DELAY);
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                        readAudioBuffer();
                    }
                }
            });
            thread.start();
            // Write thread
            Wthread = new Thread(new Runnable() {
                public void run() {
                    while (Wthread != null && !Wthread.isInterrupted()) {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
                        // Make the thread sleep for a the approximate sampling time
                        try {
                            Thread.sleep(WRITE_DELAY);
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                        try {
                            fetchWiFiResults();
                            startLogging();

                            generateNoteOnSD("RawAccel" + mode + fileNameSuffix + "_" + label, Rawaccel);
                            generateNoteOnSD("RawMic" + mode + fileNameSuffix + "_" + label, noise);
                            generateNoteOnSD("RawLight" + mode + fileNameSuffix + "_" + label, Light);
                            generateNoteOnSD("RawProximity" + mode + fileNameSuffix + "_" + label, Proximity);
                            generateNoteOnSD("RawGPS" + mode + fileNameSuffix + "_" + label, GPS);
                            generateNoteOnSD("RawWiFi" + mode + fileNameSuffix + "_" + label, WiFi);
                            generateNoteOnSD("RawCell" + mode + fileNameSuffix + "_" + label, listOfPSCs);

                            Rawaccel = new ArrayList<>();
                            noise = new ArrayList<>();
                            Light = new ArrayList<>();
                            Proximity = new ArrayList<>();
                            GPS = new ArrayList<>();
                            WiFi = new ArrayList<>();
                            listOfPSCs = new ArrayList<>();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            Wthread.start();
        } catch (NullPointerException e) {
            Toast.makeText(getApplicationContext(), "Application was closed!", Toast.LENGTH_LONG).show();
        }
        return Service.START_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        long curTime = System.currentTimeMillis();
        String value = curTime + ", " + location.getLatitude() + ", " + location.getLongitude();
        GPS.add(value);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(getBaseContext(), "Gps is turned off",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(getBaseContext(), "Gps is turned on",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();
            String value = curTime + ", " + event.values[0] + ", " + event.values[1] + ", " + event.values[2];
            Rawaccel.add(value);

            if(check){
                prevX =event.values[0];
                prevY =event.values[1];
                prevZ =event.values[2];

                check =false;
            }
                //Low pass filter to get the gravity
                double gravityX = alpha*prevX+ (1-alpha)*event.values[0];
                double gravityY = alpha*prevY+ (1-alpha)*event.values[1];
                double gravityZ = alpha*prevZ+ (1-alpha)*event.values[2];
                //high pass filter to get linear accelation
                double linearX = event.values[0] - gravityX;
                double linearY = event.values[1] - gravityY;
                double linearZ = event.values[2] - gravityZ;

                prevX = gravityX;
                prevY = gravityY;
                prevZ = gravityZ;

            Double resultant = Math.sqrt(linearX*linearX+linearY*linearY+linearZ*linearZ);
            //For Classifier
            input_signal.add(resultant);
            //activityPrediction();
        }
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            long curTime = System.currentTimeMillis();
            String value = curTime + ", " + event.values[0];
            Light.add(value);
        }
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            long curTime = System.currentTimeMillis();
            String value = curTime + ", " + event.values[0];
            Proximity.add(value);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {/**/}

    // Write SD
    private void generateNoteOnSD(String sFileName, ArrayList<String> values) throws IOException {

        final String GPSfiledir = Environment.getExternalStorageDirectory() + "/AAContext";
        final String fileName = Environment.getExternalStorageDirectory() + "/AAContext/" + sFileName + ".txt";
        File fout = new File(fileName);
        long fileLength = fout.length();
        try {
            File dirfile = new File(GPSfiledir);
            if (!dirfile.exists()) {
                dirfile.mkdir();
            }
            String content = "";
            for (int i = 0; i < values.size(); i++) {
                content += values.get(i) + "\n";
            }
            byte[] bytes = content.getBytes();
            RandomAccessFile raf = new RandomAccessFile(fout, "rw");
            raf.seek(fileLength);
            raf.write(bytes);
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Functionality that gets the sound level out of the sample
    private double readAudioBuffer() {
        Double sumLevel = 0.0;
        try {
            short[] buffer = new short[bufferSize];
            Integer bufferReadResult = 1;
            if (audio != null) {
                bufferReadResult = audio.read(buffer, 0, bufferSize);
                for (int i = 0; i < bufferReadResult; i++) {
                    sumLevel += buffer[i];
                }
                long curTime = System.currentTimeMillis();
                String value = curTime + ", " + sumLevel / bufferReadResult;
                noise.add(value);
                //Log.d("TAG", bufferReadResult.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sumLevel;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    @Override
    public void onDestroy() {
        thread.interrupt();
        thread = null;
        Wthread.interrupt();
        Wthread = null;
        try {
            if (audio != null) {
                audio.stop();
                audio.release();
                audio = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mSensorManager.unregisterListener(this);
        Toast.makeText(this, "End of " + mode + " mode", Toast.LENGTH_LONG).show();
    }

    private void fetchWiFiResults() {
        wifiManager.startScan();
        // Point Cloud *************************************************************
        List<ScanResult> scanResults = wifiManager.getScanResults();
        int APs = scanResults.size();
        NodeCount = APs;
        StringBuilder wifiString = new StringBuilder();
        wifiString.append(APs);
        wifiString.append(",");
        for (ScanResult result : scanResults) {
            wifiString.append(result.SSID);
            wifiString.append(",");
            wifiString.append(result.frequency);
            wifiString.append(",");
            wifiString.append(result.level);
            wifiString.append(",");
            wifiString.append(result.BSSID);
            wifiString.append(",");
        }
        wifiString.deleteCharAt(wifiString.length() - 1);
        WiFi.add(wifiString.toString());
    }

    private void setupWiFi() {
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Enabling WIFI", Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }
    }
}
