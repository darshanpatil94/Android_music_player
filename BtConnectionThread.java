package com.example.musicplayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

public class BtConnectionThread extends Thread {
    private final BluetoothSocket bluetoothSocket;
    private final InputStream inStream;
    private final OutputStream outStream;
    private static final String TAG = "BluetoothTest";
    private Handler handler;
    private int threadNumber;

    public BtConnectionThread(BluetoothSocket btSocket, Handler handler,int threadNumber) 
    {
        this.threadNumber = threadNumber;
        this.bluetoothSocket = btSocket;
        this.handler = handler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = bluetoothSocket.getInputStream();
            tmpOut = bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            Log.d(TAG, "BtConnectionThread const:problem getting streams");
        }
        inStream = tmpIn;
        outStream = tmpOut;
    }

/*    public void run() {
        byte[] buffer = new byte[64]; // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = inStream.read(buffer);
                // Send the obtained bytes to the UI activity
                String msg = new String(buffer);
                Log.d(TAG,
                     "BtConnectionThread run while loop: received from arduino:" + msg);
                handler.obtainMessage(threadNumber, bytes, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, 
                        "BtConnectionThread run while loop: problem reading");
                e.printStackTrace();
                break;
            }
        }
    } */
    
   public void run() 
    {
    	byte[] buffer = new byte[64];
    	int begin = 0;
    	int bytes = 0;
    	while (true) 
    	{
    	try {
    	//bytes = inStream.read(buffer);
    	bytes += inStream.read(buffer, bytes, buffer.length - bytes);
    	for(int i = begin; i < bytes; i++) 
    	{
    	if(buffer[i] == "#".getBytes()[0]) 
    	{
    	handler.obtainMessage(1, begin , i , buffer).sendToTarget();
    	begin = i + 1;
    	if(i == bytes - 1) 
    	{
    	bytes = 0;
    	begin = 0;
    	}    	}   	}
    	} catch (IOException e) 
    	{break;}   	}   	} 
    	

    /**
     * Get the address of the bluetooth device this object deals with
     * 
     * @return
     */
    public String getDeviceAddress() {
        if (bluetoothSocket != null
                && bluetoothSocket.getRemoteDevice() != null) {
            return bluetoothSocket.getRemoteDevice().getAddress();
        }
        return null;
    }

    /**
     * Get the name of the bluetooth device this object deals with
     * 
     * @return
     */
    public String getDeviceName() {
        if (bluetoothSocket != null
                && bluetoothSocket.getRemoteDevice() != null) {
            return bluetoothSocket.getRemoteDevice().getName();
        }
        return null;
    }

    /**
     * Get a display value (device + name) of the bluetooth device this object
     * deals with
     * 
     * @return
     */
    public String getDeviceDisplayVal() {
        return getDeviceName() + " " + getDeviceAddress();
    }

    public void disconnect() {
        try {
            outStream.close();
        } catch (Exception e) {
        }
        try {
            inStream.close();
        } catch (Exception e) {
        }
        try {
            bluetoothSocket.close();
        } catch (Exception e) {
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            outStream.write(bytes);
        } catch (IOException e) {
            Log.e(TAG, "BtConnectionThread write: problem writing to stream.");
            e.printStackTrace();
        }
    }
}
