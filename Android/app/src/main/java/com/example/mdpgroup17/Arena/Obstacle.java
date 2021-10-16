package com.example.mdpgroup17.Arena;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class Obstacle  {
    int x, y;
    int initX, initY;
    int xArena = -1, yArena = -1;
    int offset;
    float obsOffset1 = 3f;
    int obsOffset2 = 28;
    int touchCount = 0;
    int aObsX, aObsY;
    boolean actionDown = false;
    boolean longPress = false;
    String obsFace = "None";
    String obsID, targetID;

    public Obstacle(int x, int y, int initX, int initY, String obsID, int touchCount, String obsFace, String targetID){
        this.x = x;
        this.y = y;
        this.touchCount = touchCount;
        this.obsFace = obsFace;
        this.obsID = obsID;
        this.targetID = targetID;
        this.initX = initX;
        this.initY = initY;
    }

    public int getObsX(){
        return  x;
    }

    public int getObsY(){
        return  y;
    }

    public int getaObsX(){
        return  aObsX;
    }

    public int getaObsY(){
        return  aObsY;
    }

    public void setaObsX(int aObsX){
        this.aObsX = aObsX;
    }

    public void setaObsY(int aObsY){
        this.aObsY = aObsY;
    }

    public int [] getInitCoords () {
        System.out.println(initX);
        System.out.println(initY);
        return new int [] {initX,initY};}

    public String getObsFace(){
        return obsFace;
    }

    public String getObsID(){
        return obsID;
    }

    public int getTouchCount(){
        return touchCount;
    }

    public int incrTouchCount(){
        touchCount++;
        return touchCount;
    }

    public void setObsX(int x){
        this.x = x;
    }

    public void setObsY(int y){
        this.y = y;
    }

    public void setInitX(int initX){
        this.initX = initX;
    }

    public void setInitY(int initY){
        this.initY = initY;
    }

    public void setInitCoords (int initX, int initY) {
        this.initX = initX;
        this.initY = initY;
    }

    public String setObsFace(int touchCount){
        switch (touchCount){
            case 1:
                //Green: Left (West)
                obsFace = "North";
                break;
            case 2:
                //Red: Right
                obsFace = "East";
                break;
            case 3:
                //Yellow: Down
                obsFace = "South";
                break;
            case 4:
                //Blue: Front
                obsFace = "West";
                break;
            default:
                //Black
                obsFace = " ";
        }
        this.obsFace = obsFace;
        return obsFace;
    }

    public void drawObsFace(Canvas canvas, int touchCount, Paint paint){
        switch (touchCount){
            case 1:
                //Green: Left (West)
                obsFace = "North";
                canvas.drawLine(x, y + obsOffset1, x + offset, y + obsOffset1, paint);
                break;

            case 2:
                //Red: Right
                obsFace = "East";
                canvas.drawLine(x + obsOffset2, y , x + obsOffset2, y + offset, paint);
                break;
            case 3:
                //Yellow: Down
                obsFace = "South";
                canvas.drawLine(x, y + obsOffset2, x + offset, y + obsOffset2, paint);
                break;
            case 4:
                //Blue: Front
                obsFace = "West";
                canvas.drawLine(x + obsOffset1, y , x + obsOffset1, y + offset, paint);
                break;
            default:
                //Black
                obsFace = " ";
        }
    }
//
//    public String setObsID(String obsID){
//        this.obsID = obsID;
//        return obsID;
//    }

    //Setting action down status
    public void setActionDown(boolean status){
        //When touched down
        this.actionDown = status;
    }

    //Getting action down status
    public boolean getActionDown(){
        return actionDown;
    }

    //Setting long press status
    public void setLongPress(boolean status){
        //When touched down
        this.longPress = status;
    }

    //Getting long press status
    public boolean getLongPress(){
        return longPress;
    }

    //To set new position of draggable object
    public void setPosition(int x, int y){
        this.x = x - 70;
        this.y = y - 70;
    }

    //Use this for draggable object
    public boolean isTouched(int x, int y){
//        Log.d("isTouched", x + ", " + y);
//        Log.d("isTouched", this.x + ", " + this.y);

        boolean xIsInside = x > this.x && x < this.x + 100;
        boolean yIsInside = y > this.y && y < this.y + 100;

//        Log.d("isTouched", xIsInside + ", " + yIsInside);

        return xIsInside && yIsInside;
    }

    public void setObsMapCoord (int xArena, int yArena){
        this.xArena = xArena;
        this.yArena = yArena;
    }

    public int[] getObsMapCoord (){
        return new int[]{xArena, yArena};
    }

    public int resetTouchCount() {
        touchCount = 1;
        return touchCount;
    }

//    public Paint getObsPaint() {
//        return obsPaint;
//    }

    //Setting the obstacle's paint settings
//    public Paint setObsPaint(Paint obsPaint) {
//        this.obsPaint = obsPaint;
//        return obsPaint;
//    }
//
    public void setTouchCount(int touchCount) {
        this.touchCount = touchCount;
    }

    public String setTargetID(String targetID) {
        this.targetID = targetID;
        return targetID;
    }

    public String getTargetID() {
        return targetID;
    }

    public void drawObj(Canvas canvas){
        Log.d("Obstacle", "Drawing Object");
        Paint obstaclePaint = new Paint();
        obstaclePaint.setColor(Color.BLACK);
        obstaclePaint.setStyle(Paint.Style.FILL);
        obstaclePaint.setStrokeWidth(3f);

        Log.d("Drawing Object:", "Coordinates: " + x + "," + y);

        canvas.drawRect(x,y,x+offset,y+offset, obstaclePaint);
    }

    public void setResizeUp(boolean status){
        if(status == true){
            offset = 70;
        } else {
            offset = 31;
        }

    }

    public void setFaceResizeUp(boolean status){
        if(status == true){
            offset = 100;
        } else {
            offset = 31;
        }

    }



}
