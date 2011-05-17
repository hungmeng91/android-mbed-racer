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
import java.util.ArrayList;
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
import android.graphics.PointF;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
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
   public class MbedRobotTouchTankMode extends Activity implements OnTouchListener{
    // Debugging
   private static final String TAG = "BluetoothChat";
   private static final boolean D = true;

   public int state = 0;
   public int DEVICE_CONNECTED = 1;
   public int DEVICE_NOT_CONNECTED = 0;
   
    
   public static final boolean DEBUG = false;
    
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

   // Used to calculate the multi coordinates, -1 not pressed
   float leftFingerX = -1;
   float leftFingerY = -1;
   float rightFingerX = -1;
   float rightFingerY = -1; 
   float previous1stPointX = 0;
   float preprevious1stPointX = 0;
   float pointerDifference = 0;
   
   // 1 for left, 2 for right
   private boolean testDifference = false; 
   private int originalPressZone = FIRST_POINTER_NOPRESS;
   private int pressZone = FIRST_POINTER_NOPRESS ; 
   private static final int FIRST_POINTER_LEFT = 1;
   private static final int FIRST_POINTER_RIGHT = 2;
   private static final int FIRST_POINTER_NOPRESS = 0;
      
	// Used for the touch circles
	private int leftCircleXLocation;
	private int leftCircleYLocation;
	private int rightCircleXLocation;
	private int rightCircleYLocation;
	private int circleControlRadius = 30;

    // Name of the connected device
    private String mConnectedDeviceName = null;
      
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
   
    // Member object for the chat services
    private BluetoothService mChatService = null;

	private ArrayList<PointF> touchPoints = null;
	private Paint drawingPaint = null;
	private boolean isMultiTouch = false;
	private boolean firstUp = true;
	
	// Used for debugging
	private String statusText = "ready";
	private String statusText2 = "1st point up";
	private String statusText3 = "ready";
	private String statusText4 = "2nd point up";
	private String statusText5 = "ready";
		
	// Used to control the motion of the robot
	float currentLeftSpeed = 0;
	float currentRightSpeed = 0;
	float overallSpeed = 0;
	boolean isMoving = false; // a flag is set true when the robot is moving
    
	// this flag is set when the difference in movement is more than the sensitivity
	boolean hasChanged = false; 
	
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


		// resize background image 
		bitmapOrg = BitmapFactory.decodeResource(getResources(),R.drawable.touchscreen);  
		int width = bitmapOrg.getWidth();
		int height = bitmapOrg.getHeight();
		int newWidth = programsWidth;
		int newHeight = programsHeight;

		// calculate the scale - in this case = 0.4f
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		
		// Set Cicle Location Positions
		leftCircleXLocation = 7*programsWidth/64;
		rightCircleXLocation = programsWidth - 7*programsWidth/64;
		leftCircleYLocation = programsHeight/2;
		rightCircleYLocation = programsHeight/2;
		
		
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
        // setupChat() will then be called during onActivityResult
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
        
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");
        

        // Initialize the BluetoothService to perform bluetooth connections
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
        // Stop the Bluetooth services
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
           
                	state = DEVICE_CONNECTED; 
                	initialiseTimer();                   
                    break;
                case BluetoothService.STATE_CONNECTING:
            
                	state = DEVICE_NOT_CONNECTED;
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
       
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
                // Bluetooth is now enabled, so set up a session
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
			
			drawingPaint = new Paint();

			drawingPaint.setColor(Color.RED);
			drawingPaint.setStrokeWidth(2);
			drawingPaint.setStyle(Paint.Style.STROKE);
			drawingPaint.setAntiAlias(true);
			
			touchPoints = new ArrayList<PointF>();
		}

		
		
		@Override protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			count = count + 1;


			Paint paint = new Paint();
			paint.setColor(Color.RED);
			paint.setAntiAlias(true);
			
					
			// Draw background onto screen
			canvas.drawBitmap(bitmapOrg, matrix, paint);

			// Set colour to blue for balls
			paint.setColor(Color.RED);
			
			
			// Snap so it drives straight
			if(((leftFingerY - rightFingerY)<20)&&((leftFingerY - rightFingerY)>20))
			{
				leftFingerY = rightFingerY;
			}
			
			if(((rightFingerY - leftFingerY)<20)&&((rightFingerY - leftFingerY)>20))
			{
				leftFingerY = rightFingerY;
			}
			
			
			if(DEBUG)
			{
				canvas.drawText(statusText, 100, 50, paint);
				canvas.drawText(statusText2, 350, 50, paint);
				canvas.drawText(statusText3, 100, 250, paint);
				canvas.drawText(statusText4, 350, 250, paint);
			}
			
			if(firstUp)
			{
				rightCircleYLocation = programsHeight/2;
				
				canvas.drawCircle(
						rightCircleXLocation, rightCircleYLocation, 
						circleControlRadius, 	paint);		
				
				currentRightSpeed = 0;
				
				leftCircleYLocation = programsHeight/2;
				canvas.drawCircle(
						leftCircleXLocation, leftCircleYLocation, 
						circleControlRadius, 	paint);		
				
				currentLeftSpeed = 0;
			}
			else
			{
			
				// If press is on circle
				if((leftFingerX > (leftCircleXLocation -circleControlRadius*1.5))&& (leftFingerX < (leftCircleXLocation + circleControlRadius*1.5)))
				{
					
					
					
					if((leftFingerY > (leftCircleYLocation - circleControlRadius*1.5))&& (leftFingerY < (leftCircleYLocation + circleControlRadius*1.5)))
					{
		
						if((leftFingerY < (programsHeight-(programsHeight/10)))&&(leftFingerY > ((programsHeight/10))))
						{
							canvas.drawCircle(
									leftCircleXLocation, leftFingerY, 
									circleControlRadius, 	paint);		
							
							leftCircleYLocation = (int) leftFingerY;
							
							
							if(leftCircleYLocation>(programsHeight/2))
								currentLeftSpeed = (float)-((leftCircleYLocation-(float)(programsHeight/2))/(float)(programsHeight/2));
								
							else if((leftCircleYLocation)<(programsHeight/2))
							currentLeftSpeed = (float) (((float)(programsHeight/2)-(float)leftCircleYLocation)/(float)(programsHeight/2));
						}
						else
						{
							if((leftFingerY > (programsHeight-(programsHeight/10))))
							{
								canvas.drawCircle(
										leftCircleXLocation, (programsHeight-(programsHeight/10)), 
										circleControlRadius, 	paint);											
							}
							else if((leftFingerY < ((programsHeight/10))))
							{
								canvas.drawCircle(
										leftCircleXLocation, ((programsHeight/10)), 
										circleControlRadius, 	paint);	
							}
						}
						
					}		
					
					else
					{
						leftCircleYLocation = programsHeight/2;
						canvas.drawCircle(
								leftCircleXLocation, leftCircleYLocation, 
								circleControlRadius, 	paint);		
						
						currentLeftSpeed = 0;
						
					}
		
				}	
				
				else
				{
					leftCircleYLocation = programsHeight/2;
					canvas.drawCircle(
							leftCircleXLocation, leftCircleYLocation, 
							circleControlRadius, 	paint);		
					
					currentLeftSpeed = 0;
				}
					
				// If press is on circle
				if((rightFingerX > (rightCircleXLocation - circleControlRadius*1.5))&& (rightFingerX < (rightCircleXLocation + circleControlRadius*1.5)))
				{
					
					if((rightFingerY > (rightCircleYLocation - circleControlRadius*1.5))&& (rightFingerY < (rightCircleYLocation + circleControlRadius*1.5)))
					{
						if((rightFingerY < (programsHeight-(programsHeight/10)))&&(rightFingerY > ((programsHeight/10))))
						{
							canvas.drawCircle(
									rightCircleXLocation, rightFingerY, 
									circleControlRadius, 	paint);		
							
							rightCircleYLocation = (int) rightFingerY;
							
							
							// Configure Right Speed
							if(rightCircleYLocation>(programsHeight/2))
							currentRightSpeed = (float)-(((float)rightCircleYLocation-((float)programsHeight/2))/((float)programsHeight/2));
							
							else if((rightCircleYLocation)<(programsHeight/2))
							currentRightSpeed = (float)(((float)(programsHeight/2)-(float)rightCircleYLocation)/(float)(programsHeight/2));
						}
						else
						{
							if((rightFingerY > (programsHeight-(programsHeight/10))))
							{
								canvas.drawCircle(
										rightCircleXLocation, (programsHeight-(programsHeight/10)), 
										circleControlRadius, 	paint);											
							}
							else if((rightFingerY < ((programsHeight/10))))
							{
								canvas.drawCircle(
										rightCircleXLocation, ((programsHeight/10)), 
										circleControlRadius, 	paint);	
							}
						}
					 }		
					
					else
					{
						rightCircleYLocation = programsHeight/2;
						
						canvas.drawCircle(
								rightCircleXLocation, rightCircleYLocation, 
								circleControlRadius, 	paint);		
						
						currentRightSpeed = 0;
					}
		
				}	
				
				else
				{
					rightCircleYLocation = programsHeight/2;
					
					canvas.drawCircle(
							rightCircleXLocation, rightCircleYLocation, 
							circleControlRadius, 	paint);		
					
					currentRightSpeed = 0;
				}
				
					
				
				
				
				invalidate();
				
				
				
				
			}
		}
		
	}


	  // This method sends the RPC commands to the mbed
	  public String RPC(String Name, String Method, String[] Args){
	
		//write to serial port and receive result
		String Arguments = "";
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

	

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		
		
		
		return false;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
			super.onTouchEvent(event);
			
			int action = event.getAction() & MotionEvent.ACTION_MASK;
			
			if(pressZone == FIRST_POINTER_LEFT)
			{
				leftFingerX = event.getX();
				leftFingerY = event.getY();
				
				if(DEBUG)
				statusText = " Left Pointer: " + String.valueOf(event.getX()) + "Y: " + String.valueOf(event.getY());
			}
			else if (pressZone == FIRST_POINTER_RIGHT)
			{
				rightFingerX = event.getX();
				rightFingerY = event.getY();
				
				if(DEBUG)
				statusText3 = " Right Pointer: " + String.valueOf(event.getX()) + "Y: " + String.valueOf(event.getY());
			}
			
					
			if(event.getPointerCount()==2)
			{
				if(pressZone == FIRST_POINTER_LEFT)
				{
					rightFingerX = event.getX(1);
					rightFingerY = event.getY(1);
					
					if(DEBUG)
					statusText3 = " Right Pointer: " + String.valueOf(event.getX(1)) + "Y: " + String.valueOf(event.getY(1));
				}
				else if (pressZone == FIRST_POINTER_RIGHT)
				{
					leftFingerX = event.getX(1);
					leftFingerY = event.getY(1);
					
					if(DEBUG)
					statusText = " left Pointer: " + String.valueOf(event.getX(1)) + "Y: " + String.valueOf(event.getY(1));
				}
			}
			

			
			displayView.invalidate();
			
			// USED TO FIX MULTITOUCH BUG
			if(testDifference)
			{
			pointerDifference = (previous1stPointX - event.getX());
				
				if((pointerDifference > programsWidth/4) || (pointerDifference < -programsWidth/4))
				{
					if(pressZone == FIRST_POINTER_RIGHT)
					{
						pressZone = FIRST_POINTER_LEFT;
						rightFingerX = -1;
						rightFingerY = -1;

						if(DEBUG)
						statusText3 = " Right Pointer: " + String.valueOf(event.getX()) + "Y: " + String.valueOf(event.getY());
						
					}
					else if (pressZone == FIRST_POINTER_LEFT)
					{
						pressZone = FIRST_POINTER_RIGHT;
						leftFingerX = -1;
						leftFingerY = -1;

						
						if(DEBUG)
						statusText = " Left Pointer: " + String.valueOf(event.getX()) + "Y: " + String.valueOf(event.getY());
					}
					testDifference = false;
				}
				displayView.invalidate();
			}
			
			switch(action)
			{
				case MotionEvent.ACTION_DOWN:
				{
					
					statusText2 = "1st point Down";
					pressZone = 0;
					
					firstUp = false;
					
					if(event.getX() < programsWidth/2)
					{
						
						pressZone = FIRST_POINTER_LEFT;
						originalPressZone = FIRST_POINTER_LEFT;
						leftFingerX = event.getX();
						leftFingerY = event.getY();		
						
						if(DEBUG)
						statusText = " Left Pointer: " + String.valueOf(event.getX()) + "Y: " + String.valueOf(event.getY());
						
					}
					else
					{
						
						pressZone = FIRST_POINTER_RIGHT;
						originalPressZone = FIRST_POINTER_RIGHT;
						rightFingerX = event.getX();
						rightFingerY = event.getY();
						
						if(DEBUG)
						statusText3 = " Right Pointer: " + String.valueOf(event.getX()) + "Y: " + String.valueOf(event.getY());
						
						
					}
					
					displayView.invalidate();
					break;
				}
				
				case MotionEvent.ACTION_UP:
				{
					isMultiTouch = false;
					statusText2 = "1st Point Up";
					firstUp = true;
				
					
					pressZone = FIRST_POINTER_NOPRESS;
					rightCircleYLocation = programsHeight/2;
					currentRightSpeed = 0;
					leftCircleYLocation = programsHeight/2;
					currentLeftSpeed = 0;
					
					displayView.invalidate();
					break;
				}

				case MotionEvent.ACTION_POINTER_DOWN:
				{
					
					statusText4 = "2nd point Down";
					if(pressZone == originalPressZone)
					{
						if(pressZone == FIRST_POINTER_RIGHT)
						{
							leftFingerX = event.getX(1);
							leftFingerY = event.getY(1);	
							
							if(DEBUG)
							statusText = " Left Pointer: " + String.valueOf(event.getX(1)) + "Y: " + String.valueOf(event.getY(1));
							
						}
					
						if(pressZone == FIRST_POINTER_LEFT)
						{
							rightFingerX = event.getX(1);
							rightFingerY = event.getY(1);	
							
							if(DEBUG)
							statusText3 = " Right Pointer: " + String.valueOf(event.getX(1)) + "Y: " + String.valueOf(event.getY(1));
							
						}
					 
					}
					
					if(pressZone != originalPressZone)
					{
						pressZone = originalPressZone;
						
						if(pressZone == FIRST_POINTER_RIGHT)
						{
							leftFingerX = event.getX(1);
							leftFingerY = event.getY(1);	
							
							if(DEBUG)
							statusText = " Left Pointer: " + String.valueOf(event.getX(1)) + "Y: " + String.valueOf(event.getY(1));
							
						}
					
						else if(pressZone == FIRST_POINTER_LEFT)
						{
							rightFingerX = event.getX(1);
							rightFingerY = event.getY(1);	
													
							if(DEBUG)
							statusText3 = " Right Pointer: " + String.valueOf(event.getX(1)) + "Y: " + String.valueOf(event.getY(1));
							
						}
					}
		
					displayView.invalidate();
					break;
				}
				

				
				case MotionEvent.ACTION_POINTER_1_UP:
				{
					testDifference = true;
					
					if(pressZone == FIRST_POINTER_RIGHT)
					{
						pressZone = FIRST_POINTER_NOPRESS;
						leftFingerX = -1;
						leftFingerY = -1;
						leftCircleYLocation = programsHeight/2;
						currentLeftSpeed = 0;
						
						testDifference = false;
						
					}
					else if(pressZone == FIRST_POINTER_LEFT)
					{
						pressZone = FIRST_POINTER_NOPRESS;
						rightFingerX = -1;
						rightFingerY = -1;
						rightCircleYLocation = programsHeight/2;
						currentRightSpeed = 0;
						
						testDifference = false;
					}
					
					displayView.invalidate();
					
					if(DEBUG)
					statusText4 = "2nd Point Up";
					
					displayView.invalidate();
					break;
				}
				
				
				
				
			}
			
			// FIXES BUG
			previous1stPointX = event.getX();
			preprevious1stPointX = previous1stPointX;
			setPoints(event);
			return true;
		}	
	
	
	public void setPoints(MotionEvent event){
		touchPoints.clear();
		
		int pointerIndex = 0;
	    
		for(int index=0; index<event.getPointerCount(); ++index)
		{
			pointerIndex = event.getPointerId(index);
			
			displayView.invalidate();
			touchPoints.add(new PointF(event.getX(pointerIndex),event.getY(pointerIndex)));
		}	
		

	}
}