package com.vance.shaketag;
import java.util.ArrayList;

public class SensorManager {
    private static final SensorManager instance = new SensorManager();
    private final ArrayList<Sensor> sensors = new ArrayList<>();

    private SensorManager(){
    }

    public static SensorManager getInstance(){
        return instance;
    }
    public void addSensor(Sensor sensor){
        sensors.add(sensor);
    }
    public ArrayList<Sensor> getSensors() {
        return sensors;
    }
}
