package com.example.imnurse.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.imnurse.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class NewNurseActivity extends AppCompatActivity {
    //strings
    static final String url = "http://egynursingsyndicate.org/%D8%A7%D8%B3%D8%AA%D8%B9%D9%84%D8%A7%D9%85" +
            "-%D8%A7%D9%84%D8%A7%D8%B9%D8%B6%D8%A7%D8%A1/";
    private String nurseID;
    private String password;
    private String email;

    public static boolean codeSent = false;

    //jsoup
    static Document document;
    static Elements elements;

    //views
    EditText emailEditText, passwordEditText, nurseIDEditText;
    TextView notAMemberTextView, newMember;
    Button signInButton;
    LinearLayout linearLayout;
    private ProgressBar progressBar;

    //firebase
    private FirebaseAuth firebaseAuth;

    //shared preferences
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkLanguage();
        setContentView(R.layout.activity_new_nurse);

        firebaseAuth = FirebaseAuth.getInstance();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = sharedPreferences.edit();
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.new_nurse_activity_title);
        setSupportActionBar(toolbar);
        progressBar = findViewById(R.id.progress_circular);
        progressBar.setVisibility(View.INVISIBLE);

        linearLayout = findViewById(R.id.linear);
        notAMemberTextView = findViewById(R.id.if_member);

        emailEditText = findViewById(R.id.email_editText);
        passwordEditText = findViewById(R.id.password_editText);

        nurseIDEditText = findViewById(R.id.id_number);

        signInButton = findViewById(R.id.login);
        signInButton.setOnClickListener(v -> {
            if (validateFields())
                if (checkInternet()){
                    makeHttpRequest();
                    firebaseSignUp();
                }
        });

        newMember = findViewById(R.id.new_member);
        newMember.setOnClickListener(v -> {
            View view = View.inflate(getApplicationContext(), R.layout.alert_dialog_mobile_number, null);
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(NewNurseActivity.this);
            alertDialog.setView(view);
            final EditText editText = view.findViewById(R.id.edit_text);
            alertDialog.setTitle(R.string.enter_mobile);
            alertDialog.setPositiveButton(R.string.ok, (dialog, which) -> {
                String mobile = editText.getText().toString();
                if(mobile.length() != 10)
                    Toast.makeText(getApplicationContext(), R.string.mobile_error, Toast.LENGTH_LONG).show();
                else {
                    Intent intent = new Intent(NewNurseActivity.this, CodeActivity.class);
                    intent.putExtra("mobileNumber", mobile);
                    startActivity(intent);
                    finish();
                }
            }).setNegativeButton(R.string.cancel, (dialog, which) ->
                    dialog.dismiss());
            alertDialog.show();
            newMember.setVisibility(View.GONE);
        });
    }

    public boolean validateFields() {
        email = emailEditText.getText().toString().trim();
        password = passwordEditText.getText().toString().trim();
        nurseID = nurseIDEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getResources().getString(R.string.empty_field));
            emailEditText.requestFocus();
            return false;
        }
        else if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getResources().getString(R.string.empty_field));
            passwordEditText.requestFocus();
            return false;
        }
        else if (TextUtils.isEmpty(nurseID)) {
            nurseIDEditText.setError(getResources().getString(R.string.empty_field));
            nurseIDEditText.requestFocus();
            return false;
        }
        else
            return true;
    }

    public boolean checkInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        try {
            if (networkInfo != null && networkInfo.isConnected())
                return true;
            else
                Toast.makeText(this, R.string.check_connection, Toast.LENGTH_LONG).show();
        } catch (NullPointerException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    public void makeHttpRequest() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, NewNurseActivity.url,
                response -> {
                    if (parseHtml(response)) {
                        progressBar.setVisibility(View.GONE);
                    }
                    else {
                        progressBar.setVisibility(View.GONE);
                        notAMemberTextView.setVisibility(View.VISIBLE);
                        notAMemberTextView.setText(R.string.you_are_not_a_member);
                    }
                }, error -> {
            parseHtml("response"); //remove this line when app comes to live
            linearLayout.setVisibility(View.GONE);
            notAMemberTextView.setVisibility(View.VISIBLE);
            notAMemberTextView.setText(R.string.unexpected_response_code);
            progressBar.setVisibility(View.GONE);
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map params = new HashMap();
                params.put("member_code", nurseID);
                return params;
            }
        };
        int socketTimeOut = 10000;
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut,
                0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);
    }

    public boolean parseHtml(String response) {
        String nurseName = "ahmed";
//        String nurseID = nurseIDEditText.getText().toString();
        editor.putString("name", nurseName);
        editor.putString("nurseId", nurseID);
        editor.commit();
        return true;
//        document = Jsoup.parse(response);
//        elements = document.getElementsByAttributeValue("class", "comment-reply-title");
//        String nurseData = elements.text();
//        if (nurseData.contains("معلومات العضو")) {
//            elements = document.select("#main-content > div.content-wrap > div > article > div:nth-child(1) > " +
//                    "div.entry > table:nth-child(3) > thead > tr > th:nth-child(2)");
//            String nurseName = elements.text();
//
//            elements = document.select("#main-content > div.content-wrap > div > article > div:nth-child(1) > " +
//                    "div.entry > table:nth-child(3) > tbody > tr:nth-child(1) > td:nth-child(2)");
//            nurseID = elements.text();
//            nurseID = nurseID.replace("/", "-");
//            editor.putString("name", nurseName);
//            editor.putString("nurseId", nurseID);
//            editor.commit();
//            addInfoToGoogleSheet();
//            return true;
//        }
//        return true;
    }

    public void firebaseSignUp() {
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        try {
            Objects.requireNonNull(firebaseAuth.getCurrentUser()).linkWithCredential(credential).
                    addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                editor.putString("email", email);
                                editor.putString("password", password);
                                editor.putString("nurseId", nurseID);
                                editor.commit();
                            }
                        }
                    }).addOnFailureListener(e -> {
                Log.e("sign_in_error", e.getMessage());
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
        catch (NullPointerException ignored){}
    }


//    void addInfoToGoogleSheet() {
//        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://script.google.com/macros/s/AKfycbwkevOCyKDWQlHr6svw5hqK8fkai6Uize6k5Md4cyVKABXBdRM/exec",
//                new Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
//            }
//        }) {
//            @Override
//            protected Map<String, String> getParams() {
//                Map params = new HashMap();
//                params.put("name", NewNurseActivity.nurseName);
//                params.put("idNumber", NewNurseActivity.nurseID);
//                params.put("mobileNumber", NewNurseActivity.nurseMobile);
//                return params;
//            }
//        };
//        int socketTimeOut = 30000;  //30 seconds
//        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
//        stringRequest.setRetryPolicy(retryPolicy);
//        RequestQueue queue = Volley.newRequestQueue(this);
//        queue.add(stringRequest);
//    }

    public void language(String langCode){
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
    protected void onResume() {
        super.onResume();
        checkLanguage();
    }
}