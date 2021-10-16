package com.example.mdpgroup17;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.mdpgroup17.Arena.ArenaMap;
import com.example.mdpgroup17.Bluetooth.BTConnectionPage;
import com.example.mdpgroup17.Controller.ControllerFragment;
import com.example.mdpgroup17.Tab.PageViewModel;
import com.example.mdpgroup17.Arena.Obstacle;

/**
 * Will include settings like:
 * 1. Add obstacles -- See if interactive portion can work here. Else, move to main activity
 * 2. Set StartPoint
 * 3. Change direction of robot
 * 4. Toggle manual and auto
 * 5. Reset Map
 * */

public class MapSettingsFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "Map Settings Fragment";

    private PageViewModel pageViewModel;

    Button btnStartPt, btnAddObstacle, btnResetMap, btnChangeObsFace, btnStartFastestCar;
    ArenaMap arenaMap;

    Button btButton;

    public static Fragment newInstance(int index) {
        MapSettingsFragment fragment = new MapSettingsFragment();
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
        SharedPreferences sharedPreferences;
        View root = inflater.inflate(R.layout.activity_settings, container, false);

        arenaMap = MainActivity.getArenaMap();
        btnAddObstacle = root.findViewById(R.id.btnAddObstacles);
        btnResetMap = root.findViewById(R.id.btnResetMap);
        btnStartPt = root.findViewById(R.id.btnStartPoint);
        btButton = root.findViewById(R.id.bluetoothButton);
        btnChangeObsFace = root.findViewById(R.id.btnChangeObsFace);
        btnStartFastestCar = root.findViewById(R.id.btnStartFastestCar);


        btnResetMap.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Log.d(TAG,"btnResetMap: Reset Map");
                Toast.makeText(getContext(),"Resetting Map!",Toast.LENGTH_SHORT).show();
                arenaMap.resetArena();
                //ControllerFragment.counter=0;
            }
        });

        btnStartPt.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Log.d(TAG,"btnStartPt: Set starting point");
                arenaMap.setStartingPoint(true);
                Log.d(TAG,"btnStartPt: Starting point set to TRUE.");

//                MainActivity.printMessage("START");
            }
        });

        btnAddObstacle.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Log.d(TAG,"btnAddObstacle: Add obstacle");
                Toast.makeText(getContext(),"Sending obstacles!",Toast.LENGTH_SHORT).show();
                MainActivity.printMessage(arenaMap.getObstacles());
            }
        });

        //To bluetooth page
        btButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent (getActivity(), BTConnectionPage.class);
                //((MainActivity) getActivity()).startActivity(intent);
                startActivity(intent);
            }
        });

        btnChangeObsFace.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.d(TAG,"btnChangeObsFace: Setting obstacle face!");
                arenaMap.setObstacleFace();
            }
        });

        btnStartFastestCar.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.d(TAG,"btnStartFastestCar: Start!");
                arenaMap.setObstacleFace();

                MainActivity.printMessage("START");
            }
        });

        return root;
    }

}
