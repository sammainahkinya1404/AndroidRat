package com.example.androidrat;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class mainService extends Service {
    static String TAG ="mainServiceClass";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"in");
        new com.example.androidrat.jumper(getApplicationContext()).init();
        return START_STICKY;
    }
}
