package com.michaelstark.bustersdemise;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by mstark on 8/7/13.
 */
public class SensorRunViewer
{
    private View runView;

    private ToggleButton editButton;
    private Button clearButton;
    private Button exportButton;

    private ListView runViewer;

    private SensorDataManager sensorData;

    private boolean isEditMode;

    private Context context;

    private List<SensorRun> selectedRuns;
    private List<String> tempFileNames;

    public SensorRunViewer(final Context context)
    {
        this.context = context;

        selectedRuns = new LinkedList<SensorRun>();
        tempFileNames = new LinkedList<String>();

        sensorData = new SensorDataManager(context);

        runView = LayoutInflater.from(context).inflate(R.layout.sensor_runs, null);

        editButton = (ToggleButton) runView.findViewById(R.id.editButton);
        clearButton = (Button) runView.findViewById(R.id.clearButton);
        exportButton = (Button) runView.findViewById(R.id.exportButton);

        runViewer = (ListView) runView.findViewById(R.id.runViewer);

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isEditMode)
                {
                    isEditMode = true;
                }
                else
                {
                    isEditMode = false;
                    selectedRuns.clear();
                }
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayAdapter adapter = ((ArrayAdapter)runViewer.getAdapter());
                if(isEditMode)
                {
                    for(SensorRun sensorRun : selectedRuns)
                    {
                        sensorData.deleteRun(sensorRun);
                        adapter.remove(sensorRun);
                    }
                    adapter.notifyDataSetChanged();
                    selectedRuns.clear();
                }
                else
                {
                    sensorData.clearStorage();
                    adapter.clear();
                    adapter.notifyDataSetChanged();
                }
            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<SensorRun> selectedRuns = null;
                if(isEditMode)
                {
                    selectedRuns = SensorRunViewer.this.selectedRuns;
                }
                else
                {
                    selectedRuns = ((SensorRunAdapter)runViewer.getAdapter()).getSensorData();
                }
                ExportUtil.exportEmail(context, selectedRuns, tempFileNames);
            }
        });

        runViewer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(isEditMode)
                {
                    view.setBackgroundColor(Color.rgb(0xFF, 0x10, 0x10));
                    selectedRuns.add((SensorRun)adapterView.getAdapter().getItem(i));
                }
                else
                {
                    createInfoDialog((SensorRun)adapterView.getAdapter().getItem(i));
                }
            }
        });

    }

    public View getContentView()
    {
        return runView;
    }

    public void clearTemporaryFiles()
    {
        for(String fileName : tempFileNames)
        {
            new File(fileName).delete();
        }
    }

    public void refreshData()
    {
        AsyncTask<Object, Object, Object> refreshTask = new AsyncTask<Object, Object, Object>() {
            private AlertDialog loadingDialog;
            @Override
            protected void onPreExecute()
            {
                loadingDialog = LoadingDialog.create(context, "Loading...");
                loadingDialog.show();
            }
            @Override
            protected Object doInBackground(Object... objects) {
                return sensorData.getAllSets();
            }
            @Override
            protected void onPostExecute(Object result)
            {
                runViewer.setAdapter(new SensorRunAdapter(context, (List<SensorRun>)result));
                loadingDialog.dismiss();
                cancel(true);
            }
        };
        refreshTask.execute();
    }

    private void createInfoDialog(final SensorRun sensorRun)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        View contentView = LayoutInflater.from(context).inflate(R.layout.run_summary, null);
        TextView startTime = (TextView) contentView.findViewById(R.id.startTimeLabel);
        TextView endTime = (TextView) contentView.findViewById(R.id.endTimeLabel);
        TextView gyroscopeOn = (TextView) contentView.findViewById(R.id.gyroscopeOnLabel);
        TextView accelerometerOn = (TextView) contentView.findViewById(R.id.accelerometerOnLabel);
        TextView eventCount = (TextView) contentView.findViewById(R.id.eventCountLabel);
        TextView avgFrequencyLabel = (TextView) contentView.findViewById(R.id.avgFrequencyLabel);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

        double runLength = (double)(sensorRun.getEndTime().getTime()
                - sensorRun.getStartTime().getTime()) / 1000;
        double avgFrequency = sensorRun.getEventCount() / runLength;

        startTime.setText(dateFormat.format(sensorRun.getStartTime()));
        endTime.setText(dateFormat.format(sensorRun.getEndTime()));
        gyroscopeOn.setText(sensorRun.wasGyroscopeRecorded() ? "Yes" : "No");
        accelerometerOn.setText(sensorRun.wasAccelerometerRecorded() ? "Yes" : "No");
        eventCount.setText(Integer.toString(sensorRun.getEventCount()));
        avgFrequencyLabel.setText(Double.toString(avgFrequency));

        builder.setView(contentView);
        builder.setTitle(String.format("Sensor Run Summary for \"%s\"",
                sensorRun.getDataSetName()));

        builder.setPositiveButton("Export", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ExportUtil.exportEmail(context, sensorRun, tempFileNames);
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.create().show();
    }

}
