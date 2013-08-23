package com.michaelstark.bustersdemise;

import android.*;
import android.R;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by mstark on 8/10/13.
 */
public class BluetoothDeviceAdapter extends ArrayAdapter<BluetoothDevice> {
    public BluetoothDeviceAdapter(Context context, Set<BluetoothDevice> bluetoothDevices) {
        super(context, R.layout.simple_list_item_1,
                new LinkedList<BluetoothDevice>(bluetoothDevices));
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = convertView;
        if(view == null)
        {
            view = LayoutInflater.from(getContext()).inflate(R.layout.simple_list_item_1, null);
        }
        ((TextView)view).setText(getItem(position).getName());
        return view;
    }
}
