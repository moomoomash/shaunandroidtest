package com.example.shaun.shaunandroidtest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by Hypnotic on 28/2/2017.
 */

public class RobotControl extends AppCompatActivity{
    private Button left;
    private Button right;
    private Button forward;
    private Button backward;
    public String control="n";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

// Turn left
        left = (Button) findViewById(R.id.ButtonLeft);
        left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //String msg = "a";
                //sendMessage(msg);
                //outputFeedback("MovingL");
                control="a";
                //endControl(control);
            }
        });

        // Turn Right
        right = (Button) findViewById(R.id.ButtonRight);
        right.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                control="d";
                //sendControl(control);
                //String msg = "d";
                //sendMessage(msg);
                //outputFeedback("MovingR");

            }
        });

        // Turn Back
        backward = (Button) findViewById(R.id.ButtonDown);

        backward.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                control="s";
                //sendControl(control);
                //String msg = "s";
                //sendMessage(msg);
                //outputFeedback("MovingB");
                // autoSend = false;
            }
        });

        // Go Forward
        forward = (Button) findViewById(R.id.ButtonUP);
        forward.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                control="w";
                //String msg = "w";
                //sendMessage(msg);
                //("MovingF");
                //sendControl(control);

            }

        });

    }

  public String getControl(){
        return control;
        //ctrlMsg(control);
    }

//    public void ctrlMsg(String msg) {
//        // Check that there's actually something to send
//        //Intent intent = new Intent(this, DisplayMessageActivity.class);
//        byte[] send = null;
//        String message = msg;
//        if (message.length() > 0) {
//            // Get the message bytes and tell the BluetoothChatService to write
//            send = message.getBytes();
//            MainActivity.write(send);
//            //mChatService.write(send);
//        }
//    }
}
