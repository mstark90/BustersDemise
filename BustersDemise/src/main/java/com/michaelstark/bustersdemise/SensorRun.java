package com.michaelstark.bustersdemise;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by mstark on 7/18/13.
 */
public class SensorRun {
    private String dataSetName;
    private Date startTime;
    private Date endTime;
    private boolean wasAccelerometerRecorded;
    private boolean wasGyroscopeRecorded;
    private int sensorRunId;
    private int eventCount;
    private String dataFileName;
    private List<SensorRecord> sensorRecords;
    private boolean isInserting;
    private boolean canInsert = true;

    public SensorRun()
    {
        sensorRecords = new LinkedList<SensorRecord>();
        isInserting = true;
    }
    public SensorRun(boolean isInserting)
    {
        if(isInserting)
        {
            sensorRecords = new LinkedList<SensorRecord>();
        }
        this.isInserting = isInserting;
    }

    public void convertTimestamp()
    {
        for(SensorRecord record : sensorRecords)
        {
            record.convertTimestamp();
        }
    }

    public void lock()
    {
        canInsert = false;
    }

    public void addRecord(SensorRecord sensorRecord)
    {
        if(canInsert)
        {
            getSensorRecords().add(sensorRecord);
        }
    }

    public String getDataSetName()
    {
        return dataSetName;
    }

    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public boolean wasAccelerometerRecorded() {
        return wasAccelerometerRecorded;
    }

    public void setAccelerometerRecorded(boolean value) {
        this.wasAccelerometerRecorded = value;
    }

    public boolean wasGyroscopeRecorded() {
        return wasGyroscopeRecorded;
    }

    public void setGyroscopeRecorded(boolean value) {
        this.wasGyroscopeRecorded = value;
    }

    public List<SensorRecord> getSensorRecords() {
        if(!isInserting)
        {
            try
            {
                if(sensorRecords == null)
                {
                    sensorRecords = new LinkedList<SensorRecord>();
                    BufferedReader reader = new BufferedReader(new FileReader(getDataFileName()));
                    String json = "";
                    String line = "";
                    while((line = reader.readLine()) != null)
                    {
                        json = json + line +"\n";
                    }
                    JSONArray objects = new JSONArray(json);
                    for(int i = 0; i < objects.length(); i++)
                    {
                        JSONObject object = objects.getJSONObject(i);
                        SensorRecord record = new SensorRecord(new Date(object.getLong("timestamp")));
                        record.setGyroscopeX((float)object.getDouble("gyroscope_x"));
                        record.setGyroscopeY((float)object.getDouble("gyroscope_y"));
                        record.setGyroscopeZ((float)object.getDouble("gyroscope_z"));
                        record.setAccelerationX((float)object.getDouble("accelerometer_x"));
                        record.setAccelerationY((float)object.getDouble("accelerometer_y"));
                        record.setAccelerationZ((float)object.getDouble("accelerometer_z"));
                        sensorRecords.add(record);
                    }
                    reader.close();
                }
            }
            catch (Exception e)
            {

            }
        }
        return sensorRecords;
    }

    @Override
    public String toString()
    {
        return getDataSetName();
    }

    public int getSensorRunId() {
        return sensorRunId;
    }

    public void setSensorRunId(int sensorRunId) {
        this.sensorRunId = sensorRunId;
    }

    public String getDataFileName() {
        return dataFileName;
    }

    public void setDataFileName(String dataFileName) {
        this.dataFileName = dataFileName;
    }

    public int getEventCount() {
        return eventCount;
    }

    public void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }
}
