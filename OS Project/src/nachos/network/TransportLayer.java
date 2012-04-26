package nachos.network;

import nachos.machine.*;
import nachos.threads.*;

public class TransportLayer extends PostOffice {
	//Set timeout length for each retry 
	public static int timeoutLength = 10000;
	//Set max retry here
	public static int maxRetry = 3;
	//Keep a track of ports and sockets that has been used
	public int[] freePorts = new int[128];
	//public datastructure li 
	private Lock portLock = new Lock();
	public TransportLayer(){
		portLock.acquire();
		for(int i=0;i < 128; i++){
			freePorts[i] = 1;
		}
		portLock.release();
	}

	public boolean getFreePort(int port){
		portLock.acquire();
		if(freePorts[port] == 1){
			portLock.release();
			return true;
		}
		portLock.release();
		return false;
	}


	protected class Window {
		//window size is fixed 16 packets
		int Size = 0; //Size of the buffer
		byte[] WindowBuffer = new byte[Size]; //Buffer size of the window

		Window(){
		}
		//public class SlidingWindow(Packet HeaderPacket){
		//this.Size = HeaderPacket.maxPacketLength;


	}

}


/*Sliding Window is the method of flow control TCP uses. The algorithm basically places a buffer between the application and the network data flow. 
The purpose of sliding window is to prevent from the sender to send too many packets to over flow the network resource or the receiver's buffer.
The "sliding window size" is the maximum amount of data we can send without having to wait for ACK. TCP achieve the flow control by using the 
sliding Window algorithm which takes into consideration 2 important parameters. The first one is the receiver advertised window size which basically
tells the sender what is the current buffer of TCP receiver, the second parameter is congestion window which control the number of packets a TCP
flow may have in the network in any given time.
 */
