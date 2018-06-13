package com.aware.app.stop;

/*
 * Class for Symptom object
 */

public class Symptom {

    private String name;
    private String rate0;
    private String rate1;
    private String rate2;
    private String rate3;
    private String rate4;

    public Symptom(String name, String rate0, String rate1, String rate2, String rate3, String rate4) {
        this.name = name;
        this.rate0 = rate0;
        this.rate1 = rate1;
        this.rate2 = rate2;
        this.rate3 = rate3;
        this.rate4 = rate4;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRate0() {
        return rate0;
    }

    public void setRate0(String rate0) {
        this.rate0 = rate0;
    }

    public String getRate1() {
        return rate1;
    }

    public void setRate1(String rate1) {
        this.rate1 = rate1;
    }

    public String getRate2() {
        return rate2;
    }

    public void setRate2(String rate2) {
        this.rate2 = rate2;
    }

    public String getRate3() {
        return rate3;
    }

    public void setRate3(String rate3) {
        this.rate3 = rate3;
    }

    public String getRate4() {
        return rate4;
    }

    public void setRate4(String rate4) {
        this.rate4 = rate4;
    }

}
