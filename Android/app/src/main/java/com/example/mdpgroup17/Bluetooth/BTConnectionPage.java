package com.example.mdpgroup17.Bluetooth;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.mdpgroup17.Adapter.DeviceListAdapter;
import com.example.mdpgroup17.R;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BTConnectionPage extends AppCompatActivity{
    private static final String TAG = "BTConnectionPage";
    private String connStatus;
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;

    TextView mStatusBlueTv, mBTStatus;
    Button mOnBtn, mOffBtn, mDiscoverBtn, mPairedBtn, mBackBtn, btnStartConnection;
    BluetoothAdapter mBlueAdapter;

    //Holds the bluetooth devices it discovers
    public ArrayList<BluetoothDevice> mNewBTDevices = new ArrayList<>();
    public ArrayList<BluetoothDevice> mPairedBTDevices = new ArrayList<>();
    public DeviceListAdapter mNewDeviceListAdapter;
    public DeviceListAdapter mPairedDeviceListAdapter;
    ListView lvPairedDevices, lvNewDevices;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    ProgressDialog myDialog;
    boolean retryConnection = false;
    Handler reconnectionHandler = new Handler();

    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothDevice mBTDevice;

    //Chat portion
    BluetoothConnectionService mBluetoothConnection;

    Runnable reconnectionRunnable = new Runnable() {
        @Override
        public void run() {
            // Magic here
            try {
                if (BluetoothConnectionService.BluetoothConnectionStatus == false) {
                    startBTConnection(mBTDevice, MY_UUID_INSECURE);
                    Toast.makeText(BTConnectionPage.this, "Reconnection Success", Toast.LENGTH_SHORT).show();
                }
                reconnectionHandler.removeCallbacks(reconnectionRunnable);
                retryConnection = false;
            } catch (Exception e) {
                Toast.makeText(BTConnectionPage.this, "Failed to reconnect, trying in 5 second", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_connection);

        mStatusBlueTv = findViewById(R.id.statusBluetoothTv);
        mOnBtn        = findViewById(R.id.onBtn);
        mOffBtn       = findViewById(R.id.offBtn);
        mDiscoverBtn  = findViewById(R.id.discoverableBtn);
        mPairedBtn    = findViewById(R.id.pairedBtn);
        mBackBtn      = findViewById(R.id.btnBack);

        lvPairedDevices = findViewById(R.id.lvPairedDevices);
        lvNewDevices = findViewById(R.id.lvNewDevices);
        mNewBTDevices = new ArrayList<>();
        mPairedBTDevices = new ArrayList<>();

        mBTStatus = findViewById(R.id.connStatusTextView);
        connStatus = "Disconnected";
        sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        if (sharedPreferences.contains("connStatus")){
            connStatus = sharedPreferences.getString("connStatus", "");
        }
        mBTStatus.setText(connStatus);

        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor = sharedPreferences.edit();
                editor.putString("connStatus", mBTStatus.getText().toString());
                editor.commit();
                finish();
            }
        });

        myDialog = new ProgressDialog(BTConnectionPage.this);
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(false);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        btnStartConnection = findViewById(R.id.btnStartConnection);

        IntentFilter filter2 = new IntentFilter("ConnectionStatus");
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver5, filter2);

        //Broadcasts when bond state changes (pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        //adapter
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();

        //check if bluetooth is available or not
        if (mBlueAdapter == null){
            mStatusBlueTv.setText("Bluetooth is not available");
        }
        else {
            mStatusBlueTv.setText("Bluetooth is available");
        }

        //on btn click
        mOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"onClick: enabling BT");

                if (!mBlueAdapter.isEnabled()){
                    showToast("Turning On Bluetooth...");
                    //intent to on bluetooth
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(intent);

                    //Catch state change and log it
                    IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                    registerReceiver(mBroadcastReceiver1, BTIntent);
                }
                else {
                    showToast("Bluetooth is already on");
                }
            }
        });

        //off btn click
        mOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"onClick: disabling BT");

                if (mBlueAdapter.isEnabled()){
                    mBlueAdapter.disable();
                    showToast("Turning Bluetooth Off");

                    //Catch state change and log it
                    IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                    registerReceiver(mBroadcastReceiver1, BTIntent);
                }
                else {
                    showToast("Bluetooth is already off");
                }
            }
        });

        //discover bluetooth btn click
        mDiscoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"onDiscoverBtn: Making device discoverable");

                if (!mBlueAdapter.isDiscovering()){
                    showToast("Making Your Device Discoverable");
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivity(intent);

                    //Catch state change and log it
                    IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                    registerReceiver(mBroadcastReceiver2, BTIntent);
                }
            }
        });

        //get all  devices btn click
//        mPairedBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//        });

        lvNewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mBlueAdapter.cancelDiscovery();
                lvPairedDevices.setAdapter(mPairedDeviceListAdapter);

                String deviceName = mNewBTDevices.get(i).getName();
                String deviceAddress = mNewBTDevices.get(i).getAddress();
                Log.d(TAG, "onItemClick: A device is selected.");
                Log.d(TAG, "onItemClick: DEVICE NAME: " + deviceName);
                Log.d(TAG, "onItemClick: DEVICE ADDRESS: " + deviceAddress);

                //Create bond
                //Check API version
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Log.d(TAG, "onItemClick: Initiating pairing with " + deviceName);
                    mNewBTDevices.get(i).createBond();

                    //Assign bluetooth device
                    //Start Connection Service, AcceptThread will run
                    mBluetoothConnection = new BluetoothConnectionService(BTConnectionPage.this);
                    mBTDevice = mNewBTDevices.get(i);
                    Log.d(TAG,"Touched Device: " + mBTDevice);
                }
            }
        });

        lvPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mBlueAdapter.cancelDiscovery();
                lvNewDevices.setAdapter(mNewDeviceListAdapter);

                String deviceName = mPairedBTDevices.get(i).getName();
                String deviceAddress = mPairedBTDevices.get(i).getAddress();
                Log.d(TAG, "onItemClick: A device is selected.");
                Log.d(TAG, "onItemClick: DEVICE NAME: " + deviceName);
                Log.d(TAG, "onItemClick: DEVICE ADDRESS: " + deviceAddress);

                mBluetoothConnection = new BluetoothConnectionService(BTConnectionPage.this);
                mBTDevice = mPairedBTDevices.get(i);
                Log.d(TAG,"Touched Device: " + mBTDevice);
            }
        });

        btnStartConnection.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(mBTDevice == null) {
                    Toast.makeText(BTConnectionPage.this, "Please Select a Device before connecting.", Toast.LENGTH_LONG).show();
                } else {
                    startConnection();
                }
            }
        });
    } //End of OnCreate


    // Create a BroadcastReceiver for ACTION_FOUND
    // Broadcast Receiver 1 for bt state change on and off
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"mBroadcastReceiver 1: Changing state");
            String action = intent.getAction();
            // When discovery finds a device
            // Catch action state changed request sent
            if (action.equals(mBlueAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBlueAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"mBroadcastReceiver2 Discoverability Mode change");
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };

    /*
     * Broadcast Receiver for listing devices that are not yet paired
     * Executed by mPairedBtn method
     * */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"mBroadcastReceiver3 List unpaired devices");

            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Store device
                mNewBTDevices.add(device);

                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());

                mNewDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mNewBTDevices);
                lvNewDevices.setAdapter(mNewDeviceListAdapter);
            }

        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BroadcastReceiver 4 detecting bond state change");

            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;
                }
                //case2: creating a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };

    //connectionReceiver
    private BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"BroadcastReceiver5 handle reconnection");

            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();

            if(status.equals("connected")){
                try {
                    myDialog.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }

//                Log.d(TAG, "mBroadcastReceiver5: Device now connected to "+mDevice.getName());
                try{
                    Toast.makeText(BTConnectionPage.this, "Device now connected", Toast.LENGTH_LONG).show();
                    editor.putString("connStatus", "Connected ");
                    mBTStatus.setText("Connected");
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
            else if(status.equals("disconnected") && retryConnection == false){
                Log.d(TAG, "mBroadcastReceiver5: Disconnected");
                Toast.makeText(BTConnectionPage.this, "Disconnected", Toast.LENGTH_LONG).show();

                mBluetoothConnection = new BluetoothConnectionService(BTConnectionPage.this);
                //mBluetoothConnection.startAcceptThread();

                sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
                editor = sharedPreferences.edit();
                editor.putString("connStatus", "Disconnected");
                TextView connStatusTextView = findViewById(R.id.connStatusTextView);
                connStatusTextView.setText("Disconnected");
                editor.commit();

                try {
                    myDialog.show();
                }catch (Exception e){
                    Log.d(TAG, "BTConnectionPage: mBroadcastReceiver5 Dialog show failure");
                }
                retryConnection = true;
                reconnectionHandler.postDelayed(reconnectionRunnable, 5000);

            }
            editor.commit();
        }
    };

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called");
        super.onDestroy();
        try {
            unregisterReceiver(mBroadcastReceiver1);
            unregisterReceiver(mBroadcastReceiver2);
            unregisterReceiver(mBroadcastReceiver3);
            unregisterReceiver(mBroadcastReceiver4);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: called");
        super.onPause();
        try {
            unregisterReceiver(mBroadcastReceiver1);
            unregisterReceiver(mBroadcastReceiver2);
            unregisterReceiver(mBroadcastReceiver3);
            unregisterReceiver(mBroadcastReceiver4);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra("mBTDevice", mBTDevice);
        data.putExtra("myUUID",MY_UUID_INSECURE);
        setResult(RESULT_OK, data);
        super.finish();
    }

    //Broadcast receiver
    /*BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");
            messages.append(text + "\n");
            incomingMessages.setText(messages);
        }
    };*/

    //Start Connection method, initiate thread
    //Must be paired first
    public void startConnection() {
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }


    //Chat Service Method
    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnection.startClient(device, uuid);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK){
                    //bluetooth is on
                    showToast("Bluetooth is on");
                }
                else {
                    //user denied to turn bluetooth on
                    showToast("could't on bluetooth");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //toast message function
    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");

        }
    }

    public void searchDevices(View view){
        Log.d(TAG, "mPairedBtn: Looking for unpaired devices.");
        mNewBTDevices.clear();

        if(mBlueAdapter != null) {
            if(!mBlueAdapter.isEnabled()){
                Toast.makeText(BTConnectionPage.this, "Please turn on Bluetooth",
                        Toast.LENGTH_SHORT).show();
            }
            if (mBlueAdapter.isDiscovering()) {
                mBlueAdapter.cancelDiscovery();
                Log.d(TAG, "mPairedBtn: Canceling discovery.");

                //Check BT permission in manifest
                checkBTPermissions();

                mBlueAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
            }
            if (!mBlueAdapter.isDiscovering()) {
                //Check BT permission in manifest
                checkBTPermissions();

                mBlueAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
            }

            mPairedBTDevices.clear();
            Set<BluetoothDevice> pairedDevices = mBlueAdapter.getBondedDevices();
            Log.d(TAG, "Search: Number of paired devices found: "+ pairedDevices.size());
            for(BluetoothDevice d : pairedDevices){
                Log.d(TAG, "Paired Devices: "+ d.getName() +" : " + d.getAddress());
                mPairedBTDevices.add(d);
                mPairedDeviceListAdapter = new DeviceListAdapter(this,R.layout.device_adapter_view, mPairedBTDevices);
                lvPairedDevices.setAdapter(mPairedDeviceListAdapter);
            }

        }

    }
}
