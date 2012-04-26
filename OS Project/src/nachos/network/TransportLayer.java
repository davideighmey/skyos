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

}