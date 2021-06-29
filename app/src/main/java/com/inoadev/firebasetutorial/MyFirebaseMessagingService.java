package com.inoadev.firebasetutorial;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

// This class is to setup the notify when the app is open.
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Looper.prepare();
        new Handler().post(() ->
                Toast.makeText(getBaseContext(), remoteMessage.getNotification().getTitle(), Toast.LENGTH_LONG).show()
        );
        Looper.loop();

    }

    private void showMessage(String msg) {
    }
}
