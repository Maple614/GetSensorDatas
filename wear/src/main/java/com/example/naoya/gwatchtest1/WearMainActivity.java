package com.example.naoya.gwatchtest1;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.example.naoya.gwatchtest1.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import static android.R.attr.x;
import static android.R.attr.y;
import static android.content.ContentValues.TAG;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.widget.Toast;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

import java.net.InetAddress;
import java.text.BreakIterator;

import static android.content.ContentValues.TAG;



public class WearMainActivity extends Activity implements SensorEventListener{

    private TextView mTextView,mTextConnected;
    private  SensorManager mSensorManager_a;
    private  SensorManager mSensorManager_g;
    private  SensorManager mSensorManager_h;

    private GoogleApiClient mGoogleApiClient;
    private String mNode;
    String data;
    String accelData = "";
    String gyroData = "";
    String heartRateData = "";
    private TextView koki,heartrate_text, gyro_Count;
    private float a_x,a_y,a_z,g_x,g_y,g_z,h_b;
    long deltaTime=0,adjustTime_a,adjustTime_g;
    boolean hrSend;

    private final int BODY_SENSORS_REQUEST_CODE = 1;

    PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_main);
        deltaTime("ntp.nict.jp",10000);
        try{

            Thread.sleep(5000); //3000ミリ秒Sleepする

        }catch(InterruptedException e){}

        checkPermission();

        hrSend = false;

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Keep screen on Wear

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "AppName:WatchFaceWakelockTag"); // note WakeLock spelling


        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                //mTextConnected = (TextView)stub.findViewById(R.id.connect);
                koki=(TextView) findViewById(R.id.textView);
                koki.setText(String.valueOf(deltaTime));
                heartrate_text=(TextView) findViewById(R.id.heartrate_text);
                gyro_Count=(TextView) findViewById(R.id.textView2);
                gyro_Count.setText((String.valueOf(0)));

            }
        });
        this.mTextConnected = (TextView)findViewById(R.id.connect);

        //mTextConnected.setText("gchch");
        mSensorManager_a = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensorManager_g = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensorManager_h = (SensorManager)getSystemService(SENSOR_SERVICE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks(){
                    @Override
                    public void onConnected(Bundle bundle){
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                            @Override
                            public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                                if(nodes.getNodes().size() > 0){
                                    mNode = nodes.getNodes().get(0).getId();
                                }
                            }
                        });
                    }

                    @Override
                    public void onConnectionSuspended(int i){
                        Log.d(TAG, "onConnectionSuspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult){
                        Log.d(TAG, "onConnectionFailed :" + connectionResult.toString());
                    }
                })
                .build();

    }
    // Permissionの確認
    @TargetApi(Build.VERSION_CODES.M)
    public void checkPermission() {
        // 既に許可している
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS)== PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // 許可していない場合、パーミッションの取得を行う
        // 以前拒否されている場合は、なぜ必要かを通知し、手動で許可してもらう
        if (!shouldShowRequestPermissionRationale(Manifest.permission.BODY_SENSORS)) {
            Toast.makeText(this, "センサ使用の許可をしてください", Toast.LENGTH_SHORT).show();
        }
        // パーミッションの取得を依頼
        requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, BODY_SENSORS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == BODY_SENSORS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 許可された。

            }
        }
    }

    // @Override
    boolean resumeFlag = true;
    protected void onResume(){
        super.onResume();
        if(resumeFlag) {
            Sensor sensor_a = mSensorManager_a.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager_a.registerListener(this, sensor_a, SensorManager.SENSOR_DELAY_NORMAL);

            Sensor sensor_g = mSensorManager_g.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorManager_g.registerListener(this, sensor_g, 20000);

            Sensor sensor_h = mSensorManager_h.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            mSensorManager_h.registerListener(this, sensor_h, SensorManager.SENSOR_DELAY_NORMAL);

            mGoogleApiClient.connect();

            resumeFlag = false;
        }
        if((wakeLock != null) && (wakeLock.isHeld() == false)){
            wakeLock.acquire();
        }


    }


    protected void onDestroy(){
        super.onDestroy();

        if(wakeLock != null){
            wakeLock.release();
            wakeLock = null;
        }
    }



    @Override
    protected void onPause(){
        super.onPause();
        mSensorManager_a.unregisterListener(this);
        mSensorManager_g.unregisterListener(this);
        mSensorManager_h.unregisterListener(this);


        mGoogleApiClient.disconnect();
    }


    boolean SendFlag = true;
    public void disconnected(View view){

        SendFlag = false;

        finish();
        onDestroy();
    }

    long acc_t  = 0;
    long geo_t = 0;
    long heart_t = 0;
    int accelcount = 0;
    float temp_heartRate = 0;
    int gyrocount = 0;

    public void onSensorChanged(SensorEvent event){
        if(SendFlag == true) {
            if (hrSend == true) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    a_x = event.values[0];
                    a_y = event.values[1];
                    a_z = event.values[2];
                    acc_t = System.currentTimeMillis() + deltaTime;

                    accelData = "Accel" + "," + acc_t + "," + a_x + "," + a_y + "," + a_z;
                    //Log.d(TAG, "Timestamp: " + acc_t);
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, mNode, accelData, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult result) {
                            if (!result.getStatus().isSuccess()) {
                                Log.d(TAG, "Error : faild to send AccelData Message" + result.getStatus());
                            }
                        }
                    });
                    accelcount++;
                    if (accelcount == 10) {
                        heart_t = System.currentTimeMillis() + deltaTime;
                        heartrate_text.setText(String.valueOf(h_b));
//            Log.d(TAG, "h_t"+heart_t+"h_b: " + h_b);
                        heartRateData = "HeartRate" + "," + heart_t + "," + h_b;
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, mNode, heartRateData, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult result) {
                                if (!result.getStatus().isSuccess()) {
                                    Log.d(TAG, "Error : faild to send HeartRateData Message" + result.getStatus());
                                }
                            }
                        });
                        accelcount = 0;
                    }

                    //Log.d(TAG, "a_t"+acc_t+"a_x: " + a_x + " " + "a_y: " + a_y + " " + "a_z: " + a_z);
                }


                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    g_x = event.values[0];
                    g_y = event.values[1];
                    g_z = event.values[2];
                    geo_t = System.currentTimeMillis() + deltaTime;
                    gyroData = "Gyro" + "," + geo_t + "," + g_x + "," + g_y + "," + g_z;
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, mNode, gyroData, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult result) {
                            if (!result.getStatus().isSuccess()) {
                                Log.d(TAG, "Error : faild to send GyroData Message" + result.getStatus());
                            }
                        }
                    });
                    gyrocount++;
                    if (gyrocount % 200 == 0) {
                        gyro_Count.setText(String.valueOf(gyrocount));
                    }
//            Log.d(TAG, "g_t"+geo_t+"g_x: " + g_x + " " + "g_y: " + g_y + " " + "g_z: " + g_z);
                }
            }

            if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                temp_heartRate = event.values[0];
                if (temp_heartRate != 0) {
                    h_b = event.values[0];
                }
                Log.d(TAG, "Watch HeartRate" + h_b);
                if (h_b != 0) {
                    hrSend = true;
                    heart_t = System.currentTimeMillis() + deltaTime;
                    heartrate_text.setText(String.valueOf(h_b));
//            Log.d(TAG, "h_t"+heart_t+"h_b: " + h_b);
                    heartRateData = "HeartRate" + "," + heart_t + "," + h_b;
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, mNode, heartRateData, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult result) {
                            if (!result.getStatus().isSuccess()) {
                                Log.d(TAG, "Error : faild to send HeartRateData Message" + result.getStatus());
                            }
                        }
                    });
                }
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor,int accuracy){

    }

    public void deltaTime(String url,int timeout) {
        final String myUrl = url;
        final int myTimeout = timeout;
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                long ntpTime = 0L;
                long deviceTime = 0L;
                SntpClient sntp = new SntpClient();
                System.out.println(myUrl + "," + myTimeout);

                if (sntp.requestTime(myUrl, myTimeout)) {
                    ntpTime = sntp.getNtpTime() + SystemClock.elapsedRealtime() - sntp.getNtpTimeReference();
                    deviceTime = System.currentTimeMillis();
                }

                System.out.println("NTP: " + ntpTime);
                System.out.println("device: " + deviceTime);

                deltaTime = ntpTime - deviceTime;
                System.out.println("delta: " + deltaTime);


                return String.valueOf(ntpTime);
            }

            @Override
            protected void onPostExecute(String result) {
                // Log.d(TAG,result);
            }
        }.execute();
    }
}
