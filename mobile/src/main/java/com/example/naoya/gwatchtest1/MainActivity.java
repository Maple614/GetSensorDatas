package com.example.naoya.gwatchtest1;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
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

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener, SensorEventListener {
    private  static final String TAG = MainActivity.class.getName();
    private  GoogleApiClient mGoogleApiClient;
    TextView a_xTextView;
    TextView a_yTextView;
    TextView a_zTextView;
    TextView g_xTextView;
    TextView g_yTextView;
    TextView g_zTextView;
    TextView line;
    TextView d_TTextView;
    int count;

    Button stopButton;

    EditText editText;
    TextView acctimestampTextView,geotimestampTextView;
    Date d;
    SimpleDateFormat sdf;
    BufferedWriter bw;
    FileOutputStream fileOutputStream;
    OutputStreamWriter outputStreamWriter;
    double acc=0;
    boolean start=false,stop=false;

    long deltaTime;
    TextView ap_text;
    private SensorManager mSensorManager_ap;
    BufferedWriter bw_sp;
    FileOutputStream fileOutputStream_sp;
    OutputStreamWriter outputStreamWriter_sp;
    String savedata_sp = "AP_TimeStamp,pressure\n";
    boolean startFlag = false;



    String savedata="Acc_TimeStamp,AccX,AccY,AccZ,Gyro_TimeStamp,GyroX,GyroY,GyroZ\n";
    private final int EXTERNAL_STORAGE_REQUEST_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deltaTime("ntp.nict.jp",10000);
        try{

            Thread.sleep(5000); //3000ミリ秒Sleepする

        }catch(InterruptedException e){}

        d_TTextView = (TextView)findViewById(R.id.textView2);
        d_TTextView.setText(String.valueOf(deltaTime));

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        acctimestampTextView = (TextView)findViewById(R.id.acc_timestamp);
//        geotimestampTextView = (TextView)findViewById(R.id.geo_timestamp);
//        a_xTextView = (TextView)findViewById(R.id.a_xValue);
//        a_yTextView = (TextView)findViewById(R.id.a_yValue);
//        a_zTextView = (TextView)findViewById(R.id.a_zValue);
//        g_xTextView = (TextView)findViewById(R.id.g_xValue);
//        g_yTextView = (TextView)findViewById(R.id.g_yValue);
//        g_zTextView = (TextView)findViewById(R.id.g_zValue);
        editText=(EditText)findViewById(R.id.editText);
        editText.setText("_");
        d = new Date(System.currentTimeMillis());
        sdf = new SimpleDateFormat("yyMMddHHmmss");
        line = (TextView)findViewById(R.id.textView);
        count = 0;

        stopButton = (Button)findViewById(R.id.stop);

        ap_text = (TextView)findViewById(R.id.textView3);
        mSensorManager_ap = (SensorManager)getSystemService(SENSOR_SERVICE);

    }

    // Permissionの確認
    @TargetApi(Build.VERSION_CODES.M)
    public void checkPermission() {
        // 既に許可している
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // 許可していない場合、パーミッションの取得を行う
        // 以前拒否されている場合は、なぜ必要かを通知し、手動で許可してもらう
        if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "ファイル書き込みのために許可してください", Toast.LENGTH_SHORT).show();
        }
        // パーミッションの取得を依頼
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == EXTERNAL_STORAGE_REQUEST_CODE) {
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
            Sensor sensor_ap = mSensorManager_ap.getDefaultSensor(Sensor.TYPE_PRESSURE);
            mSensorManager_ap.registerListener(this, sensor_ap, SensorManager.SENSOR_DELAY_NORMAL);

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

            Log.d(TAG, "onMessageReceived : " + messageEvent.getPath());
            acctimestampTextView.setText("接続状態：データ取得中");

            //acc_t+","+a_x+","+a_y+","+a_z+","+geo_t+","+g_x+","+g_y+","+g_z
            //a_xTextView.setText(String.valueOf(values[1]));
            //a_yTextView.setText(String.valueOf(values[2]));
            //a_zTextView.setText(String.valueOf(values[3]));
            //        g_xTextView.setText(String.valueOf(values[5]));
            //        g_yTextView.setText(String.valueOf(values[6]));
            //        g_zTextView.setText(String.valueOf(values[7]));

            SimpleDateFormat sdf_local = new SimpleDateFormat(TIME_FORMAT);
            Long acc_timestamp_local = new Long(values[0]);
            //Long geo_timestamp_local = new Long(values[4]);
            //acctimestampTextView.setText(sdf_local.format(acc_timestamp_local.longValue()));

            //geotimestampTextView.setText(sdf_local.format(geo_timestamp_local.longValue()));

            //String str_data = String.valueOf(values[0]) + "," + sdf_local.format(acc_timestamp_local.longValue()) + "," + String.valueOf(values[1]) + "," + String.valueOf(values[2]) + ","
                    //+ String.valueOf(values[3]) + "\n";//+","+String.valueOf(values[4])+","+sdf_local.format(geo_timestamp_local.longValue())+","+String.valueOf(values[5])+","+String.valueOf(values[6])+","+String.valueOf(values[7])+"\n";

            //加速度の大きさを計算
            //acc = Math.sqrt(Double.parseDouble(values[1]) * Double.parseDouble(values[1]) + Double.parseDouble(values[2]) * Double.parseDouble(values[2]) + Double.parseDouble(values[3]) * Double.parseDouble(values[3]));
        try{
            bw.write(msg+"\n");
        }catch (Exception e) {
            // text = "error: FileOutputStream";
            e.printStackTrace();
        }



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
        count=0;
        String filePath =
                Environment.getExternalStorageDirectory().getPath()
                        + "/" + sdf.format(d) + "_gwatch_wrist" + editText.getText() + ".csv";
        System.out.println("debug:" + filePath);
        File file = new File(filePath);
        file.getParentFile().mkdir();

        String filePath_smartphone =
                Environment.getExternalStorageDirectory().getPath()
                        + "/" + sdf.format(d) + "_smartphone_" + editText.getText() + ".csv";

        File file_smartphone = new File (filePath_smartphone);
        file_smartphone.getParentFile().mkdir();

        try {
            fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream_sp = new FileOutputStream(file_smartphone,true);

            outputStreamWriter
                    = new OutputStreamWriter(fileOutputStream, "UTF-8");
            outputStreamWriter_sp
                    = new OutputStreamWriter(fileOutputStream_sp, "UTF-8");


            bw = new BufferedWriter(outputStreamWriter);
            bw.write(savedata);

            bw_sp = new BufferedWriter(outputStreamWriter_sp);
            bw_sp.write(savedata_sp);

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
        mGoogleApiClient.disconnect();
       try {
            acctimestampTextView.setText("接続状態：SAVING");
            bw.flush();
            bw.close();
            outputStreamWriter.close();
            fileOutputStream.close();

           bw_sp.flush();
           bw_sp.close();
           outputStreamWriter_sp.close();
           fileOutputStream_sp.close();
            startFlag = false;

           acctimestampTextView.setText("接続状態：SAVED");
            // text = "saved";
        } catch (Exception e) {
            // text = "error: FileOutputStream";
            acctimestampTextView.setText("接続状態：FAILED");
            e.printStackTrace();
        }
        // textView.setText(text);
        // text = "";
    }

    float ap_val = 0;
    String data_sp;
    long ap_time = 0L;
    @Override
    public void onSensorChanged(SensorEvent event) {

        if(startFlag){
            if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {// air pressure
                ap_val = event.values[0];

                // 取得した気圧をログに出力する
                Log.d("**pressure", "気圧=" + ap_val + "hPa");
                ap_text.setText(String.valueOf(ap_val));

                //pseudo time
                ap_time = System.currentTimeMillis()+deltaTime;;
                data_sp = ap_time+","+ap_val+"\n";
                try {
                    bw_sp.write(data_sp);
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
