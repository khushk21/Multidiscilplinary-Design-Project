package com.example.mdpgroup17;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.example.mdpgroup17.Adapter.SectionsPagerAdapter;
import com.example.mdpgroup17.Arena.ArenaMap;
import com.example.mdpgroup17.Bluetooth.BluetoothConnectionService;
import com.example.mdpgroup17.Communication.ChatFragment;
import com.google.android.material.tabs.TabLayout;

import java.nio.charset.Charset;
import java.util.UUID;

/** Main Activity:
 *  Will receive details like messages from chat, sending messages to RPI
 *  connStatusTextView, BT status will be shared onto MainActivity and across the whole app
 *  through sharedPreferences
 */

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // Declare Variables
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static Context context;
    BluetoothDevice mBTDevice;

    private static ArenaMap arenaMap;
    static TextView txtRobotDirection, txtRobotCoord, txtXPosition, txtYPosition;
    //Button btButton;
    BluetoothConnectionService mBluetoothConnection;
    private static UUID myUUID;
    ProgressDialog myDialog;
    //TextView obs1;


    @Override
    protected void onCreate (Bundle savedInstanceState) {

        //Initialization
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(9999);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        //Get broadcasted msg
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        //btButton = findViewById(R.id.bluetoothButton);

        //sharedPreferences set up
        MainActivity.context = getApplicationContext();
        this.sharedPreferences();
        editor.putString("message", "");
        editor.putString("direction","None");
        editor.putString("connStatus", "Disconnected");
        editor.commit();

        //Arena map
        arenaMap = new ArenaMap(this);
        arenaMap = findViewById(R.id.mapView);
        txtRobotCoord = findViewById(R.id.txtRobotPosition);
        txtRobotDirection = findViewById(R.id.txtRobotDirection);
        txtXPosition = findViewById(R.id.txtXPosition);
        txtYPosition = findViewById(R.id.txtYPosition);

        //Obstacles
//        obs1 = (TextView) findViewById(R.id.ob1);
//
//        obs1.setOnLongClickListener(longClickListener);
//
//        arenaMap.setOnDragListener(dragListener);

        //Process Dialog
        myDialog = new ProgressDialog(MainActivity.this);
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(false);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }


    private static void showLog(String message) {
        Log.d(TAG, message);
    }

    public static void sharedPreferences() {
        sharedPreferences = MainActivity.getSharedPreferences(MainActivity.context);
        editor = sharedPreferences.edit();
    }

    private static SharedPreferences getSharedPreferences(Context context){
        return context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 1:
                if(resultCode == Activity.RESULT_OK){
                    mBTDevice = (BluetoothDevice) data.getExtras().getParcelable("mBTDevice");
                    myUUID = (UUID) data.getSerializableExtra("myUUID");
                }
        }
    }

    // Send message to bluetooth
    public static void printMessage(String message) {
        showLog("Entering printMessage");
        editor = sharedPreferences.edit();

        if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
        }
        showLog(message);
        editor.putString("message", ChatFragment.getMessageReceivedTextView().getText() + "\n" + message);
        editor.commit();
        refreshMessageReceived();
    }

    public static void refreshMessageReceived() {
        String received = sharedPreferences.getString("message", "");
        ChatFragment.getMessageReceivedTextView().setText(sharedPreferences.getString("message", ""));
    }

    private BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"BroadcastReceiver 5: ");

            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences();

            if(status.equals("connected")){
                try {
                    myDialog.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }

                Log.d(TAG, "mBroadcastReceiver5: Successfully connected to ");
                Toast.makeText(MainActivity.this, "Successfully connected", Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Connected");

            }
            else if(status.equals("disconnected")){
                Log.d(TAG, "mBroadcastReceiver5: Disconnected");
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Disconnected");
                myDialog.show();
            }
            editor.commit();
        }
    };

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("theMessage");
            showLog("Message Received: " + message);

            //Protocol for receiving message
            String newMessage = getCheckMessage(message);
            if (newMessage.equals("invalid")){
                Log.d(TAG,"Message is invalid");
            } else {
                arenaMap.updateMap(newMessage);

                sharedPreferences();
                String receivedText = sharedPreferences.getString("message", "") + "\n" + newMessage;
                editor.putString("message", receivedText);
                editor.commit();

                refreshMessageReceived();
            }
        }
    };

    public static String getCheckMessage(String message){
        String newMessage;

        if ((message.contains("<") || message.contains(">"))) {
            int startIndex = message.indexOf("<") + 1;
            int endIndex = message.indexOf(">");
            newMessage = message.substring(startIndex, endIndex);
            Log.d(TAG, "Message: "+ newMessage);
            return newMessage;
        } else {
            newMessage = "invalid";
        }

        return newMessage;

    }

    public static ArenaMap getArenaMap(){
        return arenaMap;
    }

    public static void setRobotDetails(int x, int y, String direction){
        Log.d(TAG, "setRobotDetails: Getting current robot coordinates");

        if (x == -1 && y == -1) {
            txtRobotCoord.setVisibility(View.INVISIBLE);
            txtRobotDirection.setVisibility(View.INVISIBLE);
        } else {
            txtRobotCoord.setText(String.valueOf(x) + "," +
                    String.valueOf(y));
            txtRobotDirection.setText(direction);
        }
    }

    public static void setXPosition(int x){
        Log.d(TAG, "ZXC setXPosition" + x);
        txtXPosition.setText(String.valueOf(x));
    }

    public static void setYPosition(int Y){
        Log.d(TAG, "ZXC setYPosition" + Y);
        txtYPosition.setText(String.valueOf(Y));
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        try{
            IntentFilter filter2 = new IntentFilter("ConnectionStatus");
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver5, filter2);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        showLog("Entering onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putString(TAG, "onSaveInstanceState");
        showLog("Exiting onSaveInstanceState");
    }
}