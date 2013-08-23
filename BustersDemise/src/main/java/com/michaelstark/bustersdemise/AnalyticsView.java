package com.michaelstark.bustersdemise;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by mstark on 8/10/13.
 */
public class AnalyticsView
{
    private View contentView;

    private LinearLayout graphContainer;
    private ListView runViewer;

    private Context context;

    private SensorDataManager sensorData;

    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    private XYSeriesRenderer mCurrentRenderer;

    public AnalyticsView(Context context)
    {
        this.context = context;

        sensorData = new SensorDataManager(context);

        contentView = LayoutInflater.from(context).inflate(R.layout.analytics_view, null);

        graphContainer = (LinearLayout)contentView.findViewById(R.id.graphContainer);
        runViewer = (ListView)contentView.findViewById(R.id.runViewer);

        XYSeriesRenderer accelerometerXRenderer = new XYSeriesRenderer();
        accelerometerXRenderer.setColor(Color.rgb(0xE0, 0x1B, 0x25));
        XYSeriesRenderer accelerometerYRenderer = new XYSeriesRenderer();
        accelerometerYRenderer.setColor(Color.rgb(0x08, 0x0B, 0xC7));
        XYSeriesRenderer accelerometerZRenderer = new XYSeriesRenderer();
        accelerometerZRenderer.setColor(Color.rgb(0x08, 0xC7, 0x08));
        XYSeriesRenderer gyroscopeXRenderer = new XYSeriesRenderer();
        gyroscopeXRenderer.setColor(Color.rgb(0xAA, 0x3B, 0xF5));
        XYSeriesRenderer gyroscopeYRenderer = new XYSeriesRenderer();
        gyroscopeYRenderer.setColor(Color.rgb(0x3B, 0xDF, 0xF5));
        XYSeriesRenderer gyroscopeZRenderer = new XYSeriesRenderer();
        gyroscopeZRenderer.setColor(Color.rgb(0x09, 0x73, 0x09));

        mRenderer.addSeriesRenderer(accelerometerXRenderer);
        mRenderer.addSeriesRenderer(accelerometerYRenderer);
        mRenderer.addSeriesRenderer(accelerometerZRenderer);
        mRenderer.addSeriesRenderer(gyroscopeXRenderer);
        mRenderer.addSeriesRenderer(gyroscopeYRenderer);
        mRenderer.addSeriesRenderer(gyroscopeZRenderer);

        runViewer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final SensorRun sensorRun = (SensorRun)adapterView.getAdapter().getItem(i);
                AsyncTask<Object, Object, Object> generatePlotTask = new AsyncTask<Object, Object, Object>() {
                    private AlertDialog loadingDialog;
                    @Override
                    protected void onPreExecute()
                    {
                        loadingDialog = LoadingDialog.create(AnalyticsView.this.context,
                                "Generating Plot...");
                        loadingDialog.show();
                    }
                    @Override
                    protected Object doInBackground(Object... objects) {
                        XYSeries accelerometerXData = new XYSeries("Accelerometer (X axis)");
                        XYSeries accelerometerYData = new XYSeries("Accelerometer (Y axis)");
                        XYSeries accelerometerZData = new XYSeries("Accelerometer (Z axis)");
                        XYSeries gyroscopeXData = new XYSeries("Gyroscope (X axis)");
                        XYSeries gyroscopeYData = new XYSeries("Gyroscope (Y axis)");
                        XYSeries gyroscopeZData = new XYSeries("Gyroscope (Z axis)");
                        for(int i = 0; i < sensorRun.getSensorRecords().size(); i++)
                        {
                            SensorRecord record = sensorRun.getSensorRecords().get(i);
                            accelerometerXData.add(i, record.getAccelerationX());
                            accelerometerYData.add(i, record.getAccelerationY());
                            accelerometerZData.add(i, record.getAccelerationZ());
                            gyroscopeXData.add(i, record.getGyroscopeX());
                            gyroscopeYData.add(i, record.getGyroscopeY());
                            gyroscopeZData.add(i, record.getGyroscopeZ());
                        }
                        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                        dataset.addSeries(accelerometerXData);
                        dataset.addSeries(accelerometerYData);
                        dataset.addSeries(accelerometerZData);
                        dataset.addSeries(gyroscopeXData);
                        dataset.addSeries(gyroscopeYData);
                        dataset.addSeries(gyroscopeZData);
                        return dataset;
                    }
                    @Override
                    protected void onPostExecute(Object result)
                    {
                        XYMultipleSeriesDataset dataset = (XYMultipleSeriesDataset)result;
                        GraphicalView chart = ChartFactory.getLineChartView(
                                AnalyticsView.this.context, dataset, mRenderer);
                        ViewGroup.LayoutParams layoutParams
                                = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT);
                        chart.setLayoutParams(layoutParams);
                        graphContainer.addView(chart);
                        loadingDialog.dismiss();
                        cancel(true);
                    }
                };
                generatePlotTask.execute();
            }
        });
    }

    public View getContentView()
    {
        return contentView;
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
}
