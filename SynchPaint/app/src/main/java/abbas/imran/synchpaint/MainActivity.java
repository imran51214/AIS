package abbas.imran.synchpaint;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import java.util.UUID;
import android.view.View.OnClickListener;
import android.provider.MediaStore;

public class MainActivity extends AppCompatActivity implements OnClickListener {


    public static final String TAG = "SynchPaint";


    //Drawing
    private DrawingView drawView;
    private ImageButton currPaint, drawBtn,eraseBtn, newBtn, saveBtn;
    public float smallBrush, mediumBrush, largeBrush;

    public UMTDirect umtDirect;

    Handler handler;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        handler=new Handler(){
            @Override
            public void handleMessage(Message msg) {

                drawView.parseJSON(msg.getData().getString("msg"));
            }
        };


        umtDirect=new UMTDirect(MainActivity.this,handler);




        //Drawing
        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);

        drawBtn = (ImageButton)findViewById(R.id.draw_btn);
        drawBtn.setOnClickListener(this);

        drawView = (DrawingView)findViewById(R.id.drawing);
        drawView.setMainActivity(MainActivity.this);

        LinearLayout paintLayout = (LinearLayout)findViewById(R.id.paint_colors);
        currPaint = (ImageButton)paintLayout.getChildAt(0);

        currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));

        drawView.setBrushSize(mediumBrush);
        drawView.setCurrentBrush("mediumBrush");

        eraseBtn = (ImageButton)findViewById(R.id.erase_btn);
        eraseBtn.setOnClickListener(this);

        newBtn = (ImageButton)findViewById(R.id.new_btn);
        newBtn.setOnClickListener(this);

        saveBtn = (ImageButton)findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(this);

        //Drawing



        umtDirect.discoverPeers(new UMTCallback() {
            @Override
            public void response(boolean cmdStatus, int failureReason) {
                if (cmdStatus) {
                    Toast.makeText(MainActivity.this, "Peer Discovery Initiated",Toast.LENGTH_SHORT).show();
                }
                else {

                    Toast.makeText(MainActivity.this, "Peer Discovery Failed : " + failureReason, Toast.LENGTH_SHORT).show();




                }


            }
        });







    }




//Drawing
    public void paintClicked(View view) {
        if(view!=currPaint){

            drawView.setBrushSize(drawView.getLastBrushSize());

            //update color
            ImageButton imgView = (ImageButton)view;
            String color = view.getTag().toString();
            drawView.setColor(color);
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currPaint=(ImageButton)view;


        }

    }


    @Override
    public void onClick(View view){
        //respond to clicks
        if(view.getId()==R.id.draw_btn){
            //draw button clicked
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Brush size:");

            brushDialog.setContentView(R.layout.brush_chooser);

            ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setBrushSize(smallBrush);
                    drawView.setCurrentBrush("smallBrush");
                    drawView.setLastBrushSize(smallBrush);
                    drawView.setErase(false);
                    brushDialog.dismiss();
                }
            });

            ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setBrushSize(mediumBrush);
                    drawView.setCurrentBrush("mediumBrush");
                    drawView.setLastBrushSize(mediumBrush);
                    drawView.setErase(false);
                    brushDialog.dismiss();
                }
            });

            ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setBrushSize(largeBrush);
                    drawView.setCurrentBrush("largeBrush");
                    drawView.setLastBrushSize(largeBrush);
                    drawView.setErase(false);
                    brushDialog.dismiss();
                }
            });

            brushDialog.show();



        }
        else if(view.getId()==R.id.erase_btn){
            //switch to erase - choose size

            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Eraser size:");
            brushDialog.setContentView(R.layout.brush_chooser);

            ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(smallBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(mediumBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(largeBrush);
                    brushDialog.dismiss();
                }
            });

            brushDialog.show();


        }
        else if(view.getId()==R.id.new_btn){
            //new button
            AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
            newDialog.setTitle("New drawing");
            newDialog.setMessage("Start new drawing (you will lose the current drawing)?");
            newDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    drawView.startNew();
                    dialog.dismiss();
                }
            });
            newDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            newDialog.show();


        }
        else if(view.getId()==R.id.save_btn){
            //save drawing
            AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
            saveDialog.setTitle("Save drawing");
            saveDialog.setMessage("Save drawing to device Gallery?");
            saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    //save drawing
                    drawView.setDrawingCacheEnabled(true);
                    String imgSaved = MediaStore.Images.Media.insertImage(
                            getContentResolver(), drawView.getDrawingCache(),
                            UUID.randomUUID().toString()+".png", "drawing");

                    if(imgSaved!=null){
                        Toast savedToast = Toast.makeText(getApplicationContext(),
                                "Drawing saved to Gallery!", Toast.LENGTH_SHORT);
                        savedToast.show();
                    }
                    else{
                        Toast unsavedToast = Toast.makeText(getApplicationContext(),
                                "Oops! Image could not be saved.", Toast.LENGTH_SHORT);
                        unsavedToast.show();
                    }

                    drawView.destroyDrawingCache();

                }
            });
            saveDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            saveDialog.show();

        }



    }



    public void btnDiscover(View view) {
        umtDirect.connectToService("SynchPaint", new UMTCallback() {
            @Override
            public void response(boolean cmdStatus, int failureReason) {
                if (cmdStatus) {
                    Toast.makeText(MainActivity.this, "Connected with the service", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(MainActivity.this, "Service discovery failed", Toast.LENGTH_SHORT).show();

                }

            }
        });
    }



    public void btnRegister(View view) {


        umtDirect.broadcastService("SynchPaint",4545, new UMTCallback() {
            @Override
            public void response(boolean cmdStatus, int failureReason) {

                if (cmdStatus) {
                    Toast.makeText(MainActivity.this, "Service Registered", Toast.LENGTH_SHORT).show();

                    umtDirect.createGroup(new UMTCallback() {
                        @Override
                        public void response(boolean cmdStatus, int failureReason) {

                            if (cmdStatus){
                                Toast.makeText(MainActivity.this, "Group Created Successfully", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });


                } else {

                    Toast.makeText(MainActivity.this, "Service Not Registered", Toast.LENGTH_SHORT).show();

                }

            }
        });


    }



    public void btnDisconnect(View view) {

    umtDirect.disconnectWithService(new UMTCallback() {
        @Override
        public void response(boolean cmdStatus, int failureReason) {
            if (cmdStatus) {
                Toast.makeText(MainActivity.this, "Disconnected with service", Toast.LENGTH_SHORT).show();
            } else {

                Toast.makeText(MainActivity.this, "Disconnection Failed", Toast.LENGTH_SHORT).show();

            }

        }
    });


    }



    @Override
    protected void onDestroy() {

        /*
       if (umtDirect.isServiceHost()){

            // Remove Service

            umtDirect.removeService(new UMTCallback() {
                @Override
                public void response(boolean cmdStatus, int failureReason) {
                    if (cmdStatus){

                        Toast.makeText(MainActivity.this, "Service removed successfully", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Failed to Remove a service", Toast.LENGTH_SHORT).show();

                    }

                }
            });

            // Remove Service


       }
       */

        umtDirect.teardown();
        super.onDestroy();

    }


    @Override
    public void onBackPressed() {

        Toast.makeText(MainActivity.this, "Synch Paint Closed", Toast.LENGTH_SHORT).show();
        this.finish();

    }


}
