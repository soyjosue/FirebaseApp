package com.inoadev.firebasetutorial;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.HashMap;
import java.util.Map;

enum ProviderType {
    BASIC,
    GOOGLE,
    FACEBOOK
}

public class HomeActivity extends AppCompatActivity {

    private TextView emailTextView;
    private TextView providerTextView;
    private EditText addressEditText;
    private EditText phoneEditText;
    private Button saveButton;
    private Button getButton;
    private Button deleteButton;
    private Button logOutButton;
    private Button errorButton;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Getting elements of the UI
        getElementsUI();

        // Setup

        final Bundle bundle = getIntent().getExtras();
        final String email = bundle.getString("email");
        final String provider = bundle.getString("provider");

        setup(email, provider);

        // saving data

        final SharedPreferences.Editor prefs = (SharedPreferences.Editor) getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit();
        prefs.putString("email", email);
        prefs.putString("provider", provider);
        prefs.apply();

        // Remote Config
        errorButton.setVisibility(View.INVISIBLE);
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Boolean showErrorButton = remoteConfig.getBoolean("show_error_button");
                String errorButtonText = remoteConfig.getString("error_button_text");

                if (showErrorButton) {
                    errorButton.setVisibility(View.VISIBLE);
                }
                errorButton.setText(errorButtonText);
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void setup(String email, String provider) {

        // Setting a title in the ActionBar
        getSupportActionBar().setTitle("Inicio");

        emailTextView.setText(email);
        providerTextView.setText(provider);

        logOutButton.setOnClickListener(view -> {

            // Deleting Date
            final SharedPreferences.Editor prefs = (SharedPreferences.Editor) getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit();
            prefs.clear();
            prefs.apply();

            if (provider.equals(ProviderType.FACEBOOK.name())) {
                LoginManager.getInstance().logOut();
            }

            FirebaseAuth.getInstance().signOut();
            onBackPressed();
        });
        errorButton.setOnClickListener(view -> {

            // Enviar informacion adicional
            FirebaseCrashlytics.getInstance().setUserId(email);
            FirebaseCrashlytics.getInstance().setCustomKey("provider", provider);

            // Enviar log de contexto
            FirebaseCrashlytics.getInstance().log("Se ha pulsado el boton FORZAR ERROR.");

            // Forzado de error
            throw new RuntimeException("Forzado de error");
        });
        saveButton.setOnClickListener(view -> {
            db.collection("users").document(email).set(Map.of(
                    "provider", provider,
                    "address", addressEditText.getText().toString(),
                    "phone", phoneEditText.getText().toString()
            ));
        });
        getButton.setOnClickListener(view -> {
            db.collection("users").document(email).get().addOnSuccessListener(documentSnapshot -> {
                addressEditText.setText(documentSnapshot.get("address").toString());
                phoneEditText.setText(documentSnapshot.get("phone").toString());
            });
        });
        deleteButton.setOnClickListener(view -> {
            db.collection("users").document(email).delete();
        });

    }

    private void getElementsUI() {
        emailTextView = (TextView) findViewById(R.id.emailTextView);
        providerTextView = (TextView) findViewById(R.id.providerTextView);
        addressEditText = (EditText) findViewById(R.id.addressEditText);
        phoneEditText = (EditText) findViewById(R.id.phoneEditText);
        saveButton = (Button) findViewById(R.id.saveButton);
        getButton = (Button) findViewById(R.id.getButton);
        deleteButton = (Button) findViewById(R.id.deleteButton);
        logOutButton = (Button) findViewById(R.id.logOutButton);
        errorButton = (Button) findViewById(R.id.errorButton);
    }

}