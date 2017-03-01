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

public class BluetoothService extends AppCompatActivity {
    // GUI Components
    public final static String EXTRA_MESSAGE = "com.example.shaun.MESSAGE";
    BluetoothAdapter mBluetoothAdapter;
    private TextView mBluetoothStatus;
    private ListView mDevicesListView;
    private ListView mChatListView;
    private String mConnectedDeviceName = null;
    private MainActivity mChatService = null;
    //Array for storing stuff
    private ArrayAdapter<String> mBTArrayAdapter;
    //private BluetoothAdapter mBTAdapter;
    private Button mOnBtn;
    private static final String TAG = "MainActivity";
    private ArrayAdapter<String> mConversationArrayAdapter;
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    private int mState;
    private int mNewState;
    //private Handler mHandler;
    //Handler mHandler = new Handler();
    //private Handler mHandler; // Our main handler that will receive callback notifications // bluetooth background worker thread to send and receive data
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    //private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    private int connectStatus=0;
    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //mConversationArrayAdapter.add("");
        mNewState= mState;
        mOnBtn = (Button) findViewById(R.id.on);
        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);
        mConversationArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
        mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);
        mChatListView = (ListView)findViewById(R.id.chatView);
        mChatListView.setAdapter(mConversationArrayAdapter);
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
    /**
     * Update UI title according to the current state of the chat connection
     */
    private synchronized void updateUserInterfaceTitle() {
        mState = getBTState();
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MessageConstants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }
    /**
     * Return the current connection state.
     */
    public synchronized int getBTState() {
        return mState;
    }
    /**
     * Set the current connection state.
     */
    public synchronized void setBTState(int state) {
        mState = state;
    }
    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = BluetoothService.this;
            //setSupportActionBar(MainActivity);
            // getSupportActionBar().setDisplayShowHomeEnabled(true);
            switch (msg.what) {
                case MessageConstants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case MainActivity.STATE_CONNECTED:
                            mBluetoothStatus.setText("Successfully connected to: "+ mConnectedDeviceName);
                            //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case MainActivity.STATE_CONNECTING:
                            mBluetoothStatus.setText("Connection in progress");
                            break;
                        case MainActivity.STATE_LISTEN:
                        case MainActivity.STATE_NONE:
                            mBluetoothStatus.setText("Disconnected!");
                            break;
                    }
                    break;
                case MessageConstants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    mConversationArrayAdapter.notifyDataSetChanged();
                    break;
                case MessageConstants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mConversationArrayAdapter.add(mConnectedDeviceName+ ":  " + readMessage);
                    mConversationArrayAdapter.notifyDataSetChanged();
                    Log.d(TAG, readMessage);
                    Toast.makeText(getApplicationContext(),readMessage, Toast.LENGTH_SHORT).show();
                    break;
                case MessageConstants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(MessageConstants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MessageConstants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(MessageConstants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
//                case MessageConstants.MESSAGE_CONTROL:
//                    byte[] controlBuf = (byte[]) msg.obj;
//                    // construct a string from the buffer
//                    String controlMessage = new String(controlBuf);
//                    mConversationArrayAdapter.add("Me:  " + controlMessage);
//                    mConversationArrayAdapter.notifyDataSetChanged();
//                    break;
            }
        }
    };

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

    public void controller(View view) {
        //RobotControl RC = new RobotControl();
        Intent intent = new Intent(this, RobotControl.class);
        startActivity(intent);
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

            mState = STATE_CONNECTING;
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            Toast.makeText(getBaseContext(), address, Toast.LENGTH_LONG).show();
            mBluetoothStatus.setText("Test");
            connect(device);

        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }
    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Update UI title
        updateUserInterfaceTitle();
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
        if (getBTState() == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

        // Update UI title
        updateUserInterfaceTitle();
    }
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
//        if (mSecureAcceptThread != null) {
//            mSecureAcceptThread.cancel();
//            mSecureAcceptThread = null;
//        }
//        if (mInsecureAcceptThread != null) {
//            mInsecureAcceptThread.cancel();
//            mInsecureAcceptThread = null;
//        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        // new ConnectedThread(socket,socketType);

        mConnectedThread.start();
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MessageConstants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MessageConstants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setBTState(STATE_CONNECTED);
        // Update UI title
        updateUserInterfaceTitle();
    }


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
            //setBTState(STATE_CONNECTING);
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
                    //mBluetoothStatus.setText("Could not close client socket");
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            //mBluetoothStatus.setText("Connected!");
            Message msg = mHandler.obtainMessage(MessageConstants.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(MessageConstants.DEVICE_NAME, mmDevice.getName());
            msg.setData(bundle);
            mHandler.sendMessage(msg);
            setBTState(STATE_CONNECTED);
            if (mState==STATE_CONNECTED) {
                Log.d(TAG, "CONNECTED to: " + mmDevice);
                //mBluetoothStatus.setText("Connected to: " +mmDevice);
            }
            //Reset connectthread when done
            mConnectThread = null;
            // Start the connected thread
            connected(mmSocket, mmDevice);
            // Update UI title
            updateUserInterfaceTitle();
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
            //mState = STATE_CONNECTED;
        }

        public void run() {
            byte[] mmBuffer = new byte[1024];
            //mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()
            //String msg ="";
            // Keep listening to the InputStream until an exception occurs.
//            while (mState == STATE_CONNECTED) {
            while (getBTState() == STATE_CONNECTED) {
                try {
                    //mConversationArrayAdapter.clear();
                    // Read from the InputStream.
                    Log.d(TAG, "1");
                    numBytes = mmInStream.read(mmBuffer);
                    Log.d(TAG, "2");
                    //String message = numBytes.toString();
                    // Send the obtained bytes to the UI activity.
                    //String message = new String(mmInStream.read(mmBuffer), "UTF-8");
                    Message readMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_READ, numBytes, -1, mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }
        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                // Share the sent message with the UI activity.
//                Message writtenMsg = mHandler.obtainMessage(
//                        MessageConstants.MESSAGE_WRITE, -1, -1, bytes);
//                writtenMsg.sendToTarget();
                mHandler.obtainMessage(MessageConstants.MESSAGE_WRITE, -1, -1, bytes)
                        .sendToTarget();
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
    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
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
            this.write(send);
            //mChatService.write(send);
        }
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MessageConstants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        BluetoothService.this.start();
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
