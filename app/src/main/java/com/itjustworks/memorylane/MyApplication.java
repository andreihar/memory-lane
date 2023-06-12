package com.itjustworks.memorylane;

import android.app.Application;
import android.content.Context;

import com.google.firebase.database.FirebaseDatabase;

/*
 * MyApplication.java
 *
 * Class Description: Helper class, returns context of the application
 *                    that is required for some methods.
 * Class Invariant: -
 *
 */

public class MyApplication extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }
}
