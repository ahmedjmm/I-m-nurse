package com.example.imnurse.activities;

import static com.example.imnurse.Home.FCMToken;
import static com.example.imnurse.Home.firebaseAuth;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.imnurse.Home;
import com.example.imnurse.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static boolean locationPermissionGranted = false;
    private GoogleMap mMap;
    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    LocationRequest locationRequest;
    Location lastLocation;
    private LocationCallback locationCallback;

    //firestore
    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    public static DocumentReference nurseDocRef;

    //strings
    public static String name, nurseId, authId, email, mobileNumber, password, longitude, latitude;

    //register broadcast receiver to receive messages from users
    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), "messageRequest")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setTitle(R.string.alert_dialog_message_title);
                builder.setMessage(getString(R.string.alert_dialog_message_message)
                        + intent.getStringExtra("messageFrom"))
                        .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent chatActivity = new Intent(getApplicationContext(), ChatActivity.class);
                                chatActivity.putExtra("nurseDocumentId", nurseDocRef.getId());
                                chatActivity.putExtra("FCM_token", FCMToken);
                                chatActivity.putExtra("name", name);
                                chatActivity.putExtra("nurseId", nurseId);
                                startActivity(chatActivity);
                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                                dialog.dismiss();
                            }
                        });
                // Create the AlertDialog object and return it
                builder.create().show();
            }
        }
    };
    IntentFilter mIntentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkLanguage();

        nurseId = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("nurseId", "");
        name = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("name", "");
        email = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("email", "");
        password = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("password", "");
        if (name.equals("") || nurseId.equals("") || email.equals("") || password.equals("")) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        }

        setContentView(R.layout.activity_maps);

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.app_name);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("messageRequest");
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseSignIn();

        getLocationPermission();
        createLocationRequest();
        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                try {
                    for (Location location : locationResult.getLocations()) {
                        // Update UI with location data
                        // ...
                        lastLocation = location;
                        latitude = String.valueOf(lastLocation.getLatitude());
                        longitude = String.valueOf(lastLocation.getLongitude());
                        Map<String, Object> locationUpdates = new HashMap<>();
                        locationUpdates.put("latitude", latitude);
                        locationUpdates.put("longitude", longitude);
                        nurseDocRef.update(locationUpdates);
                        LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(16f));
                    }
                } catch (NullPointerException e) {
                    Log.e("location callback", e.getMessage());
                }
            }
        };
//        startLocationUpdates();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
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
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        if (locationPermissionGranted) {
            getDeviceLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationPermissionGranted = false;
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int item : grantResults) {
                    if (grantResults[item] != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
            }
            locationPermissionGranted = true;
            initMap();
        }
    }

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void nurseSignOut() {
        try {
            nurseDocRef.delete().addOnSuccessListener(aVoid -> {
            }).addOnFailureListener(e ->
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        } catch (Exception ignored) {
        }
        FirebaseAuth.getInstance().signOut();
    }

    private void firebaseSignIn() {
        email = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("email", "");
        password = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("password", "");
        if (email.equals("") || Objects.requireNonNull(password).equals("")) {
            startActivity(new Intent(MapsActivity.this, WelcomeActivity.class));
            finish();
        } else {
            firebaseAuth.signInWithEmailAndPassword(
                    Objects.requireNonNull(email), Objects.requireNonNull(password))
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), R.string.signedInSuccessfully, Toast.LENGTH_LONG).show();
                            String deviceTokenId = FirebaseInstanceId.getInstance().getId();
                            authId = firebaseAuth.getUid();
                            mobileNumber = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getPhoneNumber();
                            Map<String, String> nurseDataMap = new HashMap<>();
                            nurseDataMap.put("nurseId", nurseId);
                            nurseDataMap.put("device_token", deviceTokenId);
                            nurseDataMap.put("auth_id", authId);
                            nurseDataMap.put("FCM_token", Home.FCMToken);
                            nurseDataMap.put("name", name);
                            nurseDataMap.put("mobile", mobileNumber);
                            nurseDataMap.put("latitude", latitude);
                            nurseDataMap.put("longitude", longitude);
                            nurseDocRef = firebaseFirestore.collection("nurses_available").document(authId);
                            nurseDocRef.set(nurseDataMap);

                            //                        databaseReference = FirebaseDatabase.getInstance().getReference().child("nurses available");
                            //                        nurseReference = databaseReference.child(nurseAuthId);
                            //                        nurseReference.child("nurse_id").setValue(nurseID);
                            //                        nurseReference.child("nurse_name").setValue(nurseName);
                            //                        requestRefrence = nurseReference.child("device_token").setValue(deviceToken);
                            //                        Toast.makeText(MapsActivity.this, R.string.signedInSuccessfully, Toast.LENGTH_LONG).show();
                        }
                    }).addOnFailureListener(e -> {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("sign in fail", e.toString());
                startActivity(new Intent(this, NewNurseActivity.class));
                finish();
            });
        }
    }

    private void initMap() {
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        Objects.requireNonNull(supportMapFragment).getMapAsync(this);
    }

    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        try {
                            Location currentLocation = (Location) task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 12f));
                            mMap.setMyLocationEnabled(true);
                            mMap.getUiSettings().setMapToolbarEnabled(true);
                            //to add mark on current location
//                            mMap.addMarker(new MarkerOptions().position(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())).title("Marker in Sydney"));
//                                addDataToFirebase(currentLocation);
                        } catch (NullPointerException e) {
                            Toast.makeText(MapsActivity.this, R.string.enable_location, Toast.LENGTH_LONG).show();
                        }
                    } else
                        Toast.makeText(MapsActivity.this, "Unable to get current location", Toast.LENGTH_LONG).show();
                });
            }
        } catch (SecurityException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void getLocationPermission() {
        String[] permissions = {FINE_LOCATION, COURSE_LOCATION};
        if (ContextCompat.checkSelfPermission(getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            if (ContextCompat.checkSelfPermission(getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                initMap();
            } else
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        else
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
    }

//    private void addDataToFirebase(Location currentLocation) {
//        Map<String, Object> map = new HashMap<>();
//        map.put("name", PreferenceManager.getDefaultSharedPreferences(this).getString("name", null));
//        map.put("ID", PreferenceManager.getDefaultSharedPreferences(this).getString("id", null));
//        map.put("mobile", PreferenceManager.getDefaultSharedPreferences(this).getString("mobile", null));
//        map.put("location", new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
//        documentReference.set(map).addOnSuccessListener(new OnSuccessListener<Void>() {
//            @Override
//            public void onSuccess(Void aVoid) {
//                documentReference.update("token ID", documentReference.getId());
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.i(getClass().getName(), "failed to get token ID");
//            }
//        });
//    }

    public void language(String langCode) {
        Resources res = getResources();
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        config.setLayoutDirection(locale);
        res.updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    public void checkLanguage() {
        String langCode = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("language", "en");
        if (langCode.equals("ar"))
            language(langCode);
        else
            language("en");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            case R.id.reset_app:
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                sharedPreferences.edit().clear().apply();
                startActivity(new Intent(MapsActivity.this, WelcomeActivity.class));
                finish();
                break;
            case R.id.arabic:
                PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putString("language", "ar").apply();
                language("ar");
                i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                break;
            case R.id.english:
                PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putString("language", "en").apply();
                language("en");
                i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkLanguage();
        if (email == "" || password == "" || name == "" || nurseId == "") {
            startActivity(new Intent(MapsActivity.this, WelcomeActivity.class));
            finish();
        } else if (firebaseAuth == null) {
            firebaseAuth = FirebaseAuth.getInstance();
        }
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }
}