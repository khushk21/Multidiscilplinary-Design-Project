package com.example.mdpgroup17.Controller;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.mdpgroup17.Arena.ArenaMap;
import com.example.mdpgroup17.MainActivity;
import com.example.mdpgroup17.Tab.PageViewModel;
import com.example.mdpgroup17.R;

import static android.content.Context.SENSOR_SERVICE;

public class ControllerFragment extends Fragment implements SensorEventListener{
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "Controller Fragment";

    private PageViewModel pageViewModel;

    ImageButton moveForwardImageBtn, turnRightImageBtn, moveBackImageBtn, turnLeftImageBtn, StopImageBtn;
    private com.example.mdpgroup17.Arena.ArenaMap ArenaMap;
    Switch tiltSwitch;

    private Sensor mSensor;
    private SensorManager mSensorManager;
    Handler sensorHandler = new Handler();
    boolean sensorFlag= false;

    public static ControllerFragment newInstance(int index) {
        ControllerFragment fragment = new ControllerFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_map, container, false);

        ArenaMap = MainActivity.getArenaMap();
        
        moveForwardImageBtn = root.findViewById(R.id.forwardImageBtn);
        turnRightImageBtn = root.findViewById(R.id.rightImageBtn);
        moveBackImageBtn = root.findViewById(R.id.backImageBtn);
        turnLeftImageBtn = root.findViewById(R.id.leftImageBtn);
        StopImageBtn = root.findViewById(R.id.StopImageBtn);
        tiltSwitch = root.findViewById(R.id.swMotion);


        //TODO: Make sure moving of robot can happen before sending info to RPI/AMDTOOL (Don't go over Arena (done) and obstacle)

        moveForwardImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"onClick: Move Forward");
//                ArenaMap.moveRobot("forward");
                if(checkValidMovement() == true){
                    MainActivity.printMessage("w");
                    updateStatus("Moving forward");
                }
            }
        });

        turnRightImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"onClick: Turn Right/Change Direction");
//                ArenaMap.moveRobot("right");
                if(checkValidMovement() == true) {
                    MainActivity.printMessage("d");
                    updateStatus("Turning Right");
                }
            }
        });

        moveBackImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"onClick: Reverse");
//                ArenaMap.moveRobot("back");
                if(checkValidMovement() == true) {
                    MainActivity.printMessage("s");
                    updateStatus("Reversing");
                }
            }
        });

        turnLeftImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"onClick: Turn Left/Change Direction");
//                ArenaMap.moveRobot("left");
                if(checkValidMovement() == true) {
                    MainActivity.printMessage("a");
                    updateStatus("Turning Left");
                }
            }
        });

        StopImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"onClick: Stop");
//                ArenaMap.moveRobot("Stop");
                if(checkValidMovement() == true) {
                    MainActivity.printMessage("stop");
                    updateStatus("Stopping");
                }
            }
        });

        mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        tiltSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (ArenaMap.getCanDrawRobot()) {
                    if(tiltSwitch.isChecked()){
                        updateStatus("MOTION SENSOR ON");
                        moveForwardImageBtn.setVisibility(View.GONE);
                        moveBackImageBtn.setVisibility(View.GONE);
                        turnRightImageBtn.setVisibility(View.GONE);
                        turnLeftImageBtn.setVisibility(View.GONE);
                        StopImageBtn.setVisibility(View.GONE);
                        tiltSwitch.setPressed(true);

                        mSensorManager.registerListener(ControllerFragment.this, mSensor, mSensorManager.SENSOR_DELAY_NORMAL);
                        sensorHandler.post(sensorDelay);
                    }else{
                        updateStatus("MOTION SENSOR OFF");
                        moveForwardImageBtn.setVisibility(View.VISIBLE);
                        moveBackImageBtn.setVisibility(View.VISIBLE);
                        turnRightImageBtn.setVisibility(View.VISIBLE);
                        turnLeftImageBtn.setVisibility(View.VISIBLE);
                        StopImageBtn.setVisibility(View.VISIBLE);
                        Log.d(TAG,"unregistering Sensor Listener");
                        try {
                            mSensorManager.unregisterListener(ControllerFragment.this);
                        }catch(IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                        sensorHandler.removeCallbacks(sensorDelay);
                    }
                } else {
                    updateStatus("Please set the 'STARTING POINT'");
                    tiltSwitch.setChecked(false);
                }
                if(tiltSwitch.isChecked()){
                    compoundButton.setText("Motion Sensor On");
                }else
                {
                    compoundButton.setText("Motion Sensor Off");
                }
            }
        });

        return root;
    }

    private void updateStatus(String message) {
        Toast toast = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
        toast.show();
    }

    public boolean checkValidMovement(){
        int coordinates [] = ArenaMap.getCurCoord();
        if ((coordinates[0] != 0 && coordinates[0] != 19) && (coordinates[1] != 0 && coordinates[1] != 19)) {
            return  true;
        } else {
            return false;
        }
    }

    private final Runnable sensorDelay = new Runnable() {
        @Override
        public void run() {
            sensorFlag = true;
            sensorHandler.postDelayed(this,1000);
        }
    };

    /*TODO: Call for update from RPI/AMDTOOL upon tilt.
    * */
    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        Log.d(TAG,"SensorChanged X: "+x);
        Log.d(TAG,"SensorChanged Y: "+y);
        Log.d(TAG,"SensorChanged Z: "+z);

        if(sensorFlag) {
            if (y < -2) {
                Log.d(TAG,"Sensor Move Forward Detected");
//                ArenaMap.moveRobot("forward");
                MainActivity.printMessage("w");
            } else if (y > 2) {
                Log.d(TAG,"Sensor Move Backward Detected");
//                ArenaMap.moveRobot("back");
                MainActivity.printMessage("s");
            } else if (x > 2) {
                Log.d(TAG,"Sensor Move Left Detected");
//                ArenaMap.moveRobot("left");
                MainActivity.printMessage("a");
            } else if (x < -2) {
                Log.d(TAG,"Sensor Move Right Detected");
//                ArenaMap.moveRobot("right");
                MainActivity.printMessage("d");
            }
        }
        sensorFlag = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
