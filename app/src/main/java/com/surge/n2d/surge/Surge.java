package com.surge.n2d.surge;

/**
 * Created by nrv on 4/3/16.
 */

    import android.app.Activity;
    import android.bluetooth.BluetoothAdapter;
    import android.bluetooth.BluetoothDevice;
    import android.content.Intent;
    import android.os.Bundle;
    import android.os.Handler;
    import android.os.Message;
    import android.util.Log;
    import android.view.Menu;
    import android.view.MenuInflater;
    import android.view.MenuItem;
    import android.view.View;
    import android.view.Window;
    import android.view.View.OnClickListener;
    import android.widget.Button;
    import android.widget.TextView;
    import android.widget.Toast;
/**
 * This is the main Activity that displays app functions.
 */
public class Surge extends Activity implements OnClickListener{
    // Debugging
    private static final String TAG = "Surge";
    private static final boolean D = true;
    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    // Layout Views
    private Button ConnectBtn;
    private Button DisconnectBtn;
    private TextView CurrentSensorValue;
    private TextView DisplayAmpValue;
    private TextView DisplayPowerValue;
    private TextView mTitle;
    private GraphView mGraph;
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mChatService = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
// Set up the window layout
        try {

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
            setContentView(R.layout.main);
            getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        }
        catch (Exception e){
            Log.e("Surge",e.getMessage());
            e.printStackTrace();
        }

// Set up the custom title

        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
// Get local Bluetooth adapter

            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


// Set up Layout Buttons and Views
        ConnectBtn = (Button) findViewById(R.id.connect_btn);
        DisconnectBtn = (Button) findViewById(R.id.disconnect_btn);
        CurrentSensorValue = (TextView) findViewById(R.id.rawValue);
        DisplayAmpValue = (TextView) findViewById(R.id.currentValue);
        DisplayPowerValue = (TextView) findViewById(R.id.powerValue);
        mGraph = (GraphView)findViewById(R.id.graph);
        mGraph.setMaxValue(800);
        ConnectBtn.setOnClickListener(this);
        DisconnectBtn.setOnClickListener(this);
// If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
// If BT is not on, request that it be enabled.
// setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
// Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
// Performing this check in onResume() covers the case in which BT was
// not enabled during onStart(), so we were paused to enable it...
// onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
// Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothService.STATE_NONE) {
// Start the Bluetooth chat services
                mChatService.start();
//Check function is called when connection is made
                String check = "3";
                sendMessage(check);
            }
        }
    }
    private void setupChat() {
        Log.d(TAG, "setupChat()");
// Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothService(this, mHandler);
//String check = "3";
//sendMessage(check);
    }
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect_btn:
//On button sends a 1 to microcontroller
                String flagon = "1";
                sendMessage(flagon);
                break;
            case R.id.disconnect_btn:
//Off button sends a 2 to microcontroller
                String flagoff = "2";
                sendMessage(flagoff);
                break;
            default:
                break;
        }
    }
    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }
    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
// Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    /**
     * Sends a message.
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
// Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
// Get the message bytes and tell the BluetoothChatService to write
        byte[] send = message.getBytes();
        mChatService.write(send);
    }
    private void receive(String message) {
        String AmpOut = null;
        String PowerOut = null;
        float AmpValue;
        float Power;
        float Plot;
//Parse string message into int
        final int bluetoothReading = Integer.parseInt(message);
        if(bluetoothReading == 7777){
//If 7777 received, turn green indicator on
            findViewById(R.id.connected).setBackgroundResource(R.color.connected_on);
            findViewById(R.id.disconnected).setBackgroundResource(R.color.disconnected_off);
        }
        else if(bluetoothReading == 8888){
//If 8888 received, turn red indicator on
            findViewById(R.id.connected).setBackgroundResource(R.color.connected_off);
            findViewById(R.id.disconnected).setBackgroundResource(R.color.disconnected_on);
        }
        else{
//Convert value to Amps
            AmpValue = (float)bluetoothReading / 1000;
            Power = AmpValue*120;
            Plot = Power + 300;
//Converts values to string to display as text
            AmpOut = String.valueOf(AmpValue);
            PowerOut = String.valueOf(Power);
//Displays data
            CurrentSensorValue.setText(message);
            DisplayAmpValue.setText(AmpOut);
            DisplayPowerValue.setText(PowerOut);
//Plots data with 300 offset
            mGraph.addDataPoint(Plot);
        }
    }
    // The Handler that gets information back from the BluetoothChatService
    private  final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            try {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:
                        if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                        switch (msg.arg1) {
                            case BluetoothService.STATE_CONNECTED:
                                Log.e("Err",msg.toString()+"1");
                                try {
                                    mTitle.setText(R.string.title_connected_to);
                                    mTitle.append(mConnectedDeviceName);
                                }
                                catch (Exception e){
                                    e.printStackTrace();
                                }
                                break;
                            case BluetoothService.STATE_CONNECTING:
                                Log.e("Err",msg.toString()+"2");
                                mTitle.setText(R.string.title_connecting);
                                break;
                            case BluetoothService.STATE_LISTEN:
                            case BluetoothService.STATE_NONE:
                                Log.e("Err",msg.toString()+"3");

                                mTitle.setText(R.string.title_not_connected);

                                Log.e("Err",msg.toString()+"3.1");
                                break;
                        }
                        break;
                    case MESSAGE_WRITE:
                        byte[] writeBuf = (byte[]) msg.obj;
                        String writeMessage = new String(writeBuf);
                        break;
                    case MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;
// construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
//Splits string coming in by /
                        String[] tokens = readMessage.split("/");
                        int i = 0;
                        String data = null;
//converts string[] to string
                        for (i = 0; i < tokens.length; i++) {
                            data = tokens[i];
                        }
                        receive(data);
                        break;
                    case MESSAGE_DEVICE_NAME:
// save the connected device's name
                        mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                        Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                        break;
                    case MESSAGE_TOAST:
                        Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    };
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
// When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
// Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
// Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
// Attempt to connect to the device
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
// When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
// Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
// User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
// Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.discoverable:
// Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
        }
        return false;
    }
}