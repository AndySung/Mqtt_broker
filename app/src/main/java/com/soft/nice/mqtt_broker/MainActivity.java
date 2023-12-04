package com.soft.nice.mqtt_broker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import com.soft.nice.mqtt_broker.broker.MQTTService;
import com.soft.nice.mqtt_broker.broker.ServerInstance;
import com.soft.nice.mqtt_broker.util.InputFilterMinMax;
import com.soft.nice.mqtt_broker.util.Utils;
import org.apache.log4j.BasicConfigurator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import io.moquette.BrokerConstants;
import io.moquette.spi.impl.subscriptions.Subscription;

public class MainActivity extends Activity {
    private MQTTService mService;
    private boolean mBound = false;
    Context context;

    TextView port, username, password, host, username_title, password_title, auth_text;
    Switch MqttServerSwitch, MqttAuthSwitch;
    LinearLayout layout_username, layout_password, layout_port;
    RelativeLayout enable_mqtt_server_relativeLayout, enable_auth_relativeLayout;

    File confFile, passwordFile, aclFile, mqttFile;
    Properties props;
    private IntentFilter intentFilter;
    private NetworkChangeReceiver networkChangeReceiver;

    protected void onStart() {
        super.onStart();
        this.bindService(new Intent(this, MQTTService.class), mConnection, BIND_IMPORTANT);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            MainActivity.this.mService = ((MQTTService.LocalBinder) service).getService();
            MainActivity.this.mBound = ((MQTTService.LocalBinder) service).getServerStatus();
//             MainActivity.this.updateStartedStatus();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            MainActivity.this.mBound = false;
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.blue));
        setContentView(R.layout.activity_main);
        context = this;
        BasicConfigurator.configure();
        intentFilter=new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");  //广播接收器想要监听什么广播
        initView();
        // 配置文件
        confFile = new File(getApplicationContext().getDir("media", 0).getAbsolutePath() +"/"+ Utils.BROKER_CONFIG_FILE);
        passwordFile = new File(getApplicationContext().getDir("media", 0).getAbsolutePath() +"/"+ Utils.PASSWORD_FILE);
        aclFile = new File(getApplicationContext().getDir("media",0).getAbsolutePath() + "/"+Utils.ACL_FILE);
        mqttFile = new File(getApplicationContext().getDir("media",0).getAbsolutePath()+"/"+Utils.MQTT_FILE);
        Log.i("andysong---MAIN", confFile.getAbsolutePath());
        loadConfig();
        MqttAuthSwitch.setChecked(true);
        startService();
        initData();
    }

    private void initView() {
        host = findViewById(R.id.text_input_host);
        port = findViewById(R.id.text_input_port);
        port.setFilters(new InputFilter[]{new InputFilterMinMax(1, 65535)});
        username = findViewById(R.id.text_input_username);
        password = findViewById(R.id.text_input_password);
        MqttServerSwitch = findViewById(R.id.mqtt_server_switch);
        MqttAuthSwitch = findViewById(R.id.mqtt_auth_switch);
        username_title = findViewById(R.id.username_title);
        password_title = findViewById(R.id.password_title);
        layout_username = findViewById(R.id.layout_username);
        layout_password = findViewById(R.id.layout_password);
        layout_port = findViewById(R.id.layout_port);
        auth_text = findViewById(R.id.auth_text);
        enable_mqtt_server_relativeLayout = findViewById(R.id.enable_mqtt_server_relativeLayout);
        enable_auth_relativeLayout = findViewById(R.id.enable_auth_relativeLayout);
        props = new Properties();
        MqttAuthSwitch.setChecked(true);

        // 判断服务是否连接
        if(Utils.isServiceRunning(context, "com.soft.nice.mqtt_broker.broker.MQTTService")){
            MqttServerSwitch.setChecked(true);
        }else{
            MqttServerSwitch.setChecked(false);
        }
    }

    private void initData() {
        MqttServerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {//选中
                    startService();
                    MqttServerSwitch.setChecked(true);
                }else {
                    stopService();
                    MqttServerSwitch.setChecked(false);
                }
            }
        });

        MqttAuthSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setAuthView(isChecked);
                if(MqttServerSwitch.isChecked() || Utils.isServiceRunning(context, "com.soft.nice.mqtt_broker.broker.MQTTService")) {
                    restartService();
                }
                Toast.makeText(MainActivity.this, "MQTT broker config updated", Toast.LENGTH_SHORT).show();
            }
        });

        //username Layout点击事件
        layout_username.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditText("Username", R.mipmap.username_icon);
            }
        });

        //password Layout点击事件
        layout_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditText("Password", R.mipmap.password_icon);
            }
        });

        //port Layout点击事件
        layout_port.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditText("Port", R.mipmap.port_icon);
            }
        });
        //监听网络广播，判断网络是否连接
        networkChangeReceiver=new NetworkChangeReceiver();
        registerReceiver(networkChangeReceiver,intentFilter);       //调用resigerReceiver()方法进行注册
    }

    private void showEditText(String strTitle, int drawableID) {
        final EditText inputServer = new EditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(strTitle).setIcon(drawableID).setView(inputServer)
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDialog(dialog, true);
            }
        });
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if(strTitle.equals("Username")){
                    if(inputServer.getText().toString().equals("")) {
                        username.setText("admin");
                    }else{
                        if(!username.getText().toString().equals(inputServer.getText().toString())) {
                            username.setText(inputServer.getText().toString());
                            Toast.makeText(MainActivity.this, "You need to restart the server for applying the config changes", Toast.LENGTH_SHORT).show();
                        }
                        dismissDialog(dialog, true);
                    }
                    username.setText(inputServer.getText().toString().equals("") ? "admin" : inputServer.getText().toString());
                    dismissDialog(dialog, true);
                }else if(strTitle.equals("Password")) {
                    if(inputServer.getText().toString().equals("")) {
                        password.setText("admin");
                    }else{
                        if(!password.getText().toString().equals(inputServer.getText().toString())) {
                            password.setText(inputServer.getText().toString());
                            Toast.makeText(MainActivity.this, "You need to restart the server for applying the config changes", Toast.LENGTH_SHORT).show();
                        }
                        dismissDialog(dialog, true);
                    }
                }else if(strTitle.equals("Port")){
                    if(inputServer.getText().toString().equals("")) {
                        dismissDialog(dialog, false);
                        Toast.makeText(MainActivity.this, "Cannot be empty, range is \"1~65535\"", Toast.LENGTH_SHORT).show();
                    }else{
                        if(!port.getText().toString().equals(inputServer.getText().toString())) {
                            port.setText(inputServer.getText().toString());
                            Toast.makeText(MainActivity.this, "You need to restart the server for applying the config changes", Toast.LENGTH_SHORT).show();
                        }
                        dismissDialog(dialog, true);
                    }
                }
            }
        });
        builder.show();
        if(strTitle.equals("Port")){
            inputServer.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            inputServer.setText(port.getText().toString());
        }else if(strTitle.equals("Username")){
            inputServer.setText(username.getText().toString());
        }else if(strTitle.equals("Password")){
            inputServer.setText(password.getText().toString());
        }
        inputServer.setSingleLine();
        showInputTips(inputServer);
    }

    private void dismissDialog(DialogInterface dialog, boolean isWhether2Close){
        Field field = null;
        try {//通过反射获取dialog中的私有属性mShowing
            field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
            field.setAccessible(true);//设置该属性可以访问
        } catch (Exception ex) {}
        try {
            field.set(dialog, isWhether2Close);
            dialog.dismiss();
        } catch (Exception ex) {}
    }

    private void showInputTips(EditText et_text) {
        et_text.setFocusable(true);
        et_text.setFocusableInTouchMode(true);
        et_text.requestFocus();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //此处activity为对话框所在Activity类
                InputMethodManager imm = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
            }
        }, 200);//这里的200是设置的延时时间毫秒值，可按需要自行更改
    }

    public void setAuthView(boolean isChecked) {
        if(isChecked == true) {
            layout_username.setEnabled(true);
            layout_password.setEnabled(true);
            auth_text.setText(context.getResources().getText(R.string.enable_auth));
            username_title.setTextColor(getResources().getColor(R.color.black));
            password_title.setTextColor(getResources().getColor(R.color.black));
            username.setTextColor(getResources().getColor(R.color.blue));
            password.setTextColor(getResources().getColor(R.color.blue));
            MqttAuthSwitch.setChecked(true);

        }else {
            layout_username.setEnabled(false);
            layout_password.setEnabled(false);
            auth_text.setText(context.getResources().getText(R.string.enable_no_auth));
            username_title.setTextColor(getResources().getColor(R.color.gray));
            password_title.setTextColor(getResources().getColor(R.color.gray));
            username.setTextColor(getResources().getColor(R.color.gray));
            password.setTextColor(getResources().getColor(R.color.gray));
            MqttAuthSwitch.setChecked(false);
        }
    }

    private void restartService() {
        stopService();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                startService();
            }
        }, 1000);
    }

    //配置信息
    private Properties defaultConfig() {
        //Properties props = new Properties();
        props.setProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, context.getExternalFilesDir(null).getAbsolutePath() + File.separator + BrokerConstants.DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME);
        props.put(BrokerConstants.PORT_PROPERTY_NAME, "1883");
        props.setProperty(BrokerConstants.NEED_CLIENT_AUTH, "false");
        props.setProperty(BrokerConstants.HOST_PROPERTY_NAME, BrokerConstants.HOST);    //这里直接设置成 0.0.0.0
        props.setProperty(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, String.valueOf(BrokerConstants.WEBSOCKET_PORT));
        writeToAclFile(aclFile);
        props.setProperty(BrokerConstants.ACL_FILE_PROPERTY_NAME, aclFile.getAbsolutePath());
        return props;
    }


    //填写ACL文件（测试数据）【Test ACL file availability】
    private void writeToAclFile(File aclFile) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(aclFile)) {
            fileOutputStream.write("# This affects access control for clients with no username.\n".getBytes());
            fileOutputStream.write("topic write SYS/test\n".getBytes());
            fileOutputStream.write("topic read SYS/test\n".getBytes());
            fileOutputStream.write("# This only affects clients with username \"admin\".\n".getBytes());
            fileOutputStream.write("user admin\n".getBytes());
            fileOutputStream.write("topic beijing\n".getBytes());
            fileOutputStream.write("# This only affects clients with username \"admin123\".\n".getBytes());
            fileOutputStream.write("user admin123\n".getBytes());
            fileOutputStream.write("topic nanjing\n".getBytes());
            fileOutputStream.write("# This only affects clients with username \"admin345\".\n".getBytes());
            fileOutputStream.write("user admin345\n".getBytes());
            fileOutputStream.write("topic shanghai\n".getBytes());
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Unable to save password", Toast.LENGTH_SHORT).show();
    }

    //填写密码文件（测试数据）【Test password file availability】
    private void writeToPasswordFile(File passwordFile) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(passwordFile)) {
            fileOutputStream.write(username.getText().toString().getBytes());
            fileOutputStream.write(":".getBytes());
            fileOutputStream.write(Utils.getSHA(password.getText().toString()).getBytes());
            fileOutputStream.write("\n".getBytes());

            fileOutputStream.write("admin123".getBytes());
            fileOutputStream.write(":".getBytes());
            fileOutputStream.write(Utils.getSHA(password.getText().toString()).getBytes());
            fileOutputStream.write("\n".getBytes());

            fileOutputStream.write("admin345".getBytes());
            fileOutputStream.write(":".getBytes());
            fileOutputStream.write(Utils.getSHA(password.getText().toString()).getBytes());
            fileOutputStream.write("\n".getBytes());


            return;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Unable to save password", Toast.LENGTH_SHORT).show();
    }

    //保存设置
    private Properties saveAndGetConfig() {
        String vPort = port.getText().toString();
        Boolean vAuth = MqttAuthSwitch.isChecked() ? true : false;
        //  Properties props = new Properties();
        props.setProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, context.getExternalFilesDir(null).getAbsolutePath() + File.separator + BrokerConstants.DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME);
        props.setProperty(BrokerConstants.PORT_PROPERTY_NAME, vPort);
        props.setProperty(BrokerConstants.HOST_PROPERTY_NAME, Utils.getBrokerURL(MainActivity.this)); //这里直接设置成 0.0.0.0
        props.setProperty(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, String.valueOf(BrokerConstants.WEBSOCKET_PORT));
        props.setProperty(BrokerConstants.NEED_CLIENT_AUTH, String.valueOf(vAuth));
        if (vAuth) {
            props.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, "false");
            writeToPasswordFile(passwordFile);
            props.setProperty(BrokerConstants.PASSWORD_FILE_PROPERTY_NAME, passwordFile.getAbsolutePath());
        }else{
            props.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, "true");
        }

        Log.i("MAIN", props.toString());

        try (OutputStream output = new FileOutputStream(confFile)) {
            props.store(output, "Last saved on " + new Date().toString());
            return props;
        } catch (IOException io) {
            Log.i("MAIN", "Unable to load broker config file. Using default config");
            return defaultConfig();
        }
    }

    private Properties loadConfig() {
        try (InputStream input = new FileInputStream(confFile)) {
            //Properties props = new Properties();
            props.load(input);
            updateUI(props);
            return props;
        } catch (FileNotFoundException e) {
            Log.e("MAIN", "Config file not found. Using default config");
        } catch (IOException ex) {
            Log.e("MAIN", "IOException. Using default config");
        }
        Properties props = defaultConfig();
        updateUI(props);
        return props;
    }

    private void updateUI(Properties props) {
        port.setText(props.getProperty(BrokerConstants.PORT_PROPERTY_NAME));
        host.setText(props.getProperty(BrokerConstants.HOST_PROPERTY_NAME));
        props.setProperty(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, String.valueOf(BrokerConstants.WEBSOCKET_PORT));
        if (props.getProperty(BrokerConstants.NEED_CLIENT_AUTH) != null && Boolean.valueOf(props.getProperty(BrokerConstants.NEED_CLIENT_AUTH))) {
            MqttAuthSwitch.setChecked(true);
        } else {
            MqttAuthSwitch.setChecked(false);
        }
    }

    public void startService() {
        saveAndGetConfig();     //启动服务前先保存下配置文件
        if(!Utils.isServiceRunning(context, "com.soft.nice.mqtt_broker.broker.MQTTService")) {
            if (mBound == true && mService != null) {
                Log.i("MainActivity", "Service already running");
                return;
            }
            Intent serviceIntent = new Intent(this, MQTTService.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("config", defaultConfig());
            serviceIntent.putExtras(bundle);
            startService(serviceIntent);
            this.bindService(new Intent(this, MQTTService.class), mConnection, BIND_IMPORTANT);
        }
    }

    public void stopService() {
        if(Utils.isServiceRunning(context, "com.soft.nice.mqtt_broker.broker.MQTTService")) {
            Intent serviceIntent = new Intent(this, MQTTService.class);
            stopService(serviceIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            this.unbindService(mConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        unregisterReceiver(networkChangeReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Tips");
            builder.setMessage("The notification permission is not enabled. Do you want to enable it?");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    jumpNotificationSetting();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });
            builder.create();
            builder.show();
        }
        if(Utils.isServiceRunning(context, "com.soft.nice.mqtt_broker.broker.MQTTService")){
            MqttServerSwitch.setChecked(true);
        }else{
            MqttServerSwitch.setChecked(false);
        }
        setAuthView(MqttAuthSwitch.isChecked());
    }

    private void jumpNotificationSetting() {
        final ApplicationInfo applicationInfo = getApplicationInfo();
        try {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", applicationInfo.packageName);
            intent.putExtra("android.provider.extra.APP_PACKAGE", applicationInfo.packageName);
            intent.putExtra("app_uid", applicationInfo.uid);
            startActivity(intent);
        } catch (Throwable t) {
            t.printStackTrace();
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", applicationInfo.packageName, null));
            startActivity(intent);
        }
    }


    class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent){
            ConnectivityManager connectionManager=(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);  //得到系统服务类
            NetworkInfo networkInfo=connectionManager.getActiveNetworkInfo();
            if(networkInfo!=null && networkInfo.isAvailable()){
                host.setText(Utils.getBrokerURL(MainActivity.this));
            }else{
                host.setText(BrokerConstants.HOST);
            }
        }
    }


    /**
     * getConnectionsManager: 代理的连接管理器
     * getSubscriptions： Broker 嵌入式应用程序使用 SPI 方法来获取订阅者列表。 如果返回 null 代理未启动。
     * internalPublish(MqttPublishMessage msg, final String clientId) : 使用代理发布消息。 它适用于嵌入应用程序。 可以用仅在使用 startServer 正确启动服务器后。
     * **/
    public void getSubs(View v) {
        Collection<String> clientArray = new ArrayList<>();
        Collection<Subscription> SubArrays = new ArrayList<>();
        try {
            Collection<String> clients = ServerInstance.getServerInstance().getConnectionsManager().getConnectedClientIds();
            clients.forEach(client -> {
                Log.i("andysong---->Client是否连接上client：", String.valueOf(ServerInstance.getServerInstance().getConnectionsManager().isConnected(client)));

                Log.i("andysong---->Clients_1:",  ServerInstance.getServerInstance().getSubscriptions() +"");
                SubArrays.addAll(ServerInstance.getServerInstance().getSubscriptions());
                Log.i("andysong---->Clients_2:",  ServerInstance.getServerInstance().getConnectionsManager().getConnectedClientIds().toString()+ "");
                clientArray.addAll(ServerInstance.getServerInstance().getConnectionsManager().getConnectedClientIds());
                Log.i("andysong---->Clients_3:",  client+ "");
                Log.i("andysong---->Clients-count：",  ServerInstance.getServerInstance().getConnectionsManager().countActiveConnections()+"");
                Log.i("andysong---->Clients-listenOnHazelCastMsg",  ServerInstance.getServerInstance().getHazelcastInstance()+"");
                Log.i("andysong---->andy：",  ServerInstance.getServerInstance().getSubscriptions().size()+"");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        Iterator<String> it = clientArray.iterator();
        Log.i("andysong-->clientArray--it：", String.valueOf(it));
        while (it.hasNext()){
            String s = it.next();
            Log.i("andysong-->clientArray", s);
        }

        Iterator<Subscription> subs = SubArrays.iterator();
        Log.i("andysong-->clientArray--subs：", String.valueOf(subs.hasNext()));
        while (subs.hasNext()){
            Subscription s = subs.next();
            Log.i("andysong-->Subscription", s.getClientId() +":"+ s.getTopicFilter() +":"+s.getRequestedQos());
        }

    }
}
