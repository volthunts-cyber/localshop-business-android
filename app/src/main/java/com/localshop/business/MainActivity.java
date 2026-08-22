package com.localshop.business;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        OrderNotification.createChannel(this);
        webView = new WebView(this);
        setContentView(webView);
        if (android.os.Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new NativeBridge(), "LocalShopNative");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("http".equals(u.getScheme()) || "https".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
        });
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            getSharedPreferences("localshop", MODE_PRIVATE).edit().putString("fcm_token", token).apply();
            if (webView != null) webView.post(() -> webView.evaluateJavascript("window.dispatchEvent(new Event('localshop-fcm-token-ready'))", null));
        });
        openFromIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openFromIntent(intent);
    }

    private void openFromIntent(Intent intent) {
        String path = intent.getStringExtra("order_path");
        if (path == null || path.isEmpty()) {
            String role=intent.getStringExtra("role");
            String shopId=intent.getStringExtra("shop_id");
            String orderId=intent.getStringExtra("order_id");
            if (orderId != null && !orderId.isEmpty()) {
                if ("super_founder".equals(role)) {
                    path="/superfounder/?shop="+Uri.encode(shopId==null?"":shopId)+"&order="+Uri.encode(orderId)+"&alert=1";
                } else {
                    path="/admin/?order="+Uri.encode(orderId)+"&alert=1";
                }
            }
        }
        if (path == null || path.isEmpty()) path = "/admin/";
        webView.loadUrl(BuildConfig.BUSINESS_BASE_URL + path);
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
