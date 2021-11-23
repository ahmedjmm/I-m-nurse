package com.example.imnurse.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.imnurse.R;

import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkLanguage();

        setContentView(R.layout.activity_welcome);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.main_activity_title);
        setSupportActionBar(toolbar);

        toolbar.setOnMenuItemClickListener(menuItem -> {
            switch(menuItem.getItemId()){
                case R.id.arabic:
                    PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit()
                            .putString("language", "ar").commit();
                    language("ar");
                    recreate();
                    break;
                case R.id.english:
                    PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit()
                            .putString("language", "en").commit();
                    language("en");
                    recreate();
                    break;
            }
            return true;
        });

        TextView textView = (TextView)findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setText(R.string.main_activity_textView);

        Button register = (Button)findViewById(R.id.button);
        register.setOnClickListener(v ->{
            startActivity(new Intent(WelcomeActivity.this, NewNurseActivity.class));
            finish();
        });
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

    public void checkLanguage() {
        String langCode = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("language", "en");
        if (langCode.equals("ar"))
            language(langCode);
        else
            language("en");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.removeItem(R.id.reset_app);
        return true;
    }
}