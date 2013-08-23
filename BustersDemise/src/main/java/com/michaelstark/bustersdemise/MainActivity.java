package com.michaelstark.bustersdemise;

import android.app.AlertDialog;
import android.app.LocalActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity {

    private DataCollectionView dataCollection;
    private SensorRunViewer sensorRunViewer;
    private AnalyticsView analyticsView;
    private AdminView adminView;

    private BluetoothAdapter bluetoothAdapter;

    private static Messenger messenger, replyDump;

    private ServiceConnection serviceConnection;

    private Handler messageHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what) {
                case -1:
                    messenger = null;
                    break;
                case -2:
                    messenger = (Messenger)msg.obj;
                    Message message = new Message();
                    message.what = DataManagerService.MSG_REGISTER_CLIENT;
                    message.replyTo = replyDump;
                    try
                    {
                        messenger.send(message);
                    }
                    catch (Exception e)
                    {

                    }
                    if(dataCollection != null)
                    {
                        dataCollection.bindMessenger(messenger);
                    }
                    break;
                default:
                    if(dataCollection != null && dataCollection.handleMessage(msg))
                    {
                       break;
                    }
                    super.handleMessage(msg);
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(savedInstanceState == null)
        {
            serviceConnection = DataManagerService.getServiceConnection(messageHandler);
            DataManagerService.getMessenger(this, serviceConnection);
            replyDump = new Messenger(messageHandler);
        }

        TabHost.TabContentFactory contentFactory = new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String s) {
                if(s.equals("DataCollection"))
                {
                    dataCollection = new DataCollectionView(MainActivity.this, replyDump);
                    if(messenger != null)
                    {
                        dataCollection.bindMessenger(messenger);
                    }
                    return dataCollection.getContentView();
                }
                else if(s.equals("SensorRuns"))
                {
                    sensorRunViewer = new SensorRunViewer(MainActivity.this);
                    return  sensorRunViewer.getContentView();
                }
                else if(s.equals("Analytics"))
                {
                    analyticsView = new AnalyticsView(MainActivity.this);
                    return analyticsView.getContentView();
                }
                else if(s.equals("Admin"))
                {
                    adminView = new AdminView(MainActivity.this, messenger);
                    return adminView.getContentView();
                }
                return null;
            }
        };

        TabHost tabHost = (TabHost)findViewById(R.id.tabHost);

        tabHost.setup();

        TabHost.TabSpec spec = tabHost.newTabSpec("DataCollection");
        spec.setContent(contentFactory);
        spec.setIndicator("Data Collection");
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("SensorRuns");
        spec.setContent(contentFactory);
        spec.setIndicator("Sensor Runs");
        tabHost.addTab(spec);

        if(isLandscape() && isTablet())
        {
            spec = tabHost.newTabSpec("Analytics");
            spec.setContent(contentFactory);
            spec.setIndicator("Analytics");
            tabHost.addTab(spec);
        }

        if(isLandscape() && isTablet())
        {
            spec = tabHost.newTabSpec("Admin");
            spec.setContent(contentFactory);
            spec.setIndicator("Remote Administration");
            tabHost.addTab(spec);
        }

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String s) {
                if(s.equals("SensorRuns"))
                {
                    sensorRunViewer.refreshData();
                }
                else if(s.equals("Analytics"))
                {
                    analyticsView.refreshData();
                }
            }
        });

    }

    private boolean isLandscape()
    {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private boolean isTablet()
    {
        return (getResources().getConfiguration().screenLayout
            & Configuration.SCREENLAYOUT_SIZE_MASK)
            >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("isRunning", "");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if(isFinishing())
        {
            Message message = new Message();
            message.what = DataManagerService.MSG_UNREGISTER_CLIENT;
            message.replyTo = replyDump;
            try
            {
                messenger.send(message);
            }
            catch (Exception e)
            {

            }
            if(adminView != null)
            {
                adminView.destroy();
                adminView = null;
            }
            if(dataCollection != null)
            {
                dataCollection.dispose();
                dataCollection = null;
            }
        }
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data){
        super.onActivityResult(reqCode, resultCode, data);
        switch(reqCode){
            case (0xFF9903):

                if (resultCode == RESULT_OK){
                    Toast.makeText(this, "Your data has been sent.", Toast.LENGTH_SHORT).show();
                }
                else if(resultCode == RESULT_CANCELED)
                {
                    Toast.makeText(this, "Your data could not be sent.", Toast.LENGTH_SHORT).show();
                }

                sensorRunViewer.clearTemporaryFiles();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.generate_reports:
                generateReports();
                return true;
            case R.id.bluetooth_sync:
                tryBtSync();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void tryBtSync()
    {
        if(bluetoothAdapter == null)
        {
            Toast.makeText(this, "Bluetooth sync is unavailable.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Bluetooth Sync");
            final BluetoothDeviceAdapter devices = new BluetoothDeviceAdapter(MainActivity.this,
                    bluetoothAdapter.getBondedDevices());
            View contentView = LayoutInflater.from(MainActivity.this)
                    .inflate(R.layout.bluetooth_devices, null);
            builder.setView(contentView);
            builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            final AlertDialog alertDialog = builder.create();

            ListView deviceList = (ListView) contentView.findViewById(R.id.deviceList);

            deviceList.setAdapter(devices);
            deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    alertDialog.dismiss();

                    Message message = new Message();
                    message.what = DataManagerService.MSG_PUSH_DATA;
                    message.obj = devices.getItem(i);
                    message.replyTo = replyDump;
                    try
                    {
                        messenger.send(message);
                    }
                    catch (Exception e)
                    {

                    }
                }
            });

            alertDialog.show();
        }
    }

    private void generateReports()
    {
        final SensorDataManager dataManager = new SensorDataManager(this);
        AsyncTask<Object, String, Object> exportTask = new AsyncTask<Object, String, Object>() {

            private AlertDialog loadingDialog;

            @Override
            protected void onPreExecute()
            {
                loadingDialog = LoadingDialog.create(MainActivity.this, "Generating Report...");
                loadingDialog.show();
            }
            @Override
            protected Object doInBackground(Object... objects)
            {
                File outputDir = new File(Environment.getExternalStorageDirectory(),
                        "BustersDemise");
                outputDir.mkdir();
                List<SensorRun> sensorRuns = dataManager.getAllSets();
                for(SensorRun sensorRun : sensorRuns)
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
                }
                return null;
            }
            @Override
            protected void onPostExecute(Object result)
            {
                loadingDialog.dismiss();
                Toast.makeText(MainActivity.this, "The reports have been generated.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        exportTask.execute();
    }

    private void generateExport(SensorRun sensorRun, BufferedWriter writer) throws IOException
    {
        writer.write("timestamp, accelerometer_x, accelerometer_y, accelerometer_z," +
                " gyroscope_x, gyroscope_y, gyroscope_z\n");
        for(SensorRecord record : sensorRun.getSensorRecords())
        {
            writer.write(record.format());
        }
    }
}
