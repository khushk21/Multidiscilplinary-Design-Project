package com.example.mdpgroup17.Bluetooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.UUID;

/*
 * Created by Lydia on 25/08/2021
 * Edited by Jerald on
 * */

public class BluetoothConnectionService {

    private static final String TAG = "Debugging Tag";

    private static final String appName = "MDPGroup17";

    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //Create bluetooth adapter
    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    //Part 1:
    private AcceptThread mInsecureAcceptThread;

    //Part 2:
    private ConnectThread mConnectThread;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;
    Intent connectionStatus;

    //Part 3:
    public static boolean BluetoothConnectionStatus = false;
    private static ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        startAcceptThread(); //Once created, call start method to initiate AcceptThread
    }

    /*
     * Thread runs while listening for incoming connection.
     * Waits for a connection.
     * Behaves like a server-side client.
     * Runs until a connection is accepted (or cancelled)
     * */
    private class AcceptThread extends Thread {
        //Local server socket
        private final BluetoothServerSocket mServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            //Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                Log.d(TAG, "AcceptThread: Setting up Server using: " + MY_UUID_INSECURE);

            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }

            mServerSocket = tmp;

        }

        public void run() {
            Log.d(TAG, "run: AcceptThread Running.");

            BluetoothSocket socket = null;

            try{
                //This is a blocking call and will only return on a
                //successful connection or an exception
                Log.d(TAG, "run: RFCOM server socket start.....");
                socket = mServerSocket.accept();

                Log.d(TAG, "run: RFCOM server accepted connection.");
            }catch (IOException e){
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }

            //If null, move on to next step
            if(socket != null){
                connected(socket, mDevice);
            }

            Log.i(TAG, "END mAcceptThread");

        }

        //Closing the server socket
        public void cancel() {
            Log.d(TAG, "cancel: Cancelling AcceptThread.");
            try {
                if(isConnected(mServerSocket)){
                    mServerSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
        }
    }

    /*
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     *
     * Both devices will sit on socket until another device connectThread starts
     * */
    private class ConnectThread extends Thread {
        private BluetoothSocket mSocket;

        public ConnectThread (BluetoothDevice device, UUID uuid){
            Log.d(TAG, "ConnectThread: started");
            mDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN mConnectThread");

            //Get a BluetoothSocket for a connection with the given device
            try{
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                        + MY_UUID_INSECURE);
                tmp = mDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e){
                Log.e(TAG, "ConnectThread: Could not create InsecureRFcommSocket. " + e.getMessage());
            }
            mSocket = tmp;

            //Always cancel discovery. It will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            //Make a connection to the BluetoothSocket
            //Blocking call and will only return a successful connection or an exception
            try{
                mSocket.connect();

                Log.d(TAG, "run: ConnectThread connected.");
                connected(mSocket, mDevice);

            } catch (IOException e){
                //Close socket if failed
                try{
                    if(isConnected(mSocket)){
                        mSocket.close();
                    }
                    Log.d(TAG, "run: ConnectThread socket closed.");
                } catch (IOException e1){
                    Log.e(TAG, "ConnectThread: run: Unable to close socket. " + e1.getMessage());
                }
                Log.d(TAG, "ConnectThread: Could not connect to UUID. " + MY_UUID_INSECURE);

                try {
                    com.example.mdpgroup17.Bluetooth.BTConnectionPage mBluetoothPopUpActivity = (com.example.mdpgroup17.Bluetooth.BTConnectionPage) mContext;
                    mBluetoothPopUpActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Failed to connect to the Device.", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception z) {
                    z.printStackTrace();
                }
            }
            try {
                mProgressDialog.dismiss();
            } catch(NullPointerException e){
                e.printStackTrace();
            }

        }

        //Closing the server socket
        public void cancel() {
            Log.d(TAG, "cancel: Closing Client Socket.");
            try {
                if(isConnected(mSocket)){
                    mSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of mSocket in ConnectThread failed. " + e.getMessage());
            }
        }
    }

    //Start the Accept Thread
    /*
     * If Connect Thread exist, cancel and create new.
     * Accept Thread x exist, create.
     * */
    public synchronized void startAcceptThread(){
        Log.d(TAG, "start");

        if(mConnectThread!=null){
            mConnectThread.cancel();
            mConnectThread=null;
        }
        if(mInsecureAcceptThread == null){
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    /*
     * AcceptThread starts and sits waiting for a connection
     * ConnectThread starts and attempts to make a connection with the other devices AcceptThread.
     * Initiates connection
     * */
    public void startClient (BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient: Started.");

        //initprogress dialog
        try {
            mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please Wait...", true);
        } catch (Exception e) {
            Log.d(TAG, "StartClientThread Dialog show failure");
        }

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    //Manages connection when connection is successful
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ConnectedThread (BluetoothSocket socket){
            Log.d(TAG, "Connectedthread: Starting...");

            //Connection Status
            connectionStatus = new Intent("ConnectionStatus");
            connectionStatus.putExtra("Status", "connected");
            connectionStatus.putExtra("Device", mDevice);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatus);
            BluetoothConnectionStatus = true;

            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //dismiss the progressdialog when connection is established
            /*try{
                mProgressDialog.dismiss();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }*/

            try{
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;

        }

        //Byte array obj gets input from input stream.
        public void run(){
            //Buffer store for the stream
            byte [] buffer = new byte[1024];

            //Returned from read()
            int bytes;

            //Keep listening to the InputStream until an exception occurs
            while (true){
                if(isInterrupted()){
                    return;
                }
                try{
                    //Read from InputStream
                    bytes = mInStream.read(buffer);

                    //Convert into string msg
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    //Pass data to broadcast
                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("theMessage", incomingMessage);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);
                    Log.d(TAG, "InputStream: Sending to Broadcast ->" + incomingMessage);


                } catch (IOException e) {
                    Log.e(TAG, "run: Error reading inputStream. " + e.getMessage());
                    //Break when there is a problem with InputStream, and end connection

                    connectionStatus = new Intent("ConnectionStatus");
                    connectionStatus.putExtra("Status", "disconnected");
                    connectionStatus.putExtra("Device", mDevice);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatus);
                    BluetoothConnectionStatus = false;

                    break;
                }

            }
        }

        //Call this from main activity to send data to remote device
        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to output stream: "+text);
            try {
                if(isConnected(mSocket)){
                    mOutStream.write(bytes);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error writing to output stream. "+e.getMessage());
            }
        }

        //Call this from main activity to shutdown connection
        public void cancel(){
            Log.d(TAG, "cancel: Closing Client Socket");
            try{
                if(isConnected(mSocket)){
                    mSocket.close();
                }
            } catch(IOException e){
                Log.e(TAG, "cancel: Failed to close ConnectThread mSocket " + e.getMessage());
            }
        }

    }

    public boolean isConnected(BluetoothServerSocket socket){
        if(socket != null){
            return true;
        } else {
            return false;
        }
    }

    public boolean isConnected(BluetoothSocket socket){
        if(socket != null){
            return true;
        } else {
            return false;
        }
    }

    private void connected(BluetoothSocket mSocket, BluetoothDevice device) {
        Log.d(TAG, "Connected: Starting.");
        mDevice = device;
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        //Start the Thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    //Another write method that can access connection service and thread
    //Unsynchronized manner
    public static void write(byte[] out){
        ConnectedThread r;

        Log.d(TAG, "write: Write is called." );

        mConnectedThread.write(out);
    }

}
