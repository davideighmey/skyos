package nachos.network;

import java.util.Hashtable;
import java.util.LinkedList;

import nachos.machine.*;
import nachos.network.Sockets.socketStates;
import nachos.threads.*;

public class TransportLayer  {
	//Set timeout length for each retry 
	public static int timeoutLength = 20000;
	//Set max retry here
	public static int maxRetry = 3;
	//Keep a track of ports and sockets that has been used
	public int[] freePorts = new int[128];

	private SynchList[] queues;
	private Semaphore messageReceived;	// V'd when a message can be dequeued
	private Semaphore messageSent;	// V'd when a message can be queued
	private Lock sendLock;
	private Lock portLock = new Lock();
	public Hashtable<Integer, LinkedList<Sockets>> activeSockets;
	public Hashtable<Integer, TCPpackets> RecievePacketTable;
	
	
	Sockets ReadSocket;
	Sockets WriteSocket;
	final private int MAXWINDOWSIZE = 16;
	private int reTransmission = 20000;//20,000 clock ticks.

	public TransportLayer(){
		messageReceived = new Semaphore(0);
		messageSent = new Semaphore(0);
		sendLock = new Lock();
		RecievePacketTable =  new Hashtable<Integer, TCPpackets>(128);
		for(int i=0;i < 128; i++){
			freePorts[i] = 1;
		}
		
		queues = new SynchList[TCPpackets.portLimit];
		for (int i=0; i<queues.length; i++)
			queues[i] = new SynchList();

		
		Runnable receiveHandler = new Runnable() {
			public void run() { receiveInterrupt(); }
		};
		Runnable sendHandler = new Runnable() {
			public void run() { sendInterrupt(); }
		};
		Machine.networkLink().setInterruptHandlers(receiveHandler,
				sendHandler);

		KThread RecieveGuy = new KThread(new Runnable() {
			public void run() {packetReceive(); }
		});

		KThread SendGuy = new KThread(new Runnable(){
			public void run() {packetSend();}
		});

		KThread TimeOutGuy = new KThread(new Runnable(){
			public void run() {timeOut();}
		});

		RecieveGuy.fork();
		SendGuy.fork();
		//TimeOutGuy.fork();
	}
	
	/*
	 * Recieves a packet and puts it onto the correct ports 
	 */
	public void packetReceive(){
		while(true){
			messageReceived.P();
			Packet p = Machine.networkLink().receive();
			TCPpackets mail;
			
			try{
				mail = new TCPpackets(p);
			}
			catch (MalformedPacketException e) {
				continue;
			}
			// atomically add message to the mailbox and wake a waiting thread
			queues[mail.dstPort].add(mail);
			//Need to be kept somewhere on a type of list or something...
		}
	}
	
	Alarm sendagain = new Alarm();
	public void timeOut(){
		while(true){
			//sendagain.waitUntil(reTransmission);
		}
	}

	public void packetSend(){
		
	}
	
	/*
	 * Retrieve a message on the specified port, waiting if necessary.
	 */
    public TCPpackets receive(int port) {
	TCPpackets mail = (TCPpackets) queues[port].removeFirst();
	return mail;
    }

    
	//Ports 0 to 127
	public int findPort(){
		int PORT = 0;
		while(getFreePort(PORT) == false){
			PORT++;
			if(PORT >= 128){
				PORT = -1;
			}
		}
		return PORT;
	}
	public boolean getFreePort(int port){
		if(freePorts[port] == 1){
			return true;	
		}
		return false;
	}

	
	public void rememberSocket(LinkedList<Sockets> scktList, int portnum){
		activeSockets.put(portnum, scktList); //Will need to pass along a list of the sockets beting used at that port
	}

	public TCPpackets receives(int port) {
		Lib.assertTrue(port >= 0 && port < queues.length);
		TCPpackets mail = (TCPpackets) queues[port].removeFirst();
		return mail;
	}
	public void send() {
	
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
	private void receiveInterrupt() {
		messageReceived.V();
	}
	/**
	 * Read this file starting at the specified position and return the number
	 * of bytes successfully read. If no bytes were read because of a fatal
	 * error, returns -1
	 *
	 * @param	buf	the buffer to store the bytes in.
	 * @param	offset	the offset in the buffer to start storing bytes.
	 * @param	length	the number of bytes to read.
	 * @return	the actual number of bytes successfully read, or -1 on failure.
	 */   

	//Three-way-handshake: SYN, SYN-ACK, ACK
	//Try to connect from the host to the dest
	public boolean createConnection(int _destID, int _destPort, Sockets sckt){
		sckt.destID = _destID;
		sckt.destPort = _destPort;
		
		//have to send a syn packet
		try {
			TCPpackets syn = new TCPpackets(sckt.destID,sckt.destPort,sckt.hostID,sckt.hostPort, new byte[0],true,false,false,false,0);
			sckt.states = socketStates.SYNSENT;
		} catch (MalformedPacketException e) {
			// TODO Auto-generated catch block
			System.out.println("Malformed Packet has been detected");
			//e.printStackTrace();
			return false;
		}
		
		int count = 0;
		Alarm alarm = new Alarm();
		
		while(sckt.states == socketStates.SYNSENT && count < TransportLayer.maxRetry){
			alarm.waitUntil(TransportLayer.timeoutLength);
			count++;
		}
		
		//if(states == socketStates.SYNRECEIVED)
		//check if sent
		//keep sending until either timeout is reached or connection 
		//if  received an ack, connection is established, return with a value saying connected
		//else return -1
		return false;
	}

	//Try to accept the connection from the sender
	public int acceptConnection(int _hostID, Sockets sckt){
		sckt.hostID = _hostID;	
		
		
		
		return -1;
	}


	//attempt to bind the socket to the selected port
	int bindSocket(int port){
		//states = socketStates.LISTENING;
		return -1;
	}
	public boolean closeConnection(int _destID, int _destPort, Sockets sckt){
		sckt.destID = _destID;
		sckt.destPort = _destPort;

		//have to send a fin packet
		try {
			TCPpackets fin = new TCPpackets(sckt.destID,sckt.destPort,sckt.hostID,sckt.hostPort, new byte[0],false,false,false,true,0);
			sckt.states = socketStates.SYNSENT;
		} catch (MalformedPacketException e) {
			// TODO Auto-generated catch block
			System.out.println("Malformed Packet has been detected");
			//e.printStackTrace();
			return false;
		}
		int count = 0;
		Alarm alarm = new Alarm();
		while(sckt.states== socketStates.SYNSENT && count < TransportLayer.maxRetry){
			alarm.waitUntil(TransportLayer.timeoutLength);
			count++;
		}
		//if(states == socketStates.SYNRECEIVED)
		//check if sent
		//keep sending until either timeout is reached or connection 
		//if  received an ack, connection is established, return with a value saying connected
		//else return -1
		return false;
	}

}