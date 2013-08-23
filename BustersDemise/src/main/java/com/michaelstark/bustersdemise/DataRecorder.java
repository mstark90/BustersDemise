package com.michaelstark.bustersdemise;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.*;
import android.os.Process;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by mstark on 7/18/13.
 */
public class DataRecorder implements SensorEventListener {

    private Sensor mAccelerometer, mGyroscope;
    private SensorManager sensorManager;

    private long accelerometerRecordIndex, gyroscopeRecordIndex;

    private int recordingFrequency;

    private HandlerThread accelerometerThread, gyroscopeThread;

    private SensorRun sensorRun;

    private boolean recordAccelerometer;
    private boolean recordGyroscope;

    private boolean isRecording;

    public DataRecorder(Context context)
    {
        sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);

        recordingFrequency = 50;
        recordGyroscope = true;
        recordAccelerometer = true;

        isRecording = false;
    }

    public void terminate()
    {
        isRecording = false;
        sensorManager.unregisterListener(this, mAccelerometer);
        sensorManager.unregisterListener(this, mGyroscope);
        if(accelerometerThread.isAlive())
        {
            accelerometerThread.quit();
        }
        if(gyroscopeThread.isAlive())
        {
            gyroscopeThread.quit();
        }
        if(sensorRun != null)
        {
            sensorRun.setEventCount(sensorRun.getSensorRecords().size());
            sensorRun.setEndTime(new Date());
            sensorRun.lock();
            sensorRun.convertTimestamp();
            Collections.sort(sensorRun.getSensorRecords(),
                    new Comparator<SensorRecord>() {
                        @Override
                        public int compare(SensorRecord first,
                                           SensorRecord second) {
                            if (first == null) {
                                return -1;
                            }
                            if (second == null) {
                                return 1;
                            }
                            return first.getTimestamp()
                                    .compareTo(second.getTimestamp());
                        }
                    });
        }
    }

    public SensorRun getSensorRun()
    {
        return sensorRun;
    }

    public boolean isRecording()
    {
        return isRecording;
    }

    public void start(Bundle bundle) throws Exception
    {
        if(isRecording())
        {
            throw new Exception("The sensors are already recording.");
        }

        accelerometerThread = new HandlerThread("AccelerometerRecorder",
                Process.THREAD_PRIORITY_LOWEST);
        gyroscopeThread = new HandlerThread("GyroscopeRecorder", Process.THREAD_PRIORITY_LOWEST);


        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorRun = new SensorRun();
        sensorRun.setDataSetName(bundle.getString("dataSetName"));
        sensorRun.setAccelerometerRecorded(bundle.getBoolean("accelerometerRecorded")
                && mAccelerometer != null);
        sensorRun.setGyroscopeRecorded(bundle.getBoolean("gyroscopeRecorded")
                && mGyroscope != null);
        sensorRun.setDataFileName(bundle.getString("dataFileName"));
        sensorRun.setStartTime(new Date());

        setRecordingFrequency(bundle.getInt("recordingFrequency"));

        try
        {
            if(mAccelerometer != null && isRecordAccelerometer())
            {
                accelerometerThread.start();
                try
                {
                    sensorManager.registerListener(this, mAccelerometer, 1000000 / recordingFrequency,
                            new Handler(accelerometerThread.getLooper()));
                }
                catch (Exception e)
                {
                    sensorManager.unregisterListener(this, mAccelerometer);
                    accelerometerThread.quit();
                }
            }
            if(mGyroscope != null && isRecordGyroscope())
            {
                gyroscopeThread.start();
                try
                {
                    sensorManager.registerListener(this, mGyroscope, 1000000 / recordingFrequency,
                            new Handler(gyroscopeThread.getLooper()));
                }
                catch (Exception e)
                {
                    sensorManager.unregisterListener(this, mGyroscope);
                    gyroscopeThread.quit();
                }
            }
            isRecording = true;
        }
        catch (Exception e)
        {
            terminate();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        try
        {
            SensorRecord record = null;
            if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            {
                if(accelerometerRecordIndex >= sensorRun.getSensorRecords().size())
                {
                    record = new SensorRecord(sensorEvent.timestamp);
                    sensorRun.addRecord(record);
                }
                else
                {
                    record = sensorRun.getSensorRecords().get((int)accelerometerRecordIndex);
                }
                record.setAccelerationX(sensorEvent.values[0]);
                record.setAccelerationY(sensorEvent.values[1]);
                record.setAccelerationZ(sensorEvent.values[2]);
                accelerometerRecordIndex++;
            }
            else if(sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE)
            {
                if(gyroscopeRecordIndex >= sensorRun.getSensorRecords().size())
                {
                    record = new SensorRecord(sensorEvent.timestamp);
                    sensorRun.addRecord(record);
                }
                else
                {
                    record = sensorRun.getSensorRecords().get((int)gyroscopeRecordIndex);
                }
                record.setGyroscopeX(sensorEvent.values[0]);
                record.setGyroscopeY(sensorEvent.values[1]);
                record.setGyroscopeZ(sensorEvent.values[2]);
                gyroscopeRecordIndex++;
            }
        }
        catch(Exception e)
        {

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public int getRecordingFrequency() {
        return recordingFrequency;
    }

    public void setRecordingFrequency(int recordingFrequency) {
        this.recordingFrequency = recordingFrequency;
    }

    public boolean isRecordAccelerometer() {
        return recordAccelerometer;
    }

    public void setRecordAccelerometer(boolean recordAccelerometer) {
        this.recordAccelerometer = recordAccelerometer;
    }

    public boolean isRecordGyroscope() {
        return recordGyroscope;
    }

    public void setRecordGyroscope(boolean recordGyroscope) {
        this.recordGyroscope = recordGyroscope;
    }
}
