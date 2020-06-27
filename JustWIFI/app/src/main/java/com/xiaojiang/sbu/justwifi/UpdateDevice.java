package com.xiaojiang.sbu.justwifi;

public class UpdateDevice {
    private Double lat;
    private Double longit;
    private String deviceId;
    private String deviceName;

    public UpdateDevice(){}

    public UpdateDevice(Double lat, Double longit, String deviceId, String deviceName) {
        this.lat = lat;
        this.longit = longit;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }


    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLongit() {
        return longit;
    }

    public void setLongit(Double longit) {
        this.longit = longit;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

}
