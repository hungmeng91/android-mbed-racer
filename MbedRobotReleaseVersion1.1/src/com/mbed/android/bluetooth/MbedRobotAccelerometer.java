/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mbed.android.bluetooth;



import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.mbed.RPC.mbed;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class MbedRobotAccelerometer extends Activity implements SensorEventListener{
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    public int state = 0;
    public int DEVICE_CONNECTED = 1;
    public int DEVICE_NOT_CONNECTED = 0;
    
    
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


    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread

    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mChatService = null;

    
	private mbed mbed; 			//Create an mbed object
	private m3pi ourRobot;       

	//private DisplayView displayView; //Used for Graphics

	// Set these to calibrate the zero point on mobile phone
	private float startRollPos = 6;
	private float startPitchPos = - 5;

	private float height;
	private float width;
	private int startXCircleLocation;
	private float rightRatio = 1;
	private float leftRatio = 1;
	
	
	
	// Used to control the motion of the robot
	private float roll = startRollPos;
	private float pitch = startPitchPos;
	private float oldRoll = startRollPos;
	private float oldPitch = startPitchPos;
	float currentLeftSpeed = 0;
	float currentRightSpeed = 0;
	float overallSpeed = 0;
	boolean isMoving = false; // a flag is set true when the robot is moving

	// this flag is set when the difference in movement is more than the sensitivity
	boolean hasChanged = false; 

	// Stores the difference in movement
	float rollDifference = 0;
	float pitchDifference = 0;

	// change these values to make the robot more sensitive over the whole range. 
	float rollSensitivity = (float) 0;
	float pitchSensitivity = (float) 0;

	// change this value to change the sensitivity of activating the movement
	float startThreshold = (float) 0;  

	// Change these values to create the range of movement you want to use on the phone.
	float pitchRange = 100; // Set the no. of degrees of max speed for going forwards
	float rollRange = 100;  // Set the no. of degrees of max speed for spinning

	// Used to obtain the accelerometer readings
	SensorManager sensorManager = null;
	private int programsWidth;
	private int programsHeight;

	// Graphics 
	boolean forward = false;
	boolean backwards = false;
	boolean left = false;
	boolean right = false;
	
	private DisplayView displayView;

	// Used for sending the instructions
	public Timer timer;
	private String address;
	
	// Vectors used for moving average filter
	private Vector<Float>  pitchVector = new Vector<Float>();
	private Vector<Float>  rollVector = new Vector<Float>();


	// VariableUsed for drawing the background
	private Bitmap backgroundBitmap;
	private BitmapDrawable backgroundDrawable;
	private Matrix matrix;
	private Bitmap bitmapOrg; 
    
	int count = 0;

   @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");


        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
		
        
        
        // Make Full Screen, remove program title
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

                
		// Get screen dimensions
		Display display = getWindowManager().getDefaultDisplay();
		programsWidth = display.getWidth();
		programsHeight = display.getHeight();
		height = (float) (32* programsHeight/64) - (20* programsHeight/64);
		width = (float) (32* programsWidth/64) - (20* programsWidth/64);
		startXCircleLocation = (int)(32* programsHeight/64);
		
		// resize background image 
		bitmapOrg = BitmapFactory.decodeResource(getResources(),R.drawable.mbedracer);  
		int width = bitmapOrg.getWidth();
		int height = bitmapOrg.getHeight();
		int newWidth = programsWidth;
		int newHeight = programsHeight;

		// calculate the scale - in this case = 0.4f
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;

		// create matrix for the manipulation
		matrix = new Matrix();
		// resize the bit map
		matrix.postScale(scaleWidth, scaleHeight);
		// rotate the Bitmap
		matrix.postRotate(0);

		// recreate the new Bitmap
		backgroundBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0,
				width, height, matrix, true);

		// make a Drawable from Bitmap to allow to set the BitMap
		// to the ImageView, ImageButton or what ever
		backgroundDrawable = new BitmapDrawable(backgroundBitmap);

		// Creates the graphical display
		displayView = new DisplayView(this);
		setContentView(displayView);
		
		// Set the sensor manager to start listening to movement.
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
   		
		
		// Make user choose what mbed device to go for
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		
    }

   private void initialiseTimer()
   {
	// Set ups timer to send instructions
		int delay = 0;   // delay for 5 sec.
		int period = 100;  // repeat every sec.
		timer = new Timer();


		// Sends a instruction to the mbed every 100ms
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				
				// Send RPC commands. 
				String s = String.valueOf(currentLeftSpeed);
				String[] Args = {s};
				RPC("m3pi", "right_motor", Args);
				String s2 = String.valueOf(currentRightSpeed);
				String[] Args2 = {s2};
				RPC("m3pi", "left_motor", Args2);

			}
		}, delay, period);
	   
   }
   
   
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setup() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the session
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
              // Start the Bluetooth services
              mChatService.start();
            }
        }
        
        // Starts listening again for the accelerometer readings
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), sensorManager.SENSOR_DELAY_FASTEST);
        
             
	   
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");
        

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
        
        if (timer != null)
        {
        	// Send RPC commands. 
			String s = String.valueOf(0);
			String[] Args = {s};
			RPC("m3pi", "right_motor", Args);
			String s2 = String.valueOf(0);
			String[] Args2 = {s2};
			RPC("m3pi", "left_motor", Args2);
        	
        	timer.cancel();
        }
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

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
       }
    }
 
    
    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    //mTitle.setText(R.string.title_connected_to);
                    //mTitle.append(mConnectedDeviceName);
                	state = DEVICE_CONNECTED; 
                	initialiseTimer();                   
                    break;
                case BluetoothService.STATE_CONNECTING:
                    //mTitle.setText(R.string.title_connecting);
                	state = DEVICE_NOT_CONNECTED;
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                    //mTitle.setText(R.string.title_not_connected);
                	state = DEVICE_NOT_CONNECTED;
                	
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;

                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer

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
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                address = data.getExtras()
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
        }
        return false;
    }

    
	// Contains the code which draws the user interface
	private class DisplayView extends View{
		public DisplayView(Context context){
			super(context);
		}

		@Override protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			count = count + 1;

			int x, y;

			// Calculates the position of the red balls on screen
			x = programsWidth/2-(int)roll;
			y = programsHeight/2+(int)pitch;

			// Set up the paint variable
			Paint paint = new Paint();

			// Draw background onto screen
			canvas.drawBitmap(bitmapOrg, matrix, paint);
			paint.setAntiAlias(true);

			// Set colour to blue for balls
			paint.setColor(Color.BLUE);

			// Draw the blue ball on screen to represent side direction
			canvas.drawCircle(x, 21*programsHeight/32, 20, paint);

			// Draw the blue ball on screen to represent forward direction
			canvas.drawCircle(47*programsWidth/64, y, 20, paint);
			
		
			
			if(y>(programsHeight/2))
			overallSpeed = - (float)((float)((float)y -(float)(programsHeight/2))/  (float)height);
				
			else if((y)<(programsHeight/2))
			overallSpeed = (float)((((float)(programsHeight/2) - (float) y) /height));
			

			
			startXCircleLocation = (programsWidth/2);
			
			if(x >(startXCircleLocation))
			{
				rightRatio = (float)((1)-(float)((x-(float)(startXCircleLocation))/(float)(width)));
				leftRatio = 1;
				currentRightSpeed = overallSpeed * rightRatio;
				currentLeftSpeed = overallSpeed;
				
			}	
			else if(x < (startXCircleLocation))
			{
				leftRatio = (float) (1-(((float)(startXCircleLocation)-(float)x)/(float)(width)));
				rightRatio = 1;
				currentLeftSpeed= overallSpeed * leftRatio;
				currentRightSpeed = overallSpeed;
			}				
			
			
			
			if((overallSpeed < 0.1) && (overallSpeed > -0.1))
			{
				//if(((x > (startXCircleLocation+5))||(x > (startXCircleLocation-5))))
				//{
					if(x>(startXCircleLocation))
				
					{
						rightRatio = (float)((1)-(float)((x-(float)(startXCircleLocation))/(float)(width)));
						leftRatio = 1;
						currentRightSpeed = -(1-rightRatio);
						currentLeftSpeed =  (1-rightRatio);
						
					}	
					else if((x)<(startXCircleLocation))
					{
						leftRatio = (float) (1-(((float)(startXCircleLocation)-(float)x)/(float)(width)));
						rightRatio = 1;
						currentLeftSpeed= -(1-leftRatio);
						currentRightSpeed = (1-leftRatio);
					}		
			//	}	
					
			}
			
			
			
			// To increase stability 
			
			//safetychecks
			if(currentLeftSpeed > 1)
			{ 
				currentLeftSpeed = 1;
			}

			//safetychecks
			if(currentRightSpeed > 1)
			{ 
				currentRightSpeed = 1;
			}
			
			//safetychecks
			if((currentLeftSpeed > 0)&&(currentLeftSpeed < 0.1))
	        currentLeftSpeed = 0;
			
			else if((currentLeftSpeed < 0)&&(currentLeftSpeed > -0.1))
				currentLeftSpeed = 0;

			//safetychecks
			if((currentRightSpeed > 0) &&(currentRightSpeed < 0.1))
         	currentRightSpeed = 0;
			
			else if((currentRightSpeed < 0)&&(currentRightSpeed > -0.1))
			currentRightSpeed = 0;
	

		}
	}


	  // This method sends the RPC commands to the mbed
	  public String RPC(String Name, String Method, String[] Args){

		//write to serial port and receive result
		String Arguments = "";
		String Response;
		if(Args != null){
			int s = Args.length;
			for(int i = 0; i < s; i++){
				Arguments = Arguments + " " + Args[i];
			}
		}
		String RPCString = "/" + Name + "/" + Method + Arguments + "\n";

		sendMessage(RPCString);
		
		// Send message
		return(RPCString);

	}

		// This method is called every time there is a change of orientation, it also handles the control of the robot. 
		public void onSensorChanged(SensorEvent event) {
			synchronized (this) {
				switch (event.sensor.getType()){
				case Sensor.TYPE_ACCELEROMETER:

					break;
				case Sensor.TYPE_ORIENTATION:

					// Get roll value from phone
					roll = event.values[1];

					// Get pitch value from phone
					pitch = event.values[2];

					break;

				}


			displayView.invalidate();   
			}
		}  

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
}