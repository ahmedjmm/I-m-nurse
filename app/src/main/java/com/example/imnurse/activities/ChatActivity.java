package com.example.imnurse.activities;

import static com.example.imnurse.Home.database;
import static com.example.imnurse.activities.MapsActivity.nurseDocRef;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.imnurse.Messages;
import com.example.imnurse.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {
    //views
    EditText messageEditText;
    ImageButton sendImageButton;
    ListView messagesListView;

    //for listview
    List<Messages> messagesList = new ArrayList<>();
    MessagesAdapter messagesAdapter;

    String patientFCMToken;

    //firestore
    private DocumentReference chatReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkLanguage();

        setContentView(R.layout.activity_chat);

        Objects.requireNonNull(getSupportActionBar()).setTitle(MapsActivity.name);
        messagesListView = findViewById(R.id.messages_list);
        messagesAdapter = new MessagesAdapter(this, messagesList);
        messagesListView.setAdapter(messagesAdapter);
        messageEditText = findViewById(R.id.text_message);
        sendImageButton = findViewById(R.id.send_button);
        chatReference = database.collection("chats").document(MapsActivity.authId);
        chatReference.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("snapshot", "listen:error", error);
                return;
            }
            if (value != null && value.exists()) {
                messagesAdapter.add(new Messages(value.getString("messageBody"),
                        value.getString("messageTitle")));
                patientFCMToken = value.getString("patientFCMToken");
                Map<String,String > map = new HashMap<>();
                map.put("patientFCMToken", patientFCMToken);
                chatReference.set(map);

                Log.i("chat object", "my snapshot " + value.getString("messageBody"));
            }
        });
        sendImageButton.setOnClickListener(v -> {
            Thread thread = new Thread(() -> {
                try {
                    sendMessage();
                } catch (IOException e) {
                    Log.d("sendMessage() error", e.getMessage());
                }
            });
            thread.start();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        nurseSignOut();
        chatReference.delete().addOnFailureListener(e -> {
            Log.d("delete document", e.getMessage());
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    public void nurseSignOut() {
        FirebaseAuth.getInstance().signOut();
        nurseDocRef.delete().addOnFailureListener(e ->
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show());
    }

    public void sendMessage() throws IOException {
        String messageBody = "";
        messageBody = messageEditText.getText().toString();
        if(messageBody.equals("")) {
            messageEditText.setError(getString(R.string.edit_text_error_chat));
            return;
        }
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("to", patientFCMToken);
        } catch (JSONException e) {
            Log.d("JSON exception 1", e.getMessage());
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("message", messageBody);
            jsonObject.put("sender", MapsActivity.name);
        } catch (JSONException e) {
            Log.d("JSON exception 2", e.getMessage());
        }
        try {
            jsonBody.putOpt("data", jsonObject);
        } catch (JSONException e) {
            Log.d("JSON exception 3", e.getMessage());
        }
        final String requestBody = jsonBody.toString();
        RequestQueue queue = Volley.newRequestQueue(this);
        String finalMessageBody = messageBody;
        StringRequest stringRequest = new StringRequest(Request.Method.POST,"https://fcm.googleapis.com/fcm/send",
                response -> {
            Log.d("volley response", "Response: " + response);
            Map<String, String> map = new HashMap<>();
            map.put("messageBody", finalMessageBody);
            map.put("messageTitle", MapsActivity.name);
            chatReference.set(map);
        }, error -> Log.d("volley response", "That didn't work..." + error)){
            @Override
            protected Map<String,String> getParams(){

                return new HashMap<>();
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> params = new HashMap<>();
                params.put("Authorization","key=AAAAX_L5Ko4:APA91bEmEbR8_YtvAKIngtVDXvulInxufWCgwblA" +
                        "caTbvPM2tdxqzquYiuT3R-piD54vO0z_eiFKcu8Z5p_C4kGhvohWJpBnZmn3qJu4k5SC0vcf0CC" +
                        "rJdsPXkNNOftRUjeTB_xAji-A\t\n");
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    return requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }

        };
        queue.add(stringRequest);

//        Map<String, Object> chatMap = new HashMap<>();
//        chatMap.put("messageBody", messageBody);
//        chatMap.put("messageTitle", name + ":");
//        chatReference.set(chatMap).addOnSuccessListener((unused) -> {}).
//                addOnFailureListener(e ->
//                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show());
//
//        URL url = new URL("https://fcm.googleapis.com/fcm/send");
//        HttpURLConnection httpURLConnection;
//        httpURLConnection = (HttpURLConnection) url.openConnection();
//        httpURLConnection.setDoOutput(true);
//        httpURLConnection.setRequestMethod("POST");
//        httpURLConnection.setRequestProperty("Content-Type", "application/json");
//        httpURLConnection.setRequestProperty("Authorization", "key=AAAAX_L5Ko4:APA91bEmEbR8_YtvAKIngtVDXvulInxufWCgwblAcaTbvPM2tdxqzquYiuT3R-piD54vO0z_eiFKcu8Z5p_C4kGhvohWJpBnZmn3qJu4k5SC0vcf0CCrJdsPXkNNOftRUjeTB_xAji-A\t\n");
//        String postJsonData = "{\"to\": \""+FCMToken+", \"data\": {\"message\": \""+messageBody+"}}";
//        DataOutputStream dataOutputStream = null;
//        try {
//            dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
//        } catch (IOException e) {
//            Log.e("dataOutputStream", Objects.requireNonNull(e.getMessage()));
//            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
//        }
//        Objects.requireNonNull(dataOutputStream).writeBytes(postJsonData);
//        dataOutputStream.flush();
//        dataOutputStream.close();
//        int responseCode = httpURLConnection.getResponseCode();
//        if (responseCode == HttpURLConnection.HTTP_OK){
//            Log.d("http response success", responseCode + "data added");
//        }
//        else {
//            Log.d("http response error", String.valueOf(responseCode));
//        }
        //////////////////////////////////////////////////////////////////////////////////////////////////////////
//        Toast.makeText(getApplicationContext(), "fhkjshfkjsfkds", Toast.LENGTH_LONG).show();
//        Toast.makeText(getApplicationContext(), responseCode, Toast.LENGTH_LONG).show();
//        messageEditText.setText("");
//        messageEditText.clearFocus();
        //         Instantiate the RequestQueue.
//        RequestQueue queue = Volley.newRequestQueue(this);
//        String url = "https://fcm.googleapis.com/fcm/send";
//        String contentType = "Content-Type:application/json";
//        // Request a string response from the provided URL.
//        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
//                new Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//                        Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT).show();
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Log.d("volley error", Objects.requireNonNull(error.getMessage()));
//                Toast.makeText(getApplicationContext(),
//                        Objects.requireNonNull(error.getMessage()), Toast.LENGTH_SHORT).show();
//            }
//
//        }){
//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                Map<String, String> params = new HashMap<>();
//                params.put("Content-Type", "application/json");
//                params.put("Authorization", "key=AAAAX_L5Ko4:APA91bEmEbR8_YtvAKIngtVDXvulInxufWCgwblAcaTbvPM2tdxqzquYiuT3R-piD54vO0z_eiFKcu8Z5p_C4kGhvohWJpBnZmn3qJu4k5SC0vcf0CCrJdsPXkNNOftRUjeTB_xAji-A\t\n");
//                params.put("to", nurseFCM_token);
//                return params;
//            }
//        };
//        // Add the request to the RequestQueue.
//        queue.add(stringRequest);

        //on response success
    }

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

    static class MessagesAdapter extends ArrayAdapter<Messages> {
        List<Messages> messagesList;
        Context context;
        ViewHolder viewHolder;

        public MessagesAdapter(@NonNull Context context, @NonNull List<Messages> messagesList) {
            super(context, 0,messagesList);
            this.messagesList = messagesList;
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if(convertView == null){
                convertView = LayoutInflater.from(getContext()).
                        inflate(R.layout.messages_list_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.messageBodyTextView = convertView.findViewById(R.id.message_body);
                viewHolder.messageBodyTextView.setMovementMethod(new ScrollingMovementMethod());
                viewHolder.messageTitleTextView = convertView.findViewById(R.id.message_title);
                viewHolder.messageTitleTextView.setMovementMethod(new ScrollingMovementMethod());
                convertView.setTag(viewHolder);
            }
            viewHolder = (ViewHolder) convertView.getTag();
            viewHolder.messageBodyTextView.setText(messagesList.get(position).messageBody);
            viewHolder.messageTitleTextView.setText(messagesList.get(position).messageTitle);
            return convertView;
        }

        @Override
        public int getCount() {
            return messagesList.size();
        }

        static class ViewHolder{
            public TextView messageBodyTextView, messageTitleTextView;
        }
    }
}