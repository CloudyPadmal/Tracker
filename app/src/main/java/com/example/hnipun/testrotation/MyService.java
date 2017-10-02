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
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

/**
 * Created by hnipun on 7/28/2017.
 */

public class MyService extends Service implements SensorEventListener, LocationListener {
    //GPS
    private LocationManager locationManager;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 0;
    //Sensor intialization
    private SensorManager mSensorManager;
    private Sensor ASensor;
    private Sensor LSensor;
    private Sensor PSensor;
    //array list for samples
    ArrayList<String> Rawaccel = new ArrayList<>();
    ArrayList<String> noise = new ArrayList<>();
    ArrayList<String> Light = new ArrayList<>();
    ArrayList<String> Proximity = new ArrayList<>();
    ArrayList<String> GPS = new ArrayList<>();
    ArrayList<String> WiFi = new ArrayList<>();
    ArrayList<String> listOfPSCs = new ArrayList<>();

    HashMap<Integer, String> cellIDwithPSC = new HashMap<>();
    HashMap<Integer, String> cellIDwithLAC = new HashMap<>();
    HashMap<Integer, String> cellIDwithMCC = new HashMap<>();
    HashMap<Integer, String> cellIDwithMNC = new HashMap<>();

    //Random number for .text id
    Random rand = new Random();
    //For audio recording
    private static final int sampleRate = 8000; //8000
    private AudioRecord audio;
    private int bufferSize;
    private static final int SAMPLE_DELAY = 30;
    //treads
    private Thread thread;
    private Thread Wthread;

    private WifiManager wifiManager;
    private TelephonyManager tm;

    private static final int WRITE_DELAY = 10000; //8000
    String mode = null;

    private String TAG = "Debug";
    private String label = "";
    //Tensorflow definitions begins here
    //private TensorFlowInferenceInterface inferenceInterface;

    static {
        System.loadLibrary("tensorflow_inference");
    }

    //Tensorflow Parameters
    private static final String MODEL_FILE = "file:///android_asset/optimized_tfdroid.pb";
    private static final String INPUT_NODE = "input:0";
    private static final String OUTPUT_NODE = "y_:0";
    private static final String[] OUTPUT_NODES = {"y_:0"};
    private static final int OUTPUT_SIZE = 2;
    //Broadcast Reciver
    public final String ACTION_LOCATION_BROADCAST = "ActivityBroadcast";
    public final String EXTRA_MRT = "extra_MRT";
    public final String EXTRA_Bicycle = "extra_Bicycle";
    public final String EXTRA_Bus = "extra_Bus";
    public final String EXTRA_Still = "extra_Still";
    public final String EXTRA_Taxi = "extra_Taxi";
    public final String EXTRA_Walking = "extra_Walking";
    //For save accel values
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
        //initialize the inferenceInterface and load the model file
        //inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);
        //initialize sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        ASensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        LSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        PSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        //initialize location
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.PASSIVE_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    this);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    this);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    this);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
        //intialize audio
        try {
            bufferSize = AudioRecord
                    .getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
        } catch (Exception e) {
            android.util.Log.e("TrackingFlow", "Exception", e);
        }
//        //We perform inference by first filling the input nodes with our desired values
//        double[] inputDouble =
//                {0.67164774, 1.08891623, 0.40907031, 0.805582, 1.17978179, 0.4024208
//                        , 0.73503187, 1.24003253, 0.48030417, 0.70389132, 1.27361526, 0.51686847
//                        , 0.57463954, 1.28176389, 0.62015711, 0.53357897, 1.32843042, 0.71418733
//                        , 0.91939874, 1.27558871, 0.43922421, 1.10845161, 0.95705894, 0.0756939
//                        , 0.28995898, 1.09311172, -0.02403242, -1.20647752, 1.87314517, 0.1865823
//                        , 0.05846502, 1.5728861, 0.57955528, 0.65897023, 1.13187827, 0.6458013
//                        , 0.45944583, 1.3262084, 0.78993377, 0.32303156, 1.36719703, 0.51734371
//                        , 0.24007788, 1.4674481, 0.01015905, 0.33984008, 1.39584156, 0.1457414
//                        , 0.36381814, 1.44547299, 0.35754246, 0.32606306, 1.56399784, 0.38603785
//                        , 0.43326527, 1.55461228, 0.41429408, 0.54708199, 1.4916485, 0.42260221
//                        , 0.53026997, 1.51658849, 0.31694069, 0.5462565, 1.51337813, 0.20130361
//                        , 0.66062474, 1.59584953, 0.11012444, 0.72566335, 1.64301337, 0.02701715
//                        , 0.75156642, 1.68301373, 0.03057953, 0.70499431, 1.63437063, 0.21365212
//                        , 0.50491842, 1.53559879, 0.43637609, 0.40515622, 1.51658849, 0.49051279
//                        , 0.38916968, 1.50424128, 0.45774383, 0.28610198, 1.56918163, 0.44896066
//                        , 0.22409486, 1.63461613, 0.4491969, 0.30980253, 1.55609471, 0.41096794
//                        , 0.27452746, 1.528193, 0.65126428, 0.37814666, 1.47732466, 0.62680643
//                        , 0.42306775, 1.38176635, 0.36229124, 0.2491724, 1.35485291, 0.19299261
//                        , 0.25606091, 1.29978905, 0.16734822, 0.25688992, 1.24102076, 0.18254479
//                        , 0.26984144, 1.26694593, 0.12389543, 0.30318801, 1.28595938, 0.14858938
//                        , 0.32192855, 1.26842836, 0.26161456, 0.22602336, 1.33287459, 0.12484579
//                        , 0.32716255, 1.39905191, -0.10120143, 0.3963357, 1.46621429, -0.05846296
//                        , 0.22106335, 1.42942429, 0.1614131, 0.16181022, 1.41905364, 0.23383345
//                        , 0.2486209, 1.40250773, 0.26945041, 0.35554912, 1.43337433, 0.17637053
//                        , 0.37153214, 1.455104, -0.02902021, 0.16567074, 1.42719911, 0.66005063
//                        , 0.13480418, 1.36053083, 0.42782598, 0.38972118, 1.38571634, 0.16806254
//                        , 0.45558882, 1.41386672, 0.07854494, 0.10311212, 1.370656, 0.18848296
//                        , 0.0915376, 1.36028533, 0.50119676, 0.05956802, 1.31880254, 0.63772941
//                        , 0.22988036, 1.3953474, 0.27135113, 0.54019348, 1.45337607, 0.0386544
//                        , 0.28692748, 1.39435911, 0.24238059, 0.18771678, 1.45534952, 0.43874896
//                        , 0.40763623, 1.49139984, 0.34875627, 0.43188827, 1.48053501, 0.23027115
//                        , 0.26984144, 1.42497704, 0.20557718, 0.0978746, 1.44695538, 0.26256492
//                        , -0.05700271, 1.32522006, 0.41643092, -0.0994438, 1.23682217, 0.48790091
//                        , -0.08456376, 1.34769257, 0.41975409, -0.17523144, 1.32176425, 0.37012709
//                        , -0.19369447, 1.20176008, 0.42972677, -0.08318325, 1.21262491, 0.5750426
//                        , 0.02705046, 1.18422904, 0.58121687, 0.22354335, 1.20077179, 0.6579138
//                        , 0.290651, 1.34269135, 0.6320304, 0.32744006, 1.44738654, 0.49229557
//                        , 0.36195638, 1.50800555, 0.24012574, 0.59165885, 1.43942364, -0.05240671
//                        , 0.36319638, 1.47066164, -0.34238777, -0.30834021, 1.7670903, 0.13470038
//                        , -0.1306546, 1.57090953, -0.00586677, -0.12989585, 1.54609543, -0.26462544
//                        , -0.33837776, 1.53164883, -0.33508759, -0.43317995, 1.44991704, 0.03794013
//                        , -0.69168, 1.53905782, 0.90367015, -0.19176599, 1.40596675, 0.88324973
//                        , 0.66999323, 1.53609301, 0.18871904, 1.68856178, 2.01858365, -0.17670893
//                        , 2.08099957, 2.23686533, -0.29567225, 1.88891521, 2.43539106, -0.28807248
//                        , 1.65383821, 2.48354004, -0.36856777, 1.46285682, 2.22451812, -0.54213993};
//
//        float[] inputFloats = new float[inputDouble.length];
//        for (int i = 0; i < inputDouble.length; i++) {
//            inputFloats[i] = (float) inputDouble[i];
//        }
        setupWiFi();
        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    private void startLogging() {

        GsmCellLocation location = (GsmCellLocation) tm.getCellLocation();

        if (location == null) {
            Log.d("Padmal", "CellLocation null");
        }

        final int cid = location.getCid();
        final int lac = location.getLac();

        final String networkOperator = tm.getNetworkOperator();
        final int mcc = Integer.parseInt(networkOperator.substring(0, 3));
        final int mnc = Integer.parseInt(networkOperator.substring(3));
        int psc = location.getPsc();

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CID -> ").append(cid)
                .append(" Lac -> ").append(lac)
                .append(" Mcc -> ").append(mcc) // mobile country code
                .append(" Mnc -> ").append(mnc) // mobile network code
                .append(" PSC -> ").append(location.getPsc())
                .append("\n\n");

        for (NeighboringCellInfo i : tm.getNeighboringCellInfo()) {
            stringBuilder.append("CID : ");
            stringBuilder.append(i.getCid()); // cell id
            stringBuilder.append("; RSSI : ");
            stringBuilder.append(i.getRssi()); // received signal strength
            stringBuilder.append("; PSC : ");
            stringBuilder.append(i.getPsc()); // primary scrambling code
            stringBuilder.append("; LAC : ");
            stringBuilder.append(i.getLac()); // location area code
            stringBuilder.append("; NET : ");
            stringBuilder.append(i.getNetworkType());
            stringBuilder.append(";\n");
            if (!listOfPSCs.contains(i.getPsc())) {
                listOfPSCs.add(String.valueOf(i.getPsc()));
            }
        }

        if (!cellIDwithPSC.containsKey(psc)) {
            cellIDwithPSC.put(psc, String.valueOf(cid));
            cellIDwithLAC.put(psc, String.valueOf(lac));
            cellIDwithMCC.put(psc, String.valueOf(mcc));
            cellIDwithMNC.put(psc, String.valueOf(mnc));
        }
        stringBuilder.append("\nCell IDs and PSC Values\n");
        stringBuilder.append(Collections.singletonList(cellIDwithPSC));
        stringBuilder.append("\nCell IDs and LAC Values\n");
        stringBuilder.append(Collections.singletonList(cellIDwithLAC));
        stringBuilder.append("\nCell IDs and MCC Values\n");
        stringBuilder.append(Collections.singletonList(cellIDwithMCC));
        stringBuilder.append("\nCell IDs and MNC Values\n");
        stringBuilder.append(Collections.singletonList(cellIDwithMNC));

        listOfPSCs.add(stringBuilder.toString());
    }

    //Classify the activity
    /*public float[] getActivityProb(float[] input_signal) {
        float[] result = new float[OUTPUT_SIZE];
        inferenceInterface.feed(INPUT_NODE, input_signal, 1, N_SAMPLES);
        inferenceInterface.run(OUTPUT_NODES);
        inferenceInterface.fetch(OUTPUT_NODE, result);
        return result;
    }*/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
                    //Mmake the thread sleep for a the approximate sampling time
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
        //write tread
        Wthread = new Thread(new Runnable() {
            public void run() {
                while (Wthread != null && !Wthread.isInterrupted()) {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
                    //make the thread sleep for a the approximate sampling time
                    try {
                        Thread.sleep(WRITE_DELAY);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    try {
                        fetchWiFiResults();
                        startLogging();

                        generateNoteOnSD("RawAccel" + mode + System.currentTimeMillis() + "_" + label, Rawaccel);
                        generateNoteOnSD("RawMic" + mode + System.currentTimeMillis() + "_" + label, noise);
                        generateNoteOnSD("RawLight" + mode + System.currentTimeMillis() + "_" + label, Light);
                        generateNoteOnSD("RawProximity" + mode + System.currentTimeMillis() + "_" + label, Proximity);
                        generateNoteOnSD("RawGPS" + mode + System.currentTimeMillis() + "_" + label, GPS);
                        generateNoteOnSD("RawWiFi" + mode + System.currentTimeMillis() + "_" + label, WiFi);
                        generateNoteOnSD("RawCell" + mode + System.currentTimeMillis() + "_" + label, listOfPSCs);

                        Rawaccel = new ArrayList<>();
                        noise = new ArrayList<>();
                        Light = new ArrayList<>();
                        Proximity = new ArrayList<>();
                        GPS = new ArrayList<>();
                        WiFi = new ArrayList<>();
                        listOfPSCs = new ArrayList<>();

                        Log.d(TAG, "Write on the SD");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        Wthread.start();
        // Toast.makeText(this, "onStartCommand Started", Toast.LENGTH_LONG).show();
        return Service.START_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        long curTime = System.currentTimeMillis();
        String value = curTime + ", " + location.getLatitude() + ", " + location.getLongitude();
        GPS.add(value);
        Log.d(TAG, "GPS function");
    }

    @Override
    public void onProviderDisabled(String provider) {
        //Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        //startActivity(intent);
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

    //Broadcast
    private void sendBroadcastMessage(float[] result) {
        if (result != null) {
            Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
            intent.putExtra(EXTRA_MRT, 0.0);
            intent.putExtra(EXTRA_Bicycle, 0.0);
            intent.putExtra(EXTRA_Bus, 0.0);
            intent.putExtra(EXTRA_Still, result[0]);
            intent.putExtra(EXTRA_Taxi, 0.0);
            intent.putExtra(EXTRA_Walking, result[1]);

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    //write SD
    private void generateNoteOnSD(String sFileName, ArrayList<String> values) throws IOException {
        final String GPSfiledir = Environment.getExternalStorageDirectory() + "/Context";
        final String fileName = Environment.getExternalStorageDirectory() + "/Context/" + sFileName + ".txt";
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

    //Tesnorflow Classifier
    /*private void activityPrediction() {
        if (input_signal.size() == N_SAMPLES) {
            // Perform inference using Tensorflow
            float[] results = getActivityProb(getFloats(input_signal));
            sendBroadcastMessage(results);
            // Clear all the values
            input_signal.clear();
        }
    }*/

    //Convert the double array to float array
    /*private float[] getFloats(List<Double> values) {
        int length = values.size();
        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            result[i] = values.get(i).floatValue();
        }
        return result;
    }*/

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
