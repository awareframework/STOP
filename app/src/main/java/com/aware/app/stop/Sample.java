package com.aware.app.stop;

/*
 * Class for Sample object
 */

public class Sample {

    private long timestamp;
    private String device_id;
    private double double_values_0;
    private double double_values_1;
    private double double_values_2;
    private int accuracy;
    private String label;

    public Sample() {

    }

    public Sample(long timestamp, String device_id, double double_values_0, double double_values_1, double double_values_2, int accuracy, String label) {
        this.timestamp = timestamp;
        this.device_id = device_id;
        this.double_values_0 = double_values_0;
        this.double_values_1 = double_values_1;
        this.double_values_2 = double_values_2;
        this.accuracy = accuracy;
        this.label = label;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public double getDouble_values_0() {
        return double_values_0;
    }

    public void setDouble_values_0(double double_values_0) {
        this.double_values_0 = double_values_0;
    }

    public double getDouble_values_1() {
        return double_values_1;
    }

    public void setDouble_values_1(double double_values_1) {
        this.double_values_1 = double_values_1;
    }

    public double getDouble_values_2() {
        return double_values_2;
    }

    public void setDouble_values_2(double double_values_2) {
        this.double_values_2 = double_values_2;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
