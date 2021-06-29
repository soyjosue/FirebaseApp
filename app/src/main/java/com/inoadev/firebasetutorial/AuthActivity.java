package com.inoadev.firebasetutorial;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.Arrays;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private LinearLayout authLayout;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button signUpButton;
    private Button logInButton;
    private Button googleButton;
    private Button facebookButton;

    private final int GOOGLE_SIGN_IN = 100;
    private CallbackManager callbackManager = CallbackManager.Factory.create();

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Splash
        SystemClock.sleep(2000); // HACK:
        setTheme(R.style.Theme_FirebaseTutorial);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Getting elements of the UI
        getElementsUI();

        // Calling Firebase Analytics
        final FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);

        // Make an analytic event
        final Bundle bundle = new Bundle();
        bundle.putString("message", "Integración de Firebase Completa");
        analytics.logEvent("InitScreen", bundle);

        // Remote Config
        FirebaseRemoteConfig firebaseConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(10)
                .build();
        firebaseConfig.setConfigSettingsAsync(configSettings);
        firebaseConfig.setDefaultsAsync(Map.of("show_error_button", false, "error_button_text", "Forzar Error"));

        // Setup
        notification();
        setup();
        session();
    }

    @Override
    protected void onStart() {
        super.onStart();
        authLayout.setVisibility(View.VISIBLE);
    }

    private void session() {
        final SharedPreferences prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE);
        String email = prefs.getString("email", null);
        String provider = prefs.getString("provider", null);

        if (email != null && provider != null) {
            showHome(email, ProviderType.valueOf(provider));
            authLayout.setVisibility(View.INVISIBLE);
        }
    }

    private void notification() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    System.out.println("Este es el token del dispositivo: " + task.getResult());
                }
            }
        });

        // Temas (Topics)
        FirebaseMessaging.getInstance().subscribeToTopic("tutorial");

        // Recuperar informacion
        String url = getIntent().getStringExtra("url");
        if (url != null) {
            Toast.makeText(getBaseContext(), "Ha llegado información en una push: " + url, Toast.LENGTH_LONG).show();
        }
    }

    private void setup() {

        // Setting a title in the ActionBar
        getSupportActionBar().setTitle("Autenticación");

        signUpButton.setOnClickListener(View -> {
            if (!getText(emailEditText).isEmpty() && !getText(passwordEditText).isEmpty()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(getText(emailEditText), getText(passwordEditText))
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {

                                if (task.isSuccessful()) {
                                    showHome(task.getResult().getUser().getEmail(), ProviderType.BASIC);
                                } else {
                                    showAlert();
                                }

                            }
                        });
            }
        });

        logInButton.setOnClickListener(View -> {
            if (!getText(emailEditText).isEmpty() && !getText(passwordEditText).isEmpty()) {

                FirebaseAuth.getInstance()
                        .signInWithEmailAndPassword(getText(emailEditText), getText(passwordEditText))
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {

                                if (task.isSuccessful()) {
                                    showHome(task.getResult().getUser().getEmail(), ProviderType.BASIC);
                                } else {
                                    showAlert();
                                }

                            }
                        });
            }
        });

        googleButton.setOnClickListener(View -> {
            // Setup
            GoogleSignInOptions googleConf = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            GoogleSignInClient googleClient = GoogleSignIn.getClient(this, googleConf);
            // Checking that another account isn't logged in
            googleClient.signOut();
            // ----------
            Intent googleIntent = googleClient.getSignInIntent();

            startActivityForResult(googleIntent, GOOGLE_SIGN_IN);
        });

        facebookButton.setOnClickListener(View -> {

            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email"));

            LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    if (loginResult != null) {
                        AccessToken token = loginResult.getAccessToken();
                        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());

                        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(AuthActivity.this, task -> {
                            if (task.isSuccessful()) {
                                String email = task.getResult().getUser().getEmail();
                                showHome(email, ProviderType.FACEBOOK);
                            } else {
                                showAlert();
                            }
                        });
                    }
                }

                @Override
                public void onCancel() {

                }

                @Override
                public void onError(FacebookException error) {
                    showAlert();
                }
            });

        });

    }

    private void showAlert() {

        AlertDialog.Builder builder = new AlertDialog.Builder(AuthActivity.this);
        builder.setTitle("Error");
        builder.setMessage("Se ha producido un error autenticado al usuario");
        builder.setPositiveButton("Aceptar", null);
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void showHome(String email, ProviderType provider) {

        Intent homeIntent = new Intent(this, HomeActivity.class);
        homeIntent.putExtra("email", email);
        homeIntent.putExtra("provider", provider.name());
        startActivity(homeIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        callbackManager.onActivityResult(requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);

                if (account != null) {
                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnCompleteListener(this, task1 -> {

                                if (task1.isSuccessful()) {
                                    showHome(account.getEmail(), ProviderType.GOOGLE);
                                } else {
                                    showAlert();
                                }

                            });
                }

            } catch (ApiException e) {
                Log.d("err", e.getMessage());
                showAlert();
            }
        }
    }

    private void getElementsUI() {
        authLayout = (LinearLayout) findViewById(R.id.authLayout);
        emailEditText = (EditText) findViewById(R.id.emailEditText);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        signUpButton = (Button) findViewById(R.id.signUpButton);
        logInButton = (Button) findViewById(R.id.logInButton);
        googleButton = (Button) findViewById(R.id.googleButton);
        facebookButton = (Button) findViewById(R.id.facebookButton);
    }

    private String getText(EditText et) {
        return et.getText().toString();
    }

}