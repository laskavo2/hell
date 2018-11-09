package com.example.pc.autopetfeeder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

/**
 * Created by GHKwon on 2016-02-17.
 */
public class BroadcastD extends BroadcastReceiver {
    String INTENT_ACTION = Intent.ACTION_BOOT_COMPLETED;

    @Override
    public void onReceive(Context context, Intent intent) {//알람 시간이 되었을때 onReceive를 호출함
        //NotificationManager 안드로이드 상태바에 메세지를 던지기위한 서비스 불러오고
        String channelId = "channel";
        String channelName = "Channel Name";



        NotificationManager notificationmanager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);

            notificationmanager.createNotificationChannel(mChannel);

        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,channelId);
        builder.setSmallIcon(R.drawable.ccat);
        builder.setTicker("밥 줄 시간");
        builder.setWhen(System.currentTimeMillis());
        builder.setNumber(1).setContentTitle("[예약 알림]");
        builder.setContentText("밥주기 완료!");
        //builder.setContentIntent(pendingIntent);
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        builder.setAutoCancel(true);
        notificationmanager.notify(0, builder.build());
    }


}