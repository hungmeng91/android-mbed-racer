package blue.tooth;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Vector;

import org.mbed.RPC.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.shapes.Shape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MbedRobot extends Activity implements SensorEventListener {
	private mbed mbed; 			//Create an mbed object
	private m3pi ourRobot;       

	private LinearLayout linLayout; // Set up for the graphics
	private DemoView demoview; //Used for Graphics

	// Set these to calibrate the zero point on mobile phone
	private float startRollPos = 6;
	private float startPitchPos = - 5;
	
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


	// Used for sending the instructions
	private Timer timer;
	
	// Vectors used for moving average filter
	private Vector<Float>  pitchVector = new Vector<Float>();
	private Vector<Float>  rollVector = new Vector<Float>();
	
	// Number of taps for the moving average filter
	private int numTaps = 2;
	
	// Fields for the bluetooth
    private static final String TAG = "THINBTCLIENT";
    private static final boolean D = true;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;  
    private OutputStream outStream = null;
    
    private static final String PCTAG = "THINDEBUGBTCLIENT";
    private static final boolean PCD = true;
    private BluetoothAdapter mDebugBluetoothAdapter = null;
    private BluetoothSocket btDebugSocket = null;  
    private OutputStream outDebugStream = null;

    
    // Used for the background
    private Bitmap backgroundBitmap;
    private BitmapDrawable backgroundDrawable;
    private Matrix matrix;
    private Bitmap bitmapOrg; 
    
    // Well known SPP UUID (will *probably* map to
    // RFCOMM channel 1 (default) if not in use);
    // see comments in onResume().
    private static final UUID MY_UUID =
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // ==> hardcode your server's MAC address here <==
 
    private static String address = null;
//  private static String address = "00:15:83:41:AC:FF";
	
  int count = 0;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
	
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);
		alert.setView(input);
		
		alert.setTitle("Enter mac address (XX:XX:XX:XX:XX:XX)");
				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = input.getText().toString().trim();
					address = value;
					Toast.makeText(getApplicationContext(), "connecting to " + value,
							Toast.LENGTH_SHORT).show();				
					onResume();
				}
			});
	
		 alert.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							dialog.cancel();
							Toast.makeText(getApplicationContext(), "Mac Address Needed to Run",
									Toast.LENGTH_SHORT).show();
						}
					});
			
		 alert.show();

		// Initialise the roll vector with 5 "zero" values
		for(int i = 0; i < numTaps; i ++)
		{
			rollVector.add(startRollPos);
		}
		
		
		// Initialise the pitch vector with 5 "zero" values
		for(int i = 0; i < numTaps; i ++)
		{
			pitchVector.add(startPitchPos);
		}
		
		
		// Make Full Screen
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		Display display = getWindowManager().getDefaultDisplay();
		programsWidth = display.getWidth();
		programsHeight = display.getHeight();

		 
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
		demoview = new DemoView(this);
		setContentView(demoview);

		// Set the sensor manager to start listening to movement.
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		
		
	        if (D)
	            Log.e(TAG, "+++ ON CREATE +++");
	
	    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    if (mBluetoothAdapter == null) {
	            Toast.makeText(this,
	                    "Bluetooth is not available.",
	                    Toast.LENGTH_LONG).show();
	            finish();
	            return;
	    }
	
	    if (!mBluetoothAdapter.isEnabled()) {
	            Toast.makeText(this,
	                    "Please enable your BT and re-run this program.",
	                    Toast.LENGTH_LONG).show();
	            finish();
	            return;
	    }
	
	    if (D)
	            Log.e(TAG, "+++ DONE IN ON CREATE, GOT LOCAL BT ADAPTER +++");
	
	// Create a bluetooth android RPC connection with a new mbed device
	//	mbed = new androidBluetoothRPC("00:15:83:41:AC:FF");	//You need to be able make this mbed = new androidBluetoothRPC("Necessary args");              

		// Create new mbed robot
	//	ourRobot = new m3pi(mbed);

	}     

	@Override
	public void onStart() {
		super.onStart();
        if (D)
                Log.e(TAG, "++ ON START ++");
	
	}

	@Override
	public void onResume() {
    
		super.onResume();
		
			if (D) {
	            Log.e(TAG, "+ ON RESUME +");
	            Log.e(TAG, "+ ABOUT TO ATTEMPT CLIENT CONNECT +");
	    }
	 if(address != null)
	 {
	    // When this returns, it will 'know' about the server,
	    // via it's MAC address.
	    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
	
	    // We need two things before we can successfully connect
	    // (authentication issues aside): a MAC address, which we
	    // already have, and an RFCOMM channel.
	    // Because RFCOMM channels (aka ports) are limited in
	    // number, Android doesn't allow you to use them directly;
	    // instead you request a RFCOMM mapping based on a service
	    // ID. In our case, we will use the well-known SPP Service
	    // ID. This ID is in UUID (GUID to you Microsofties)
	    // format. Given the UUID, Android will handle the
	    // mapping for you. Generally, this will return RFCOMM 1,
	    // but not always; it depends what other BlueTooth services
	    // are in use on your Android device.
	    try {
	            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
	    } catch (IOException e) {
	            Log.e(TAG, "ON RESUME: Socket creation failed.", e);
	    }
	
	    // Discovery may be going on, e.g., if you're running a
	    // 'scan for devices' search from your handset's Bluetooth
	    // settings, so we call cancelDiscovery(). It doesn't hurt
	    // to call it, but it might hurt not to... discovery is a
	    // heavyweight process; you don't want it in progress when
	    // a connection attempt is made.
	    mBluetoothAdapter.cancelDiscovery();
	
	    // Blocking connect, for a simple client nothing else can
	    // happen until a successful connection is made, so we
	    // don't care if it blocks.
	    try {
	            btSocket.connect();
	            Log.e(TAG, "ON RESUME: BT connection established, data transfer link open.");
	    } catch (IOException e) {
	            try {
	                    btSocket.close();
	            } catch (IOException e2) {
	                    Log.e(TAG,
	                            "ON RESUME: Unable to close socket during connection failure", e2);
	            }
	    }
	
	    // Create a data stream so we can talk to server.
	    if (D)
	            Log.e(TAG, "+ ABOUT TO SAY SOMETHING TO SERVER +");
	
	    try {
	            outStream = btSocket.getOutputStream();
	    } catch (IOException e) {
	            Log.e(TAG, "ON RESUME: Output stream creation failed.", e);
	    }
/*	
	    String message = "Hello message from client to server.";
	    byte[] msgBuffer = message.getBytes();
	    try {
	            outStream.write(msgBuffer);
	    } catch (IOException e) {
	            Log.e(TAG, "ON RESUME: Exception during write.", e);
	    }*/

		
		// Needs to establish a connection was the program is back in focus
//		androidBluetoothRPC tmpRPC = (androidBluetoothRPC) mbed;
//		tmpRPC.establishConnection();

		// Starts listening again for the accelerometer readings
//		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), sensorManager.SENSOR_DELAY_FASTEST);
		
		
		int delay = 0;   // delay for 5 sec.
		int period = 100;  // repeat every sec.
		timer = new Timer();
		
		timer.scheduleAtFixedRate(new TimerTask() {
	        public void run() {
	        	
	        	// Send RPC commands. 
	        	String s = String.valueOf(currentRightSpeed);
	    		String[] Args = {s};
	        	RPC("m3pi", "right_motor", Args);
	        	String s2 = String.valueOf(currentLeftSpeed);
	        	String[] Args2 = {s2};
	        	RPC("m3pi", "left_motor", Args2);
	        	  
	        }
	    }, delay, period);
	 }
	}

	@Override
public void onPause() {
		
		// pauses the bluetooth connection
// androidBluetoothRPC tmpRPC = (androidBluetoothRPC) mbed;
		super.onPause();
		// Stops the mbed 
    	
		if(address != null)
		{
			String s = String.valueOf(0);
			String[] Args = {s};
	    	RPC("m3pi", "right_motor", Args);
	    	String s2 = String.valueOf(0);
	    	String[] Args2 = {s2};
	    	RPC("m3pi", "left_motor", Args2);
		}
    	address = null;
	// Send a stop command to the robot
	//	ourRobot.right_motor(0);
		//ourRobot.left_motor(0); 
    	timer.cancel();
    	
    	
	    if (D)
	            Log.e(TAG, "- ON PAUSE -");
	
	    if (outStream != null) {
	            try {
	                    outStream.flush();
	            } catch (IOException e) {
	                    Log.e(TAG, "ON PAUSE: Couldn't flush output stream.", e);
	            }
	    }
	
	    try     {
	            btSocket.close();
	    } catch (IOException e2) {
	            Log.e(TAG, "ON PAUSE: Unable to close socket.", e2);
	    }
				
	
		//sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
		
	
 }

	@Override
	public void onStop() {
		super.onStop();
		// stops listening to the accelerometer readings
	//	sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
		sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
		
        if (D)
            Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
        if (D)
            Log.e(TAG, "--- ON DESTROY ---");
	}

	@Override 
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
			switch (event.sensor.getType()){
			case Sensor.TYPE_ACCELEROMETER:

				break;
			case Sensor.TYPE_ORIENTATION:
				
				roll = event.values[1];
				
	/*			Un comment for average filtering
	 * 			// obtain the accelerometer readings for roll
				rollVector.insertElementAt(event.values[1], 0);
				rollVector.remove(numTaps);
				
				// Average the roll inputs 
				for(int i = 0; i < numTaps; i ++)
				{
					roll = roll + rollVector.get(i);
					roll = roll/numTaps;
				}
				*/
				
				pitch = event.values[2];
				/* 
				 * Un comment for average filtering
				// obtain the accelerometer readings for pitch
				pitchVector.insertElementAt(event.values[2], 0);
				pitchVector.remove(numTaps);
				
				//Reset the pitch 
				pitch = 0; 
				
				// Average the pitch inputs
				for(int i = 0; i < numTaps; i ++)
				{
					pitch = pitch + pitchVector.get(i);
					pitch = pitch/numTaps;
				}
				*/
				//roll = event.values[1];
				//pitch =  event.values[2];
		
				break;

			}


			// Safetychecks for pitch make sure they are in range
			if (pitch > pitchRange)
			{
				pitch = pitchRange;
			}
			if (pitch < -pitchRange)
			{
				pitch = -pitchRange;
			}
			// Safetychecks for pitch make sure they are in range
			if (roll > rollRange)
			{
				roll = rollRange;
			}
			if (roll < -rollRange)
			{
				roll = -rollRange;
			}       


			// calculate the change in readings
			pitchDifference = 0;
			pitchDifference = oldPitch - pitch; 

			oldPitch = pitch; 

			if((pitchDifference > pitchSensitivity) || (-pitchDifference > pitchSensitivity))
			{
				if(pitchDifference > pitchSensitivity) // going forwards
				{
					overallSpeed = (-1*((pitch)/pitchRange));
					currentLeftSpeed = overallSpeed;
					currentRightSpeed = overallSpeed;
					isMoving = true;
					hasChanged = true;
					forward = true;
					backwards = false;
					left = false;
					right = false;

				}

				else if(pitchDifference < -pitchSensitivity) // going backwards
				{
					overallSpeed = (-1*((pitch)/pitchRange));
					currentLeftSpeed = overallSpeed;
					currentRightSpeed = overallSpeed;
					isMoving = true;
					hasChanged = true;
					forward = false;
					backwards = true;
					left = false;
					right = false;
				}

				// Change the 0.3 value for 
				if((overallSpeed < startThreshold ) && (overallSpeed > -startThreshold ) && isMoving) // going forwards
				{
					overallSpeed = (0);
					currentLeftSpeed = overallSpeed;
					currentRightSpeed = overallSpeed;
					isMoving = false;

					forward = false;
					backwards = false;
					left = false;
					right = false;
				}

			}

			rollDifference = 0;
			rollDifference = oldRoll - roll; 
			oldRoll = roll; 
			if((rollDifference > rollSensitivity) || (-rollDifference > rollSensitivity))
			{
				if(rollDifference > rollSensitivity) // going right
				{
					currentLeftSpeed =  overallSpeed;
					currentRightSpeed = overallSpeed *((1-roll/rollRange));
					hasChanged = true;
				}

				else if(rollDifference < -pitchSensitivity) // going left
				{
					currentLeftSpeed =  overallSpeed *((1+roll/rollRange));
					currentRightSpeed = overallSpeed;
					hasChanged = true;
				}

			}


			if ((overallSpeed < 0.05) && (overallSpeed > -0.05))
			{
				if((roll > 10) || (roll < 0))
				{
					if((rollDifference > rollSensitivity) || (-rollDifference > rollSensitivity))
					{
						if(rollDifference > rollSensitivity) // going right
						{
							currentLeftSpeed =  ((roll/rollRange));
							currentRightSpeed = -(roll/rollRange);
							hasChanged = true;
							forward = false;
							backwards = false;
							left = false;
							right = true;

						}

						else if(rollDifference < -pitchSensitivity) // going left
						{
							currentLeftSpeed =  ((roll/rollRange));
							currentRightSpeed = (-(roll/rollRange));
							hasChanged = true;
							forward = false;
							backwards = false;
							left = true;
							right = false;
						}

					}
				}
			}

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
			if(currentRightSpeed < -1)
			{ 
				currentRightSpeed = -1;
			}

			//safetychecks
			if(currentLeftSpeed  < -1)
			{ 
				currentLeftSpeed  = -1;
			}

			if((rollDifference > rollSensitivity) || (-rollDifference > rollSensitivity)||(pitchDifference > pitchSensitivity) || (-pitchDifference > pitchSensitivity))
			{
				if(hasChanged)
				{

				//	ourRobot.right_motor(currentRightSpeed);
				//	ourRobot.left_motor(currentLeftSpeed);
					hasChanged = false;
				}
			}
		}


		demoview.invalidate();   
	}  

	
	
	
	// This will take a bitmap and rotate it keeping the image size constant
private BitmapDrawable rotateImageNormally(Bitmap orginal, int orgWidth, int orgHeight, int rotate)
	{


		// create a matrix for the manipulation
		Matrix aMatrix = new Matrix();
		// resize the bit map
		aMatrix.postScale(1, 1);
		// rotate the Bitmap
		aMatrix .postRotate(0);

		// create the new Bitmap with the rotated image
		Bitmap rotatedImage = Bitmap.createBitmap(orginal, 0, 0,
				orgWidth, orgHeight,  aMatrix, true);

		// Convert bitmap in to a drawable
		BitmapDrawable backgroundDrawable = new BitmapDrawable(rotatedImage);

		return backgroundDrawable; 
	}

	// This will take a bitmap and rotate it keeping the image size the same as the program in landscape mode
	// Note the position is from the centre of the screen
private BitmapDrawable rotateImageConstantSize(int xPos, int yPos, Bitmap orginal, int widthOfProgram, int heightOfProgram, int orgWidth, int orgHeight, int rotation)
	{
		// create a matrix for the manipulation
		Matrix aMatrix = new Matrix();
		// resize the bit map
		aMatrix.postScale(1, 1);
		// rotate the Bitmap
		aMatrix .postRotate(rotation);

		// create the new Bitmap with the rotated image
		Bitmap rotatedImage = Bitmap.createBitmap(orginal, 0, 0,
				orgWidth, orgHeight,  aMatrix, true);

		// create an empty bit map the size of the program in landscape
		Bitmap aBitmap = Bitmap.createBitmap(widthOfProgram, heightOfProgram, orginal.getConfig());     

		// Create a canvas to draw on using the a bitmap
		Canvas aCanvas = new Canvas(aBitmap);

		// Set up the paint variable to draw
		Paint p = new Paint();

		// Draw the rotated bitmap onto the empty canvas in the right position.
		aCanvas.drawBitmap(rotatedImage, (((widthOfProgram-rotatedImage.getWidth())/2) + (xPos)), (((heightOfProgram-rotatedImage.getHeight())/2) + yPos), p);

		// Create DrawableBitmap from the bitmap; 
		BitmapDrawable aBitmapDrawable = new BitmapDrawable(aBitmap);

		return aBitmapDrawable; 
	}

private BitmapDrawable scaleImage(Bitmap orginal, int newHeight, int newWidth, int orgWidth, int orgHeight, int rotation)
	{
		// calculate the scale - in this case = 0.4f
		float scaleWidthFactor = ((float) newWidth) / orgWidth;
		float scaleHeightFactor = ((float) newHeight) / orgHeight;

		// create a matrix for the manipulation
		Matrix aMatrix = new Matrix();
		// resize the bit map
		aMatrix.postScale(scaleWidthFactor, scaleHeightFactor);
		// rotate the Bitmap
		aMatrix .postRotate(0);

		// create the new Bitmap with the rotated image
		Bitmap scaledImage = Bitmap.createBitmap(orginal, 0, 0,
				orgWidth, orgHeight,  aMatrix, true);

		// Convert Bitmap to drawable
		BitmapDrawable aBitmapDrawable = new BitmapDrawable(scaledImage);

		return aBitmapDrawable; 
	}


	private class DemoView extends View{
		public DemoView(Context context){
			super(context);
		}

		@Override protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			count = count + 1;
			
			// custom drawing code here
			// remember: y increases from top to bottom
			// x increases from left to right
			int x, y;

			x = programsWidth/2-(int)roll;
			y = programsHeight/2+(int)pitch;

			Paint paint = new Paint();
	//		paint.setStyle(Paint.Style.FILL);  

			// create picture in background
			
			
			
			//			Shape background = new Shape();
			
			// make the entire canvas white
		//	paint.setColor(Color.WHITE);
		//	canvas.drawPaint(paint);
			canvas.drawBitmap(bitmapOrg, matrix, paint);
			
			paint.setAntiAlias(true);
			paint.setColor(Color.BLUE);
			
			// represents direction
			canvas.drawCircle(x, 21*programsHeight/32, 20, paint);
			
			// represents steering
			canvas.drawCircle(47*programsWidth/64, y, 20, paint);
			
//			paint.setColor(Color.BLACK);
//			paint.setStyle(Paint.Style.STROKE);

//			paint.setStrokeWidth(3);
//			canvas.drawCircle(programsWidth/2+10, programsHeight/2+40, 60, paint);


/*			// draw some text using STROKE style
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(2);
			paint.setColor(Color.BLACK);
			paint.setTextSize(45);
			canvas.drawText("Mbed Racer", programsWidth/2 - 2*programsWidth/8, 1*programsHeight/8, paint);
			canvas.drawText("X", programsWidth/2 - 10, programsHeight/2 + 15, paint);*/
			
	 }
	}

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
        
        byte[] msgBuffer = RPCString.getBytes();
        
		// Attempt to send command
        try {
                outStream.write(msgBuffer);
        } catch (IOException e) {
        	
        		
    	    if (outStream != null) {
	                    try {
							outStream.flush();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
    	    }
	
		    try     {
		            btSocket.close();
		    } catch (IOException e2) {
		            Log.e(TAG, "ON PAUSE: Unable to close socket.", e2);
		    }
        	
        	establishConnection();

        	
        	Log.e(TAG, "ON RESUME: Exception during write.", e);
        }	
        
        // Get Received Data
    //    Response = getReceivedData();
        
        return(RPCString);
		
	
	}
	
public void establishConnection() {	
        
			// When this returns, it will 'know' about the server,
            // via it's MAC address.
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);          
            // We need two things before we can successfully connect
            // (authentication issues aside): a MAC address, which we
            // already have, and an RFCOMM channel.
            // Because RFCOMM channels (aka ports) are limited in
            // number, Android doesn't allow you to use them directly;
            // instead you request a RFCOMM mapping based on a service
            // ID. In our case, we will use the well-known SPP Service
            // ID. This ID is in UUID (GUID to you Microsofties)
            // format. Given the UUID, Android will handle the
            // mapping for you. Generally, this will return RFCOMM 1,
            // but not always; it depends what other BlueTooth services
            // are in use on your Android device.
           try {
                    btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                    Log.e(TAG, "ON RESUME: Socket creation failed.", e);
            }

            // Discovery may be going on, e.g., if you're running a
            // 'scan for devices' search from your handset's Bluetooth
            // settings, so we call cancelDiscovery(). It doesn't hurt
            // to call it, but it might hurt not to... discovery is a
            // heavyweight process; you don't want it in progress when
            // a connection attempt is made.
            mBluetoothAdapter.cancelDiscovery();

            // Blocking connect, for a simple client nothing else can
            // happen until a successful connection is made, so we
            // don't care if it blocks.
            try {
                    btSocket.connect();
                    Log.e(TAG, "ON RESUME: BT connection established, data transfer link open.");
            } catch (IOException e) {
                    try {
                            btSocket.close();
                    } catch (IOException e2) {
                            Log.e(TAG,
                                    "ON RESUME: Unable to close socket during connection failure", e2);
                    }
            }
                    
            
            // Create a data stream so we can talk to server.
            if (D)
                    // Bluetooth Connection is Established
            		Log.e(TAG, "+ ABOUT TO SAY SOMETHING TO SERVER +");
            
            // Get the input and output streams, using temp objects because
            // member streams are final

            try {
            		outStream = btSocket.getOutputStream();
            } catch (IOException e) {
                    Log.e(TAG, "ON RESUME: Output stream creation failed.", e);
            }	
          

		}
	
}

