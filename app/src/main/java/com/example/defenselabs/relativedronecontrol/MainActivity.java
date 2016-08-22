package com.example.defenselabs.relativedronecontrol;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener{

    ImageView frontIV , topIV;
    Bitmap bitmap_front, bitmap_top;
    double init_lat, init_long, init_height;
    double curr_lat, curr_long, curr_height;
    Button b1, btn_view;
    Socket socket = null;
    int max_horizontal_angle = 70, max_vertical_angle = 45, max_depth = 200;
    boolean first_location = true;
    int frontIV_width = 1199, frontIV_height = 720;
    int topIV_width = 1199, topIV_height = 720;
    Bitmap newBitmap;
    float aa = 400, bb = 400 , cc=400;
    GestureDetector gdt;
    PrintStream PS;
    byte send_array[], input_array[];
    OutputStream out;
    String dstAddress;
    int dstPort;
    boolean top_view =false, front_view = true;
    EditText editTextAddress, editTextPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frontIV = (ImageView) findViewById(R.id.frontIV);
        topIV = (ImageView) findViewById(R.id.topIV);
        b1 = (Button) findViewById(R.id.btn1);
        btn_view = (Button) findViewById(R.id.btn_view);
        gdt = new GestureDetector(new GestureListenerFront());
        editTextAddress = (EditText)findViewById(R.id.address);
        editTextAddress.setText("192.168.1.6");
        editTextPort = (EditText)findViewById(R.id.port);
        editTextPort.setText("4444");
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SendReceiveAsyncTask obj = new SendReceiveAsyncTask(editTextAddress.getText().toString(),Integer.parseInt(editTextPort.getText().toString()));
                obj.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                frontIV.setOnTouchListener(MainActivity.this);
                b1.setVisibility(View.GONE);
            }
        });
        btn_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(front_view) {
                    btn_view.setText("TOP");
                    front_view = false;
                    top_view = true;
                    frontIV.setVisibility(View.GONE);
                    topIV.setVisibility(View.VISIBLE);

                }
                else
                {
                    btn_view.setText("FRONT");
                    front_view = true;
                    top_view = false;
                    frontIV.setVisibility(View.VISIBLE);
                    topIV.setVisibility(View.GONE);

                }
            }
        });

    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        frontIV_width = frontIV.getWidth();
        frontIV_height = frontIV.getHeight();

        topIV_width = topIV.getWidth();
        topIV_height = topIV.getHeight();


        bitmap_front = Bitmap.createBitmap((int) getWindowManager()
                .getDefaultDisplay().getWidth(), (int) getWindowManager()
                .getDefaultDisplay().getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas_front = new Canvas(bitmap_front);

        Paint paint = new Paint();
        paint.setColor(Color.rgb(140, 0, 0));
        paint.setAlpha(80);
        paint.setStrokeWidth(2);
        Log.i("TAG", "width : " + frontIV_width + " Height : " + frontIV_height);
        canvas_front.drawLine(0, (float) (frontIV_height * 0.9), frontIV_width , (float) (frontIV_height * 0.9), paint);
        canvas_front.drawLine(frontIV_width / 2, 0, frontIV_width / 2, frontIV_height, paint);
        paint.setTextSize(26);
        canvas_front.drawText("West", 20, (float) (frontIV_height * 0.9)-5 , paint);
        canvas_front.drawText("East", frontIV_width - 75, (float) (frontIV_height * 0.9)-5, paint);
        canvas_front.save();
        canvas_front.rotate((float)  270 , frontIV_width/2 -10, 100);
        canvas_front.drawText("Height",frontIV_width/2 -10 ,100, paint);
        canvas_front.restore();
        frontIV.setImageBitmap(bitmap_front);

        bitmap_top = Bitmap.createBitmap((int) getWindowManager()
                .getDefaultDisplay().getWidth(), (int) getWindowManager()
                .getDefaultDisplay().getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas_top = new Canvas(bitmap_top);

        Paint paint_top = new Paint();
        paint_top.setColor(Color.rgb(0, 140, 0));
        paint_top.setAlpha(80);
        paint_top.setStrokeWidth(2);
        Log.i("TAG", "width : " + topIV_width + " Height : " +topIV_height);
        canvas_top.drawLine(0, (float) (topIV_height * 0.8), topIV_width, (float) (topIV_height * 0.8), paint_top);
        canvas_top.drawLine(topIV_width / 2, 0, topIV_width / 2, topIV_height, paint_top);

        paint_top.setTextSize(26);
        canvas_top.drawText("West", 20, (float) (topIV_height * 0.8)-5 , paint_top);
        canvas_top.drawText("East", topIV_width - 75, (float) (topIV_height * 0.8)-5, paint_top);
        canvas_top.save();
        canvas_top.rotate((float)  270 ,topIV_width/2 -10, 100);
        canvas_top.drawText("North",topIV_width/2 -10 ,100, paint);
        canvas_top.restore();
        topIV.setImageBitmap(bitmap_top);
        topIV.setVisibility(View.GONE);
    }

    public void drawDroneFront(float horiz_disp, float vert_disp) {
        aa = horiz_disp;
        bb = vert_disp;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                newBitmap = Bitmap.createBitmap(bitmap_front);
                Canvas canvas = new Canvas(newBitmap);
                Paint paint = new Paint();
                paint.setColor(Color.rgb(255, 0, 0));
                canvas.drawCircle(aa, bb, 20, paint);
                frontIV.setImageBitmap(newBitmap);
            }
        });

    }

    public void drawDroneTop(float horiz_disp, float depth_disp) {
        aa = horiz_disp;
        cc = depth_disp;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                newBitmap = Bitmap.createBitmap(bitmap_top);
                Canvas canvas = new Canvas(newBitmap);
                Paint paint = new Paint();
                paint.setColor(Color.rgb(0, 255, 0));
                canvas.drawCircle(aa, cc, 20, paint);
                topIV.setImageBitmap(newBitmap);
            }
        });

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        gdt.onTouchEvent(motionEvent);
        return true;
    }

    private class GestureListenerFront extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            float xPer = (event.getX() - (frontIV_width / 2)) / (frontIV_width / 2);
            float yPer = (float) (((frontIV_height * 0.9) - event.getY()) / (frontIV_height * 0.9));


            return true;
        }
    }


    public class SendReceiveAsyncTask extends AsyncTask<Void, Void, Void> {

        String dstAddress, cmd;
        int dstPort;

        SendReceiveAsyncTask(String addr, int port) {
            dstAddress = addr;
            dstPort = port;
            //this.cmd = cmd;
        }


        @Override
        protected Void doInBackground(Void... voids) {

            try {
                socket = new Socket(dstAddress, dstPort);
                InputStream in = socket.getInputStream();
                out = socket.getOutputStream();
                PS = new PrintStream(socket.getOutputStream());
                BufferedReader is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PS.println("SEND");
                while (true) {
                    // String message = is.readLine();
                    send_array = new byte[1024];
                    input_array = new byte[1024];


                    IOUtils.read(in, input_array);
                    String message = new String(input_array, "UTF-8");

                    if (first_location) {
                        first_location = false;
                        drawDroneFront(frontIV_width / 2, (float) (frontIV_height * 0.9));
                        drawDroneTop(topIV_width / 2, (float) (topIV_height * 0.8));
                        String a[] = message.split(":");
                        init_lat = Double.parseDouble(a[1]);
                        init_long = Double.parseDouble(a[3]);
                        init_height = Double.parseDouble(a[5]);



                    } else {
                        String a[] = message.split(":");
                        curr_lat = Double.parseDouble(a[1]);
                        curr_long = Double.parseDouble(a[3]);
                        curr_height = Double.parseDouble(a[5]);
                        if(curr_lat==init_lat && curr_long==init_long && curr_height==init_height) {
                            //drawDroneFront(frontIV_width / 2, (float) (frontIV_height * 0.9));
                        }
                        else {
                            drawDroneFront(get_horizontal_disp(curr_lat, curr_long), get_vertical_disp(curr_lat, curr_height));
                            drawDroneTop(get_horizontal_disp(curr_lat, curr_long), get_depth_disp(curr_lat));

                        }

                        Log.i("TAG RECEIVING","Latitude:" + curr_lat + ":Longitude:" + curr_long + ":Height:" + curr_height + ":\n");
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // PS.println("SEND");
                    }
                    System.arraycopy(toByteArray("SEND"),0,send_array,0,4);
                    out.write(send_array,0,send_array.length);

                }


            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

    }
}
