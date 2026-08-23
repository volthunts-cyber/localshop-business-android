package com.localshop.business;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final int FILE_CHOOSER_REQUEST = 2002;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private Uri pendingCameraUri;

    public final class NativeBridge {
        @JavascriptInterface public String getFcmToken() { return getSharedPreferences("localshop", MODE_PRIVATE).getString("fcm_token", ""); }
        @JavascriptInterface public String getPlatform() { return "android"; }
        @JavascriptInterface public String getRole() { return BuildConfig.APP_ROLE; }
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
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback; pendingCameraUri = null;
                try {
                    Intent picker = new Intent(Intent.ACTION_GET_CONTENT); picker.addCategory(Intent.CATEGORY_OPENABLE); picker.setType("image/*");
                    Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    if (dir != null && camera.resolveActivity(getPackageManager()) != null) {
                        File photo = File.createTempFile("localshop-proof-", ".jpg", dir);
                        pendingCameraUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", photo);
                        camera.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
                        camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    }
                    Intent launch;
                    if (params != null && params.isCaptureEnabled() && pendingCameraUri != null) launch = camera;
                    else { Intent chooser = Intent.createChooser(picker, "Take photo or choose from gallery"); if (pendingCameraUri != null) chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{camera}); launch = chooser; }
                    startActivityForResult(launch, FILE_CHOOSER_REQUEST); return true;
                } catch (Exception e) { fileCallback.onReceiveValue(null); fileCallback=null; pendingCameraUri=null; return false; }
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u=request.getUrl(); String host=u.getHost()==null?"":u.getHost().toLowerCase(); String scheme=u.getScheme()==null?"":u.getScheme().toLowerCase();
                boolean maps=(host.contains("google.com")&&u.getPath()!=null&&u.getPath().contains("/maps"))||host.contains("maps.google")||host.equals("maps.app.goo.gl")||"geo".equals(scheme);
                if(maps||"tel".equals(scheme)||"upi".equals(scheme)){try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}return true;}
                if("http".equals(scheme)||"https".equals(scheme))return false; try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}return true;
            }
        });
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token->{getSharedPreferences("localshop",MODE_PRIVATE).edit().putString("fcm_token",token).apply();if(webView!=null)webView.post(()->webView.evaluateJavascript("window.dispatchEvent(new Event('localshop-fcm-token-ready'))",null));});
        openFromIntent(getIntent());
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){if(requestCode==FILE_CHOOSER_REQUEST){Uri[] result=null;if(resultCode==Activity.RESULT_OK){if(data!=null&&data.getData()!=null)result=new Uri[]{data.getData()};else if(pendingCameraUri!=null)result=new Uri[]{pendingCameraUri};}if(fileCallback!=null)fileCallback.onReceiveValue(result);fileCallback=null;pendingCameraUri=null;return;}super.onActivityResult(requestCode,resultCode,data);}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);openFromIntent(intent);}

    private void openFromIntent(Intent intent){
        String path=intent.getStringExtra("order_path");
        if(path==null||path.isEmpty()){
            String role=intent.getStringExtra("role"),shopId=intent.getStringExtra("shop_id"),orderId=intent.getStringExtra("order_id");
            if(orderId!=null&&!orderId.isEmpty()){
                if("superfounder".equals(BuildConfig.APP_ROLE)||"super_founder".equals(role))path="/superfounder/?shop="+Uri.encode(shopId==null?"":shopId)+"&order="+Uri.encode(orderId)+"&alert=1";
                else if("founder".equals(BuildConfig.APP_ROLE)||"founder".equals(role))path="/founder/?order="+Uri.encode(orderId)+"&alert=1";
                else path="/admin/";
            }
        }
        if(path==null||path.isEmpty())path=BuildConfig.START_PATH;
        webView.loadUrl(BuildConfig.BUSINESS_BASE_URL+path);
    }
    @Override public void onBackPressed(){if(webView.canGoBack())webView.goBack();else super.onBackPressed();}
}
