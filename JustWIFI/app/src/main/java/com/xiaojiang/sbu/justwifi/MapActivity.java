package com.xiaojiang.sbu.justwifi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.michaldrabik.tapbarmenulib.TapBarMenu;

import butterknife.BindView;
import butterknife.ButterKnife;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import butterknife.Unbinder;


public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener, GoogleMap.InfoWindowAdapter, LocationListener, GoogleApiClient.OnConnectionFailedListener {

    public static final boolean isGeoApp = true;//if dont need high accuracy change it to false
    private static final int SLOW_INTERVAL = 30000; // 30 sec 最短更新时间
    private static final int MAX_deviation = isGeoApp ? 60 : 100;
    public static final int FAST_UPDATE_INTERVAL = isGeoApp ? 10000 : 20000;


    private Application context;//防止内存泄漏，不使用activity的引用


    private String address;
    private LocationManager locationManager;
    private Location mLastLocation;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean mLocationUpdateState;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_CHECK_SETTINGS = 2;
    private LocationListener gpsLocationListener;
    private LocationManager lm;//【位置管理】
    private ImageView copy, share;
    @BindView(R.id.tapBarMenu) TapBarMenu tapBarMenu;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maplayout);
        ButterKnife.bind(this);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_PERMISSION_REQUEST_CODE);
        }else{
            getLocation();
        }

        copy = (ImageView) findViewById(R.id.copy);
        share = (ImageView) findViewById(R.id.share);

        tapBarMenu = findViewById(R.id.tapBarMenu);
        tapBarMenu.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                tapBarMenu.toggle();
            }
        });

        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copy(context);

            }
        });
        share.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, address);
                sendIntent.setType("text/plain");
                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
            }
        });



    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if( mGoogleApiClient != null && mGoogleApiClient.isConnected() ) {
            mGoogleApiClient.disconnect();
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        Log.i("Hint:", "Location changed!" + location);
        if (location == null) return;
        if (true)
            Toast.makeText(context, "Get the new GPS    " + location.toString() + " Accuracy is " + location.getAccuracy(), Toast.LENGTH_LONG).show();
        if (location.getAccuracy() < MAX_deviation) {
            recordLocation(context, location.getLatitude(), location.getLongitude(), location.getAccuracy());
        } else {
            Log.i("Hint:", "Drop the new Gps info");
        }

    }

    public void checkAndRequestPermission(){
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            MainActivity activity = new MainActivity();
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            return;
        }
    }

    public void upDateLocation(){



    }

    private void getLocation(){


        @SuppressLint("RestrictedApi") final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(MapActivity.this).requestLocationUpdates(locationRequest, new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LocationServices.getFusedLocationProviderClient(MapActivity.this).removeLocationUpdates(this);
                if(locationResult != null &&locationResult.getLocations().size() > 0 ){
                    int latestLocationIdex = locationResult.getLocations().size() - 1 ;
                    double latitude = locationResult.getLocations().get(latestLocationIdex).getLatitude();
                    double longitude = locationResult.getLocations().get(latestLocationIdex).getLongitude();
                    double altitude = locationResult.getLocations().get(latestLocationIdex).getAltitude();
                    int accuracy = (int)locationResult.getLocations().get(latestLocationIdex).getAccuracy();
                    float speed = locationResult.getLocations().get(latestLocationIdex).getSpeed();

                    Location location = new Location("providerNA");
                    location.setLatitude(latitude);
                    location.setLongitude(longitude);
                    setUpMap(location);
                }
                else{
                }

            }
        }, Looper.getMainLooper());
    }

    /**
     * 拿到最近一次的硬件经纬度记录,只用精确度足够高的时候才会采用这种定位
     * @return
     */
    public boolean getCurrentLocation() {
        LocationRequest locationRequest = new LocationRequest();


        checkAndRequestPermission();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, FAST_UPDATE_INTERVAL, 0, gpsLocationListener);
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.i("Hint:", "Last GPS location is==" + mLastLocation);
        if (mLastLocation == null)
        {
            return false;
        }
        double latitude = mLastLocation.getLatitude();//纬度
        double longitude = mLastLocation.getLongitude();//经度
        double altitude = mLastLocation.getAltitude();//海拔
        float last_accuracy = mLastLocation.getAccuracy();//精度
        String provider = mLastLocation.getProvider();//传感器
        float bearing = mLastLocation.getBearing();
        float speed = mLastLocation.getSpeed();//速度
        if (true)
            Toast.makeText(context, "We get last location's information " + "Latit" + latitude + " Longit" + longitude + " Accuracy" + last_accuracy, Toast.LENGTH_LONG).show();
        Log.i("AlexLocation", "Get Last location SUCCESS，Lat==" + latitude + "  Longit" + longitude + "  Hight" + altitude + "   Sensor" + provider + "   Speed" + speed + "Acc" + last_accuracy);
        if (last_accuracy < MAX_deviation) {
            recordLocation(context, latitude, longitude, last_accuracy);
        } else {
            Log.i("AlexLocation", "精确度太低，放弃last Location");
        }


        return last_accuracy < MAX_deviation;
    }


    /**
     * record the Location information from getLastLocation
     * @param context
     */
    public static void recordLocation(Context context, double latitude, double longitude, float accuracy) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("lastLocationRecord", Context.MODE_PRIVATE);

        // Save the data to Configuration file
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("latitude", String.valueOf(latitude));
        editor.putString("longitude", String.valueOf(longitude));
        editor.putFloat("accuracy", accuracy);
        editor.apply();
        Log.i("Hint:", "Finally the location info is latitude=" + latitude + "   longitude=" + longitude + "   accuracy=" + accuracy);
        MyLocation myLocationStatic = MyLocation.getInstance();
        //当以前没有记录，或者30s内连续获取的数据（Google的数据，手机自带GPS返回的数据）根据accuracy择优录取
        if (myLocationStatic.updateTime == 0 || System.currentTimeMillis() - myLocationStatic.updateTime > SLOW_INTERVAL || accuracy <= myLocationStatic.accuracy) {
            myLocationStatic.latitude = latitude;
            myLocationStatic.longitude = longitude;
            myLocationStatic.accuracy = accuracy;
            myLocationStatic.updateTime = System.currentTimeMillis();
            if (true)
                Toast.makeText(context, "Finally the location info is Latitude=" + latitude + "   Longitude=" + longitude + "   Accuracy=" + accuracy, Toast.LENGTH_LONG).show();
        } else {
            Log.i("Hint:", "the result is not accuracy, then give up it !");
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    @Override
    public void onCameraIdle() {

    }

    @Override
    public void onMapClick(LatLng latLng) {


    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));


    }

    private void setUpMap(Location location) {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                    {android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        mMap.setMyLocationEnabled(true);
        mLastLocation = location;
        // 4
        if (mLastLocation != null) {
            LatLng currentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation
                    .getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 19));
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        UiSettings setting = googleMap.getUiSettings();
        setting.setCompassEnabled(true);
        setting.setMyLocationButtonEnabled(true);
        setting.setZoomControlsEnabled(true);
        setting.setIndoorLevelPickerEnabled(true);

        mMap = googleMap;

        LatLng wifilocation = new LatLng(getIntent().getExtras().getDouble("lat"), getIntent().getExtras().getDouble("longit"));
        mMap.addMarker(new MarkerOptions().position(wifilocation).title("This it wifi location point!"));
        address = ""+wifilocation.latitude+ ","+wifilocation.longitude;


        // Add a marker in Sydney and move the camera
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraIdleListener(this);
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

    }

    public void copy(Context context){


        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);        //创建ClipData对象
        ClipData clipData = ClipData.newPlainText("address", ""+address);
        Toast.makeText(MapActivity.this,"You location has been added to clkipboard!",Toast.LENGTH_SHORT).show();
        //添加ClipData对象到剪切板中
        clipboard.setPrimaryClip(clipData);
    }


}
