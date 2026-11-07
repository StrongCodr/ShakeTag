package com.vance.shaketag;
import java.util.ArrayList;

public class SensorManager {
    private static final SensorManager instance = new SensorManager();
    private final ArrayList<Sensor> sensors = new ArrayList<>();

    private SensorManager(){
    }
    public boolean hasSensor(String address) {
        for (Sensor sensor : sensors) {
            if (sensor.getAddress().equals(address)) {
                return true;
            }
        }
        return false;
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
