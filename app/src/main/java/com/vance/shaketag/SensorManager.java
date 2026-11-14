package com.vance.shaketag;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.wit.witsdk.Device.DeviceManager;
import com.wit.witsdk.Device.DeviceModel;
import com.wit.witsdk.Device.Interface.DeviceDataListener;
import android.util.Log;
import android.annotation.SuppressLint;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class SensorManager implements DeviceDataListener{
    private static final SensorManager instance = new SensorManager();
    private final ArrayList<Sensor> sensors = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable onChange;
    private static final int RECONNECT_DELAY_MS = 2000;
    private Context appContext;
    private static final int MAX_LENGTH_NAME = 20;
    private static final double SHAKE_THRESHOLD_G = 0.5;
    private static final String ALERT_CHANNEL_ID = "sensor_alerts";
    private static final String SAVE_FILE = "sensors.json";


    public String renameSensor(Sensor sensor, String newName){
        String name = newName.trim();
        String error = checkName(sensor, name);
        if (error == null){
            sensor.setName(name);
            saveSensors();
        }
        return error;
    }
    private String checkName(Sensor sensor, String name){
        if (name.isEmpty()){
            return "name cannot be empty.";
        }
        if (name.length() > MAX_LENGTH_NAME){
            return "Name cannot be longer than 20 characters";
        }
        if (isDefaultName(name)) {
            return "Names like \"Sensor 3\" are kept for new sensors.";
        }
        for (Sensor other : sensors) {
            if (other != sensor && other.getName().equals(name)) {
                return "Another sensor already has that name.";
            }
        }
        return null;
    }

    private boolean isDefaultName(String name){
        if (!name.startsWith("Sensor ")) {
            return false;
        }
        String number = name.substring(7);
        if (number.isEmpty()) {
            return false;
        }
        for (char letter : number.toCharArray()) {
            if (!Character.isDigit(letter)) {
                return false;
            }
        }
        return true;
    }


    private SensorManager(){
        DeviceManager.getInstance().AddDeviceListener(this);
    }

    public Sensor getSensor(String address) {
        for (Sensor sensor : sensors) {
            if (sensor.getAddress().equals(address)) {
                return sensor;
            }
        }
        return null;
    }
    public boolean hasSensor(String address) {
        return getSensor(address) != null;
    }
    //onChange is the job SensorManager runs whenever a sensor’s status changes.
    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }
    public void connect (Sensor sensor, Context context){
        appContext = context;
        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        BluetoothDevice device = bluetoothManager.getAdapter().getRemoteDevice(sensor.getAddress());
        DeviceModel deviceModel = new DeviceModel(sensor.getAddress(), device);
        DeviceManager.getInstance().AddDevice(sensor.getAddress(), deviceModel);
        try {
            deviceModel.Connect(context);
        } catch (Exception e) {
            sensor.setStatus(Status.DISCONNECTED);
            tellScreen();
        }
    }
    // SC8: brings back the saved sensors with their names and reconnects them
    public void loadSensors(Context context) {
        appContext = context;
        if (!sensors.isEmpty()) {
            return;
        }
        File saveFile = new File(appContext.getFilesDir(), SAVE_FILE);
        if (!saveFile.exists()) {
            return;
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(saveFile));
            JSONArray sensorArray = new JSONArray(reader.readLine());
            reader.close();
            for (int i = 0; i < sensorArray.length(); i++) {
                JSONObject sensorObject = sensorArray.getJSONObject(i);
                Sensor sensor = new Sensor(sensorObject.getString("address"), sensorObject.getString("name"));
                sensors.add(sensor);
                connect(sensor, appContext);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void OnStatusChange(String address, boolean connected) {
        Sensor sensor = getSensor(address);
        if (sensor == null) {
            return;
        }
        Status previousStatus = sensor.getStatus();

        if (connected) {
            // SC2: the link is back, it counts as connected once data arrives
            if (previousStatus == Status.DISCONNECTED) {
                sensor.setStatus(Status.RECONNECTING);
            }
        } else {
            // SC9: only alert when a working sensor drops, not on every failed retry
            if (previousStatus == Status.CONNECTED) {
                sendDisconnectAlert(sensor);
            }
            // SC2: the sensor dropped, so keep trying in the background until it is back
            sensor.setStatus(Status.DISCONNECTED);
            DeviceManager.getInstance().GetDevice(address).CloseDevice();
            mainHandler.postDelayed(() -> reconnect(sensor), RECONNECT_DELAY_MS);
        }
        tellScreen();
    }
    private void reconnect(Sensor sensor) {
        if (!sensors.contains(sensor)) {
            return;
        }
        DeviceModel deviceModel = DeviceManager.getInstance().GetDevice(sensor.getAddress());
        try {
            deviceModel.Connect(appContext);
        } catch (Exception e) {
            sensor.setStatus(Status.DISCONNECTED);
            tellScreen();
        }
    }

    @Override
    public void OnReceive(String address, String data) {
        Sensor sensor = getSensor(address);
        if (sensor == null) {
            return;
        }

        if (sensor.getStatus() != Status.CONNECTED){
            sensor.setStatus(Status.CONNECTED);
            tellScreen();
        }
        // SC3: a still sensor reads about 1 g (gravity), so anything far from 1 g is a shake
        DeviceModel deviceModel = DeviceManager.getInstance().GetDevice(address);
        double accX = deviceModel.GetData("AccX");
        double accY = deviceModel.GetData("AccY");
        double accZ = deviceModel.GetData("AccZ");
        double strength = Math.sqrt(accX * accX + accY * accY + accZ * accZ);

        if (Math.abs(strength - 1) > SHAKE_THRESHOLD_G) {
            sensor.markShaken();
        }
    }
    // SC5: one above the highest "Sensor n", so renamed sensors don't count
    public String nextDefaultName() {
        int highest = 0;
        for (Sensor sensor : sensors) {
            if (isDefaultName(sensor.getName()) && getNumber(sensor.getName()) > highest) {
                highest = getNumber(sensor.getName());
            }
        }
        return "Sensor " + (highest + 1);
    }
    private void tellScreen(){
        if (onChange != null){
            mainHandler.post(onChange);
        }
    }


    public static SensorManager getInstance(){
        return instance;
    }
    public void addSensor(Sensor sensor){
        if (hasSensor(sensor.getAddress())) {
            return;
        }
        sensors.add(0, sensor);
        saveSensors();
    }
    public ArrayList<Sensor> getSensors() {
        return sensors;
    }
    // SC7: forgets the sensor, then moves every "Sensor n" above it down by one
    public void deleteSensor(Sensor sensor) {
        sensors.remove(sensor);

        DeviceModel deviceModel = DeviceManager.getInstance().GetDevice(sensor.getAddress());
        if (deviceModel != null) {
            deviceModel.CloseDevice();
            DeviceManager.getInstance().RemoveDevice(sensor.getAddress());
        }

        if (isDefaultName(sensor.getName())) {
            int deletedNumber = getNumber(sensor.getName());
            for (Sensor other : sensors) {
                if (isDefaultName(other.getName()) && getNumber(other.getName()) > deletedNumber) {
                    other.setName("Sensor " + (getNumber(other.getName()) - 1));
                }
            }
        }
    }
    // SC8: saves every sensor so they come back after the app is closed
    private void saveSensors() {
        try {
            JSONArray sensorArray = new JSONArray();
            for (Sensor sensor : sensors) {
                JSONObject sensorObject = new JSONObject();
                sensorObject.put("address", sensor.getAddress());
                sensorObject.put("name", sensor.getName());
                sensorArray.put(sensorObject);
            }
            FileWriter writer = new FileWriter(new File(appContext.getFilesDir(), SAVE_FILE));
            writer.write(sensorArray.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // The number in a name like "Sensor 3"
    private int getNumber(String defaultName) {
        return Integer.parseInt(defaultName.substring(7));
    }
    // SC9: tells the user a sensor has dropped, in case they are not looking at the screen
    @SuppressLint("MissingPermission")   // checked with areNotificationsEnabled()
    private void sendDisconnectAlert(Sensor sensor) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(appContext);
        if (!notificationManager.areNotificationsEnabled()) {
            return;
        }

        NotificationChannelCompat.Builder channel = new NotificationChannelCompat.Builder(ALERT_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH);
        channel.setName("Sensor alerts");
        notificationManager.createNotificationChannel(channel.build());

        NotificationCompat.Builder alert = new NotificationCompat.Builder(appContext, ALERT_CHANNEL_ID);
        alert.setSmallIcon(R.drawable.outline_bluetooth_disabled_24);
        alert.setContentTitle(sensor.getName() + " disconnected");
        alert.setContentText("Check it is switched on and in range.");
        alert.setPriority(NotificationCompat.PRIORITY_HIGH);
        alert.setDefaults(NotificationCompat.DEFAULT_ALL);
        alert.setAutoCancel(true);
        notificationManager.notify(sensor.getAddress().hashCode(), alert.build());
    }

}
