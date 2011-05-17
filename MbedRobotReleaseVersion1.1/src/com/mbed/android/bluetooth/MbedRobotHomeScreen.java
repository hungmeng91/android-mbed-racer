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
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class MbedRobotHomeScreen extends Activity{
    // Debugging
    private static final String TAG = "Bluetooth";
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

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the  services
    public BluetoothService mChatService = null;

    private Matrix matrix;
	private Bitmap bitmapOrg; 
    
	private Button accButton;
    private Button tankButton;
    private Button controllerButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        setContentView(R.layout.main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        accButton = (Button) findViewById(R.id.ButtonAcc);
        tankButton = (Button) findViewById(R.id.ButtonTank);
        controllerButton = (Button) findViewById(R.id.ButtonController);
        
        accButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	 // do something when the button is clicked
 	    	   Intent intent = new Intent();
 	    	   intent.setClass(MbedRobotHomeScreen.this, MbedRobotAccelerometer.class);
 	    	   startActivity(intent);  
            }
        });
        
        tankButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

            	Intent intent = new Intent();
            	intent.setClass(MbedRobotHomeScreen.this, MbedRobotTouchTankMode.class);
            	startActivity(intent);
            	
            }
        });
        
        controllerButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

            	Intent intent = new Intent();
            	intent.setClass(MbedRobotHomeScreen.this, MbedRobotTouchController.class);
            	startActivity(intent);
            	
            }
        });
        
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
       
            // Otherwise, setup the chat session
        } else {
           
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.troubleShoot:
            
        	// Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, TroubleShootingActivity.class);
            startActivity(serverIntent);
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



			Paint paint = new Paint();

			// Draw background onto screen
			canvas.drawBitmap(bitmapOrg, matrix, paint);
			paint.setAntiAlias(true);

     		invalidate();
			
			
		}
	}


	

}