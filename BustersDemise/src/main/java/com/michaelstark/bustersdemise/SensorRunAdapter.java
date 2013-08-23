package com.michaelstark.bustersdemise;

import android.*;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by mstark on 8/7/13.
 */
public class SensorRunAdapter extends ArrayAdapter<SensorRun>
{
    private SimpleDateFormat dateFormat;
    private List<SensorRun> data;
    public SensorRunAdapter(Context context, List<SensorRun> data)
    {
        super(context, android.R.layout.simple_list_item_1, data);
        this.data = data;
        dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
    }

    public List<SensorRun> getSensorData()
    {
        return data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View layout = convertView;
        if(layout == null)
        {
            layout = LayoutInflater.from(getContext()).inflate(R.layout.sensor_run_item, null);
        }

        SensorRun sensorRun = getItem(position);

        TextView runName = (TextView)layout.findViewById(R.id.runName);
        runName.setText(sensorRun.getDataSetName());

        TextView runLength = (TextView) layout.findViewById(R.id.runLength);
        runLength.setText(String.format("%s - %s", dateFormat.format(sensorRun.getStartTime()),
                dateFormat.format(sensorRun.getEndTime())));

        return layout;
    }
}
