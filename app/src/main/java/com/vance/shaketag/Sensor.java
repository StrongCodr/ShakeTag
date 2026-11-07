package com.vance.shaketag;
//one sensor the user has added
public class Sensor {
    private final String address;
    private final String name;

    public Sensor(String address, String name){
        this.address = address;
        this.name = name;
    }
    public String getAddress(){
        return address;
    }
    public String getName(){
        return name;
    }
}

