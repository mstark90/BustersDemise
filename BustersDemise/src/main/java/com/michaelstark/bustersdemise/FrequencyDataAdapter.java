package com.michaelstark.bustersdemise;

import android.*;
import android.R;
import android.content.Context;
import android.widget.ArrayAdapter;

/**
 * Created by mstark on 8/7/13.
 */
public class FrequencyDataAdapter extends ArrayAdapter<Integer>
{
    public FrequencyDataAdapter(Context context)
    {
        super(context, R.layout.simple_list_item_1);
        this.add(16);
        this.add(32);
        this.add(64);
    }
}
