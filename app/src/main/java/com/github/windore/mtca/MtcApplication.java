package com.github.windore.mtca;

import android.app.Application;

import com.github.windore.mtca.mtc.Mtc;

public class MtcApplication extends Application {
    private Mtc mtc;

    @Override
    public void onCreate() {
        super.onCreate();

        mtc = Mtc.constructOnlyOnce();
    }

    public Mtc getMtc() {
        return mtc;
    }
}
