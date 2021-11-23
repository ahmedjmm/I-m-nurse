package com.example.imnurse.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.imnurse.Home;
import com.example.imnurse.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class CodeActivity extends AppCompatActivity {
    EditText codeEditText;
    public static String mVerificationId, code, mobileNumber;
    Button verify;
    private FirebaseAuth mAuth;

    PhoneAuthOptions options;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    PhoneAuthCredential phoneAuthCredential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkLanguage();
        setContentView(R.layout.activity_code);

        mAuth = FirebaseAuth.getInstance();

        verify = findViewById(R.id.verify);
        codeEditText = findViewById(R.id.code);
        mobileNumber = getIntent().getStringExtra("mobileNumber");

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                code = phoneAuthCredential.getSmsCode();
                signInWithPhoneAuthCredential(phoneAuthCredential);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
                editor.putString("mobileNumber", mobileNumber);
                editor.commit();
                Intent intent = new Intent(CodeActivity.this, NewNurseActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("verification failed", e.getMessage());
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                super.onCodeSent(verificationId, token);
                mVerificationId = verificationId;
                NewNurseActivity.codeSent = true;
                if(code != null)
                    phoneAuthCredential = PhoneAuthProvider.getCredential(verificationId, code);
                Toast.makeText(getApplicationContext(), R.string.code_sent, Toast.LENGTH_SHORT).show();
            }
        };
        options = PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+971" + mobileNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);

        verify.setOnClickListener(v -> {
            code = codeEditText.getText().toString();
            if(!code.equals("")){
                phoneAuthCredential = PhoneAuthProvider.getCredential(mVerificationId, code);
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }
            else {
                codeEditText.setError(getResources().getString(R.string.empty_field));
            }
        });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful())
                Home.firebaseUser = Objects.requireNonNull(task.getResult()).getUser();
        }).addOnFailureListener(e -> {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("phone sign in exception", e.getMessage());
        });
        startActivity(new Intent(this, NewNurseActivity.class));
        finish();
    }

    public void language(String langCode){
        Resources res = getResources();
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        config.setLayoutDirection(locale);
        res.updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    public void checkLanguage(){
        String langCode = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("language","en" );
        if(langCode.equals("ar"))
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