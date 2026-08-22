package com.localshop.business;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class OrderAlertService extends Service implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private PowerManager.WakeLock wakeLock;
    private String speech="New order received. Start packing.";

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        OrderNotification.createChannel(this);
        String orderId=intent.getStringExtra("order_id");
        String orderNo=intent.getStringExtra("order_number");
        String shopName=intent.getStringExtra("shop_name");
        String total=intent.getStringExtra("total");
        String path=intent.getStringExtra("order_path");
        speech="New order received. Start packing. Order number "+(orderNo==null?"":orderNo)+".";
        startForeground(4901,OrderNotification.build(this,orderId,orderNo==null?"New":orderNo,shopName==null?"Alpha Mart":shopName,total,path));
        PowerManager pm=(PowerManager)getSystemService(Context.POWER_SERVICE);
        if(pm!=null){wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"LocalShop:OrderVoice");wakeLock.acquire(15000);}
        Vibrator v=(Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        if(v!=null){v.vibrate(VibrationEffect.createWaveform(new long[]{0,600,180,600,180,1000},-1));}
        tts=new TextToSpeech(getApplicationContext(),this);
        return START_NOT_STICKY;
    }

    @Override public void onInit(int status) {
        if(status==TextToSpeech.SUCCESS){
            tts.setLanguage(new Locale("en","IN"));
            tts.setSpeechRate(0.92f);
            tts.setPitch(1.0f);
            tts.speak(speech,TextToSpeech.QUEUE_FLUSH,null,"new_order_voice");
            new android.os.Handler(getMainLooper()).postDelayed(this::finishAlert,7000);
        } else finishAlert();
    }

    private void finishAlert(){
        if(tts!=null){tts.stop();tts.shutdown();tts=null;}
        if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();
        stopForeground(false);
        stopSelf();
    }

    @Override public void onDestroy(){
        if(tts!=null){tts.stop();tts.shutdown();tts=null;}
        if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
