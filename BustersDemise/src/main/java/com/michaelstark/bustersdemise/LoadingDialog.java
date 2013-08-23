package com.michaelstark.bustersdemise;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by mstark on 7/18/13.
 */
public class LoadingDialog {
    public static AlertDialog create(Context context, String text)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View loadingView = LayoutInflater.from(context).inflate(R.layout.loading_layout, null);
        TextView loadingMessage = (TextView)loadingView.findViewById(R.id.dialogMessage);
        if(loadingMessage != null && loadingMessage.equals("") == false)
        {
            loadingMessage.setText(text);
        }
        ProgressBar loadingAnimation = (ProgressBar)loadingView.findViewById(R.id.progressBar);
        loadingAnimation.animate();
        builder.setView(loadingView);
        return builder.create();
    }
}
