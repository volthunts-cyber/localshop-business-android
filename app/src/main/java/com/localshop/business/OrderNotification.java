package com.localshop.business;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public final class OrderNotification {
    public static final String CHANNEL="new_orders_voice_v3";
    private OrderNotification() {}

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm=context.getSystemService(NotificationManager.class);
        if (nm==null) return;
        Uri sound=Uri.parse("android.resource://"+context.getPackageName()+"/"+R.raw.order_voice_alert);
        AudioAttributes attrs=new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
        NotificationChannel c=new NotificationChannel(CHANNEL,"Urgent new orders",NotificationManager.IMPORTANCE_HIGH);
        c.setDescription("Spoken new-order alert, vibration and lock-screen notification");
        c.enableVibration(true);
        c.setVibrationPattern(new long[]{0,600,180,600,180,1000});
        c.setSound(sound,attrs);
        c.setBypassDnd(true);
        c.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(c);
    }

    public static android.app.Notification build(Context context,String orderId,String orderNo,String shopName,String total,String path) {
        Intent open=new Intent(context,MainActivity.class).putExtra("order_path",path).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int code=(orderId==null?String.valueOf(System.currentTimeMillis()):orderId).hashCode();
        PendingIntent pi=PendingIntent.getActivity(context,code,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String body="Order #"+orderNo+" received"+(total==null||total.isEmpty()?"":" • ₹"+total)+". Start packing.";
        return new NotificationCompat.Builder(context,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(shopName+" — New order")
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setVibrate(new long[]{0,600,180,600,180,1000})
            .build();
    }
}
