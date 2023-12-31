package com.soft.nice.mqtt_broker.util;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.format.Formatter;
import android.util.Log;

import java.math.BigInteger;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class Utils {
    public static final String PASSWORD_FILE = "pwd.conf";
    public static final String BROKER_CONFIG_FILE = "mqtt.properties";
    public static final String ACL_FILE = "acl_file.conf";
    public static final String MQTT_FILE = "mqtt_file.conf";

    public static void isPortOpen(int port) {
        SocketCheckTask task = new SocketCheckTask();
        task.execute(port);
    }

    public static String  getVersion() {
        String javaVersion = System.getProperty("java.version");
        System.out.format("Java Version = '%s'", javaVersion);
        return javaVersion;
    }

    public static String getBrokerURL(Context ctx) {
        return Formatter.formatIpAddress(((WifiManager) ctx.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getIpAddress());
    }

    public static void showDialog(Context content, String message) {
        new AlertDialog.Builder(content).setTitle("Error")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public static String getSHA(String input) throws NoSuchAlgorithmException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(input.getBytes());

            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw e;
        }
    }

    /**
     * 判断Service是否正在运行
     *
     * @param context     上下文
     * @param serviceName Service 类全名
     * @return true 表示正在运行，false 表示没有运行
     */
    public static boolean isServiceRunning(Context context, String serviceName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceInfoList = manager.getRunningServices(200);
        if (serviceInfoList.size() <= 0) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo info : serviceInfoList) {
            if (info.service.getClassName().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }

}

class SocketCheckTask extends AsyncTask<Integer, Void, Boolean> {
    @Override
    protected Boolean doInBackground(Integer... params) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostName(), params[0]), 1000);
            socket.close();
            return true;
        } catch (ConnectException ce) {
            ce.printStackTrace();
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean isOpen) {
        Log.i("Async", String.valueOf(isOpen));
    }
}