package com.michaelstark.bustersdemise;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.*;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by mstark on 8/11/13.
 */
public class DataManagerService extends Service {

    private Messenger messenger;
    private NotificationManager notificationManager;
    private DataRecorder recorder;
    private SensorDataManager dataManager;
    private HandlerThread handlerThread;
    private BluetoothSyncHelper btSyncHelper;
    private List<Messenger> registeredClients;

    public static final int MSG_START_RECORDING = 1;
    public static final int MSG_PUSH_DATA = 2;
    public static final int MSG_PULL_DATA = 3;
    public static final int MSG_STOP_RECORDING = 4;
    public static final int MSG_GET_RECORDING_STATUS = 5;
    public static final int MSG_REGISTER_CLIENT = 6;
    public static final int MSG_UNREGISTER_CLIENT = 7;

    class IncomingHandler extends Handler {

        public IncomingHandler(Looper looper)
        {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            Message message = new Message();
            Bundle returnData = new Bundle();
            message.setData(returnData);
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(DataManagerService.this);
            notificationBuilder.setContentTitle("Buster's Demise");
            notificationBuilder.setSmallIcon(R.drawable.scc);
            notificationBuilder.setLargeIcon(
                    BitmapFactory.decodeResource(DataManagerService.this.getResources(),
                            R.drawable.scc));
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    registeredClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    registeredClients.add(msg.replyTo);
                    break;
                case MSG_START_RECORDING:
                    message.what = MSG_START_RECORDING;
                    try
                    {
                        recorder.start(bundle);
                        notificationBuilder.setContentText("The sensor recorders have started.");
                        returnData.putBoolean("isRecording", true);
                    }
                    catch (Exception e)
                    {
                        Log.d("DataManagerService", "The recording could not start: ", e);
                        notificationBuilder.setContentText("There was an issue while starting the recorder.");
                        returnData.putBoolean("isRecording", false);
                    }
                    notificationManager.notify("BustersDemise", 2, notificationBuilder.build());
                    for(Messenger client : registeredClients)
                    {
                        try
                        {
                            client.send(message);
                        }
                        catch(Exception e)
                        {

                        }
                    }
                    break;
                case MSG_PUSH_DATA:
                    try
                    {
                        if(btSyncHelper != null)
                        {
                            btSyncHelper.syncRuns((BluetoothDevice)msg.obj);
                            notificationBuilder.setContentText("The data set has been successfully synced.");
                        }
                        else
                        {
                            notificationBuilder.setContentText("There was an issue while syncing over Bluetooth.");
                        }
                    }
                    catch (Exception e)
                    {
                        Log.d("DataManagerService", "The data set could not be saved: ", e);
                        notificationBuilder.setContentText("There was an issue while syncing over Bluetooth.");
                    }
                    notificationManager.notify("BustersDemise", 4, notificationBuilder.build());
                    break;
                case MSG_STOP_RECORDING:
                    try
                    {
                        recorder.terminate();
                        dataManager.addSet(recorder.getSensorRun());
                        notificationBuilder.setContentText("The data set has been successfully saved.");
                    }
                    catch (Exception e)
                    {
                        Log.d("DataManagerService", "The data set could not be saved: ", e);
                        notificationBuilder.setContentText("There was an issue while saving the data set.");
                    }
                    message.what = MSG_STOP_RECORDING;
                    for(Messenger client : registeredClients)
                    {
                        try
                        {
                            client.send(message);
                        }
                        catch(Exception e)
                        {

                        }
                    }
                    notificationManager.notify("Buster's Demise", 3, notificationBuilder.build());
                    break;
                case MSG_GET_RECORDING_STATUS:

                    message.what = MSG_GET_RECORDING_STATUS;

                    returnData.putBoolean("isRecording", recorder.isRecording());

                    message.setData(returnData);

                    try
                    {
                        msg.replyTo.send(message);
                    }
                    catch (Exception e)
                    {

                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private static class DataManagerServiceConnection implements ServiceConnection
    {
        private Handler handler;

        public DataManagerServiceConnection(Handler handler)
        {
            this.handler = handler;
        }

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            Messenger messenger = new Messenger(service);
            Message message = new Message();
            message.what = -2;
            message.obj = messenger;
            if(handler != null)
            {
                handler.handleMessage(message);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            Message message = new Message();
            message.what = -1;
            if(handler != null)
            {
                handler.handleMessage(message);
            }
        }
    }

    public static ServiceConnection getServiceConnection(Handler messageHandler)
    {
        return new DataManagerServiceConnection(messageHandler);
    }

    public static void getMessenger(Context context, ServiceConnection serviceConnection)
    {
        context.bindService(new Intent(context, DataManagerService.class),
                serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public static void releaseConnection(Context context, ServiceConnection serviceConnection)
    {
        context.unbindService(serviceConnection);
    }

    @Override
    public void onCreate()
    {
        registeredClients = new LinkedList<Messenger>();
        handlerThread = new HandlerThread("DataManagerServiceThread", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        messenger = new Messenger(new IncomingHandler(handlerThread.getLooper()));
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        recorder = new DataRecorder(this);
        dataManager = new SensorDataManager(this);
        try
        {
            btSyncHelper = BluetoothSyncHelper.create(this, messenger);
        }
        catch (Exception e)
        {

        }

        if(btSyncHelper != null)
        {
            btSyncHelper.startServer();
        }
    }

    @Override
    public void onDestroy()
    {
        handlerThread.quit();
        if(recorder.isRecording())
        {
            try
            {
                recorder.terminate();
                dataManager.addSet(recorder.getSensorRun());
            }
            catch (Exception e)
            {
                Log.d("DataManagerService", "The data set could not be saved: ", e);
            }
        }
    }

    public boolean isRecording()
    {
        return recorder != null && recorder.isRecording();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }
}
