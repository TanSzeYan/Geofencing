package com.example.geofencing;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoQueryEventListener, IOnLoadLocationListener {

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker currentUser;
    private DatabaseReference myLocationRef;
    private GeoFire geofire;
    private List<LatLng> dangerousArea;
    private IOnLoadLocationListener listener;
    private Location lastLocation;
    private  GeoQuery geoQuery;
    private DatabaseReference myCity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {

                        buildLocationRequest();
                        buildLocationCallback();
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);


                        initArea();
                        settingGeoFire();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MapsActivity.this,"You must enable permission",Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

    }

    private void initArea() {
        listener = this;
       /* dangerousArea = new ArrayList<>();
        dangerousArea.add(new LatLng(2.309203, 102.321007));
        dangerousArea.add(new LatLng(2.313484, 102.321064));
        dangerousArea.add(new LatLng(2.311426, 102.321955));
        dangerousArea.add(new LatLng(2.306228, 102.285316));*/
       //load from firebase
        myCity = FirebaseDatabase.getInstance()
                .getReference("DangerousArea")
                .child("MyCity");

                /*myCity.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<MylatLng>latLngList = new ArrayList<>();
                        for (DataSnapshot locationSnapshot: dataSnapshot.getChildren())
                        {
                            MylatLng latLng =locationSnapshot.getValue(MylatLng.class);
                            latLngList.add(latLng);
                        }
                        listener.onLoadLocationSuccess(latLngList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        listener.onLoadLocationFailed(databaseError.getMessage());
                    }
                });*/
                myCity.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //Update  dangerous area

                        List<MylatLng>latLngList = new ArrayList<>();
                        for (DataSnapshot locationSnapshot: dataSnapshot.getChildren())
                        {
                            MylatLng latLng =locationSnapshot.getValue(MylatLng.class);
                            latLngList.add(latLng);
                        }
                        listener.onLoadLocationSuccess(latLngList);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


        //after submit this area on firebase we will comment it
       /* FirebaseDatabase.getInstance().getReference().child("My City").setValue(dangerousArea).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(MapsActivity.this, "updated", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MapsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
*/
    }

    private void addUserMarker() {
        geofire.setLocation("You", new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if(currentUser!=null)currentUser.remove();
                currentUser = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude())).title("You"));
                //After add marker move camera
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUser.getPosition(),12.0f));
            }
        });
    }

    private void settingGeoFire() {
        myLocationRef = FirebaseDatabase.getInstance().getReference("MyLocation");
        geofire = new GeoFire(myLocationRef);

    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                if (mMap !=null)
                {

                    lastLocation = locationResult.getLastLocation();
                    addUserMarker();
                }
            }
        };

    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (fusedLocationProviderClient != null)
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
            {
                if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED);
            }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());

        //Add circle for dangerous area
      addCircleArea();
    }

    private void addCircleArea() {

        if (geoQuery!=null)
        {
            geoQuery.removeGeoQueryEventListener(this);
            geoQuery.removeAllListeners();
        }
        for(LatLng latLng:dangerousArea)
        {
            mMap.addCircle(new CircleOptions().center(latLng)
                    .radius(500)
                    .strokeColor(Color.TRANSPARENT)
                    .strokeWidth(5.0f));

             geoQuery = geofire.queryAtLocation(new GeoLocation(latLng.latitude,latLng.longitude),0.5f);
            geoQuery.addGeoQueryEventListener(MapsActivity.this);
        }
    }

    @Override
    protected void onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        sendNotification("string",String.format("%s entered the dangerous area",key));
    }

    @Override
    public void onKeyExited(String key) {
        sendNotification("string",String.format("%s leave the dangerous area",key));
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        sendNotification("string",String.format("%s move within  the dangerous area",key));
    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Toast.makeText(this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();

    }
    private void sendNotification(String title,String content) {
        String NOTIFIATION_CHANNEL_ID="multiple_location";
        NotificationManager notificationManager =(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFIATION_CHANNEL_ID,"My Notification",NotificationManager.IMPORTANCE_DEFAULT);


            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationChannel.enableLights(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder= new  NotificationCompat.Builder(this,NOTIFIATION_CHANNEL_ID);
        builder.setContentText(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

        Notification notification = builder.build();
        notificationManager.notify(new Random().nextInt(),notification);
    }

    @Override
    public void onLoadLocationSuccess(List<MylatLng > latLngs) {
        dangerousArea = new ArrayList<>();
        for (MylatLng  mylatLng: latLngs)
        {
            LatLng convert = new LatLng(mylatLng.getLatitude(),mylatLng.getLatitude());
            dangerousArea.add(convert);
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
    //clear map again
        if (mMap != null)
        {
            mMap.clear();
            // add user marker

            addUserMarker();

            //Add Circle of dangerous area
            addCircleArea();
        }
    }

    @Override
    public void onLoadLocationFailed(String message) {
        Toast.makeText(this, ""+message, Toast.LENGTH_SHORT).show();

    }
}

