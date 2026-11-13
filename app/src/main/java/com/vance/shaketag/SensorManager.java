package com.vance.shaketag;

import java.util.ArrayList;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.wit.witsdk.Device.DeviceManager;
import com.wit.witsdk.Device.DeviceModel;
import com.wit.witsdk.Device.Interface.DeviceDataListener;

public class SensorManager implements DeviceDataListener{
    private static final SensorManager instance = new SensorManager();
    private final ArrayList<Sensor> sensors = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable onChange;
    private static final int RECONNECT_DELAY_MS = 2000;
    private Context appContext;
    private static final int MAX_LENGTH_NAME = 20;
    private static final double SHAKE_THRESHOLD_G = 0.5;


    public String renameSensor(Sensor sensor, String newName){
        String name = newName.trim();
        String error = checkName(sensor, name);
        if (error == null){
            sensor.setName(name);
        }
        return error;
    }
    private String checkName(Sensor sensor, String name){
        if (name.isEmpty()){
            return "name cannot be empty.";
        }
        if (name.length() >= MAX_LENGTH_NAME){
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
        Context appContext = context;
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
    @Override
    public void OnStatusChange(String address, boolean connected) {
        Sensor sensor = getSensor(address);
        if (sensor == null) {
            return;
        }
        if (connected) {
            // SC2: the link is back, it counts as connected once data arrives
            if (sensor.getStatus() == Status.DISCONNECTED) {
                sensor.setStatus(Status.RECONNECTING);
            }
        } else {
            // SC2: the sensor dropped, so keep trying in the background until it is back
            sensor.setStatus(Status.DISCONNECTED);
            DeviceManager.getInstance().GetDevice(address).CloseDevice();
            mainHandler.postDelayed(() -> reconnect(sensor), RECONNECT_DELAY_MS);
        }
        tellScreen();
    }

    private void reconnect(Sensor sensor) {
        DeviceModel deviceModel = DeviceManager.getInstance().GetDevice(sensor.getAddress());
        try {
            deviceModel.Connect(appContext);
        } catch (Exception e) {
            sensor.setStatus(Status.DISCONNECTED);
            tellScreen();
        }
        if (!sensors.contains(sensor)) {
            return;
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

    // The number in a name like "Sensor 3"
    private int getNumber(String defaultName) {
        return Integer.parseInt(defaultName.substring(7));
    }

}
