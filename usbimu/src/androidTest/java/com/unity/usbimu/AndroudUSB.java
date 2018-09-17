package com.unity.usbimu;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public abstract class AndroudUSB extends Context {
    public String ResultStr = "";
    public ConsoleThread p;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private UsbSerialPort mPort;

    private UsbManager mUsbManager;

    private PendingIntent mPermissionIntent;
    private List<UsbSerialPort> mEntries = new ArrayList<UsbSerialPort>();
    private ArrayAdapter<UsbSerialPort> mAdapter;


    public void Init()
    {
        mPermissionIntent  = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
    }

    public void IMUStart()
    {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

        UsbSerialDriver mDriver = availableDrivers.get(0);
        Log.d(TAG, "IMUStart: "+ String.valueOf(mUsbManager.hasPermission(mDriver.getDevice())));

        if(mUsbManager.hasPermission(mDriver.getDevice())){
            p = new ConsoleThread( mDriver );
            p.start();
        }
        else
        {
            mUsbManager.requestPermission(mDriver.getDevice(),mPermissionIntent);
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    @Override
    public Resources.Theme getTheme() {
        return null;
    }

    @Override
    public void startIntentSender(IntentSender intent, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {

    }

    @Override
    public boolean startInstrumentation(@NonNull ComponentName className, @Nullable String profileFile, @Nullable Bundle arguments) {
        return false;
    }

    public class ConsoleThread extends Thread {
        private UsbDeviceConnection connection;
        private UsbSerialDriver mDriver;

        public ConsoleThread(UsbSerialDriver driver) {
            mDriver = driver;
        }

        public void run() {
            connection = mUsbManager.openDevice(mDriver.getDevice());

            if (connection != null) {
                mPort = mDriver.getPorts().get(0);
                try {
                    mPort.open(connection);
                    mPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    Log.d(TAG, "run: Step Start!!");
                    step();
                } catch (Exception e) {
                    Log.d(TAG, "run: Exception : Error " + e.toString());
                } finally {
                    try {
                        mPort.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void step() throws Exception {
            while (true) {
                if (mPort == null) {
                    Log.d(TAG, "step: mPort is Null");
                    return;
                }
                if (connection == null) {
                    Log.d(TAG, "step: Connection is Null");
                    return;
                }
                final byte buffer[] = new byte[82];
                int numBytesRead = mPort.read(buffer, 1000);
                if (numBytesRead > 0) {
                    String temp = new String(buffer);
                    ResultStr = temp;
                }
            }
        }
    }

}
