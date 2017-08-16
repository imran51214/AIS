package abbas.imran.synchpaint;

import android.graphics.Color;
import android.graphics.PointF;
import android.view.View;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.TypedValue;
import android.widget.Toast;

import java.text.DecimalFormat;


import java.util.ArrayList;
import java.util.List;


/**
 * Created by imran on 07-May-17.
 */

//View class in which the drawing will take place
public class DrawingView extends View {

    //drawing path
    private Path drawPath;

    //drawing and canvas paint
    private Paint drawPaint, canvasPaint;
    //initial color
    private int paintColor = 0xFF660000;
    private String paintColorhex="#FF660000";

    //canvas
    private Canvas drawCanvas;
    //canvas bitmap
    private Bitmap canvasBitmap;

    private float brushSize, lastBrushSize;

    private int viewWidth = 0;         // Width of the screen in pixels

    private boolean erase=false;
    private String currentBrush;


    // JSON Objects
    private ArrayList<String> history = new ArrayList<>();
    private ArrayList<PointF> points = new ArrayList<>();


    UMTData umtDataSend;
    UMTData umtDataReceive;



    private MainActivity mActivity;



    public DrawingView(Context context, AttributeSet attrs){
        super(context, attrs);
        setupDrawing();



    }

    public void setMainActivity(MainActivity activity){

        this.mActivity = activity;

    }

    public void setCurrentBrush(String brush)
    {

        currentBrush=brush;
    }



    public void setErase(boolean isErase){
        //set erase true or false
        erase=isErase;

        // Paint object to erase or switch back to drawing
        if(erase) drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        else drawPaint.setXfermode(null);

    }

    public void startNew(){
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }

    private void setupDrawing(){
    //get drawing area setup for interaction

        brushSize = getResources().getInteger(R.integer.medium_size);

        lastBrushSize = brushSize;

        drawPath = new Path();
        drawPaint = new Paint();

        drawPaint.setColor(paintColor);

        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(brushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        canvasPaint = new Paint(Paint.DITHER_FLAG);




    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        // Store view width for later
        viewWidth = w;

        super.onSizeChanged(w, h, oldw, oldh);

        // Create bitmap for the drawing
        canvasBitmap = Bitmap.createBitmap(w, (int) (6 / 5f * w), Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);


        //view given size


      //  canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
      // drawCanvas = new Canvas(canvasBitmap);


    }

    @Override
    protected void onDraw(Canvas canvas) {
    //draw view

            canvas.drawPath(drawPath, drawPaint);
            canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);

    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {


        DecimalFormat df = new DecimalFormat("#.###");

        //detect user touch
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);

                // Create the umtDataSend object to be sent and fill relevant fields
                    umtDataSend = new UMTData();

                    if (!erase){
                        umtDataSend.Put("type","pen");
                        umtDataSend.Put("color",paintColorhex);
                    }
                    else {
                        umtDataSend.Put("type","eraser");
                    }

                    umtDataSend.Put("brushsize", currentBrush);
                // Create the umtDataSend object to be sent and fill relevant fields
                points.add(new PointF(touchX, touchY));

                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);

                // Add point to the arraylist of points in current curve
                points.add(new PointF(touchX, touchY));
                break;

            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();


                //JSONArray coordinates = new JSONArray();
                List<List<String >> coordinates= new ArrayList<List<String>>();

                    for (PointF p : points) {
                        //JSONArray ja = new JSONArray();
                        List<String> ja=new ArrayList();

                        ja.add(df.format(p.x / viewWidth));
                        ja.add(df.format(p.y / viewWidth));
                        coordinates.add(ja);
                    }

                    umtDataSend.Put("coordinates",coordinates);

                    points.clear();

                umtDataSend.Put("timeStamp", mActivity.umtDirect.getTimeStamp().toString());


                String umtDataString = umtDataSend.getDataString();
                mActivity.umtDirect.broadcastMessage(umtDataString);

                /*
                if (mActivity.isConnected || mActivity.registrationIsRunning){
                    //Send JSON String
                    mActivity.umtConnection.synchDoc(jsonString);
                }
                */

              break;
            default:
                return false;
        }

        invalidate();
        return true;

    }


    public void parseJSON(String string) {

        umtDataReceive = new UMTData(string);

        Paint drawPaintReceive;

        drawPaintReceive = new Paint();


        drawPaintReceive.setAntiAlias(true);
        drawPaintReceive.setStrokeWidth(brushSize);
        drawPaintReceive.setStyle(Paint.Style.STROKE);
        drawPaintReceive.setStrokeJoin(Paint.Join.ROUND);
        drawPaintReceive.setStrokeCap(Paint.Cap.ROUND);

        Toast.makeText(mActivity, "Time Stamp" + umtDataReceive.get("timeStamp").toString(), Toast.LENGTH_SHORT).show();

                String type = umtDataReceive.get("type").toString();

                switch (type) {
                    case "pen":
                        // If type pen, extract color
                        String color = umtDataReceive.get("color").toString();

                        // setColor(color);
                        String paintColorhexReceived;
                        int paintColorReceived;

                        invalidate();
                        paintColorhexReceived=color;
                        paintColorReceived = Color.parseColor(color);
                        drawPaintReceive.setColor(paintColorReceived);

                       // setErase(false);
                        drawPaint.setXfermode(null);

                        String brush;
                        brush = umtDataReceive.get("brushsize").toString();

                        float pixelAmount;
                        if (brush.equals("smallBrush")) {

                           // setBrushSize(mActivity.smallBrush);
                            //setLastBrushSize(mActivity.smallBrush);
                            //update size

                             pixelAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                    mActivity.smallBrush, getResources().getDisplayMetrics());



                        } else if (brush.equals("mediumBrush")) {

                            //setBrushSize(mActivity.mediumBrush);
                            //setLastBrushSize(mActivity.mediumBrush);
                            pixelAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                    mActivity.mediumBrush, getResources().getDisplayMetrics());


                        } else {

                            //setBrushSize(mActivity.largeBrush);
                            //setLastBrushSize(mActivity.largeBrush);
                            pixelAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                    mActivity.largeBrush, getResources().getDisplayMetrics());

                        }

                        brushSize=pixelAmount;
                        drawPaintReceive.setStrokeWidth(brushSize);

                        break;

                    case "eraser":
                      //  setErase(true);
                        drawPaintReceive.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

                        break;

                    default:
                }


            if (type.equals("pen") || type.equals("eraser")) {
                List<List<String >> coordinates= new ArrayList<List<String>>();
                coordinates = umtDataReceive.getArray("coordinates");

                List<String> startPoint=new ArrayList();

                startPoint=coordinates.get(0);

                Path drawPath = new Path();
                float touchX = (float) Float.valueOf(startPoint.get(0)) * viewWidth;
                float touchY = (float) Float.valueOf(startPoint.get(1)) * viewWidth;
                drawPath.moveTo(touchX, touchY);

                for (int i = 1; i < coordinates.size(); i++) {
                    List<String> point = coordinates.get(i);
                    float x = (float) Float.valueOf(point.get(0)) * viewWidth;
                    float y = (float) Float.valueOf(point.get(1)) * viewWidth;
                    drawPath.lineTo(x, y);
                }

                drawCanvas.drawPath(drawPath, drawPaintReceive);
                invalidate();
                drawPath.reset();


            }










    }



    public void setColor(String newColor){
        //set color
        invalidate();

        paintColorhex=newColor;
        paintColor = Color.parseColor(newColor);
        drawPaint.setColor(paintColor);

    }

    public void setBrushSize(float newSize){
        //update size
        float pixelAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newSize, getResources().getDisplayMetrics());
        brushSize=pixelAmount;
        drawPaint.setStrokeWidth(brushSize);
    }

    public void setLastBrushSize(float lastSize){
        lastBrushSize=lastSize;
    }
    public float getLastBrushSize(){
        return lastBrushSize;
    }

}
