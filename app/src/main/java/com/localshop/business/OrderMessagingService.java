package com.localshop.business;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Locale;

public class OrderMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL = "new_orders_v1";
    private TextToSpeech tts;

    @Override public void onMessageReceived(RemoteMessage message) {
        String orderNo = value(message, "order_number", "New");
        String shopName = value(message, "shop_name", "Alpha Mart");
        String total = value(message, "total", "");
        String role = value(message, "role", "owner");
        String shopId = value(message, "shop_id", "");
        String orderId = value(message, "order_id", "");
        String path = "super_founder".equals(role) ? "/super-founder?shop=" + Uri.encode(shopId) + "&order=" + Uri.encode(orderId) : "/owner?order=" + Uri.encode(orderId);

        createChannel();
        Intent open = new Intent(this, MainActivity.class).putExtra("order_path", path).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, orderId.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String body = "Order #" + orderNo + " received" + (total.isEmpty() ? "" : " • ₹" + total) + ". Start packing.";
        NotificationCompat.Builder n = new NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(shopName + " — New order")
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setVibrate(new long[]{0, 500, 250, 500, 250, 900});
        getSystemService(NotificationManager.class).notify(orderId.isEmpty() ? (int)System.currentTimeMillis() : orderId.hashCode(), n.build());
        Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(new long[]{0,500,250,500}, -1));
        speak("New order received. Start packing.");
    }

    @Override public void onNewToken(String token) {
        getSharedPreferences("localshop", MODE_PRIVATE).edit().putString("fcm_token", token).apply();
        // Backend registration is intentionally server-authenticated; the web session should upload this token after login.
    }

    private String value(RemoteMessage m, String key, String fallback) { String v=m.getData().get(key); return v==null?fallback:v; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel c = new NotificationChannel(CHANNEL, "New orders", NotificationManager.IMPORTANCE_HIGH);
        c.setDescription("Urgent LocalShop order alerts");
        c.enableVibration(true);
        c.setVibrationPattern(new long[]{0,500,250,500,250,900});
        c.setBypassDnd(true);
        getSystemService(NotificationManager.class).createNotificationChannel(c);
    }

    private void speak(String text) {
        tts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN"));
                tts.setSpeechRate(0.95f);
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "new_order");
            }
        });
    }
}
