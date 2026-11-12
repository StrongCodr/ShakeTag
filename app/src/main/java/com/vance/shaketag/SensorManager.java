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
    }

    @Override
    public void OnReceive(String address, String data) {
        Sensor sensor = getSensor(address);
        if (sensor == null || sensor.getStatus() == Status.CONNECTED) {
            return;
        }
        sensor.setStatus(Status.CONNECTED);
        tellScreen();
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
        sensors.add(sensor);
    }
    public ArrayList<Sensor> getSensors() {
        return sensors;
    }
}
