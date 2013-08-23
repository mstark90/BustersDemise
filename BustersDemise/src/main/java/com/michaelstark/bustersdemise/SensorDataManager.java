package com.michaelstark.bustersdemise;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.util.JsonWriter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by mstark on 7/18/13.
 */
public class SensorDataManager extends SQLiteOpenHelper {


    public SensorDataManager(Context context) {
        super(context, "SensorDataManager", new SQLiteDatabase.CursorFactory() {
            @Override
            public Cursor newCursor(SQLiteDatabase sqLiteDatabase,
                                    SQLiteCursorDriver sqLiteCursorDriver, String s,
                                    SQLiteQuery sqLiteQuery) {
                return new SQLiteCursor(sqLiteCursorDriver, s, sqLiteQuery);
            }
        }, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE sensor_run (sensor_run_id integer primary key" +
                ", data_set_name text not null, startTime integer not null, endTime integer not null" +
                ", was_gyroscope_recorded integer not null, was_accelerometer_recorded integer not null" +
                ", data_file_name text not null, event_count integer not null)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        sqLiteDatabase.execSQL("drop table if exists sensor_record");
        sqLiteDatabase.execSQL("drop table if exists sensor_run");
        onCreate(sqLiteDatabase);
    }

    public List<SensorRun> getAllSets() {
        LinkedList<SensorRun> sensorRuns = new LinkedList<SensorRun>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from sensor_run", null);
        while(cursor.moveToNext())
        {
            SensorRun sensorRun = new SensorRun(false);
            sensorRun.setSensorRunId(cursor.getInt(0));
            sensorRun.setDataSetName(cursor.getString(1));
            sensorRun.setStartTime(new Date(cursor.getLong(2)));
            sensorRun.setEndTime(new Date(cursor.getLong(3)));
            sensorRun.setGyroscopeRecorded(cursor.getInt(4) == 1);
            sensorRun.setAccelerometerRecorded(cursor.getInt(5) == 1);
            sensorRun.setDataFileName(cursor.getString(6));
            sensorRun.setEventCount(cursor.getInt(7));
            sensorRuns.add(sensorRun);
        }
        cursor.close();
        db.close();
        return sensorRuns;
    }

    public void deleteRun(SensorRun sensorRun)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from sensor_run where sensor_run_id = ?",
                new Object[]{ sensorRun.getSensorRunId() });
        new File(sensorRun.getDataFileName()).delete();
        db.close();
    }

    public void addSet(SensorRun sensorRun) throws Exception
    {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("select sensor_run_id from sensor_run where data_set_name = ?"
        , new String[]{sensorRun.getDataSetName()});
        if(cursor.moveToFirst())
        {
            cursor.close();
            throw new Exception("Could not insert the dataset because one with the same name already exists.");
        }
        cursor.close();
        ContentValues contentValues = new ContentValues();
        contentValues.put("data_set_name", sensorRun.getDataSetName());
        contentValues.put("startTime", sensorRun.getStartTime().getTime());
        contentValues.put("endTime", sensorRun.getEndTime().getTime());
        contentValues.put("was_gyroscope_recorded", sensorRun.wasGyroscopeRecorded());
        contentValues.put("was_accelerometer_recorded", sensorRun.wasAccelerometerRecorded());
        contentValues.put("data_file_name", sensorRun.getDataFileName());
        contentValues.put("event_count", sensorRun.getSensorRecords().size());
        db.insert("sensor_run", null, contentValues);
        FileWriter writer = new FileWriter(new File(sensorRun.getDataFileName()));
        try
        {
            writer.write("[");
            List<SensorRecord> records = sensorRun.getSensorRecords();
            for(int i = 0; i < records.size(); i++)
            {
                SensorRecord record = records.get(i);
                if(record != null)
                {
                    writer.write(String.format("{\"timestamp\": %d, \"accelerometer_x\": %f," +
                            " \"accelerometer_y\": %f, \"accelerometer_z\": %f, \"gyroscope_x\": %f" +
                            ", \"gyroscope_y\": %f, \"gyroscope_z\": %f}",
                            record.getTimestamp().getTime(), record.getAccelerationX(),
                            record.getAccelerationY(), record.getAccelerationZ(), record.getGyroscopeX(),
                            record.getGyroscopeY(), record.getGyroscopeZ()));
                    if(i < records.size() - 1)
                    {
                        writer.write(',');
                    }
                }
            }
            writer.write("]");
            writer.flush();
            writer.close();
        }
        catch(Exception e)
        {
            writer.close();
            new File(sensorRun.getDataFileName()).delete();
            db.execSQL("delete from sensor_run where data_set_name = ?",
                    new Object[]{ sensorRun.getDataSetName() });
            throw e;
        }
        db.close();
    }

    public void clearStorage()
    {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT data_file_name FROM sensor_run", null);
        while(cursor.moveToNext())
        {
            new File(cursor.getString(0)).delete();
        }
        db.execSQL("DELETE FROM sensor_run");
        db.close();
    }

}
