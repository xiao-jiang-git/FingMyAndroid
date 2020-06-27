package com.xiaojiang.sbu.justwifi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class MainActivity extends AppCompatActivity {

    private String id, deviceId, deviceName, deviceNode;
    private String json = "";
    private Double latitude;
    private Double longitude;
    private Button mylocation, deviceLocation, messageboard;
    public static  final String USER_ID = "userId";
    DatabaseReference databaseReference;
    String node = "";
    String name, phone, email;
    private Double latGPS, longitGPS, latWIFI, longitWIFI;
    private int accWIfI, accGPS;
    private TextView device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        phone = intent.getStringExtra("phone");
        email = intent.getStringExtra("email");


        handler.post(task);//立即调用

        messageboard = findViewById(R.id.messageboard);
        mylocation = findViewById(R.id.mylocation);
        deviceLocation = findViewById(R.id.otherdevicelocation);
        device = findViewById(R.id.device);
        mylocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(MainActivity.this, MapActivity.class);
                it.putExtra("lat",WIFIManager.getInstance().lat);
                it.putExtra("longit",WIFIManager.getInstance().longit);
                startActivity(it);
            }
        });


        deviceLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(latitude == null || longitude == null){
                    Toast.makeText(MainActivity.this,"Ops: I'm trying to get the best location. Please wait a second ~", Toast.LENGTH_LONG);
                    return;
                }
                Intent it = new Intent(MainActivity.this, deviceLocation.class);
                it.putExtra("email",email);
                startActivity(it);
            }
        });

        messageboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent messa = new Intent(MainActivity.this, messageBoardActivity.class);
                messa.putExtra("name", name);
                startActivity(messa);
            }
        });
    }



    private void doWifiLocation(){
        WIFIManager.onCreateGPS(getApplication());
        WIFIManager.send();

        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }

        json = WIFIManager.getInstance().dataJson;
        latWIFI = WIFIManager.getInstance().lat;
        longitWIFI = WIFIManager.getInstance().longit;
        accWIfI = WIFIManager.getInstance().accWifi;
    }



    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

        }
        @Override
        public void onProviderDisabled(String arg0) {
            // TODO Auto-generated method stub

        }
        @Override
        public void onProviderEnabled(String arg0) {
            // TODO Auto-generated method stub

        }
        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
            // TODO Auto-generated method stub

        }
    };

    private void getGPSlocation(){
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
        String serviceString = Context.LOCATION_SERVICE;// 获取的是位置服务
        LocationManager locationManager = (LocationManager) getSystemService(serviceString);
        String provider = LocationManager.GPS_PROVIDER;// 指定LocationManager的定位方法
        Location location = locationManager.getLastKnownLocation(provider);// 调用getLastKnownLocation()方法获取当前的位置信息
        latGPS = location.getLatitude();//获取纬度
        longitGPS = location.getLongitude();//获取经度
        accGPS = (int)location.getAccuracy();//精确度
        Log.v("^^^^^^^^^^^^^^^^^^^^^^^^", "GPSACC: "+accGPS);
        //locationManager.requestLocationUpdates(provider, 2000, 10,locationListener);// 产生位置改变事件的条件设定为距离改变10米，时间间隔为2秒，设定监听位置变化
    }


    private Handler handler = new Handler();
    private Runnable task = new Runnable() {
        public void run() {
            doWifiLocation();
            getGPSlocation();
            if(accGPS<= accWIfI - 8){
                latitude = latGPS;
                longitude = longitGPS;
            }else{
                latitude = latWIFI;
                longitude = longitWIFI;
            }
            saveDeviceInfo(latitude,longitude);
            if(latitude == 0.0){
                handler.postDelayed(this,1000);//设置循环时间，此处是5秒
            }
            else{
                handler.postDelayed(this,60000);//设置循环时间，此处是5秒
            }

        }
    };


    private void saveDeviceInfo(Double lat, Double longit){

        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_PHONE_STATE},1);
        }

        DatabaseReference dataBaseUsers;
        dataBaseUsers = FirebaseDatabase.getInstance().getReference("Devices");
        //get all the values
        TelephonyManager tm = (TelephonyManager) getApplication().getSystemService(Application.TELEPHONY_SERVICE);
        deviceId = tm.getImei();
        deviceName = android.os.Build.MODEL;
        device.setText(deviceName );

        UpdateDevice device = new UpdateDevice(lat, longit, deviceId, deviceName);
        dataBaseUsers.child(email).child(deviceId).setValue(device);
    }



}
