package com.mbed.android.bluetooth;

import org.mbed.RPC.PinName;
import org.mbed.RPC.mbed;


public class m3pi{
private mbed the_mbed;
private String name;

	public m3pi(mbed connectedMbed){
		//Tie to existing instance
		the_mbed = connectedMbed;
		name = "m3pi";
	}

	public void forward(double speed){
		String s = String.valueOf(speed);
		String[] Args = {s};
		the_mbed.RPC(name, "forward", Args);
	}
	
	public void back(double speed){
		String s = String.valueOf(speed);
		String[] Args = {s};
		the_mbed.RPC(name, "backward", Args);
	}
	
	public void left(double speed){
		String s = String.valueOf(speed);
		String[] Args = {s};
		the_mbed.RPC(name, "left", Args);
	}
	public void right(double speed){
		String s = String.valueOf(speed);
		String[] Args = {s};
		the_mbed.RPC(name, "right", Args);
	}
	public void right_motor(double speed){
		String s = String.valueOf(speed);
		String[] Args = {s};
		the_mbed.RPC(name, "right_motor", Args);
	}
	public void left_motor(double speed){
		String s = String.valueOf(speed);
		String[] Args = {s};
		the_mbed.RPC(name, "left_motor", Args);
	}
	
	public void stop(){
		the_mbed.RPC(name, "stop", null);
	}
	
	public float battery(){
		String response = the_mbed.RPC(name, "battery", null);
		//Need to convert response to a float and return
		float f = Float.parseFloat(response);
		return(f);
	}
	public int sensors(){
		String response = the_mbed.RPC(name, "sensors", null);
		//Need to convert response to a float and return
		int i = Integer.parseInt(response);
		return(i);
	}
	
}
