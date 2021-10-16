package com.example.mdpgroup17.Arena;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

import com.example.mdpgroup17.MainActivity;
import com.example.mdpgroup17.R;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The Arena is 2mx2m.
 * Obstacles are 10cmx10cm.
 * Robot is 20cmx20cm.
 * Every grid box should follow obstacle measurements.
 * There will be 5 obstacles in total.
 * Number of rows and columns = 20 by 20.
 *
 * What to draw - handled by Canvas
 * How to draw - handled by Paint
 * */

public class ArenaMap extends View implements Serializable {

    private static final String TAG = "Arena Map";

    private static Cell[][] cells;
    private static final int mCols = 20, mRows = 20;
    private static float cellSize, hMargin, vMargin;
    private static String robotDirection = "north";
    private static int[] obsCoord = new int[]{-1, -1};
    private static int[] curCoord = new int[]{-1, -1};
    private static int[] oldCoord = new int[]{-1, -1};
    private static ArrayList<int[]> obstacleCoord = new ArrayList<>();

    private static Obstacle [] obstacleList = new Obstacle[8];

    private static Paint wallPaint = new Paint();
    private static Paint robotPaint = new Paint();
    private static Paint directionPaint = new Paint();
    private static Paint obstaclePaint = new Paint();
    private static Paint unexploredPaint = new Paint();
    private static Paint exploredPaint = new Paint();
    private static Paint gridNumberPaint = new Paint();
    private static Paint obstacleNumberPaint = new Paint();
    private static Paint emptyPaint = new Paint();
    private static Paint virtualWallPaint = new Paint();

    private static Paint westPaint = new Paint();
    private static Paint eastPaint = new Paint();
    private static Paint southPaint = new Paint();
    private static Paint northPaint = new Paint();
    private static Paint linePaint = new Paint();

    //Create only avail when state is true
    private static boolean createCellStatus = false;
    private static boolean setRobotPostition = false;
    private static boolean changedFaceAnnotation = false;
    private static boolean validPosition = false;
    private static boolean canDrawRobot = false;
//    private static boolean canDrawObstacle = false;
//    private static boolean canUpdateObsFace = false;
//    private static boolean canDrag = false;
//    private static boolean isOnClick = false;

    private View mapView;
    private Rect r;

//    private static final int MAX_CLICK_DURATION = 200;
//    private long mStartClickTime;

    private GestureDetectorCompat mGestureDetector;
    private LongPressGestureListener longPressGestureListener;
//    private GestureDetector.OnDoubleTapListener mDoubleTapListener;

//    Intent i = new Intent(ArenaMap.this, LongPressGestureListener.class).putExtra("Obstacle", obstacle1);

    public ArenaMap(Context context) {
        super(context);
        init(null);
    }

    public ArenaMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);

        wallPaint.setColor(Color.WHITE);
        robotPaint.setColor(Color.parseColor("#bada55"));
        directionPaint.setColor(Color.BLACK);
        unexploredPaint.setColor(Color.parseColor("#ccd8d7"));
        exploredPaint.setColor(Color.GRAY);
        emptyPaint.setColor(Color.WHITE);
        virtualWallPaint.setColor(Color.parseColor("#FFA500"));

        obstaclePaint.setColor(Color.BLACK);
        obstaclePaint.setStyle(Paint.Style.FILL);
        obstaclePaint.setStrokeWidth(3f);

        obstacleNumberPaint.setColor(Color.WHITE);
        obstacleNumberPaint.setTextSize(20);
        obstacleNumberPaint.setTypeface(Typeface.DEFAULT_BOLD);
        obstacleNumberPaint.setAntiAlias(true);
        obstacleNumberPaint.setStyle(Paint.Style.FILL);
        obstacleNumberPaint.setTextAlign(Paint.Align.LEFT);

        gridNumberPaint.setColor(Color.BLACK);
        gridNumberPaint.setTextSize(15);
        gridNumberPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        westPaint.setColor(Color.GREEN);
        westPaint.setStyle(Paint.Style.FILL);

        eastPaint.setColor(Color.RED);
        eastPaint.setStyle(Paint.Style.FILL);

        northPaint.setColor(Color.YELLOW);
        northPaint.setStyle(Paint.Style.FILL);

        southPaint.setColor(Color.BLUE);
        southPaint.setStyle(Paint.Style.FILL);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.YELLOW);
        linePaint.setStrokeWidth(3f);

        mapView = (View) findViewById(R.id.mapView);

        longPressGestureListener = new LongPressGestureListener(this.mapView);
        mGestureDetector = new GestureDetectorCompat(context, longPressGestureListener);

        obstacleList [0] = new Obstacle (670, 40, 670, 40,"1", 0, "None","1");
        obstacleList [1] = new Obstacle(670, 115, 670, 115,"2", 0, "None", "2");
        obstacleList [2] = new Obstacle(670, 190, 670, 190,"3", 0, "None", "3");
        obstacleList [3] = new Obstacle(670, 265, 670, 265,"4", 0, "None", "4");
        obstacleList [4] = new Obstacle(670, 340, 670, 340,"5", 0, "None", "5");
        obstacleList [5] = new Obstacle(670, 410, 670, 410,"6", 0, "None", "6");
        obstacleList [6] = new Obstacle(670, 485, 670, 485,"7", 0, "None", "7");
        obstacleList [7] = new Obstacle(670, 565, 670, 565,"8", 0, "None", "8");

    }

    private void init(@Nullable AttributeSet set){
    }

    //Create Cell method
    private void createCells(){
        cells = new Cell[mCols][mRows];
        for (int x = 0; x < mCols; x++) {
            for (int y = 0; y < mRows; y++) {

                cells[x][y] = new Cell(x * cellSize + (cellSize / 30),
                        y * cellSize + (cellSize / 30),
                        (x + 1) * cellSize - (cellSize / 40),
                        (y + 1) * cellSize - (cellSize / 60), unexploredPaint);

                float xMiddle = ((((x + 1) * cellSize - (cellSize / 40))-(x * cellSize + (cellSize / 30)))/2);
                float yMiddle =  ((((y + 1) * cellSize - (cellSize / 60))-(y * cellSize + (cellSize / 30)))/2);
                Log.d(TAG, "CreateCell XMid" + xMiddle);
                Log.d(TAG, "CreateCell YMid" + yMiddle);

            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        int coordinates[];
        int x = (int) event.getX();
        int y = (int) event.getY();

        mGestureDetector.onTouchEvent(event);

        //Get touched coordinate
        coordinates = findGridOnTouch(x, y);

        Log.d(TAG, "onTouchEvent: Touched coordinates are " +
                coordinates[0] + " " + coordinates[1]);

        Log.d(TAG, "onTouchEvent: Touched coordinates are " +
                x + " " + y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //Touch down code
                Log.d(TAG, "onTouchEvent: ACTION_DOWN");
                for (int i = 0; i < obstacleList.length; i++) {
                    if (obstacleList[i].isTouched(x, y) && !obstacleList[i].getActionDown()) {
                        Log.d(TAG, "onTouchEvent: this is touched--->" + obstacleList[i]);
                        Log.d(TAG, "onTouchEvent: Coordinates are " +
                                coordinates[0] + " " + coordinates[1]);

                        //Set new width and height (Resize the obstacle)
//                        obstacleList[i].setResizeUp(true);
                        obstacleList[i].setActionDown(true);
//                        if(!mGestureDetector.isLongpressEnabled()){
//                            obstacleList[i].setActionDown(true);
//                        } else {
//                            Log.d(TAG,"Enabled: " + mGestureDetector.isLongpressEnabled());
//                            obstacleList[i].setActionDown(false);
//                        }
                        //Add view to Main Activity to see where the obstacle's coordinates are at
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "onTouchEvent: ACTION_MOVE");

                MainActivity.setXPosition(coordinates[0]);
                if(coordinates[1] == 1){
                    MainActivity.setYPosition(19);
                }
                if(coordinates[1] == 2){
                    MainActivity.setYPosition(18);
                }
                if(coordinates[1] == 3){
                    MainActivity.setYPosition(17);
                }
                if(coordinates[1] == 4){
                    MainActivity.setYPosition(16);
                }
                if(coordinates[1] == 5){
                    MainActivity.setYPosition(15);
                }
                if(coordinates[1] == 6){
                    MainActivity.setYPosition(14);
                }
                if(coordinates[1] == 7){
                    MainActivity.setYPosition(13);
                }
                if(coordinates[1] == 8){
                    MainActivity.setYPosition(12);
                }
                if(coordinates[1] == 9){
                    MainActivity.setYPosition(11);
                }
                if(coordinates[1] == 10){
                    MainActivity.setYPosition(10);
                }
                if(coordinates[1] == 11){
                    MainActivity.setYPosition(9);
                }
                if(coordinates[1] == 12){
                    MainActivity.setYPosition(8);
                }
                if(coordinates[1] == 13){
                    MainActivity.setYPosition(7);
                }
                if(coordinates[1] == 14){
                    MainActivity.setYPosition(6);
                }
                if(coordinates[1] == 15){
                    MainActivity.setYPosition(5);
                }
                if(coordinates[1] == 16){
                    MainActivity.setYPosition(4);
                }
                if(coordinates[1] == 17){
                    MainActivity.setYPosition(3);
                }
                if(coordinates[1] == 18){
                    MainActivity.setYPosition(2);
                }
                if(coordinates[1] == 19){
                    MainActivity.setYPosition(1);
                }
                if(coordinates[1] == -1){
                    MainActivity.setYPosition(0);
                }

                //Touch move code
                for (Obstacle obstacles : obstacleList) {
                    if (obstacles.getActionDown()) {
                        Log.d(TAG, "C x: " + x);
                        Log.d(TAG, "C y: " + y);
                        Log.d(TAG, "C First (MOVE): " + coordinates[0]);
                        Log.d(TAG, "C Second (MOVE): " + coordinates[1]);
                        obstacles.setPosition(x, y);
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "onTouchEvent: ACTION_UP");
                //Touch up code
                for (Obstacle obstacles : obstacleList) {
                    if (obstacles.getActionDown()) {
                        if (isInArena(coordinates)) {
                            Log.d(TAG, "C First: " + coordinates[0]);
                            Log.d(TAG, "C Second: " + coordinates[1]);
                            isInCell(x,y);
                            obstacles.setObsMapCoord(coordinates[0], coordinates[1]);

                            //Direct message to Main Activity
                            //MainActivity.printMessage("ADDOBSTACLE," + obstacles.getObsID() + "," + coordinates[0] + "," + inverseCoordinates(coordinates[1] - 1));
                            obstacles.setaObsX(coordinates[0]);
                            Log.d(TAG, "Obstacle Coord x = " + obstacles.getaObsX());

                            if (coordinates[1] == -1) {
                                //When inverse, 0 = 19
                                obstacles.setaObsY(inverseCoordinates(0));
                            } else {
                                obstacles.setaObsY(coordinates[1] - 1);
                            }
                            
                            Log.d(TAG, "Obstacle Coord y = " + obstacles.getaObsY());




                        } else {
                            // Out of bounce = go back to starting point
                            obstacles.setObsX(obstacles.getInitCoords()[0]);
                            obstacles.setObsY(obstacles.getInitCoords()[1]);
                            int initX = obstacles.getObsMapCoord()[0];
                            int initY = inverseCoordinates(obstacles.getObsMapCoord()[1] -1);

//                            obstacles.setPosition(obstacles.getInitCoords()[0], obstacles.getInitCoords()[1]);
                            obstacles.setObsMapCoord(-1, -1);

                            //Direct message to Main Activity
                            if (initX != -1 && initY != -1){
                                MainActivity.printMessage("SUBOBSTACLE," + obstacles.getObsID() + "," + initX  + "," + initY);
                            }
                        }
                    }
                    obstacles.setActionDown(false);
                    obstacles.setResizeUp(false);
                    obstacles.setFaceResizeUp(false);
                    invalidate();
                }
//                String obsDetailsString = sendToRpi();

                //Send to RPI only if 5 obstacles are in the string and none of the values = 0,0
//                if((obsDetailsString.contains("ADDOBSTACLE,1")) && (obsDetailsString.contains("ADDOBSTACLE,2")) &&
//                        obsDetailsString.contains("ADDOBSTACLE,3") && obsDetailsString.contains("ADDOBSTACLE,4") &&
//                        obsDetailsString.contains("ADDOBSTACLE,5")) {
//                    if (!obsDetailsString.contains(",0,0")){
//                        MainActivity.printMessage(obsDetailsString);
//                    }
//                }

                break;
        }

        if (setRobotPostition) {
            if (isInArena(coordinates)) {
                if ((coordinates[0] != 0 && coordinates[0] != 19) && (coordinates[1] != 0 && coordinates[1] != 19)) {
                    //Robot's currenr position is set
                    setCurCoord(coordinates[0], coordinates[1]);

                    invalidate();
                }
            }
        }

            Log.d(TAG, "onTouchEvent: Exiting onTouchEvent");
        //Must be true, else it will only call ACTION_DOWN
        return true;
        //return super.onTouchEvent(event);
    }

    public String getObstacles(){
        String obsDetailsString = "";
        for (Obstacle obstacles : obstacleList) {

            if (!((obstacles.getaObsX() == 0) && (obstacles.getaObsY() == 0) && (obstacles.getObsFace().equals(" ")))){

                Log.d(TAG, "x = " + obstacles.getaObsX());
                Log.d(TAG, "y = " + obstacles.getaObsY());

                String ADD = "ADDOBSTACLE," + obstacles.getObsID() + "," + obstacles.getaObsX() + "," + obstacles.getaObsY() + ",";
                String FACE = "OBSTACLEFACE," + obstacles.getObsID() + "," + obstacles.getObsFace() + ",";
//                obsDetailsString = ADD.concat(FACE);
                obsDetailsString = obsDetailsString.concat(ADD.concat(FACE));


            }
        }
        Log.d(TAG, obsDetailsString);
        return obsDetailsString;
    }

    //Draw shapes on the canvas
    @Override
    protected  void onDraw(Canvas canvas){
        super.onDraw(canvas);

        //Background color of the canvas
//        canvas.drawColor(Color.RED);

        //Set width and height of the canvas
        int width = getWidth();
        int height = getHeight();

        Log.d(TAG,"Width and Height: " + width + height);


        //Calculate cellsize based on dimensions of the canvas
        if(width/height < mCols/mRows){
            cellSize = width / (mCols + 1);
            Log.d(TAG,"Cell size 1: " + cellSize);
        } else {
            cellSize = height / (mRows + 1);
            Log.d(TAG,"Cell size 2: " + cellSize);
        }

        //Calculate margin size of canvas
        hMargin = ((width - mCols * cellSize) / 2 - 45);
        vMargin = (height - mRows * cellSize) / 2;


        //Create cell
        if(!createCellStatus){
            //Create cell coordincates
            //Log.d(TAG, "onDraw: Creating cells");
            createCells();
            createCellStatus = true;
        }

        //Set Margin
        canvas.translate(hMargin, vMargin);

        drawBorder(canvas);
        drawCell(canvas);
        drawGridNumber(canvas);
        drawRobot(canvas);

        //Hmm.... somehow it doesnt draw the obstacles
        for(Obstacle obstacles : obstacleList) {
            obstacles.drawObj(canvas);
            obstacles.drawObsFace(canvas, obstacles.getTouchCount(), linePaint);

            canvas.drawText(obstacles.getTargetID(), obstacles.getObsX() + 9, obstacles.getObsY() + 21, obstacleNumberPaint);
//            paintObsFace(canvas);
        }

    }

    //Draw individual cell
    private void drawCell(Canvas canvas){
        //Log.d(TAG, "drawCell(): in drawCell");
        for (int x = 0; x < mCols; x++) {
            for (int y = 0; y < mRows; y++) {
                //Draw cells
                canvas.drawRect(cells[x][y].startX,cells[x][y].startY,cells[x][y].endX,cells[x][y].endY,cells[x][y].paint);

//                Log.d(TAG, "DrawRect, startX " + cells[x][y].startX + " Cell " + y);
//                Log.d(TAG, "DrawRect, startY " + cells[x][y].startY + " Cell " + y);
//                Log.d(TAG, "DrawRect, endX " + cells[x][y].endX + " Cell " + y);
//                Log.d(TAG, "DrawRect, endY " + cells[x][y].endY + " Cell " + y);
            }
        }
    }

    //Draw border for each cell
    private void drawBorder(Canvas canvas){
        for (int x = 0; x < mCols; x++) {
            for (int y = 0; y < mRows; y++) {
                //Top
                canvas.drawLine(x * cellSize, y * cellSize, (x + 1) * cellSize, y * cellSize, wallPaint);
                //Right
                canvas.drawLine((x + 1) * cellSize, y * cellSize, (x + 1) * cellSize, (y + 1) * cellSize, wallPaint);
                //Left
                canvas.drawLine(x * cellSize, y * cellSize, x * cellSize, (y + 1) * cellSize, wallPaint);
                //Bottom
                canvas.drawLine(x * cellSize, (y + 1) * cellSize, (x + 1) * cellSize, (y + 1) * cellSize, wallPaint);
            }
        }
    }

    //Draw robot on canvas
    private void drawRobot(Canvas canvas) {
        Log.d(TAG,"Drawing Robot");
        int robotCoordinates [] = getCurCoord();
        int x = robotCoordinates[0];
        int y = robotCoordinates[1];
        String direction = getRobotDirection();

        if(x != -1 && y != -1){
            float halfWidth = ((cells[x][y - 1].endX) - (cells[x][y - 1].startX)) / 2;

            //row and col is the middle of the robot
            Log.d(TAG,"drawRobot: Coordinates are= " + x + " , " + inverseCoordinates(y));

            //Draw Robot box
            canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, robotPaint);
            canvas.drawRect(cells[x][y - 1].startX, cells[x][y - 1].startY, cells[x][y - 1].endX, cells[x][y - 1].endY, robotPaint);
            canvas.drawRect(cells[x + 1][y].startX, cells[x + 1][y].startY, cells[x + 1][y].endX, cells[x + 1][y].endY, robotPaint);
            canvas.drawRect(cells[x - 1][y].startX, cells[x - 1][y].startY, cells[x - 1][y].endX, cells[x - 1][y].endY, robotPaint);
            canvas.drawRect(cells[x + 1][y - 1].startX, cells[x + 1][y - 1].startY, cells[x + 1][y - 1].endX, cells[x + 1][y - 1].endY, robotPaint);
            canvas.drawRect(cells[x - 1][y - 1].startX, cells[x - 1][y - 1].startY, cells[x - 1][y - 1].endX, cells[x - 1][y - 1].endY, robotPaint);
            canvas.drawRect(cells[x][y + 1].startX, cells[x][y + 1].startY, cells[x][y + 1].endX, cells[x][y + 1].endY, robotPaint);
            canvas.drawRect(cells[x + 1][y + 1].startX, cells[x + 1][y + 1].startY, cells[x + 1][y + 1].endX, cells[x + 1][y + 1].endY, robotPaint);
            canvas.drawRect(cells[x - 1][y + 1].startX, cells[x - 1][y + 1].startY, cells[x - 1][y + 1].endX, cells[x - 1][y + 1].endY, robotPaint);

            //Robot direction (Arrow)
            Path path = new Path();
            Log.d(TAG,"Robot direction: " + direction);

            switch (direction){
                case "north":
                    path.moveTo(cells[x][y - 1].startX + halfWidth, cells[x][y - 1].startY); // Top
                    path.lineTo(cells[x][y - 1].startX, cells[x][y - 1].endY); // Bottom left
                    path.lineTo(cells[x][y - 1].endX, cells[x][y - 1].endY); // Bottom right
                    path.lineTo(cells[x][y - 1].startX + halfWidth, cells[x][y - 1].startY); // Back to Top
                    break;
                case "south":
                    path.moveTo(cells[x][y + 1].endX - halfWidth, cells[x][y + 1].endY); // Top
                    path.lineTo(cells[x][y + 1].startX, cells[x][y + 1].startY); // Bottom left
                    path.lineTo(cells[x + 1][y + 1].startX, cells[x +1][y + 1].startY); // Bottom right
                    path.lineTo(cells[x][y + 1].endX - halfWidth, cells[x][y + 1].endY); // Back to Top
                    break;
                case "east":
                    path.moveTo(cells[x+1][y].startX + (2*halfWidth), cells[x][y].startY + halfWidth); // Top
                    path.lineTo(cells[x+1][y].startX, cells[x+1][y].startY); // Bottom left
                    path.lineTo(cells[x+1][y+1].startX, cells[x+1][y+1].startY); // Bottom right
                    path.lineTo(cells[x+1][y].startX + (2*halfWidth) , cells[x][y].startY + halfWidth); // Back to Top
                    break;
                case "west":
                    path.moveTo(cells[x-1][y].startX, cells[x][y].startY + halfWidth); // Top
                    path.lineTo(cells[x][y].startX, cells[x][y].startY); // Bottom left
                    path.lineTo(cells[x][y + 1].startX, cells[x][y  +1].startY); // Bottom right
                    path.lineTo(cells[x-1][y].startX, cells[x][y].startY + halfWidth); // Back to Top
                    break;
            }
            path.close();
            canvas.drawPath(path, directionPaint);

            //After drawing, set drawing to false
            setRobotPostition = false;
            MainActivity.setRobotDetails(x, inverseCoordinates(y), direction);
        }
    }

//    private void drawObstacle(Canvas canvas){
//        Log.d(TAG,"drawObstacle: Drawing obstacle");
//        int x, y;
//        Paint obsPaint;
//
//        for (Obstacle obstacles : obstacleList) {
//            r = new Rect((int)obstacles.getObsX(), (int)obstacles.getObsY(),(int)obstacles.getObsX() + 31, (int)obstacles.getObsY() + 31);
//
//
//            canvas.drawRect(r, obstaclePaint);
//            canvas.drawText(obstacles.getTargetID(), obstacles.getObsX() + 9, obstacles.getObsY() + 21, obstacleNumberPaint);
//
//        }
//        paintObsFace(canvas);

//        canvas.drawLine(obstacle1.getObsX(), obstacle1.getObsY(), obstacle1.getObsX() + 31, obstacle1.getObsY() + 31, linePaint);


//        Log.d(TAG,"--->" + obstacle1.getObsX() + ", " + obstacle1.getObsY());

        //Check if it is within the arena
        //if(setRobotPostition = true){ //LOL this let me move the robot
//        if(obsRow != -1 && obsCol != -1) {
//            //Redraw all obstacles and newly added obstacle
//            for(int i = 0; i < obstacleList.size(); i++){
//
//                //Check if obstacle already exist
//                x = (int) obstacleList.get(i).getObsX();
//                y = (int) obstacleList.get(i).getObsY();
//                obsPaint = obstacleList.get(i).getObsPaint();
//
//                canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, obsPaint);
//                canvas.drawText(obstacleList.get(i).getTargetID(), cells[x][y].startX, cells[x][y].endY, obstacleNumberPaint);
//
//            }
//        }
//        setObstaclePosition = false;
//        canDrawObstacle = false;
//    }


//    private void drawDigit(Canvas canvas) {
//        String text = obstacle1.getTargetID();
////        float textWidth = obstacleNumberPaint.measureText(text);
//
//        canvas.getClipBounds(r);
//        int mHeight = r.height();
//        int mWidth = r.width();
//
//        obstacleNumberPaint.getTextBounds(text, 0, text.length(), r);
//
//        float x = mWidth / 2f - r.width() / 2f - r.left;
//        float y = mHeight / 2f + r.height() / 2f - r.bottom;
//
//        Log.d(TAG,"DRAWDIGIT: " + x +", " + y);
//
//        canvas.drawText(text, x, y, obstacleNumberPaint);
//        drawTextBounds(canvas, (int)x, (int)y);
//    }
//
//    private void drawTextBounds(Canvas canvas, int x, int y) {
//        Paint rPaint = new Paint();
//        RectF bounds = new RectF(r);
//        rPaint.setColor(Color.TRANSPARENT);
//        rPaint.setStyle(Paint.Style.STROKE);
//        r.offset(x, y);
//        canvas.drawRect(r, rPaint);
//    }

    //Draw numbers
    private void drawGridNumber(Canvas canvas) {
        //Row
        for (int x = 0; x < 20; x++) {
            if(x >9 && x <20){
                canvas.drawText(Integer.toString(x), cells[x][19].startX + (cellSize / 5), cells[x][19].endY + (cellSize / 1.5f), gridNumberPaint);
            } else {
                canvas.drawText(Integer.toString(x), cells[x][19].startX + (cellSize / 3), cells[x][19].endY + (cellSize / 1.5f), gridNumberPaint);
            }
        }
        //Column
        for (int x = 0; x <20; x++) {
            if(x >9 && x <20){
                canvas.drawText(Integer.toString(19 - x), cells[0][x].startX - (cellSize / 1.5f), cells[0][x].endY - (cellSize / 3.5f), gridNumberPaint);
            } else {
                canvas.drawText(Integer.toString(19 - x), cells[0][x].startX - (cellSize / 1.2f), cells[0][x].endY - (cellSize / 3.5f), gridNumberPaint);
            }
        }
    }

    //Inverting rows
    private int inverseCoordinates(int y){
        return (19 - y);
    }

//    public int getXCoord(){
//        return curCoord[0];
//    }
//
//    public int getYCoord(){
//        return inverseCoordinates(curCoord[1]);
//    }

    public void updateMap(String message) {
        Log.d(TAG,"updateMap: Updating Map!");

        int robotCoordinates [] = getCurCoord();
        String receivedMessage [] = message.split(",");
        String item = receivedMessage[0];
        int x,y;
        String obsID, targetID;
        String direction, movement;

        switch (item.toUpperCase()){
            case "TARGET":
                //Update obstacle by displaying image ID
                obsID = receivedMessage[1];
                targetID = receivedMessage[2];

                updateTargetText(obsID, targetID);
                break;
            case "ROBOTPOSITION":
                //Get new robot position
                x = Integer.valueOf(receivedMessage[1]) + 1;
                y = Integer.valueOf(receivedMessage[2]) + 1;
                direction = receivedMessage[3];

                Log.d(TAG, "New coordinates: " + x + "," + y);
                Log.d(TAG, "Direction " + direction);

                moveRobot(x,y,direction);
                break;
            case "MOVE":
                //Get robot movement
                movement = receivedMessage[1];
                Log.d(TAG, "updateMap: Move " + movement);

                moveRobot(movement);
                break;
        }
    }

    private void updateTargetText(String obsID, String targetID) {
        //Go through list of obstacles
        String ID;
        for (Obstacle obstacles : obstacleList) {
            ID = obstacles.getObsID();
            if(ID.equals(obsID)){
                Log.d(TAG,"obsID: " + obsID);
                Log.d(TAG,"targetID: " + targetID);
                obstacles.setTargetID(targetID);
            }
        }
        invalidate();
    }

    //Resetting Arena by resetting everything
    public void resetArena(){
        curCoord = new int [] {-1, -1};
        robotDirection = "north";
        createCellStatus = false;
        setRobotPostition = false;
        canDrawRobot = false;

        for (Obstacle obstacles : obstacleList){
            obstacles.setObsX(obstacles.getInitCoords()[0]);
            obstacles.setObsY(obstacles.getInitCoords()[1]);
            obstacles.setTargetID(obstacles.getObsID());
            obstacles.setaObsX(0);
            obstacles.setaObsY(0);

            obstacles.setTouchCount(0);
            obstacles.setLongPress(false);
        }

        setStartingPoint(false);
        MainActivity.setRobotDetails(-1, -1, "north");
        MainActivity.setXPosition(0);
        MainActivity.setYPosition(0);

        invalidate();
    }

    private ArrayList<int[]> getObstacleCoord() {
        return obstacleCoord;
    }

    private boolean isInArena(int touchedCoord []){
        //Check if coordinates is within the Arena
        Log.d(TAG,"isInArena: Check if touched coordinates is within the Arena");
        boolean isInArena = false;

        //If in Arena, return true
        if (touchedCoord[0] != -1 && touchedCoord[1] != -1) {
            isInArena = true;
        } else if (touchedCoord [0] != -1 && touchedCoord[1] == -1){
            isInArena = true;
        }

        return isInArena;
    }

    private int[] isInCell(int x, int y){
        //Check if coordinates is within the Cell, set to the nearest position
        Log.d(TAG,"isInCell: Check if obstacle coordinates is within the Cell");

        return new int [] {1,2};
    }

    /**
     * Change color of the obstacle to indicate face of the image
     * Black: Default, Green: Left, Red: Right, Yellow: Down, Blue: Front
     * Need to attach count to the object
     *
     * @return*/

//    public void paintObsFace(Canvas canvas){
//        //Paint newPaint = new Paint();
////        int x, y;
//        String obsFace;
//        for (Obstacle obstacles : obstacleList) {
//            obsFace = obstacles.getObsFace();
//            switch (obsFace){
//                case "West":
//                    //Green: Left (West)
////                    obstacleList.get(i).setObsPaint(westPaint);
////                    MainActivity.printMessage("FACE,W");
//                    //Left = "West"
//                    canvas.drawLine(obstacles.getObsX() + 3f, obstacles.getObsY() , obstacles.getObsX() + 3f, obstacles.getObsY() + 31, linePaint);
//                    break;
//
//                case "East":
//                    //Red: Right
////                    obstacleList.get(i).setObsPaint(eastPaint);
////                    MainActivity.printMessage("FACE,E");
//                    //Right = "East"
//                    canvas.drawLine(obstacles.getObsX() + 28, obstacles.getObsY() , obstacles.getObsX() + 28, obstacles.getObsY() + 31, linePaint);
//                    break;
//
//                case "South":
//                    //Yellow: Down
////                    obstacleList.get(i).setObsPaint(northPaint);
////                    MainActivity.printMessage("FACE,S");
//                    //Bottom = "South"
//                    canvas.drawLine(obstacles.getObsX(), obstacles.getObsY() + 29, obstacles.getObsX() + 31, obstacles.getObsY() + 29, linePaint);
//                    break;
//
//                case "North":
//                    //Blue: Front
////                    obstacleList.get(i).setObsPaint(southPaint);
////                    MainActivity.printMessage("FACE,N");
//                    //Top = "North"
//                    canvas.drawLine(obstacles.getObsX(), obstacles.getObsY() + 2.2f, obstacles.getObsX() + 31, obstacles.getObsY() + 2.2f, linePaint);
//                    break;
//
//                default:
//                    //Black
//
//                    break;
//
//            }
//        }
//        canUpdateObsFace = false;
//    }

    public void getStartX(){

    }
    public void getStartY(){

    }
    public void getEndX(){

    }
    public void getEnd(){

    }
    //Find coordinates of cell in arena
    public static int[] findGridOnTouch(float x, float y) {
        int row = -1, cols = -1;
        //FIND COLS OF THE MAZE BASED ON ONTOUCH
        for (int i = 0; i < mCols; i++) {
            if (cells[i][0].endX >= (x - hMargin) && cells[i][0].startX <= (x - hMargin)) {
                cols = i;
                Log.d(TAG, "SDATA startX = " + cells[i][0].startX);
                Log.d(TAG, "SDATA endX = " + cells[i][0].endX);
                Log.d(TAG, "SDATA cols = " + cols);
                Log.d(TAG, "hMargin = " + hMargin);
                Log.d(TAG, "x = " + x);
                Log.d(TAG, "hMargin = " + (x - hMargin));
                break;
            }
        }
        //FIND ROW OF THE MAZE BASED ON ONTOUCH
        for (int j = 0; j < mRows; j++) {
            if (cells[0][j].endY >= (y - vMargin) && cells[0][j].startY <= (y - vMargin)) {
                row = j;
                Log.d(TAG, "SDATA startY = " + cells[0][j].startY);
                Log.d(TAG, "SDATA endY = " + cells[0][j].endY);
                Log.d(TAG, "SDATA row = " + row);
                Log.d(TAG, "hMargin = " + vMargin);
                Log.d(TAG, "y = " + y);
                Log.d(TAG, "hMargin = " + (y - vMargin));
                break;
            }
        }
        return new int[]{cols, row};
    }



    //Get current robot Coordinates
    public int[] getCurCoord(){
         return curCoord;
    }

    public void setRobotDirection(String direction){
         Log.d(TAG,"setRobotDirection");
         if(direction.equals("0")){
             robotDirection = "north";
         } else if (direction.equals("90")){
             robotDirection = "east";
         } else if (direction.equals("180")) {
             robotDirection = "south";
         } else if (direction.equals("270")){
             robotDirection = "west";
         }
        Log.d(TAG,robotDirection);
    }

    public String getRobotDirection(){
        return robotDirection;
    }

    //Allow user to set Robot position
    public void setStartingPoint(boolean status){
         canDrawRobot = true;
        setRobotPostition = status;
    }

//    //Ensuring that the number of obstacles does not go beyondOn 5
//    public void addObstacles(boolean status){
//         Log.d(TAG,"addObstacles enter");
//         if(obstacleList.size() == maxObs){
//             setObstaclePosition = false;
//         } else {
//             setObstaclePosition = status;
//             //addDroppedObstacle();
//         }
//    }

    // Move robot upon robot position
    public void moveRobot(int x, int y, String direction) {
        Log.d(TAG,"Moving robot");
//        setValidPosition(false);

        String backupDirection = robotDirection;
        int oldCoord[]= this.getCurCoord();

        Log.d(TAG, "onMoveRobot: Old coordinates are " + oldCoord[0] + "," + oldCoord[1]);

        if((oldCoord[0] == -1) && (oldCoord[1] == -1)){
            //Set initial coordinates as old Coordinate
            if(((x != 0 && x != 19) && (y != 0 && y != 19))) {
                //If robot not drawn yet, draw it.
                setCurCoord(x, y);
                setRobotDirection(direction);
                setStartingPoint(true);
            } else {
                Toast.makeText(getContext(),"Area out of bounce!",Toast.LENGTH_SHORT).show();
            }
        } else {
            setOldRobotCoord(oldCoord[0], oldCoord[1]);
            if ((x != 0 && x != 19) && (y != 0 && y != 19)) {
                //Set new coordinates as current coordinates
                setCurCoord(x, y);
                setRobotDirection(direction);
            } else {
                Toast.makeText(getContext(),"Area out of bounce!",Toast.LENGTH_SHORT).show();
                setCurCoord(oldCoord[0], oldCoord[1]);
                setRobotDirection(backupDirection);
            }
        }
        invalidate();
    }

    //Move robot upon WASD
    public void moveRobot(String movement){
        Log.d(TAG,"Entering moveRobot");
        setValidPosition(false);

        int[] oldCoord = this.getCurCoord();
        String currDirection = getRobotDirection();
        String backupDirection = getRobotDirection();

        int x = oldCoord[0];
        int y = oldCoord[1];

        Log.d(TAG, "onMoveRobot: Current coordinates => " + oldCoord[0] + "," + oldCoord[1]);
        Log.d(TAG,"onMoveRobot: Current Robot direction => " + currDirection);

//        Robot movement depends on the arrow/direction of the robot.
        switch (currDirection) {
            case "north":
                //Ensure that center of the body is within this area
                if((x != 0 && x != 19) && (y != 0 && y != 19)){
                    validPosition = true;
                }
                switch (movement) {
                    case "w": //"forward"
                        if (curCoord[1] != 1) {
                            curCoord[1] -= 1;
                            validPosition = true;
                            Toast.makeText(getContext(), "Robot: Moving Forward", Toast.LENGTH_LONG).show();
                        } else {
                            setValidPosition(false);
                        }
                        break;
                    case "d": //"right"
                        robotDirection = "east";
                        Toast.makeText(getContext(), "Robot: Turning Right", Toast.LENGTH_LONG).show();
                        break;
                    case "s": //"back"
                        if (curCoord[1] != 18) {
                            curCoord[1] += 1;
                            validPosition = true;
                        } else {
                            setValidPosition(false);
                        }
                        break;
                    case "a": //"left"
                        robotDirection = "west";
                        Toast.makeText(getContext(), "Robot: Turning Left", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        robotDirection = "error up";
                        Toast.makeText(getContext(), "Robot: Reverse", Toast.LENGTH_LONG).show();
                        break;
                }
                break;
            case "90":
            case "east":
                switch (movement) {
                    case "w":
                        if (curCoord[0] != 18) {
                            curCoord[0] += 1;
                            validPosition = true;
                            Toast.makeText(getContext(), "Robot: Moving Forward", Toast.LENGTH_LONG).show();
                        } else {
                            setValidPosition(false);
                        }
                        break;
                    case "d":
                        robotDirection = "south";
                        Toast.makeText(getContext(), "Robot: Turning Right", Toast.LENGTH_LONG).show();
                        break;
                    case "s":
                        if (curCoord[0] != 1) {
                            curCoord[0] -= 1;
                            validPosition = true;
                            Toast.makeText(getContext(), "Robot: Reverse", Toast.LENGTH_LONG).show();
                        } else {
                            setValidPosition(false);
                        }
                        break;
                    case "a":
                        robotDirection = "north";
                        Toast.makeText(getContext(), "Robot: Turning Left", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        robotDirection = "error right";
                }
                break;
            case "180":
            case "south":
                switch (movement) {
                    case "w":
                        if (curCoord[1] != 18) {
                            curCoord[1] += 1;
                            validPosition = true;
                            Toast.makeText(getContext(), "Robot: Moving Forward", Toast.LENGTH_LONG).show();
                        } else {
                            setValidPosition(false);
                        }
                        break;
                    case "d":
                        robotDirection = "west";
                        Toast.makeText(getContext(), "Robot: Turning Right", Toast.LENGTH_LONG).show();
                        break;
                    case "s":
                        if (curCoord[1] != 1) {
                            curCoord[1] -= 1;
                            validPosition = true;
                            Toast.makeText(getContext(), "Robot: Reverse", Toast.LENGTH_LONG).show();
                        } else {
                            setValidPosition(false);
                        }
                        break;
                    case "a":
                        robotDirection = "east";
                        Toast.makeText(getContext(), "Robot: Turning Left", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        robotDirection = "error down";
                }
                break;
            case "270":
            case "west":
                switch (movement) {
                    case "w":
                        if (curCoord[0] != 1) {
                            curCoord[0] -= 1;
                            validPosition = true;
                            Toast.makeText(getContext(), "Robot: Moving Forward", Toast.LENGTH_LONG).show();
                        } else {
                            setValidPosition(false);
                        }
                        break;
                    case "d":
                        robotDirection = "north";
                        Toast.makeText(getContext(), "Robot: Turning Right", Toast.LENGTH_LONG).show();
                        break;
                    case "s":
                        if (curCoord[0] != 18) {
                            curCoord[0] += 1;
                            validPosition = true;
                            Toast.makeText(getContext(), "Robot: Reverse", Toast.LENGTH_LONG).show();
                        } else {
                            setValidPosition(false);
                        }
                        break;
                    case "a":
                        robotDirection = "south";
                        Toast.makeText(getContext(), "Robot: Turning Left", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        robotDirection = "error left";
                }
                break;
            default:
                robotDirection = "error moveCurCoord";
                break;
        }

        if (getValidPosition()){
            Log.d(TAG, String.valueOf(getValidPosition()));
            Log.d(TAG,"onMoveRobot: Curr Coord is "+ curCoord[0] + "," + curCoord[1]);
            setCurCoord(curCoord[0], curCoord[1]);
            setOldRobotCoord(x,y);
        } else {
            if (movement.equals("w") || movement.equals("s")){
                robotDirection = backupDirection;
                setCurCoord(oldCoord[0], oldCoord[1]);
            }
            Log.d(TAG, "onMoveRobot: Old coordinates are " + oldCoord[0] + "," + oldCoord[1]);
        }
        this.invalidate();
        Log.d(TAG,"Robot has been moved");
    }

    public void setCurCoord(int col, int row) {
        Log.d(TAG,"Entering setCurCoord");
        curCoord[0] = col;
        curCoord[1] = row;

        Log.d(TAG, col + "," + row);

        for (int x = col - 1; x <= col + 1; x++)
            for (int y = curCoord[1] - 1; y <= curCoord[1] + 1; y++)
                cells[x][y].setType("robot");
        Log.d(TAG,"Exiting setCurCoord");
    }

    private void setOldRobotCoord(int oldCol, int oldRow) {
        Log.d(TAG,"Entering setOldRobotCoord");
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;

        Log.d(TAG, oldCol + "," + oldRow);

        //oldRow = this.inverseCoordinates(oldRow);
        for (int x = oldCoord[0] - 1; x <= oldCoord[0] + 1; x++){
            for (int y = oldCoord[1] - 1; y <= oldCoord[1] + 1; y++){
                cells[x][y].setType("explored");
            }
        }
        Log.d(TAG,"Exiting setOldRobotCoord");
    }

    private int[] getOldRobotCoord() {
        return oldCoord;
    }

    private void setValidPosition(boolean status) {
        validPosition = status;
    }

    private boolean getValidPosition() {
        return validPosition;
    }

    public boolean getCanDrawRobot() {
         return canDrawRobot;
    }


    private class Cell {
        float startX, startY, endX, endY;
        Paint paint;
        String type;

        private Cell(float startX, float startY, float endX, float endY, Paint paint){
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
        }

        public void setPaint(Paint paint){
            this.paint = paint;
        }

        public void setType(String type) {
            this.type = type;
            switch (type) {
                case "obstacle":
                    this.paint = obstaclePaint;
                    break;
                case "robot":
                    this.paint = robotPaint;
                    break;
                case "unexplored":
                    this.paint = unexploredPaint;
                    break;
                case "explored":
                    this.paint = exploredPaint;
                    break;
                case "arrow":
                    this.paint = directionPaint;
                    break;
                case "id":
                    this.paint = obstacleNumberPaint;
                    break;
                default:
                    Log.d(TAG,"setTtype default: " + type);
                    break;
            }
        }
    }

    public class LongPressGestureListener extends GestureDetector.SimpleOnGestureListener {

        public LongPressGestureListener(View arenaMap) {
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            Log.d("TAG","onLongPress: LONG PRESS!");

            int x = (int) e.getX();
            int y = (int) e.getY();

            //Increase counts for annotation
            for (Obstacle obstacles : obstacleList) {
                //Check if it obstacle in touched.
                if (obstacles.isTouched(x, y)) {
                    obstacles.setActionDown(false);
                    if(obstacles.getLongPress()){
                        Toast.makeText(getContext(), "Face annotation disabled", Toast.LENGTH_LONG).show();
                        obstacles.setLongPress(false);
                    } else {
                        Toast.makeText(getContext(), "Face annotation enabled", Toast.LENGTH_LONG).show();
                        obstacles.setLongPress(true);
                    }
                }
            }
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

    public void setObstacleFace(){
         Log.d(TAG,"setObstacleFace");
        for (Obstacle obstacles : obstacleList) {
            //Check if obstacle in touched.
            if(obstacles.getLongPress()){
                if(obstacles.getTouchCount() >= 5){
                    obstacles.resetTouchCount();
                    obstacles.setObsFace(obstacles.getTouchCount());
                } else {
                    obstacles.incrTouchCount();
                    obstacles.setObsFace(obstacles.getTouchCount());
                }
            }
        }
        invalidate();
    }

}