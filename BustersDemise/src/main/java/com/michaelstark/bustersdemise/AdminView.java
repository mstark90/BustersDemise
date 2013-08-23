package com.michaelstark.bustersdemise;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ToggleButton;

import com.michaelstark.btmessage.BluetoothMessage;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by mstark on 8/11/13.
 */
public class AdminView
{
    private Context context;

    private View contentView;

    private BluetoothDevice selectedDevice;
    private BluetoothAdapter bluetoothAdapter;

    private BluetoothSocket selectedDeviceConnection;
    private InputStream selectedDeviceInput;
    private OutputStream selectedDeviceOutput;

    private AlertDialog loadingDialog;

    private EditText dataSetNameField;
    private ToggleButton accelerometerToggle;
    private ToggleButton gyroscopeToggle;
    private Button startRecordingButton;
    private Button stopRecordingButton;
    private Button recordingFrequencyButton;
    private ListView deviceList;

    private int recordingFrequency;
    private boolean isRecording;

    private Messenger messenger;

    public AdminView(final Context context, Messenger messenger)
    {
        this.messenger = messenger;

        recordingFrequency = 50;

        this.context = context;
        contentView = LayoutInflater.from(context).inflate(R.layout.admin_view, null);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        deviceList = (ListView)contentView.findViewById(R.id.deviceList);
        startRecordingButton = (Button)contentView.findViewById(R.id.startRecordingButton);
        stopRecordingButton = (Button)contentView.findViewById(R.id.stopRecordingButton);
        dataSetNameField = (EditText)contentView.findViewById(R.id.dataSetNameField);
        accelerometerToggle = (ToggleButton)contentView.findViewById(R.id.accelerometerOnButton);
        gyroscopeToggle = (ToggleButton)contentView.findViewById(R.id.gyroscopeOnButton);
        recordingFrequencyButton = (Button)contentView.findViewById(R.id.recordingFrequencyButton);

        deviceList.setAdapter(new BluetoothDeviceAdapter(context,
                bluetoothAdapter.getBondedDevices()));
        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setSelected(true);
                closeConnection();
                selectedDevice = (BluetoothDevice)deviceList.getAdapter().getItem(i);
                AsyncTask<Object, Object, Object> connectTask =
                        new AsyncTask<Object, Object, Object>() {
                            @Override
                            protected void onPreExecute()
                            {
                                loadingDialog = LoadingDialog.create(context, "Trying to connect...");
                                loadingDialog.show();
                            }
                            @Override
                            protected Object doInBackground(Object... objects) {
                                try
                                {
                                    selectedDeviceConnection =
                                            selectedDevice.createRfcommSocketToServiceRecord(
                                                    BluetoothSyncHelper.btSyncUniqueID);
                                    selectedDeviceConnection.connect();
                                    return 0;
                                }
                                catch(Exception e)
                                {
                                    return 1;
                                }
                            }
                            @Override
                            protected void onPostExecute(Object result)
                            {
                                cancel(true);
                                loadingDialog.dismiss();
                                if((Integer)result == 1)
                                {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Error");
                                    builder.setMessage("Could not connect to the specified device.");
                                    builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    });
                                    builder.create().show();
                                }
                                else if((Integer)result == 0)
                                {
                                    updateStatus();
                                }
                            }
                        };
                connectTask.execute();
            }
        });

        startRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AsyncTask<Object, Object, Object> sendCommandTask =
                        new AsyncTask<Object, Object, Object>() {
                    @Override
                    protected Object doInBackground(Object... objects) {
                        if(selectedDevice != null)
                        {
                            try
                            {
                                String dataSetName = dataSetNameField.getText().toString();

                                BluetoothMessage message = new BluetoothMessage();
                                message.getData().put("commandName", "startRecording");
                                message.getData().put("dataSetName", dataSetName);
                                message.getData().put("accelerometerOn", Boolean.toString(accelerometerToggle.isChecked()));
                                message.getData().put("gyroscopeOn", Boolean.toString(gyroscopeToggle.isChecked()));
                                message.getData().put("recordingFrequency", Integer.toString(recordingFrequency));
                                message.serialize(selectedDeviceOutput);
                                return 0;
                            }
                            catch(Exception e)
                            {
                                return 1;
                            }
                        }
                        else
                        {
                            return 2;
                        }
                    }
                    @Override
                    protected void onPostExecute(Object result)
                    {
                        cancel(true);
                        if((Integer)result > 0)
                        {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle("Error").setMessage("There was an issue starting the recording.");
                            builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
                            builder.create().show();
                        }
                        else
                        {
                            recordingFrequencyButton.setEnabled(false);
                            startRecordingButton.setEnabled(false);
                            accelerometerToggle.setEnabled(false);
                            gyroscopeToggle.setEnabled(false);
                            stopRecordingButton.setEnabled(true);
                        }
                    }
                };
                sendCommandTask.execute();
            }
        });
        stopRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AsyncTask<Object, Object, Object> sendCommandTask =
                        new AsyncTask<Object, Object, Object>() {
                            @Override
                            protected Object doInBackground(Object... objects) {
                                if(selectedDevice != null)
                                {
                                    try
                                    {
                                        BluetoothMessage message = new BluetoothMessage();
                                        message.getData().put("commandName", "stopRecording");
                                        message.serialize(selectedDeviceOutput);
                                        return 0;
                                    }
                                    catch(Exception e)
                                    {
                                        return 1;
                                    }
                                }
                                else
                                {
                                    return 2;
                                }
                            }
                            @Override
                            protected void onPostExecute(Object result)
                            {
                                cancel(true);
                                recordingFrequencyButton.setEnabled(true);
                                startRecordingButton.setEnabled(true);
                                accelerometerToggle.setEnabled(true);
                                gyroscopeToggle.setEnabled(true);
                                stopRecordingButton.setEnabled(false);
                                if((Integer)result > 0)
                                {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Error").setMessage("There was an issue stopping the recording.");
                                    builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    });
                                    builder.create().show();
                                }
                            }
                        };
                sendCommandTask.execute();
            }
        });

        recordingFrequencyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final FrequencyDataAdapter frequencyDataAdapter = new FrequencyDataAdapter(context);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setAdapter(frequencyDataAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        recordingFrequency = frequencyDataAdapter.getItem(i);
                    }
                });
                builder.setTitle("Select Recording Frequency");
                builder.create().show();
            }
        });
    }

    private void closeConnection()
    {
        AsyncTask<Object, Object, Object> sendCommandTask =
                new AsyncTask<Object, Object, Object>() {
                    private BluetoothSocket selectedDeviceConnection;
                    private OutputStream selectedDeviceOutput;
                    @Override
                    protected void onPreExecute()
                    {
                        selectedDeviceConnection = AdminView.this.selectedDeviceConnection;
                        selectedDeviceOutput = AdminView.this.selectedDeviceOutput;
                    }
                    @Override
                    protected Object doInBackground(Object... objects) {
                        if(selectedDeviceConnection != null)
                        {
                            try
                            {
                                BluetoothMessage message = new BluetoothMessage();
                                message.getData().put("commandName", "closeConnection");
                                message.serialize(selectedDeviceOutput);
                                return 0;
                            }
                            catch(Exception e)
                            {
                                return 1;
                            }
                        }
                        else
                        {
                            return 2;
                        }
                    }
                    @Override
                    protected void onPostExecute(Object result)
                    {
                        cancel(true);
                        recordingFrequencyButton.setEnabled(true);
                        startRecordingButton.setEnabled(true);
                        accelerometerToggle.setEnabled(true);
                        gyroscopeToggle.setEnabled(true);
                        stopRecordingButton.setEnabled(false);
                        if(selectedDeviceConnection != null)
                        {
                            try
                            {
                                selectedDeviceConnection.close();
                            }
                            catch (Exception e)
                            {

                            }
                            finally
                            {
                                selectedDeviceConnection = null;
                            }
                        }
                    }
                };
        sendCommandTask.execute();
    }

    private void updateStatus()
    {
        AsyncTask<Object, Object, Object> sendCommandTask =
                new AsyncTask<Object, Object, Object>() {
                    @Override
                    protected Object doInBackground(Object... objects) {
                        if(selectedDevice != null)
                        {
                            try
                            {
                                selectedDeviceOutput = selectedDeviceConnection.getOutputStream();
                                selectedDeviceInput = selectedDeviceConnection.getInputStream();
                                BluetoothMessage message = new BluetoothMessage();
                                message.getData().put("commandName", "getRecordingStatus");
                                message.serialize(selectedDeviceOutput);
                                message = BluetoothMessage.deserialize(selectedDeviceInput);
                                if(message.getData().get("isRecording").equals("true"))
                                {
                                    isRecording = true;
                                    recordingFrequencyButton.setEnabled(false);
                                    startRecordingButton.setEnabled(false);
                                    accelerometerToggle.setEnabled(false);
                                    gyroscopeToggle.setEnabled(false);
                                }
                                else
                                {
                                    recordingFrequencyButton.setEnabled(true);
                                    startRecordingButton.setEnabled(true);
                                    accelerometerToggle.setEnabled(true);
                                    gyroscopeToggle.setEnabled(true);
                                    stopRecordingButton.setEnabled(false);
                                }
                                return 0;
                            }
                            catch(Exception e)
                            {
                                return 1;
                            }
                        }
                        else
                        {
                            return 2;
                        }
                    }
                    @Override
                    protected void onPostExecute(Object result)
                    {
                        if((Integer)result == 1)
                        {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle("Error");
                            builder.setMessage("Could not connect to the specified device.");
                            builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
                            builder.create().show();
                        }
                        cancel(true);
                    }
                };
        sendCommandTask.execute();
    }

    public View getContentView()
    {
        return contentView;
    }
    public void destroy()
    {
        closeConnection();
    }
}
