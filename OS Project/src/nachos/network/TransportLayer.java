package nachos.network;

import java.util.Hashtable;
import java.util.LinkedList;

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
	private SynchList[] queues;
	private Semaphore messageReceived;	// V'd when a message can be dequeued
	private Semaphore messageSent;	// V'd when a message can be queued
	private Lock sendLock;
	private Lock portLock = new Lock();
	public Hashtable<Integer, LinkedList<Sockets>> activeSockets;

	public TransportLayer(){
		messageReceived = new Semaphore(0);
		messageSent = new Semaphore(0);
		sendLock = new Lock();
		portLock.acquire();

		for(int i=0;i < 128; i++){
			freePorts[i] = 1;
		}

		portLock.release();
	}

	public void rememberSocket(Sockets sckt, int portnum){

	}
	public TCPpackets receive(int port) {
		Lib.assertTrue(port >= 0 && port < queues.length);
		TCPpackets mail = (TCPpackets) queues[port].removeFirst();

		return mail;
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

	public void send(TCPpackets mail) {
		sendLock.acquire();
		Machine.networkLink().send(mail.packet);
		messageSent.P();
		sendLock.release();
	}

	private void sendInterrupt() {
		messageSent.V();
	}

}