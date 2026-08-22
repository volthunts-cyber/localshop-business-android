package com.localshop.business;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
    private WebView webView;

    public final class NativeBridge {
        @JavascriptInterface public String getFcmToken() {
            return getSharedPreferences("localshop", MODE_PRIVATE).getString("fcm_token", "");
        }
        @JavascriptInterface public String getPlatform() { return "android"; }
        @JavascriptInterface public void openNotificationSettings() {
            runOnUiThread(() -> {
                Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(i);
            });
        }
        @JavascriptInterface public void openDndAccessSettings() {
            runOnUiThread(() -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        setContentView(webView);
        requestNotificationPermission();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new NativeBridge(), "LocalShopNative");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String scheme = u.getScheme();
                if ("tel".equals(scheme) || "geo".equals(scheme) || "upi".equals(scheme)) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                    return true;
                }
                if ("http".equals(scheme) || "https".equals(scheme)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectFcmTokenRegistration(url);
            }
        });
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            getSharedPreferences("localshop", MODE_PRIVATE).edit().putString("fcm_token", token).apply();
            if (webView != null) webView.post(() -> {
                webView.evaluateJavascript("window.dispatchEvent(new Event('localshop-fcm-token-ready'))", null);
                injectFcmTokenRegistration(webView.getUrl());
            });
        });
        OrderNotification.createChannel(this);
        maybeOfferDndSetup();
        openFromIntent(getIntent());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void maybeOfferDndSetup() {
        if (Build.VERSION.SDK_INT < 23) return;
        NotificationManager nm=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        boolean alreadyAsked=getSharedPreferences("localshop",MODE_PRIVATE).getBoolean("asked_dnd",false);
        if (!alreadyAsked && nm != null && !nm.isNotificationPolicyAccessGranted()) {
            getSharedPreferences("localshop",MODE_PRIVATE).edit().putBoolean("asked_dnd",true).apply();
            new AlertDialog.Builder(this)
                .setTitle("Urgent order alerts")
                .setMessage("For the strongest order alert, allow Alpha Mart Business to interrupt Do Not Disturb. You can change this later in Android settings.")
                .setNegativeButton("Later", null)
                .setPositiveButton("Open settings", (d,w) -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)))
                .show();
        }
    }

    private void injectFcmTokenRegistration(String currentUrl) {
        if (webView == null || currentUrl == null) return;
        String token=getSharedPreferences("localshop",MODE_PRIVATE).getString("fcm_token","");
        if (token == null || token.length() < 40) return;
        boolean superFounder=currentUrl.contains("/superfounder");
        String endpoint=superFounder?"/api/founder/fcm/register":"/api/admin/fcm/register";
        String safeToken=token.replace("\\","\\\\").replace("'","\\'");
        String js="fetch('"+endpoint+"',{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({token:'"+safeToken+"',device_label:'Alpha Mart Android'})}).catch(()=>{});";
        webView.evaluateJavascript(js,null);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openFromIntent(intent);
    }

    private void openFromIntent(Intent intent) {
        String path = intent.getStringExtra("order_path");
        if (path == null || path.isEmpty()) path = "/owner";
        webView.loadUrl(BuildConfig.BUSINESS_BASE_URL + path);
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
