package com.mapbox.mapboxsdk;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class StorageUtil {

    private static File mStorageDirectory;

    public static File getStorageDirectory(Context context) {
        if (mStorageDirectory == null) {
            mStorageDirectory = context.getExternalFilesDir(null);
            if (mStorageDirectory == null) {
                mStorageDirectory = context.getFilesDir();
            }
        }
        Log.d(StorageUtil.class.getSimpleName(), "Using: " + mStorageDirectory.getAbsolutePath());
        return mStorageDirectory;
    }
}
