package com.michaelstark.bustersdemise;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by mstark on 8/7/13.
 */
public class DataCollectionView {

    private SensorDataManager sensorDataManager;

    private int recordingFrequency = 50;

    private View contentView;

    private Messenger messenger;

    private Button startRecordingButton;
    private Button recordingFrequencyButton;
    private EditText dataSetField;
    private ToggleButton gyroscopeToggle;
    private ToggleButton accelerometerToggle;

    private boolean isRecording = false;

    private ServiceConnection serviceConnection;

    private AlertDialog loadingDialog;

    private Messenger replyDump;

    public DataCollectionView(final Context context, final Messenger replyDump)
    {
        contentView = LayoutInflater.from(context).inflate(R.layout.data_collection, null);

        sensorDataManager = new SensorDataManager(context);
        this.replyDump = replyDump;

        startRecordingButton = (Button)contentView.findViewById(R.id.recordingToggleButton);
        recordingFrequencyButton = (Button)contentView.findViewById(R.id.recordingFrequencyButton);
        dataSetField = (EditText)contentView.findViewById(R.id.dataSetNameField);
        gyroscopeToggle = (ToggleButton)contentView.findViewById(R.id.gyroscopeOnButton);
        accelerometerToggle = (ToggleButton)contentView.findViewById(R.id.accelerometerOnButton);

        startRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!isRecording)
                {
                    File filesDir = context.getFilesDir();
                    File output = new File(filesDir, dataSetField.getText().toString() + ".json");
                    Message message = new Message();
                    message.what = DataManagerService.MSG_START_RECORDING;

                    Bundle messageData = new Bundle();
                    messageData.putString("dataSetName", dataSetField.getText().toString());
                    messageData.putString("dataFileName", output.getAbsolutePath());
                    messageData.putBoolean("accelerometerRecorded", accelerometerToggle.isChecked());
                    messageData.putBoolean("gyroscopeRecorded", gyroscopeToggle.isChecked());
                    messageData.putInt("recordingFrequency", recordingFrequency);

                    message.setData(messageData);
                    message.replyTo = replyDump;

                    try
                    {
                        messenger.send(message);
                    }
                    catch(Exception e)
                    {
                        Log.e("BustersDemise", "Could not start the recorder because of: ", e);
                    }
                }
                else
                {
                    loadingDialog = LoadingDialog.create(context, "Persisting records...");
                    loadingDialog.show();
                    Message message = new Message();
                    message.what = DataManagerService.MSG_STOP_RECORDING;
                    message.replyTo = replyDump;
                    try
                    {
                        messenger.send(message);
                    }
                    catch(Exception e)
                    {
                        Log.e("BustersDemise", "Could not stop the recorder because of: ", e);
                    }
                }
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

    public boolean handleMessage(Message msg)
    {
        switch(msg.what)
        {
            case DataManagerService.MSG_GET_RECORDING_STATUS:
                isRecording = msg.getData().getBoolean("isRecording");
                if(isRecording)
                {
                    recordingFrequencyButton.setEnabled(false);
                    gyroscopeToggle.setEnabled(false);
                    accelerometerToggle.setEnabled(false);
                    startRecordingButton.setText("Stop Recording");
                }
                return true;
            case DataManagerService.MSG_START_RECORDING:
                isRecording = msg.getData().getBoolean("isRecording");
                if(isRecording)
                {
                    recordingFrequencyButton.setEnabled(false);
                    gyroscopeToggle.setEnabled(false);
                    accelerometerToggle.setEnabled(false);
                    startRecordingButton.setText("Stop Recording");
                }
                return true;
            case DataManagerService.MSG_STOP_RECORDING:
                isRecording = false;
                recordingFrequencyButton.setEnabled(true);
                gyroscopeToggle.setEnabled(true);
                accelerometerToggle.setEnabled(true);
                startRecordingButton.setText("Start Recording");
                if(loadingDialog != null)
                {
                    loadingDialog.dismiss();
                }
                return true;
            default:
                return false;
        }
    }

    public void bindMessenger(Messenger messenger)
    {
        this.messenger = messenger;
        Message message = new Message();
        message.what = DataManagerService.MSG_GET_RECORDING_STATUS;
        message.replyTo = replyDump;
        try
        {
            messenger.send(message);
        }
        catch (Exception e)
        {

        }
    }

    public void dispose()
    {

    }

    public View getContentView()
    {
        return contentView;
    }
}
