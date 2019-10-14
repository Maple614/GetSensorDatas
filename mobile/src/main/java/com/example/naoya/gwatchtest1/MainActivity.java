package com.example.naoya.gwatchtest1;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener, SensorEventListener, LocationListener {
    private  static final String TAG = MainActivity.class.getName();
    private  GoogleApiClient mGoogleApiClient;
    TextView line;
    TextView d_TTextView;
    int count;
    boolean onGettingData;

    Button stopButton, finishButton;

    EditText editText;
    TextView acctimestampTextView;
    Date d;
    SimpleDateFormat sdf;

    BufferedWriter bw_WA,bw_WG,bw_SA,bw_SG,bw_HR,bw_AP,bw_GPS,bw_AccessPoint;

    FileOutputStream fileOutputStream_WA,fileOutputStream_WG,fileOutputStream_SA,
            fileOutputStream_SG,fileOutputStream_HR,fileOutputStream_AP,fileOutputStream_GPS,fileOutputStream_AccessPoint;

    OutputStreamWriter outputStreamWriter_WA,outputStreamWriter_WG,outputStreamWriter_SA,
            outputStreamWriter_SG,outputStreamWriter_HR,outputStreamWriter_AP,outputStreamWriter_GPS,outputStreamWriter_AccessPoint;

    String savedata_WA="Acc_TimeStamp,AccX,AccY,AccZ\n",
            savedata_WG="Gyro_TimeStamp,GyroX,GyroY,GyroZ\n",
            savedata_SA="Acc_TimeStamp,AccX,AccY,AccZ\n",
            savedata_SG="Gyro_TimeStamp,GyroX,GyroY,GyroZ\n",
            savedata_HR="HB_TimeStamp,HeartRate\n",
            savedata_AP="AP_TimeStamp,pressure\n",
            savedata_GPS="GPS_TimeStamp,latitude,longitude\n",
            savedata_AccessPoint="AccessPoint_TimeStamp,SSID,Rssi,Frequency\n";

    double acc=0;
    boolean start=false,stop=false;

    long deltaTime;
    TextView ap_text;
    TextView lat_text, long_text;
    TextView recordCount;

    private SensorManager mSensorManager_ap, mSensorManager_Accel, mSensorManager_Gyro;
    private LocationManager mlocationManager;
    private WifiManager m_WifiManager;
    private WifiInfo m_WifiInfo;
    boolean startFlag = false;

    private final int EXTERNAL_STORAGE_REQUEST_CODE = 1;
    private final int ACCESS_COARSE_LOCATION_REQUEST_CODE = 1;
    private final int ACCESS_FINE_LOCATION_REQUEST_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        onGettingData = false;

        deltaTime("ntp.nict.jp",10000);
        try{

            Thread.sleep(5000); //3000ミリ秒Sleepする

        }catch(InterruptedException e){}

        d_TTextView = (TextView)findViewById(R.id.textView2);
        d_TTextView.setText(String.valueOf(deltaTime));

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        acctimestampTextView = (TextView)findViewById(R.id.acc_timestamp);
        editText=(EditText)findViewById(R.id.editText);
        editText.setText("_");
        d = new Date(System.currentTimeMillis());
        sdf = new SimpleDateFormat("yyMMddHHmmss");
        line = (TextView)findViewById(R.id.textView);
        count = 0;

        stopButton = (Button)findViewById(R.id.stop);
        finishButton = (Button)findViewById(R.id.button2);
        mSensorManager_Accel = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensorManager_Gyro = (SensorManager)getSystemService(SENSOR_SERVICE);

        ap_text = (TextView)findViewById(R.id.textView3);
        mSensorManager_ap = (SensorManager)getSystemService(SENSOR_SERVICE);

        lat_text = (TextView)findViewById(R.id.lat_text);
        long_text = (TextView)findViewById(R.id.long_text);
        mlocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        m_WifiManager = (WifiManager)  getApplicationContext().getSystemService(WIFI_SERVICE);

        recordCount = (TextView)findViewById(R.id.textView4);
        recordCount.setText(String.valueOf(0));

    }

    // Permissionの確認
    @TargetApi(Build.VERSION_CODES.M)
    public void checkPermission() {

        if(checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_WIFI_STATE},1);
        }
        // 既に許可している
        if ((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)&&(checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED)) {
            locationStart();
            mlocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,300, 1, this);
            Location location = mlocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            //Log.d("location",String.valueOf(location));

            return;
        }
        // 許可していない場合、パーミッションの取得を行う
        // 以前拒否されている場合は、なぜ必要かを通知し、手動で許可してもらう
        if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "ファイル書き込みのために許可してください", Toast.LENGTH_SHORT).show();
        }
        // パーミッションの取得を依頼
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, EXTERNAL_STORAGE_REQUEST_CODE);
//        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_REQUEST_CODE);
//        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, ACCESS_COARSE_LOCATION_REQUEST_CODE);
        locationStart();
        mlocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000, 1, this);
        Location location = mlocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        //Log.d("location",String.valueOf(location));


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 許可された。
            }
        }
        if (requestCode == ACCESS_FINE_LOCATION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 許可された。
            }
        }
        if (requestCode == ACCESS_COARSE_LOCATION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 許可された。
            }
        }
    }

    boolean resumeFlag = true;
    @Override
    protected void onResume() {
        super.onResume();
        if (resumeFlag) {
            Sensor sensor_Accel = mSensorManager_Accel.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager_Accel.registerListener(this, sensor_Accel, SensorManager.SENSOR_DELAY_GAME);

            Sensor sensor_Gyro = mSensorManager_Gyro.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorManager_Gyro.registerListener(this, sensor_Gyro, SensorManager.SENSOR_DELAY_GAME);

            Sensor sensor_ap = mSensorManager_ap.getDefaultSensor(Sensor.TYPE_PRESSURE);
            mSensorManager_ap.registerListener(this, sensor_ap, SensorManager.SENSOR_DELAY_GAME);

            resumeFlag = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop(){
        super.onStop();
        if(null != mGoogleApiClient && mGoogleApiClient.isConnected()){
            Wearable.MessageApi.removeListener(mGoogleApiClient,this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle){
        Log.d(TAG,"onConnected");
        Wearable.MessageApi.addListener(mGoogleApiClient,this);
    }

    @Override
    public void onConnectionSuspended(int i){
        Log.d(TAG, "onMessageReceived");
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
    }

    boolean file_flag = true;
    private String TIME_FORMAT = "yyMMddHHmmssSSS";

    public void onMessageReceived(MessageEvent messageEvent){
        count++;
        if(count % 100 == 0){
            line.setText(String.valueOf(count));
        }
        String msg = messageEvent.getPath();//
        String[] values = msg.split(",", 0);
        if ("" != values[0]&&stop==false) acctimestampTextView.setText("接続状態：OK");
        else if(""==values[0]&&stop==false) acctimestampTextView.setText("接続状態：未接続");

        //Log.d(TAG, "onMessageReceived : " + messageEvent.getPath());
        acctimestampTextView.setText("接続状態：データ取得中");
        onGettingData = true;
        SimpleDateFormat sdf_local = new SimpleDateFormat(TIME_FORMAT);
        Long acc_timestamp_local = new Long(values[1]);

        switch (values[0]){
            case "Accel":
                try{
                    bw_WA.write(values[1]+","+values[2]+","+values[3]+","+values[4]+"\n");
                    //Log.d(TAG, "Received: " + values[1]);
                }catch (Exception e) {
                    // text = "error: FileOutputStream";
                    e.printStackTrace();
                }
                break;

            case "Gyro":
                try{
                    bw_WG.write(values[1]+","+values[2]+","+values[3]+","+values[4]+"\n");
                }catch (Exception e) {
                    // text = "error: FileOutputStream";
                    e.printStackTrace();
                }
                break;

            case "HeartRate":
                try{
                    Log.d(TAG, "Phone HeartRate" + values[2]);
                    bw_HR.write(values[1]+","+values[2]+"\n");
                }catch (Exception e) {
                    // text = "error: FileOutputStream";
                    e.printStackTrace();
                }
                break;
        }

//        try{
//            bw_WA.write(values[0]+","+values[1]+","+values[2]+","+values[3]+"\n");
//        }catch (Exception e) {
//            // text = "error: FileOutputStream";
//            e.printStackTrace();
//        }
//        try{
//            bw_WG.write(values[4]+","+values[5]+","+values[6]+","+values[7]+"\n");
//        }catch (Exception e) {
//            // text = "error: FileOutputStream";
//            e.printStackTrace();
//        }
//        try{
//            bw_HR.write(values[8]+","+values[9]+"\n");
//        }catch (Exception e) {
//            // text = "error: FileOutputStream";
//            e.printStackTrace();
//        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public void startClicked(View view){
        checkPermission();

        String filePath_WA =
                Environment.getExternalStorageDirectory().getPath()
                        + "/" + sdf.format(d) + "_Watch_Accel" + editText.getText() + ".csv";
        File file_WA = new File(filePath_WA);
        file_WA.getParentFile().mkdir();

        String filePath_WG =
                Environment.getExternalStorageDirectory().getPath()
                        + "/" + sdf.format(d) + "_Watch_Gyro" + editText.getText() + ".csv";
        File file_WG = new File(filePath_WG);
        file_WG.getParentFile().mkdir();

        String filePath_SA =
                Environment.getExternalStorageDirectory().getPath()
                        + "/" + sdf.format(d) + "_Phone_Accel" + editText.getText() + ".csv";
        File file_SA = new File(filePath_SA);
        file_SA.getParentFile().mkdir();

        String filePath_SG =
                Environment.getExternalStorageDirectory().getPath()
                        + "/" + sdf.format(d) + "_Phone_Gyro" + editText.getText() + ".csv";
        File file_SG = new File(filePath_SG);
        file_SG.getParentFile().mkdir();

        String filePath_HR =
                Environment.getExternalStorageDirectory().getPath()
                        + "/" + sdf.format(d) + "_HeartRate" + editText.getText() + ".csv";
        File file_HR = new File(filePath_HR);
        file_HR.getParentFile().mkdir();

        String filePath_GPS =
                Environment.getExternalStorageDirectory().getPath()
                        + "/" + sdf.format(d) + "_GPS" + editText.getText() + ".csv";
        File file_GPS = new File(filePath_GPS);
        file_GPS.getParentFile().mkdir();

        String filePath_smartphone_ap =
                Environment.getExternalStorageDirectory().getPath()
                        + "/" + sdf.format(d) + "_smartphone_ap_" + editText.getText() + ".csv";
        File file_smartphone_ap = new File (filePath_smartphone_ap);
        file_smartphone_ap.getParentFile().mkdir();

        String filePath_smartphone_accesspoint =
                Environment.getExternalStorageDirectory().getPath()
                        + "/" + sdf.format(d) + "_smartphone_accesspoint_" + editText.getText() + ".csv";
        File file_smartphone_accesspoint = new File (filePath_smartphone_accesspoint);
        file_smartphone_accesspoint.getParentFile().mkdir();



        try {
            fileOutputStream_WA = new FileOutputStream(file_WA, true);
            fileOutputStream_WG = new FileOutputStream(file_WG, true);
            fileOutputStream_SA = new FileOutputStream(file_SA, true);
            fileOutputStream_SG = new FileOutputStream(file_SG, true);
            fileOutputStream_HR = new FileOutputStream(file_HR, true);
            fileOutputStream_AP = new FileOutputStream(file_smartphone_ap,true);
            fileOutputStream_GPS = new FileOutputStream(file_GPS, true);
            fileOutputStream_AccessPoint = new FileOutputStream(file_smartphone_accesspoint,true);

            outputStreamWriter_WA = new OutputStreamWriter(fileOutputStream_WA, "UTF-8");
            outputStreamWriter_WG = new OutputStreamWriter(fileOutputStream_WG, "UTF-8");
            outputStreamWriter_SA = new OutputStreamWriter(fileOutputStream_SA, "UTF-8");
            outputStreamWriter_SG = new OutputStreamWriter(fileOutputStream_SG, "UTF-8");
            outputStreamWriter_HR = new OutputStreamWriter(fileOutputStream_HR, "UTF-8");
            outputStreamWriter_AP = new OutputStreamWriter(fileOutputStream_AP, "UTF-8");
            outputStreamWriter_GPS = new OutputStreamWriter(fileOutputStream_GPS, "UTF-8");
            outputStreamWriter_AccessPoint = new OutputStreamWriter(fileOutputStream_AccessPoint, "UTF-8");


            bw_WA = new BufferedWriter(outputStreamWriter_WA);
            bw_WA.write(savedata_WA);
            bw_WG = new BufferedWriter(outputStreamWriter_WG);
            bw_WG.write(savedata_WG);
            bw_SA = new BufferedWriter(outputStreamWriter_SA);
            bw_SA.write(savedata_SA);
            bw_SG = new BufferedWriter(outputStreamWriter_SG);
            bw_SG.write(savedata_SG);
            bw_HR = new BufferedWriter(outputStreamWriter_HR);
            bw_HR.write(savedata_HR);
            bw_AP = new BufferedWriter(outputStreamWriter_AP);
            bw_AP.write(savedata_AP);
            bw_GPS = new BufferedWriter(outputStreamWriter_GPS);
            bw_GPS.write(savedata_GPS);
            bw_AccessPoint = new BufferedWriter(outputStreamWriter_AccessPoint);
            bw_AccessPoint.write(savedata_AccessPoint);

            acctimestampTextView.setText("接続状態：ファイル作成完了");

        } catch (Exception e) {
            // text = "error: FileOutputStream";
            acctimestampTextView.setText("接続状態：FAILED");
            e.printStackTrace();
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener(){
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult){
                        Log.d(TAG, "onConnectionFailed:" + connectionResult.toString());
                    }
                })
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();
        startFlag = true;
    }

    public void stopClicked(View view){
        startFlag = false;
        onGettingData = false;


        // textView.setText(text);
        // text = "";
    }

    public void finishClicked(View view){
        mGoogleApiClient.disconnect();

        try {
            acctimestampTextView.setText("接続状態：SAVING");
            bw_WA.flush();
            bw_WA.close();
            outputStreamWriter_WA.close();
            fileOutputStream_WA.close();

            bw_WG.flush();
            bw_WG.close();
            outputStreamWriter_WG.close();
            fileOutputStream_WG.close();

            bw_SA.flush();
            bw_SA.close();
            outputStreamWriter_SA.close();
            fileOutputStream_SA.close();

            bw_SG.flush();
            bw_SG.close();
            outputStreamWriter_WG.close();
            fileOutputStream_WG.close();

            bw_HR.flush();
            bw_HR.close();
            outputStreamWriter_HR.close();
            fileOutputStream_HR.close();

            bw_AP.flush();
            bw_AP.close();
            outputStreamWriter_AP.close();
            fileOutputStream_AP.close();

            bw_GPS.flush();
            bw_GPS.close();
            outputStreamWriter_GPS.close();
            fileOutputStream_GPS.close();

            bw_AccessPoint.flush();
            bw_AccessPoint.close();
            outputStreamWriter_AccessPoint.close();
            fileOutputStream_AccessPoint.close();


            acctimestampTextView.setText("接続状態：SAVED");
            // text = "saved";
        } catch (Exception e) {
            // text = "error: FileOutputStream";
            acctimestampTextView.setText("接続状態：FAILED");
            e.printStackTrace();
        }
        finish();
        onDestroy();
    }

    float a_x=0,a_y=0,a_z=0,g_x=0,g_y=0,g_z=0,ap_val = 0,rssi=0;
    String data_SA,data_SG,data_sp,data_AccessPoint="",SSID="";
    long a_time=0L,g_time=0L,ap_time = 0L,AccessPoint_time=0L;
    int countAccel = 0, recordCount_ = 0;


    @Override
    public void onSensorChanged(SensorEvent event) {
        if(onGettingData == true) {
            if (startFlag) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {// Accel
                    a_x = event.values[0];
                    a_y = event.values[1];
                    a_z = event.values[2];

                    //pseudo time
                    a_time = System.currentTimeMillis() + deltaTime;
                    ;
                    data_SA = a_time + "," + a_x + "," + a_y + "," + a_z + "\n";
                    //Log.d(TAG, "Timestamp: " + a_time);

                    try {
                        bw_SA.write(data_SA);
                        recordCount_++;
                        recordCount.setText(String.valueOf(recordCount_));
                    } catch (UnsupportedEncodingException k) {
                        k.printStackTrace();
                    } catch (FileNotFoundException k) {
                        k.printStackTrace();
                    } catch (IOException k) {
                        k.printStackTrace();
                    }

                    if (countAccel % 10 == 0) {
                        GPS_time = System.currentTimeMillis() + deltaTime;
                        ;
                        data_GPS = GPS_time + "," + latitude + "," + longitude + "\n";
                        try {
                            bw_GPS.write(data_GPS);
                            countAccel = 0;
                        } catch (UnsupportedEncodingException k) {
                            k.printStackTrace();
                        } catch (FileNotFoundException k) {
                            k.printStackTrace();
                        } catch (IOException k) {
                            k.printStackTrace();
                        }
                        AccessPoint_time = System.currentTimeMillis() + deltaTime;
                        m_WifiInfo = m_WifiManager.getConnectionInfo();
                        data_AccessPoint = AccessPoint_time + "," + m_WifiInfo.getSSID() + "," + m_WifiInfo.getRssi() +","+ m_WifiInfo.getFrequency()+ "\n";
                        try {
                            bw_AccessPoint.write(data_AccessPoint);
                        } catch (UnsupportedEncodingException k) {
                            k.printStackTrace();
                        } catch (FileNotFoundException k) {
                            k.printStackTrace();
                        } catch (IOException k) {
                            k.printStackTrace();
                        }
                    }
                    countAccel++;
                }
                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {// gyro
                    g_x = event.values[0];
                    g_y = event.values[1];
                    g_z = event.values[2];

                    //pseudo time
                    g_time = System.currentTimeMillis() + deltaTime;
                    data_SG = g_time + "," + g_x + "," + g_y + "," + g_z + "\n";
                    try {
                        bw_SG.write(data_SG);
                    } catch (UnsupportedEncodingException k) {
                        k.printStackTrace();
                    } catch (FileNotFoundException k) {
                        k.printStackTrace();
                    } catch (IOException k) {
                        k.printStackTrace();
                    }
                }
                if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {// air pressure
                    ap_val = event.values[0];
                    // 取得した気圧をログに出力する
                    //Log.d("**pressure", "気圧=" + ap_val + "hPa");
                    ap_text.setText(String.valueOf(ap_val));
                    //pseudo time
                    ap_time = System.currentTimeMillis() + deltaTime;

                    data_sp = ap_time + "," + ap_val + "\n";
                    try {
                        bw_AP.write(data_sp);
                    } catch (UnsupportedEncodingException k) {
                        k.printStackTrace();
                    } catch (FileNotFoundException k) {
                        k.printStackTrace();
                    } catch (IOException k) {
                        k.printStackTrace();
                    }
                }
            }
        }
    }

    private void locationStart(){
        Log.d("debug","locationStart()");

        if (mlocationManager != null && mlocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d("debug", "location manager Enabled");
        } else {
            // GPSを設定するように促す
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
            Log.d("debug", "not gpsEnable, startActivity");
        }
    }


    // 結果の受け取り
    /*
     * Android Quickstart:
     * https://developers.google.com/sheets/api/quickstart/android
     *
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

        switch (status) {
            case LocationProvider.AVAILABLE:
                Log.d("debug", "LocationProvider.AVAILABLE");
                break;
            case LocationProvider.OUT_OF_SERVICE:
                Log.d("debug", "LocationProvider.OUT_OF_SERVICE");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Log.d("debug", "LocationProvider.TEMPORARILY_UNAVAILABLE");
                break;
        }

    }

    double latitude=0,longitude=0;
    String data_GPS="";
    long GPS_time=0L;

    @Override
    public void onLocationChanged(Location location) {
        if(startFlag){
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            //pseudo time
            GPS_time = System.currentTimeMillis()+deltaTime;;

            // 緯度の表示
            String str1 = "Latitude: "+latitude;
            //Log.d("debug", "" + location.getLatitude());
            lat_text.setText(str1);

            // 経度の表示
            String str2 = "Longtude: "+longitude;
            long_text.setText(str2);

//            data_GPS = GPS_time+","+latitude+","+longitude+"\n";
//            try {
//                bw_GPS.write(data_GPS);
//            } catch (UnsupportedEncodingException k) {
//                k.printStackTrace();
//            } catch (FileNotFoundException k) {
//                k.printStackTrace();
//            } catch (IOException k) {
//                k.printStackTrace();
//            }
        }
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
