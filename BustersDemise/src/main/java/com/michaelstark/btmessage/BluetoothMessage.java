package com.michaelstark.btmessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mstark on 8/14/13.
 */
public class BluetoothMessage
{
    private Map<String, String> data;
    public BluetoothMessage()
    {
        data = new HashMap<String, String>();
    }
    public Map<String, String> getData()
    {
        return data;
    }

    public long getLong(String name)
    {
        return Long.parseLong(data.get(name));
    }
    public int getInt(String name)
    {
        return Integer.parseInt(data.get(name));
    }
    public boolean getBoolean(String name)
    {
        return Boolean.parseBoolean(data.get(name));
    }
    public float getFloat(String name)
    {
        return Float.parseFloat(data.get(name));
    }
    public double getDouble(String name)
    {
        return Double.parseDouble(data.get(name));
    }
    public String getString(String name)
    {
        return data.get(name);
    }

    public void put(String name, String value)
    {
        data.put(name, value);
    }
    public void put(String name, int value)
    {
        data.put(name, Integer.toString(value));
    }
    public void put(String name, long value)
    {
        data.put(name, Long.toString(value));
    }
    public void put(String name, boolean value)
    {
        data.put(name, Boolean.toString(value));
    }
    public void put(String name, float value)
    {
        data.put(name, Float.toString(value));
    }
    public void put(String name, double value)
    {
        data.put(name, Double.toString(value));
    }

    public void serialize(OutputStream outputStream) throws IOException
    {
        long contentLength = 0;
        for(Map.Entry<String, String> entry : data.entrySet())
        {
            contentLength = contentLength + 8 + entry.getKey().length() + entry.getValue().length();
        }
        if(contentLength > 16777215)
        {
            throw new IOException("The message is too big.");
        }
        byte[] contentLengthData = String.format("%06X", contentLength).getBytes("utf-8");
        outputStream.write(contentLengthData, 0, 6);
        for(Map.Entry<String, String> entry : data.entrySet())
        {
            String key = entry.getKey();
            if(key.length() > 255)
            {
                throw new IOException("The key size is too large.");
            }
            String value = entry.getValue();
            outputStream.write(1);
            outputStream.write(key.length());
            String dataString = String.format("%06X%s%s", value.length(), key, value);
            byte[] data = dataString.getBytes("utf-8");
            outputStream.write(data, 0, data.length);
        }
    }

    public static BluetoothMessage deserialize(InputStream inputStream) throws IOException
    {
        BluetoothMessage bluetoothMessage = new BluetoothMessage();

        byte[] lengthBuf = new byte[6];
        byte[] dataBuf = new byte[8192];
        inputStream.read(lengthBuf, 0, 6);

        String key = "", value = "";

        int total = Integer.parseInt(new String(lengthBuf, 0, 6), 16);
        int totalEntry = 0, totalKey = 0, totalValue = 0;
        int totalEntryRead = 0, totalKeyRead = 0, totalValueRead = 0, totalRead = 0;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        while(totalRead < total && (totalKey = inputStream.read()) > -1)
        {

            if(totalKey == 1)
            {
                if(!key.equals(""))
                {
                    value = new String(buffer.toByteArray(), 0, buffer.size());

                    bluetoothMessage.getData().put(key, value);

                    buffer.close();
                    buffer = new ByteArrayOutputStream();
                    Arrays.fill(dataBuf, (byte)0);
                }
                totalKey = inputStream.read();
                inputStream.read(lengthBuf, 0, 6);
                String totalValueStr = new String(lengthBuf, 0, 6);
                totalValue = Integer.parseInt(totalValueStr, 16);
                inputStream.read(dataBuf, 0, totalKey);
                key = new String(dataBuf, 0, totalKey);
                totalRead = totalRead + totalKey + 8;
            }
            else if(totalKey != 1)
            {
                buffer.write(totalKey);
                totalRead++;
            }



            int read = inputStream.read(dataBuf, 0, totalValue);

            buffer.write(dataBuf, 0, read);
            totalRead += read;
        }

        if(buffer.size() > 0)
        {
            value = new String(buffer.toByteArray(), 0, buffer.size());

            bluetoothMessage.getData().put(key, value);

            buffer.close();
        }

        return bluetoothMessage;
    }
}
