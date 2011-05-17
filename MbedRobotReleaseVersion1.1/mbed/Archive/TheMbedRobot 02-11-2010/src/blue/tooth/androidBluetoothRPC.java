package blue.tooth;

import org.mbed.RPC.mbed;



import java.io.OutputStream;
import java.util.UUID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.lang.Thread;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothSocket;

import android.util.Log;



public class androidBluetoothRPC extends mbed {
	
	private static boolean BluetoothStatus = false;
	private static final String TAG = "THINBTCLIENT";
    private static final boolean D = true;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;  
    private static final UUID MY_UUID =
    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Thread thrd; // Separate thread used for receiving data
    private String receivedData; //used for the receiving data
    private byte[] buffer;
    private BufferedReader in = null;
    
   private static String address = "00:06:66:42:17:78"; // This variable holds the Bluetooth modules Mac address
// private static String address = "00:06:66:42:17:78";   
// Passes the bluetooth mac address and sets up the connection
	
    public androidBluetoothRPC(String address2){			
		checkBluetoothAdapter();
 //     establishConnection();
		
        System.out.print("This is a test");
//        startListening();
	}

	private void checkBluetoothAdapter()
	{
		BluetoothStatus = false;
		
		if (D)
	        Log.e(TAG, "+++ ON CREATE +++");
			
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null) {
			    	//Bluetooth is not available so returns a 2    
			}
			
			if (!mBluetoothAdapter.isEnabled()) {        
					//Please enable your BT and re-run this program
			}

			if (D) // Bluetooth is working 
			{
					BluetoothStatus = true;
					Log.e(TAG, "+++ DONE IN ON CREATE, GOT LOCAL BT ADAPTER +++");
			}
	}
	

private void startListening()
	{
	
	BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

	try {
	        btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
	} catch (IOException e) {
	        Log.e(TAG, "ON RESUME: Socket creation failed.", e);
	}
	
	try {
		inStream = btSocket.getInputStream();
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
   
	// Discovery may be going on, e.g., if you're running a
    // 'scan for devices' search from your handset's Bluetooth
    // settings, so we call cancelDiscovery(). It doesn't hurt
    // to call it, but it might hurt not to... discovery is a
    // heavyweight process; you don't want it in progress when
    // a connection attempt is made.
    mBluetoothAdapter.cancelDiscovery();
    receivedData = null;
	thrd = new Thread()
	
	{
		    public void run() {
		        byte[] buffer = new byte[1024];  // buffer store for the stream
		        
		        try {
					in = new BufferedReader(new InputStreamReader(btSocket.getInputStream()));
				
		        } catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} 
		       
				// Keep listening to the InputStream until an exception occurs
		        while (true) {
		            try {
		                // Read from the InputStream
		               inStream.read(buffer);
		               	
		            	} catch (IOException e) {
		                break;
		            }
		        }		    
		    }
	 };
	 thrd.start();
}
	
public String getReceivedData(){

/*		try {
			if(in.readLine() != null)
			{
				receivedData = in.readLine();
			}
			} catch (IOException e) {
			// TODO Auto-generated catch block
			receivedData = null;
			e.printStackTrace();
		}*/
	
	return "data";
}
	
	
;public void establishConnection() {	
            
		if(BluetoothStatus){
			
		
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
		else
		{
			// Error - turn on and enable bluetooth
		}
	}	
	
	
	/**
	 * {@inheritDoc}
	 */
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
	        	
        	// If an error happens try close connection
        	try {
					outStream.flush();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
        		try {
					btSocket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} 
          	   
				// Try to re establish the connection. 
				establishConnection();
        		Log.e(TAG, "ON RESUME: Exception during write.", e);
        }	
        
        // Get Received Data
    //    Response = getReceivedData();
        
        return(RPCString);
	}
	
	
	public void pause(){ // This method should be called when the Android Activity is paused
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
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void delete(){
		
	}
	
	
}
