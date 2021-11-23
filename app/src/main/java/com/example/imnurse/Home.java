package com.example.imnurse;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class Home extends Application {
    public static FirebaseUser firebaseUser;
    public static FirebaseAuth firebaseAuth;
    public static FirebaseFirestore database;
    public static String FCMToken;

    @Override
    public void onCreate() {
        super.onCreate();

        Firebase.setAndroidContext(this);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e("FCM token failed", "Fetching FCM registration token failed", task.getException());
                Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            // Get new FCM registration token
            FCMToken = task.getResult();

            // Log and toast
            Log.d("FCM token", FCMToken);
        });
    }
}
