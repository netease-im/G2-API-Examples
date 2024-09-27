package com.netease.nertc.config;

import android.app.Activity;

import androidx.core.app.ActivityCompat;

import com.netease.lava.nertc.sdk.NERtc;

import java.util.List;

public class DemoDeploy {
    public static final String APP_KEY = "";
    private static final int PERMISSION_REQUEST_CODE = 100;

    public static void requestPermissionsIfNeeded(Activity context) {
        final List<String> missedPermissions = NERtc.checkPermission(context);
        if (missedPermissions.size() > 0) {
            ActivityCompat.requestPermissions(context, missedPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }
}
