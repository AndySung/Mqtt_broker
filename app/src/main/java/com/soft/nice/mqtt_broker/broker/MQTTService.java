package com.soft.nice.mqtt_broker.broker;

import static com.soft.nice.mqtt_broker.broker.MQTTNotificationBroker.CHANNEL_ID;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import com.soft.nice.mqtt_broker.MainActivity;
import com.soft.nice.mqtt_broker.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.FutureTask;

public class MQTTService extends Service {

    private static final String TAG = "MQTTService";

    private Thread thread;
    private MQTTBroker broker;
    private boolean status = false;
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MQTTService getService() {
            return MQTTService.this;
        }

        public boolean getServerStatus() {
            return status;
        }
    }

    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting Notification Service");

        Map<String, String> map = (HashMap) intent.getSerializableExtra("config");
        Properties config = new Properties();
        config.putAll(map);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        int flag = 0;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }else{
            flag = PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flag);

        try {
            broker = new MQTTBroker(config);
            FutureTask<Boolean> futureTask = new FutureTask<>(broker);
            if (thread == null || !thread.isAlive()) {
                thread = new Thread(futureTask);
                thread.setName("MQTT Server");
                thread.start();
                if (futureTask.get()) {
                    status = true;
                    Log.i("MQTT Service", "Thread is running");
                    Toast.makeText(this, "MQTT Broker Service started", Toast.LENGTH_LONG).show();
                }
            }
            startForeground(1, mostrarNotificacion(""));
//            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
//                    .setContentTitle("Server MQTT running...")
//                    .setOngoing(true)
//                    .setContentText("Started MQTT Broker Service")
//                    .setSmallIcon(R.drawable.ic_robot)
//                    .setContentIntent(pendingIntent)
//                    .build();


            return START_NOT_STICKY;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        status = false;
        this.stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (status) {
            try {
                Log.d(TAG, "Trying to stop mqtt server");
                broker.stopServer();
                thread.interrupt();
                status = false;
                Toast.makeText(this, "MQTT Broker Service stopped", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Server is not running");
        }
        super.onDestroy();
    }


    private Notification mostrarNotificacion(String contenido){
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Notificacion Broker",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager notificationManager=getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        Notification.Builder notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentText(contenido)
                .setContentTitle("Server MQTT running...")
                .setOngoing(true)
                .setTicker("MQTT")
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_robot);
        return notification.build();
    }
}