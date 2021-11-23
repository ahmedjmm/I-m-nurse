package com.example.imnurse.services;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.imnurse.Home;
import com.example.imnurse.activities.MapsActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    String messageFrom = "";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From:1 " + remoteMessage.getFrom());


        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d("From:2 ", "Message data payload: " + remoteMessage.getData());
            Home.database.collection("chats").
                    document(MapsActivity.authId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    messageFrom = documentSnapshot.getString("messageTitle");
                }
            });
            Intent intent = new Intent("messageRequest");
            intent.putExtra("messageFrom", messageFrom);
//            intent.putExtra("messageBody", remoteMessage.getNotification().getBody());
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MyFirebaseMessagingService.this);
            localBroadcastManager.sendBroadcast(intent);

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
//                scheduleJob();
            } else {
                // Handle message within 10 seconds
//                handleNow();
            }
        }
        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d("From: ", "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
        else {
            Log.d( "empty message", "null message");
        }
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    /**
     * There are two scenarios when onNewToken is called:
     * 1) When a new token is generated on initial app startup
     * 2) Whenever an existing token is changed
     * Under #2, there are three scenarios when the existing token is changed:
     * A) App is restored to a new device
     * B) User uninstalls/reinstalls the app
     * C) User clears app data
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("FCM_token", token);
        database.collection("nurses_available").
                document(MapsActivity.name).update(hashMap);
    }
}
