package blue.tooth;
import org.mbed.RPC.*;


public class m3piDemo implements mbedRPC{
	mbed mbed; 			//Create an mbed object
	DigitalOut d1;					//This is an digital out part of the mbedRPC library
	m3pi ourRobot;
	
/*	public static void main(String[] args) {
		m3piDemo demo = new m3piDemo();
		
		demo.Robot();
	}*/
	
	public m3piDemo(){
		mbed = new SerialRxTxRPC("COM5", 9600);	//You need to be able make this mbed = new androidBluetoothRPC("Necessary args");
		ourRobot = new m3pi(mbed);
		d1 = new DigitalOut(mbed, LED1);
	}
	
	public void Robot(){
		
		d1.write(1); 	//turn on LED1
		
		for(int i = 0; i < 5; i++){
			ourRobot.forward(0.5);
			wait(1000);
			ourRobot.stop();
			wait(100);
			ourRobot.back(0.5);
		
		}
	}
	
	//A function to create a wait
	public static void wait (int n){
        long startTime,currentTime;
        startTime =System.currentTimeMillis();
        do{
            currentTime = System.currentTimeMillis();
        }
        while ((currentTime - startTime) < n);
	}
}

