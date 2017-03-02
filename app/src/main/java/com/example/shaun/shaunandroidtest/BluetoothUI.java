package com.example.shaun.shaunandroidtest;

import com.example.shaun.shaunandroidtest.RobotControl;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;

import static android.app.PendingIntent.getActivity;

public class BluetoothUI extends AppCompatActivity{
    // GUI Components
    public final static String EXTRA_MESSAGE = "com.example.shaun.MESSAGE";
    //BluetoothAdapter mBluetoothAdapter;
    private TextView mBluetoothStatus;
    private ListView mDevicesListView;
    private ListView mChatListView;
    private static final String TAG = "BluetoothService";
    //Array for storing stuff
    private ArrayAdapter<String> mBTArrayAdapter;
    private ArrayAdapter<String> mConversationArrayAdapter;
//    public BluetoothService BTServ = new BluetoothService();
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;
    public interface MessageConstants {
        // Message types sent from the BluetoothChatService Handler
        public static final int MESSAGE_STATE_CHANGE = 1;
        public static final int MESSAGE_READ = 2;
        public static final int MESSAGE_WRITE = 3;
        public static final int MESSAGE_DEVICE_NAME = 4;
        public static final int MESSAGE_TOAST = 5;
        public static final int MESSAGE_CONTROL = 6;
        // Key names received from the BluetoothChatService Handler
        public static final String DEVICE_NAME = "device_name";
        public static final String TOAST = "toast";

    }
    private MyService BTServ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        BTServ = AppServiceController.getInstance().getService();// <--added


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //mConversationArrayAdapter.add("");
        //mNewState= mState;
        //mOnBtn = (Button) findViewById(R.id.on);
        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);
        mConversationArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
        mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);
        mChatListView = (ListView)findViewById(R.id.chatView);
        mChatListView.setAdapter(mConversationArrayAdapter);
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        mBluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
        //if (mBluetoothAdapter.isEnabled()) {
        //    mBluetoothStatus.setText("Bluetooth ON");
       // }
       // else {
        //    mBluetoothStatus.setText("Bluetooth OFF");
       // }
    }


    //Methods
    //Bluetooth on clicked
    public void onBT(View view) {
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(),"Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            if(mBluetoothAdapter.isEnabled()) {
                mBluetoothStatus.setText("Bluetooth On");
                Toast.makeText(getApplicationContext(), "Bluetooth turned On", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //Bluetooth off clicked
    public void offBT(View view) {
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(),"Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            mBluetoothStatus.setText("Bluetooth Off");
        }
    }

    //Show paired devices button action
    public void pairedBT(View view) {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(mBluetoothAdapter.isEnabled()) {
            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    mBTArrayAdapter.add(deviceName + "\n" + deviceHardwareAddress);
                    mBTArrayAdapter.notifyDataSetChanged();
                }
            }
            else {
                Toast.makeText(getApplicationContext(), "No paired devices", Toast.LENGTH_SHORT).show();
            }

        }
        else{
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
        }
    }

    //Discover devices button action
    public void discoverBT(View view) {
// Check if the device is already discovering
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBluetoothAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBluetoothAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                mBTArrayAdapter.add(deviceName + "\n" + deviceHardwareAddress);
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if(!mBluetoothAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            // Get the device MAC address, which is the last 17 chars in the View
            final String name = info.substring(0,info.length() - 17);
            System.out.print("d");
            System.out.print(address);
            BTServ.setBTState(BluetoothService.STATE_CONNECTING);
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            Toast.makeText(getBaseContext(), address, Toast.LENGTH_LONG).show();
            mBluetoothStatus.setText("Test");
            BTServ.connect(device);

        }
    };


    /** Called when the user clicks the Send button */
    public void sendMessage(View view) {
        // Check that there's actually something to send
        //Intent intent = new Intent(this, DisplayMessageActivity.class);
        byte[] send = null;
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            send = message.getBytes();
            BTServ.write(send);
            //mChatService.write(send);
        }
    }


    public void controller(View view) {
//        //RobotControl RC = new RobotControl();
//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
        finish();
    }

//        intent.putExtra(EXTRA_MESSAGE, message);
//        startActivity(intent);


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.activity_main, menu);
//        return true;
//    }


}
