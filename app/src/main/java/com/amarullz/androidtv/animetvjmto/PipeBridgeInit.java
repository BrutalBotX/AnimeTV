package com.amarullz.androidtv.animetvjmto;

import android.app.Activity;
import android.webkit.WebView;

import com.miruronative.data.remote.PipeBridge;

public class PipeBridgeInit {
    public static void init(Activity activity) {
        activity.runOnUiThread(() -> {
            try {
                WebView wv = new WebView(activity);
                wv.getSettings().setJavaScriptEnabled(true);
                wv.getSettings().setDomStorageEnabled(true);
                wv.getSettings().setUserAgentString(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"
                );
                PipeBridge.INSTANCE.attach(wv);
            } catch (Exception e) {
                android.util.Log.e("PipeBridgeInit", "Failed to init PipeBridge", e);
            }
        });
    }
}
