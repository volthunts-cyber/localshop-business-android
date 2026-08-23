package com.localshop.business;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.content.ContextCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class OrderMessagingService extends FirebaseMessagingService {
    @Override public void onMessageReceived(RemoteMessage message) {
        if(!"new_order".equals(value(message,"type","new_order"))) return;

        String targetRole=value(message,"role","");
        String appRole=BuildConfig.APP_ROLE;
        if("admin".equals(appRole)) return;
        if(!targetRole.isEmpty()) {
            boolean roleMatches=("founder".equals(appRole)&&"founder".equals(targetRole))
                || ("superfounder".equals(appRole)&&("super_founder".equals(targetRole)||"superfounder".equals(targetRole)));
            if(!roleMatches) return;
        }

        String orderId=value(message,"order_id",String.valueOf(System.currentTimeMillis()));
        String orderNo=value(message,"order_number","New");
        String shopName=value(message,"shop_name","Alpha Mart");
        String total=value(message,"total","");
        String shopId=value(message,"shop_id","");
        String path="superfounder".equals(appRole)
            ? "/superfounder/?shop="+android.net.Uri.encode(shopId)+"&order="+android.net.Uri.encode(orderId)+"&alert=1"
            : "/founder/?order="+android.net.Uri.encode(orderId)+"&alert=1";

        // Give immediate tactile feedback before starting the foreground alert service.
        try {
            Vibrator vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);
            if(vibrator!=null&&vibrator.hasVibrator()) {
                long[] pattern=new long[]{0,650,160,650,160,1100};
                if(Build.VERSION.SDK_INT>=26) vibrator.vibrate(VibrationEffect.createWaveform(pattern,-1));
                else vibrator.vibrate(pattern,-1);
            }
        } catch(Exception ignored) {}

        Intent alert=new Intent(this,OrderAlertService.class)
            .putExtra("order_id",orderId)
            .putExtra("order_number",orderNo)
            .putExtra("shop_name",shopName)
            .putExtra("total",total)
            .putExtra("order_path",path);
        try {
            ContextCompat.startForegroundService(this,alert);
        } catch (RuntimeException e) {
            OrderNotification.createChannel(this);
            NotificationManager nm=getSystemService(NotificationManager.class);
            if(nm!=null)nm.notify(orderId.hashCode(),OrderNotification.build(this,orderId,orderNo,shopName,total,path));
        }
    }

    @Override public void onNewToken(String token) {
        getSharedPreferences("localshop",MODE_PRIVATE).edit().putString("fcm_token",token).apply();
    }

    private String value(RemoteMessage m,String key,String fallback){
        String v=m.getData().get(key);return v==null?fallback:v;
    }
}
