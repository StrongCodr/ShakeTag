package com.vance.shaketag;
//one sensor the user has added
public class Sensor {
    private final String address;
    private  String name;
    private Status status;

    public void setName(String name) {
        this.name = name;
    }

    public Sensor(String address, String name){
        this.address = address;
        this.name = name;
        this.status = Status.CONNECTING;
    }
    public String getAddress(){
        return address;
    }
    public String getName(){
        return name;
    }
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
    public String getStatusText() {
        if (status == Status.CONNECTED){
            return "connected";
        } else if (status == Status.RECONNECTING) {
            return "reconnecting...";
        } else if (status == Status.DISCONNECTED) {
            return "disconnected";
        } else {
            return "connecting...";
        }
    }
}

