package com.localshop.business;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class LocalShopApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .build();
            FirebaseApp.initializeApp(this, options);
        }
    }
}
