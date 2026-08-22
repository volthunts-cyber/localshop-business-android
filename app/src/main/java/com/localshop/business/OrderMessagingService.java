package com.localshop.business;

import android.app.NotificationManager;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class OrderMessagingService extends FirebaseMessagingService {
    @Override public void onMessageReceived(RemoteMessage message) {
        if(!"new_order".equals(value(message,"type","new_order"))) return;
        String orderId=value(message,"order_id",String.valueOf(System.currentTimeMillis()));
        String orderNo=value(message,"order_number","New");
        String shopName=value(message,"shop_name","Alpha Mart");
        String total=value(message,"total","");
        String role=value(message,"role","owner");
        String shopId=value(message,"shop_id","");
        String path="super_founder".equals(role)
            ? "/superfounder/?shop="+android.net.Uri.encode(shopId)+"&order="+android.net.Uri.encode(orderId)+"&alert=1"
            : "/admin/?order="+android.net.Uri.encode(orderId)+"&alert=1";

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
