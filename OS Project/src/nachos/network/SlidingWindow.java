package nachos.network;

import nachos.machine.*;
import nachos.network.TransportLayer.Window;
public class SlidingWindow {
	
	
	//The receiver advertised window(adwn) is the buffer size sent in each ACK
	//Congestion Window(cwnd) controls the number of packets a TCP flow may have in the network in a given time 
	//need to make something to hold the message
	//attempt to bind the socket to the selected port
	//int bindSocket(int port){

		//return -1;
	//}
	//window size is fixed 16 packets
	int awndSize = 16;
	int cwndSize = 1;
		
	public SlidingWindow(){
		
	}
	//public class SlidingWindow(Packet HeaderPacket){
	//this.Size = HeaderPacket.maxPacketLength;
	
}
