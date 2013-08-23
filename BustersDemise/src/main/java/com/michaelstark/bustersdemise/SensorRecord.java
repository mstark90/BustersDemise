package com.michaelstark.bustersdemise;

import android.os.SystemClock;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by mstark on 7/9/13.
 */
public class SensorRecord implements Serializable {

    private Date timestamp;
    private long trueTimestamp;
    private float accelerationX;
    private float accelerationY;
    private float accelerationZ;
    private float gyroscopeX;
    private float gyroscopeY;
    private float gyroscopeZ;
    private float battery_percentage;

    private boolean accelerationXset, accelerationYset, accelerationZset;
    private boolean gyroscopeXset, gyroscopeYset, gyroscopeZset;
    private boolean battery_percentage_set;

    public SensorRecord(long timestamp)
    {
        this.trueTimestamp = timestamp;
    }

    public SensorRecord(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public void convertTimestamp()
    {
        timestamp = new Date();
        long bootDate = timestamp.getTime() - SystemClock.uptimeMillis();
        timestamp.setTime((trueTimestamp / 1000000) + bootDate);
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public float getAccelerationX() {
        return accelerationX;
    }

    public void setAccelerationX(float accelerationX) {
        this.accelerationX = accelerationX;
    }

    public float getAccelerationY() {
        return accelerationY;
    }

    public void setAccelerationY(float accelerationY) {
        this.accelerationY = accelerationY;
    }

    public float getAccelerationZ() {
        return accelerationZ;
    }

    public void setAccelerationZ(float accelerationZ) {
        this.accelerationZ = accelerationZ;
    }

    public float getGyroscopeX() {
        return gyroscopeX;
    }

    public void setGyroscopeX(float gyroscopeX) {
        this.gyroscopeX = gyroscopeX;
    }

    public float getGyroscopeY() {
        return gyroscopeY;
    }

    public void setGyroscopeY(float gyroscopeY) {
        this.gyroscopeY = gyroscopeY;
    }

    public float getGyroscopeZ() {
        return gyroscopeZ;
    }

    public void setGyroscopeZ(float gyroscopeZ) {
        this.gyroscopeZ = gyroscopeZ;
    }

    public String format()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(timestamp);
        return String.format("%02d/%02d/%04d %02d:%02d:%02d:%04d, %f, %f, %f, %f, %f, %f %n",
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.HOUR),
                calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND),
                calendar.get(Calendar.MILLISECOND), accelerationX, accelerationY, accelerationZ,
                gyroscopeX, gyroscopeY, gyroscopeZ);
    }

}
