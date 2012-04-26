package nachos.network;

import java.util.LinkedList;

import nachos.machine.*;

public class Window {
	
	
	//The receiver advertised window(adwn) is the buffer size sent in each ACK
	//Congestion Window(cwnd) controls the number of packets a TCP flow may have in the network in a given time 
	//need to make something to hold the message
	//attempt to bind the socket to the selected port
	//int bindSocket(int port){

		//return -1;
	//}
	//window size is fixed 16 packets
	
	//Socket Takes care of that buffer.
	//Actually no buffers in window, just checks for the flow, or amount of packets you may send right there
	
	private static int MAXWINDOWSIZE = 16;
	
	public Window(){}
	
	/*
	 * Will return the amount of space left for the amount of packets to be sent based on the window size;
	 * Each time it receives an ACK it should be incremented. It should never be incremented over 16.
	 */
	
	
	
	public int getSize(){
		return MAXWINDOWSIZE;
	}
	/**
	 * For every ACK recieved this function should be called to increase the amount of packets you can send through the window
	 * the max size for this window is only a size of MAX 16.
	 * 
	 * @return It will return the number of packets you may still send through the window
	 */

	public int recieveACK(){
		if(MAXWINDOWSIZE == 16){
			return MAXWINDOWSIZE;
		}
		else{
			if(MAXWINDOWSIZE > 16){
				System.out.println("Something went terribly wrong, window size increased over 16 to " + MAXWINDOWSIZE);
				return -1;
			}
		MAXWINDOWSIZE++;
		return MAXWINDOWSIZE;
		}
	}
	
	/**
	 * For every packet sent, the Window size will decrease by 1. If the size is 0, the packet needs to wait until the window size
	 * has increased by receiving an ACK. If the window size is 0, The recieving window must drop the package.
	 * @return	Will return the current Window Size 
	 */
	public int sentPacket(){
		if(MAXWINDOWSIZE == 0){
			return 0;
		}
		else{
			MAXWINDOWSIZE--;
		}
		return MAXWINDOWSIZE;
	}
	
	
	
	
}
