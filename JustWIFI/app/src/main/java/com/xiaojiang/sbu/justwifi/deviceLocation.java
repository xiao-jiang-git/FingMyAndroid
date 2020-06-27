package com.xiaojiang.sbu.justwifi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class deviceLocation extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener, GoogleMap.InfoWindowAdapter, LocationListener, GoogleApiClient.OnConnectionFailedListener {

    private TextView text;
    private String email;
    ListView listViewUsers;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private String deviceId;
    private GoogleMap mMap;
    private Double lat, longit;
    List<UpdateDevice> devicelist;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.devicelocation);

        Intent intent = getIntent();
        email = intent.getStringExtra("email");


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(deviceLocation.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }else{
            getLocation();
        }


        listViewUsers = (ListView) findViewById(R.id.ImageListView);

        devicelist = new ArrayList<>();




        listViewUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                UpdateDevice devices = devicelist.get(i);
                deviceId = devices.getDeviceId();
                Drawable drawable=getResources().getDrawable(R.drawable.org);
                listViewUsers.setSelector(drawable);
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Devices");


                databaseReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        lat = dataSnapshot.child(email).child(deviceId).child("lat").getValue(Double.class);
                        longit = dataSnapshot.child(email).child(deviceId).child("longit").getValue(Double.class);

                        Location location = new Location("providerNA");
                        location.setLatitude(lat);
                        location.setLongitude(longit);
                        LatLng devicelocation = new LatLng(lat, longit);
                        setUpMap(location);
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(devicelocation).title("This it wifi location point!"));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(devicelocation, 15));

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        });



    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Devices").child(email);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                devicelist.clear();
                for(DataSnapshot userSnapshot: dataSnapshot.getChildren()){

                    UpdateDevice users = userSnapshot.getValue(UpdateDevice.class);

                    devicelist.add(users);

                }
                DeviceList adapter = new DeviceList(deviceLocation.this, devicelist);
                listViewUsers.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }





    @Override
    public void onLocationChanged(Location location) {
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

    }


    private void setUpMap(Location location) {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                    {android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        mMap.setMyLocationEnabled(true);
        mLastLocation = location;
        // 4
        if (mLastLocation != null) {
            LatLng currentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation
                    .getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
        }

    }


    @Override
    protected void onStop() {
        super.onStop();
        if( mGoogleApiClient != null && mGoogleApiClient.isConnected() ) {
            mGoogleApiClient.disconnect();
        }
    }

    private void getLocation(){


        @SuppressLint("RestrictedApi") final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(deviceLocation.this).requestLocationUpdates(locationRequest, new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LocationServices.getFusedLocationProviderClient(deviceLocation.this).removeLocationUpdates(this);
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        UiSettings setting = googleMap.getUiSettings();
        setting.setCompassEnabled(true);
        setting.setMyLocationButtonEnabled(true);
        setting.setZoomControlsEnabled(true);
        setting.setIndoorLevelPickerEnabled(true);

        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraIdleListener(this);
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }
}

