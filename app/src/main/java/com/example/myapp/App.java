package com.example.myapp;

import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.example.myapp.extractor.NewPipeDownloader;

import org.schabi.newpipe.extractor.NewPipe;

import java.lang.reflect.Method;

/**
 * Application class.
 *
 * FIX 1: Extends MultiDexApplication — required on API 28 for large dex counts.
 * FIX 2: StrictMode permissive on API 28 — some OEM ROMs (Pie) have aggressive
 *         thread policies that kill reflective init inside NewPipe.
 * FIX 3: NewPipe.init() called on main thread — safe, it only stores the reference.
 * FIX 4: Android 9 (API 28) TLS workaround — some API 28 builds restrict TLS
 *         connections using hidden APIs; we unlock them via reflection.
 *         Without this, NewPipe network calls silently fail → blank screen.
 */
public class App extends MultiDexApplication {

    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();

        // FIX 2: Relax StrictMode on Android 9 to prevent OEM ROM crashes during init
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            StrictMode.setThreadPolicy(
                new StrictMode.ThreadPolicy.Builder().permitAll().build()
            );
            // FIX 4: Unlock Android 9 hidden API / network restrictions
            allowAndroid9HiddenApis();
        }

        // Init NewPipe — safe on main thread (no network, just stores downloader ref)
        NewPipe.init(NewPipeDownloader.getInstance());
    }

    /**
     * Android 9 (API 28) introduced "hidden API" restrictions that also block
     * some internal TLS/SSL setup paths used by OkHttp + NewPipe on certain OEM
     * builds. This reflection trick (safe — only targets API 28) removes those
     * restrictions before any network call is made.
     *
     * Without this, on some Android 9 devices all HTTP calls return silently empty
     * bodies or throw SSLHandshakeException, leaving the UI permanently blank.
     */
    private void allowAndroid9HiddenApis() {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P) return;
        try {
            Class<?> vmRuntimeClass = Class.forName("dalvik.system.VMRuntime");
            Method getRuntime = vmRuntimeClass.getDeclaredMethod("getRuntime");
            getRuntime.setAccessible(true);
            Object vmRuntime = getRuntime.invoke(null);

            Method setHiddenApiExemptions =
                vmRuntimeClass.getDeclaredMethod("setHiddenApiExemptions", String[].class);
            setHiddenApiExemptions.setAccessible(true);
            setHiddenApiExemptions.invoke(vmRuntime,
                new Object[]{new String[]{"L"}}); // exempt all
        } catch (Exception e) {
            Log.w(TAG, "Could not unlock hidden APIs on Android 9: " + e.getMessage());
            // Non-fatal — app continues; network may still work without this
        }
    }
}
