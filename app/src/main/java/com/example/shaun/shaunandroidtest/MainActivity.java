package com.example.shaun.shaunandroidtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
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

public class MainActivity extends AppCompatActivity {
    // GUI Components
    public final static String EXTRA_MESSAGE = "com.example.shaun.MESSAGE";
    BluetoothAdapter mBluetoothAdapter;
    private TextView mBluetoothStatus;
    private ListView mDevicesListView;
    //Array for storing stuff
    private ArrayAdapter<String> mBTArrayAdapter;
    //private BluetoothAdapter mBTAdapter;
    private Button mOnBtn;
    private static final String TAG = "MainActivity";

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    private int mState;
    //private Handler mHandler;
    Handler mHandler = new Handler();
    //private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    private int connectStatus=0;
    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    public interface MessageConstants {
//        public static final int MESSAGE_READ = 0;
//        public static final int MESSAGE_WRITE = 1;
//        public static final int MESSAGE_TOAST = 2;
//
//        // ... (Add other message types here as needed.)
// Message types sent from the BluetoothChatService Handler
        public static final int MESSAGE_STATE_CHANGE = 1;
        public static final int MESSAGE_READ = 2;
        public static final int MESSAGE_WRITE = 3;
        public static final int MESSAGE_DEVICE_NAME = 4;
        public static final int MESSAGE_TOAST = 5;

        // Key names received from the BluetoothChatService Handler
        public static final String DEVICE_NAME = "device_name";
        public static final String TOAST = "toast";

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOnBtn = (Button) findViewById(R.id.on);
        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);

        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
        mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothStatus.setText("Bluetooth ON");
        }
        else {
            mBluetoothStatus.setText("Bluetooth OFF");
        }


    }

    public void handleMessage(Message msg) {
        String message = (String) msg.obj; //Extract the string from the Message
        Log.d(TAG, "MSG: " + message);
        //....
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

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);
            System.out.print("d");
            System.out.print(address);

            mState = STATE_CONNECTING;
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            Toast.makeText(getBaseContext(), address, Toast.LENGTH_LONG).show();
            mBluetoothStatus.setText("Test");
            connect(device);


//             Spawn a new thread to avoid blocking the GUI one
//            new Thread()
//            {
//                public void run() {
//                    boolean fail = false;
//
//                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//
//                    try {
//
//                        mBTSocket = createBluetoothSocket(device);
//                    } catch (IOException e) {
//                        fail = true;
//                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
//                    }
//                    // Establish the Bluetooth socket connection.
//                    try {
//                        //mBluetoothStatus.setText("Connecting...");
//                        mBTSocket.connect();
//                    } catch (IOException e) {
//                        try {
//                            fail = true;
//                            mBTSocket.close();
//                            //mBluetoothStatus.setText("Failed...Closing socket");
//                            //mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
//                            //        .sendToTarget();
//                        } catch (IOException e2) {
//                            //insert code to deal with this
//                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                    if(fail == false) {
//                        mConnectedThread = new ConnectThread(device);
//                        mConnectedThread.start();
//                        Toast.makeText(getBaseContext(), "Starting Connection", Toast.LENGTH_SHORT).show();
//                        //mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
//                        //        .sendToTarget();
//                    }
//                }
//            }.start();
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param //secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectedThread = new ConnectThread(device);
        mConnectedThread.start();


    }
    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
//    private class ConnectThread extends Thread {
//        private final BluetoothSocket mmSocket;
//        private final BluetoothDevice mmDevice;
//        private String mSocketType;
//
//        public ConnectThread(BluetoothDevice device) {
//            mmDevice = device;
//            BluetoothSocket tmp = null;
//            //mSocketType = secure ? "Secure" : "Insecure";
//
//            // Get a BluetoothSocket for a connection with the
//            // given BluetoothDevice
//            try {
//                //if (secure) {
//                mBluetoothStatus.setText("Creating Socket");
//                    tmp = createBluetoothSocket(device);
//                //} else {
//                //    tmp = device.createInsecureRfcommSocketToServiceRecord(
//                //            BTMODULEUUID);
//               // }
//            } catch (IOException e) {
//                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
//            }
//            mmSocket = tmp;
//            mState = STATE_CONNECTING;
//        }
//
//        public void run() {
//            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
//            setName("ConnectThread" + mSocketType);
//
//            // Always cancel discovery because it will slow down a connection
//            mBluetoothAdapter.cancelDiscovery();
//
//            // Make a connection to the BluetoothSocket
//            try {
//                // This is a blocking call and will only return on a
//                // successful connection or an exception
//                mmSocket.connect();
//                mBluetoothStatus.setText("Connecting!");
//            } catch (IOException e) {
//                // Close the socket
//                try {
//                    mmSocket.close();
//                } catch (IOException e2) {
//                    Log.e(TAG, "unable to close() " + mSocketType +
//                            " socket during connection failure", e2);
//                }
//                //connectionFailed();
//                mBluetoothStatus.setText("Connection Failed");
//                return;
//            }
//
//            // Reset the ConnectThread because we're done
//            synchronized (MainActivity.this) {
//                mConnectedThread = null;
//            }
//
//            // Start the connected thread
//            //connected(mmSocket, mmDevice, mSocketType);
//        }
//
//        public void cancel() {
//            try {
//                mmSocket.close();
//            } catch (IOException e) {
//                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
//            }
//        }
//    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;
            mBluetoothStatus.setText("Starting ConnectThread");
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
                //mBluetoothStatus.setText("Create Socket");
            } catch (IOException e) {
                mBluetoothStatus.setText("Socket creation failed");
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                    mBluetoothStatus.setText("Could not close client socket");
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            //mBluetoothStatus.setText("Connected!");
            connectStatus=1;
            mState = STATE_CONNECTED;
            if (mState==STATE_CONNECTED) {
                Log.d(TAG, "CONNECTED to: " + mmDevice);
                //mBluetoothStatus.setText("Connected to: " +mmDevice);
            }
            ConnectedThread chat = new ConnectedThread(mmSocket);
            chat.start();
            //Log.d(TAG, "The message is:" + mHandler);
            // Send the name of the connected device back to the UI Activity
            Message msg = mHandler.obtainMessage(MessageConstants.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(MessageConstants.DEVICE_NAME, mmDevice.getName());
            msg.setData(bundle);
            mHandler.sendMessage(msg);
          // manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                //Log.e(TAG, "Could not close the client socket", e);
            }
        }

    }


    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream


        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()
            String msg ="";
            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    //String message = numBytes.toString();
                    // Send the obtained bytes to the UI activity.
                   //String message = new String(mmInStream.read(mmBuffer), "UTF-8");
                    Message readMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_READ, numBytes, -1, mmBuffer);
                    readMsg.sendToTarget();
                    readMsg.obj = msg; // Put the string into Message, into "obj" field.
                    //byte[] writeBuf = (byte[]) readMsg.obj;
                    //msg.setTarget(mHandler); // Set the Handler
                    //msg.sendToTarget(); //Send the message
                    //handleMessage(readMsg);
                    //String readMessage = new String(writeBuf);

                    Log.d(TAG, "Message:" + numBytes);
                    //mBluetoothStatus.setText(readMsg.toString());
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
               //Handler mHandler = new Handler();
                Message writeErrorMsg =
                        mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    /** Called when the user clicks the Send button */
    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.activity_main, menu);
//        return true;
//    }


}
