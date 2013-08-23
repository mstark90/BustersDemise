package com.michaelstark.bustersdemise;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;

/**
 * Created by mstark on 8/10/13.
 */
public class ExportUtil
{
    public static void exportEmail(final Context context, final List<SensorRun> selectedRuns,
                                   final List<String> tempFileNames)
    {
        AsyncTask<Object, String, Object> exportTask = new AsyncTask<Object, String, Object>() {
            private Intent emailIntent;
            private AlertDialog loadingDialog;
            @Override
            protected void onPreExecute()
            {
                emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("text/plain");
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Buster's Demise Output Data");
                loadingDialog = LoadingDialog.create(context, "Generating Report...");
                loadingDialog.show();
            }
            @Override
            protected Object doInBackground(Object... objects)
            {
                File outputDir = new File(Environment.getExternalStorageDirectory(),
                        "BustersDemise");
                outputDir.mkdir();
                for(SensorRun sensorRun : selectedRuns)
                {
                    File tempFile = new File(outputDir, String.format("%s.csv",
                            sensorRun.getDataSetName()));
                    try
                    {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
                        generateExport(sensorRun, writer);
                        writer.flush();
                        writer.close();
                    }
                    catch (Exception e)
                    {
                        Log.d("SensorRunViewer", "Could not generate the report: ", e);
                    }
                    publishProgress(tempFile.getAbsolutePath());
                }
                selectedRuns.clear();
                return null;
            }
            @Override
            protected void onProgressUpdate(String... progress)
            {
                File tempFile = new File(progress[0]);
                if(tempFileNames != null)
                {
                    tempFileNames.add(tempFile.getAbsolutePath());
                }
                emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile));
            }
            @Override
            protected void onPostExecute(Object result)
            {
                try
                {
                    loadingDialog.dismiss();
                    ((Activity)context).startActivityForResult
                            (Intent.createChooser(emailIntent, "Send mail..."), 0xFF9903);
                    Toast.makeText(context, "Sending e-mail...", Toast.LENGTH_SHORT)
                            .show();
                }
                catch(ActivityNotFoundException e)
                {
                    Toast.makeText(context, "No e-mail clients are installed", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        };
        exportTask.execute();
    }
    public static void exportEmail(final Context context, final SensorRun selectedRun,
                                   final List<String> tempFileNames)
    {
        AsyncTask<Object, String, Object> exportTask = new AsyncTask<Object, String, Object>() {
            private Intent emailIntent;
            private AlertDialog loadingDialog;
            @Override
            protected void onPreExecute()
            {
                emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("text/plain");
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Buster's Demise Output Data");
                loadingDialog = LoadingDialog.create(context, "Generating Report...");
                loadingDialog.show();
            }
            @Override
            protected Object doInBackground(Object... objects)
            {
                File outputDir = new File(Environment.getExternalStorageDirectory(),
                        "BustersDemise");
                outputDir.mkdir();
                File tempFile = new File(outputDir, String.format("%s.csv",
                        selectedRun.getDataSetName()));
                try
                {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
                    generateExport(selectedRun, writer);
                    writer.flush();
                    writer.close();
                }
                catch (Exception e)
                {
                    Log.d("SensorRunViewer", "Could not generate the report: ", e);
                }
                publishProgress(tempFile.getAbsolutePath());
                return null;
            }
            @Override
            protected void onProgressUpdate(String... progress)
            {
                File tempFile = new File(progress[0]);
                if(tempFileNames != null)
                {
                    tempFileNames.add(tempFile.getAbsolutePath());
                }
                emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile));
            }
            @Override
            protected void onPostExecute(Object result)
            {
                try
                {
                    loadingDialog.dismiss();
                    ((Activity)context).startActivityForResult
                            (Intent.createChooser(emailIntent, "Send mail..."), 0xFF9903);
                    Toast.makeText(context, "Sending e-mail...", Toast.LENGTH_SHORT)
                            .show();
                }
                catch(ActivityNotFoundException e)
                {
                    Toast.makeText(context, "No e-mail clients are installed", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        };
        exportTask.execute();
    }
    private static void generateExport(SensorRun sensorRun, BufferedWriter writer) throws IOException
    {
        writer.write("timestamp, accelerometer_x, accelerometer_y, accelerometer_z," +
                " gyroscope_x, gyroscope_y, gyroscope_z\n");
        for(SensorRecord record : sensorRun.getSensorRecords())
        {
            writer.write(record.format());
        }
    }
    public static List<SensorRun> deserializeRecord(InputStream dataIn,
                                                    byte[] buf,
                                                    final Context context) throws IOException
    {
        int readCount = 0;
        int recordCount = Integer.parseInt(new String(buf, 0, 10), 16);
        List<SensorRun> sensorRuns = new LinkedList<SensorRun>();
        android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Started loading the datasets.", Toast.LENGTH_SHORT).show();
            }
        });
        for(int i = 0; i < recordCount; i++)
        {
            readCount = dataIn.read(buf, 0, 512);

            int offset = 0;
            int toRead = Integer.parseInt(new String(buf, offset, 10), 16);

            offset += 10;

            String dataSetName = new String(buf, offset, toRead);

            offset += toRead;

            Date startDate = new Date(Long.parseLong(new String(buf, offset, 20), 16));

            offset += 20;

            Date endDate = new Date(Long.parseLong(new String(buf, offset, 20), 16));

            offset += 20;

            int eventCount = Integer.parseInt(new String(buf, offset, 10), 16);

            offset += 10;

            boolean wasAccelerometerRecorded = buf[offset] == '1' ? true : false;

            offset++;

            boolean wasGyroscopeRecorded = buf[offset] == '1' ? true : false;

            offset++;

            final SensorRun sensorRun = new SensorRun();
            sensorRun.setDataSetName(dataSetName);
            sensorRun.setStartTime(startDate);
            sensorRun.setEndTime(endDate);
            sensorRun.setAccelerometerRecorded(wasAccelerometerRecorded);
            sensorRun.setGyroscopeRecorded(wasGyroscopeRecorded);
            sensorRun.setEventCount(eventCount);

            for(int ix = 0; ix < sensorRun.getEventCount(); ix++)
            {
                readCount = dataIn.read(buf, 0, 512);
                offset = 0;
                long timestamp = Long.parseLong(new String(buf, offset, 20), 16);

                offset += 20;

                toRead = Integer.parseInt(new String(buf, offset, 10), 16);

                offset += 10;

                float accelerometerX = Float.parseFloat(new String(buf, offset, toRead));

                offset += toRead;

                toRead = Integer.parseInt(new String(buf, offset, 10));

                offset += 10;

                float accelerometerY = Float.parseFloat(new String(buf, offset, toRead));

                offset += toRead;

                toRead = Integer.parseInt(new String(buf, offset, 10), 16);

                offset += 10;

                float accelerometerZ = Float.parseFloat(new String(buf, offset, toRead));

                offset += toRead;

                toRead = Integer.parseInt(new String(buf, offset, 10), 16);

                offset += 10;

                float gyroscopeX = Float.parseFloat(new String(buf, offset, toRead));

                offset += toRead;

                toRead = Integer.parseInt(new String(buf, offset, 10), 16);

                offset += 10;

                float gyroscopeY = Float.parseFloat(new String(buf, offset, toRead));

                offset += toRead;

                toRead = Integer.parseInt(new String(buf, offset, 10), 16);

                offset += 10;

                float gyroscopeZ = Float.parseFloat(new String(buf, offset, toRead));

                SensorRecord record = new SensorRecord(timestamp);
                record.setAccelerationX(accelerometerX);
                record.setAccelerationY(accelerometerY);
                record.setAccelerationZ(accelerometerZ);
                record.setGyroscopeX(gyroscopeX);
                record.setGyroscopeY(gyroscopeY);
                record.setGyroscopeZ(gyroscopeZ);

                sensorRun.getSensorRecords().add(record);
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context,
                            "Finished loading the \""+ sensorRun.getDataSetName() +"\" dataset.",
                            Toast.LENGTH_SHORT).show();
                }
            });
            sensorRuns.add(sensorRun);
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Finished loading the datasets.", Toast.LENGTH_SHORT).show();
            }
        });
        return sensorRuns;
    }
}
