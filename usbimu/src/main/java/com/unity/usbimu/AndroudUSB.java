package com.unity.usbimu;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import android.util.Log;
import android.widget.ArrayAdapter;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;


public class AndroudUSB {

    public static AndroudUSB _AndroidUSB;
    public Context context;

    public String ResultStr = "";
    public Boolean OpenDeviceState = false;
    public Boolean PermissionState = false;

    private ConsoleThread p;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private UsbSerialPort mPort;

    private UsbManager mUsbManager;

    private PendingIntent mPermissionIntent;
    private List<UsbSerialPort> mEntries = new ArrayList<UsbSerialPort>();
    private ArrayAdapter<UsbSerialPort> mAdapter;

    private List<UsbSerialDriver> availableDrivers;
    private UsbSerialDriver mDriver;

    private AndroudUSB()
    {
        //this._AndroidUSB = this;
    }

    public static AndroudUSB AndroidUSBinstance()
    {
        if(_AndroidUSB == null)
        {
            _AndroidUSB = new AndroudUSB();
        }
        return _AndroidUSB;
    }

    public void setContext()
    {
        Log.d(TAG, "setContext: UNITY CALL");
        this.context = _AndroidUSB.context;
    }

    public void Init()
    {
        Log.d(TAG, "Init: first");
        mPermissionIntent  = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbReceiver, filter);

        availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
        mDriver = availableDrivers.get(0);
        PermissionCheck();
    }



    public void PermissionCheck()
    {
        if(mUsbManager.hasPermission(mDriver.getDevice())){
            PermissionState = true;
        }
        else
        {
            mUsbManager.requestPermission(mDriver.getDevice(),mPermissionIntent);
        }
    }


    public void IMUStart()
    {

        Log.d(TAG, "IMUStart: "+ String.valueOf(mUsbManager.hasPermission(mDriver.getDevice())));
        p = new ConsoleThread( mDriver );
        p.start();

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
                    if(mPort != null)
                    {
                        OpenDeviceState = true;
                    }
                    mPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    Log.d(TAG, "run: Step Start!!");
                    step();
                } catch (Exception e) {
                    Log.d(TAG, "run: Exception : Error " + e.toString());
                } finally {
                    try {
                        mPort.close();
                        OpenDeviceState = false;
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
