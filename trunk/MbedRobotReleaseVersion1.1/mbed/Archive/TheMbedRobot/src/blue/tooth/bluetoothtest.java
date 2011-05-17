package blue.tooth;
 

import org.mbed.RPC.*;
 
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

 
public class bluetoothtest extends Activity implements SensorEventListener {
		private mbed mbed; 			//Create an mbed object
		private m3pi ourRobot;       
       
	    private LinearLayout linLayout; // Set up for the graphics
		private DemoView demoview; //Used for Graphics
		
	    // Used to control the motion of the robot
		private float roll = 6;
		private float pitch = -5;
		private float oldRoll = 6;
		private float oldPitch = -5;
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
		float rollSensitivity = (float) 1;
		float pitchSensitivity = (float) 1;
		
		// change this value to change the sensitivity of activating the movement
		float startThreshold = (float) 0.2;  
		
		// Change these values to create the range of movement you want to use on the phone.
		float pitchRange = 40; // Set the no. of degrees of max speed for going forwards
		float rollRange = 50;  // Set the no. of degrees of max speed for spinning
		
        // Used to obtain the accelerometer readings
		SensorManager sensorManager = null;
		private int programsWidth;
		private int programsHeight;
		
		// Graphics 
		boolean forward = false;
		boolean backwards = false;
		boolean left = false;
		boolean right = false;
		
		
		
		/** Called when the activity is first created. */
        @Override
        public void onCreate(Bundle savedInstanceState){
               super.onCreate(savedInstanceState);
               
               
               // Make Full Screen
               requestWindowFeature(Window.FEATURE_NO_TITLE); 
               getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
               
               Display display = getWindowManager().getDefaultDisplay();
               programsWidth = display.getWidth();
               programsHeight = display.getHeight();
               
               
               linLayout = new LinearLayout(this);
               Paint circlePaint = new Paint();

               
	       		demoview = new DemoView(this);
	    		setContentView(demoview);
	    		
               
               // Set the sensor manager to start listening to movement.
               sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
               
               // Create a bluetooth android RPC connection with a new mbed device
               mbed = new androidBluetoothRPC("00:15:83:41:AC:FF");	//You need to be able make this mbed = new androidBluetoothRPC("Necessary args");              
               
               // Create new mbed robot
               ourRobot = new m3pi(mbed);
               
        }     
               
        @Override
        public void onStart() {
                super.onStart();
        }
 
        @Override
        public void onResume() {
                
        super.onResume();
       	// Needs to establish a connection was the program is back in focus
        androidBluetoothRPC tmpRPC = (androidBluetoothRPC) mbed;
        tmpRPC.establishConnection();
        
        // Starts listening again for the accelerometer readings
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sensorManager.SENSOR_DELAY_GAME);
	    sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), sensorManager.SENSOR_DELAY_GAME);
	  
        }
 
        @Override
        public void onPause() {
                super.onPause();
                // pauses the bluetooth connection
                androidBluetoothRPC tmpRPC = (androidBluetoothRPC) mbed;
                tmpRPC.pause();
        }
 
        @Override
        public void onStop() {
          super.onStop();
          // stops listening to the accelerometer readings
          sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
     	  sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
     	 
        }
 
        @Override
        public void onDestroy() {
                super.onDestroy();

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
                	
                	// obtain the accelerometer readings
	            	roll = event.values[1];
	                pitch =  event.values[2];

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

	
			if (overallSpeed == 0)
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
				
				ourRobot.right_motor(currentRightSpeed);
			   	ourRobot.left_motor(currentLeftSpeed);
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

		// custom drawing code here
		// remember: y increases from top to bottom
		// x increases from left to right
		int x, y;
		

			x = programsWidth/2-(int)roll;
			y = programsHeight/2+(int)pitch;
	
		
		
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		
		// make the entire canvas white
		paint.setColor(Color.WHITE);
		canvas.drawPaint(paint);

		paint.setAntiAlias(true);
		paint.setColor(Color.BLUE);
		canvas.drawCircle(x+6, y, 20, paint);
		paint.setColor(Color.BLACK);
		paint.setStyle(Paint.Style.STROKE);

		paint.setStrokeWidth(5);
		canvas.drawCircle(programsWidth/2, programsHeight/2, 80, paint);
		

    	// draw some text using STROKE style
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(2);
		paint.setColor(Color.BLACK);
		paint.setTextSize(45);
		canvas.drawText("Mbed Racer", programsWidth/2 - 2*programsWidth/8, 1*programsHeight/8, paint);
		canvas.drawText("X", programsWidth/2 - 10, programsHeight/2 + 15, paint);

		
		
		/*
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawRect(rect, paint);
		// undo the translate
		canvas.translate(-x, -y);

		// rotate the canvas on center of the text to draw
		canvas.rotate(-45, x + rect.exactCenterX(),
                                           y + rect.exactCenterY());
		// draw the rotated text
		paint.setStyle(Paint.Style.FILL);
		canvas.drawText(str2rotate, x, y, paint);

		//undo the rotate
		canvas.restore();
		canvas.drawText("After canvas.restore()", 50, 250, paint);

		// draw a thick dashed line
		DashPathEffect dashPath =
                        new DashPathEffect(new float[]{20,5}, 1);
		paint.setPathEffect(dashPath);
		paint.setStrokeWidth(8);
		canvas.drawLine(0, 300 , 320, 300, paint);*/

	}
 }
}