package com.michaelstark.bustersdemise;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.michaelstark.btmessage.BluetoothMessage;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by mstark on 8/10/13.
 */
public class BluetoothSyncHelper
{
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket receiverSocket;
    public static final UUID btSyncUniqueID = UUID.fromString("c73eb5eb-3012-497a-a8da-1046cda99532");
    private BluetoothTransferThread bluetoothTransferThread;
    private Context context;
    private SensorDataManager dataManager;

    private Messenger messenger;

    private BluetoothSocket socket;
    private InputStream dataIn;

    private BluetoothSyncHelper(DataManagerService context, Messenger messenger) throws Exception
    {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        receiverSocket =
                bluetoothAdapter.listenUsingRfcommWithServiceRecord("BustersDemiseBluetoothSync",
                        btSyncUniqueID);
        bluetoothTransferThread = new BluetoothTransferThread(receiverSocket, context);

        dataManager = new SensorDataManager(context);

        this.messenger = messenger;
    }

    public void startServer()
    {
        bluetoothTransferThread.start();
    }

    protected class BluetoothTransferThread extends HandlerThread
    {
        private BluetoothServerSocket serverSocket;
        private boolean running;
        private SensorDataManager dataManager;
        private Context context;
        private HashMap<String, SensorRun> sensorRuns = new HashMap<String, SensorRun>();
        public BluetoothTransferThread(BluetoothServerSocket serverSocket,
                                       Context context) {
            super("BDBluetoothListener", HandlerThread.MIN_PRIORITY);
            this.serverSocket = serverSocket;
            this.dataManager = new SensorDataManager(context);
            running = true;
            this.context = context;
        }

        @Override
        public void run()
        {
            while(running)
            {
                try
                {
                    if(socket == null)
                    {
                        socket = serverSocket.accept();
                        dataIn = socket.getInputStream();
                    }

                    BluetoothMessage btMessage = null;
                    try
                    {
                        btMessage = BluetoothMessage.deserialize(dataIn);
                    }
                    catch (Exception e)
                    {
                        Log.d("BluetoothSyncHelper", "Could not parse the BT message: ", e);
                    }
                    if(btMessage != null)
                    {
                        if(btMessage.getData().containsKey("commandName"))
                        {
                            String commandName = btMessage.getString("commandName");
                            if(commandName.equals("startRecording"))
                            {
                                String dataSetName = btMessage.getString("dataSetName");
                                boolean accelerometerRecord = btMessage.getBoolean("accelerometerOn");
                                boolean gyroscopeRecord = btMessage.getBoolean("gyroscopeOn");
                                int recordingFrequency = btMessage.getInt("recordingFrequency");

                                File filesDir = context.getFilesDir();
                                File output = new File(filesDir, dataSetName + ".json");

                                Message message = new Message();
                                message.what = DataManagerService.MSG_START_RECORDING;

                                Bundle messageData = new Bundle();
                                messageData.putString("dataSetName", dataSetName);
                                messageData.putString("dataFileName", output.getAbsolutePath());
                                messageData.putBoolean("accelerometerRecorded", accelerometerRecord);
                                messageData.putBoolean("gyroscopeRecorded", gyroscopeRecord);
                                messageData.putInt("recordingFrequency", recordingFrequency);

                                message.setData(messageData);

                                try
                                {
                                    messenger.send(message);
                                }
                                catch(Exception e)
                                {
                                    Log.e("BustersDemise", "Could not start the recorder because of: ", e);
                                }
                            }
                            else if(commandName.equals("stopRecording"))
                            {
                                Message message = new Message();
                                message.what = DataManagerService.MSG_STOP_RECORDING;

                                try
                                {
                                    messenger.send(message);
                                }
                                catch(Exception e)
                                {
                                    Log.e("BustersDemise", "Could not stop the recorder because of: ", e);
                                }
                            }
                            else if(commandName.equals("closeConnection"))
                            {
                                try
                                {
                                    socket.close();
                                }
                                catch (Exception e)
                                {

                                }
                                finally
                                {
                                    socket = null;
                                }
                            }
                            else if(commandName.equals("getRecordingStatus"))
                            {
                                try
                                {
                                    btMessage = new BluetoothMessage();
                                    btMessage.put("isRecording", ((DataManagerService)context).isRecording());
                                    btMessage.serialize(socket.getOutputStream());
                                }
                                catch (Exception e)
                                {

                                }
                            }
                            else if(commandName.equals("createRun"))
                            {
                                File filesDir = context.getFilesDir();
                                File output = new File(filesDir, btMessage.getData().get("dataSetName") + ".json");
                                SensorRun sensorRun = new SensorRun();
                                sensorRun.setDataSetName(btMessage.getString("dataSetName"));
                                sensorRun.setDataFileName(output.getAbsolutePath());
                                sensorRun.setStartTime(new Date(btMessage.getLong("startTime")));
                                sensorRun.setEndTime(new Date(btMessage.getLong("endTime")));
                                sensorRun.setAccelerometerRecorded(btMessage.getBoolean("accelerometerRecorded"));
                                sensorRun.setGyroscopeRecorded(btMessage.getBoolean("gyroscopeRecorded"));
                                sensorRun.setEventCount(btMessage.getInt("eventCount"));
                                sensorRuns.put(sensorRun.getDataSetName(), sensorRun);
                            }
                            else if(commandName.equals("addRecords"))
                            {
                                String dataSetName = btMessage.getString("dataSetName");
                                int recordCount = btMessage.getInt("recordCount");
                                for(int i = 1; i <= recordCount; i++)
                                {
                                    SensorRecord record = new SensorRecord(new Date(btMessage.getLong("timestamp_"+ i)));
                                }
                            }
                            else if(commandName.equals("persistRun"))
                            {
                                SensorRun sensorRun = sensorRuns.get(btMessage.getData().get("dataSetName"));
                                dataManager.addSet(sensorRun);
                            }
                        }
                    }
                    /*else
                    {
                        List<SensorRun> sensorRuns =
                                ExportUtil.deserializeRecord(dataIn, buf, context);

                        for(SensorRun sensorRun : sensorRuns)
                        {
                            try
                            {
                                File output = new File(filesDir, sensorRun.getDataSetName() + ".json");
                                sensorRun.setDataFileName(output.getAbsolutePath());
                                dataManager.addSet(sensorRun);
                            }
                            catch (Exception e)
                            {
                                Log.d("BluetoothSyncHelper", "Could not save the data because of :", e);
                            }
                        }
                    }*/
                }
                catch (Exception e)
                {
                    if(!socket.isConnected())
                    {
                        socket = null;
                    }
                    Log.d("BluetoothSyncHelper", "Could not sync the record because of :", e);
                }
            }
        }

        @Override
        public boolean quit()
        {
            running = false;
            return super.quit();
        }
    }

    public void stop()
    {
        try
        {
            receiverSocket.close();
        }
        catch (Exception e)
        {

        }
        bluetoothTransferThread.quit();
    }

    public static BluetoothSyncHelper create(DataManagerService context, Messenger messenger)
    {
        try
        {
            return new BluetoothSyncHelper(context, messenger);
        }
        catch (Exception e)
        {
            Log.d("BluetoothSyncHelper", "Could not start get access to Bluetooth: ", e);
            return null;
        }
    }

    private void copyArray(byte[] dest, byte[] source)
    {
        System.arraycopy(source, 0, dest, 0, source.length);
    }

    public boolean syncRuns(final BluetoothDevice syncDevice) throws Exception
    {
        AsyncTask<Object, Object, Object> syncTask = new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... objects) {
                BluetoothSocket syncSocket = null;
                boolean result = true;
                try
                {
                    syncSocket =
                        syncDevice.createRfcommSocketToServiceRecord(btSyncUniqueID);
                    syncSocket.connect();
                    OutputStream dataOut = syncSocket.getOutputStream();
                    List<SensorRun> sensorRuns = dataManager.getAllSets();
                    String syncString = String.format("%010X", sensorRuns.size());
                    byte[] transferBuf = new byte[512];
                    copyArray(transferBuf, syncString.getBytes("utf-8"));
                    dataOut.write(transferBuf, 0, 512);
                    for(SensorRun sensorRun : sensorRuns)
                    {
                        long startTime = sensorRun.getStartTime().getTime();
                        long endTime = sensorRun.getEndTime().getTime();
                        syncString = String.format("%010X%s%020X%020X%010X%s%s",
                                sensorRun.getDataSetName().length(), sensorRun.getDataSetName(),
                                startTime, endTime, sensorRun.getEventCount(),
                                sensorRun.wasAccelerometerRecorded() ? "1" : "0",
                                sensorRun.wasGyroscopeRecorded() ? "1" : "0");
                        copyArray(transferBuf, syncString.getBytes("utf-8"));
                        dataOut.write(transferBuf);
                        for(int i = 0; i < sensorRun.getEventCount(); i++)
                        {
                            SensorRecord record = sensorRun.getSensorRecords().get(i);
                            long timestamp = record.getTimestamp().getTime();
                            String accelerometerX = Float.toString(record.getAccelerationX());
                            String accelerometerY = Float.toString(record.getAccelerationY());
                            String accelerometerZ = Float.toString(record.getAccelerationZ());
                            String gyroscopeX = Float.toString(record.getGyroscopeX());
                            String gyroscopeY = Float.toString(record.getGyroscopeY());
                            String gyroscopeZ = Float.toString(record.getGyroscopeZ());
                            syncString = String.format("%020X%010X%s%010X%s%010X%s%010X%s%010X%s%010X%s",
                                    timestamp, accelerometerX.length(), accelerometerX,
                                    accelerometerY.length(), accelerometerY, accelerometerZ.length(),
                                    accelerometerZ, gyroscopeX.length(), gyroscopeX,
                                    gyroscopeY.length(), gyroscopeY, gyroscopeZ.length(),
                                    gyroscopeZ);

                            copyArray(transferBuf, syncString.getBytes("utf-8"));
                            dataOut.write(transferBuf, 0, 512);
                        }
                    }
                }
                catch (Exception e)
                {
                    Log.d("BluetoothSyncHelper", "Could not sync the record because of :", e);
                    result = false;
                }
                finally
                {
                    try
                    {
                        if(syncSocket != null)
                        {
                            syncSocket.close();
                        }
                    }
                    catch (Exception e)
                    {

                    }
                }
                return result;
            }
            @Override
            protected void onPostExecute(Object result)
            {
                Toast.makeText(context, "Bluetooth sync completed.", Toast.LENGTH_SHORT).show();
                cancel(true);
            }
        };
        syncTask.execute();
        return (boolean)((Boolean)syncTask.get());
    }
}
