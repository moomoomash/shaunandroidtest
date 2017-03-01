package com.example.shaun.shaunandroidtest;

import android.content.Intent;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    RobotControl rc = new RobotControl();

    //public BluetoothService btServ = BluetoothUI.BLUETOOTH_SERVICE;
    private Button left;
    private Button right;
    private Button forward;
    private Button backward;
    public String control="n";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        setupControl();
        BluetoothUI BTui = new BluetoothUI();
//        if (savedInstanceState == null) {
//            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//            BluetoothUI fragment = new BluetoothUI();
//            transaction.replace(R.id.activity_controller, fragment);
//            transaction.commit();
//        }
    }

    public String getControl(){
        return control;
        //ctrlMsg(control);
    }

    public void setupControl(){
        // Turn left
        left = (Button) findViewById(R.id.ButtonLeft);
        left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //String msg = "a";
                //sendMessage(msg);
                //outputFeedback("MovingL");
                control="a";
                sendControl(control);
                //endControl(control);
            }
        });

        // Turn Right
        right = (Button) findViewById(R.id.ButtonRight);
        right.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                control="d";
                sendControl(control);
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
                sendControl(control);
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
                sendControl(control);
                //String msg = "w";
                //sendMessage(msg);
                //("MovingF");
                //sendControl(control);

            }

        });

    }

    public void sendControl(String input){
        if (input.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            //this.write(send);
            btServ.write(input.getBytes());
            btServ.write("lolll".getBytes());
        }
    }

    public void startBTServ(View view){
        //BluetoothService BTServ = new BluetoothService();
//        Intent intent = new Intent(this, BluetoothService.class);
//        startActivity(intent);
//        BTui.start()
        Intent intent = new Intent(this, BluetoothService.class);
        startActivity(intent);
    }
}
