package com.github.windore.mtca;

import android.app.Application;

public class MtcApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        System.loadLibrary("rustmtca");
    }

    public static native String test();
}
