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
	public final int MaxPorts = 128;
	//Keep a track of ports and sockets that has been used
	Boolean[] freePorts = new Boolean[MaxPorts];

	//private SynchList[] queues;
	private Semaphore messageReceived;	// V'd when a message can be dequeued
	private Semaphore messageSent;	// V'd when a message can be queued
	private Semaphore packetSending;
	private Lock sendLock;
	private Lock portLock = new Lock();
	/**
	 * Will keep the port number and the list of sockets occupying that port
	 */
	public Hashtable<Integer, LinkedList<Sockets>> activeSockets;
	
	/**
	 * Holds the packets that will need to be sent by the sending thread
	 * If you want to send a specific packet a port use the recievedPacketList to send it
	 * Otherwise this list is for the sending thread to keep sending packets at all times.
	 */
	public LinkedList<TCPpackets> sendPacketList;
	
	/**
	 * Holds the recently received packets
	 */
	public Hashtable<Integer, TCPpackets> recievedPacketList;


	Sockets ReadSocket;
	Sockets WriteSocket;
	final private int MAXWINDOWSIZE = 16;
	private int reTransmission = 20000;//20,000 clock ticks.

	public TransportLayer(){
		messageReceived = new Semaphore(0);
		messageSent = new Semaphore(0);
		packetSending = new Semaphore(0);
		sendLock = new Lock();
		sendPacketList = new LinkedList<TCPpackets>();
		recievedPacketList = new Hashtable<Integer, TCPpackets>();
		
		for(int i=0;i < 128; i++){
			freePorts[i] = true;
		}

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
			recievedPacketList.put(mail.dstPort, mail);
			//Need to be kept somewhere on a type of list or something...
		}
	}

	/*
	 * Try to always send packets
	 */
	public void packetSend(){
		while(true){
			packetSending.P();
			if(sendPacketList.size() == 0){
				continue;
			}
			TCPpackets pckt = sendPacketList.removeFirst();
			send(pckt);
		}
	}

	public void timeOut(){
		while(true){

		}
	}

	/*
	 * Retrieve a message on the specified port, waiting if necessary.
	 */
	public TCPpackets receive(int port) {
		Lib.assertTrue(port >= 0 && port < MaxPorts);
		TCPpackets pckt = recievedPacketList.remove(port);;
		return pckt;
	}

	/**
	 * 
	 * @param scktList
	 * Will need to pass the list of sockets that is currently using the same port. Passing a new list will update the current one
	 * @param portnum 
	 * The specified port number that the sockets use
	 */
	public void rememberSocket(LinkedList<Sockets> scktList, int portnum){
		activeSockets.put(portnum, scktList); //Will need to pass along a list of the sockets beting used at that port
	}
	
	/**
	 * Will check if the current port is free or not
	 * @param Port the specified port number
	 * @return True if port is free
	 */
	public boolean checkPort(int port){
		if(freePorts[port] == true){
			return true;	
		}
		return false;
	}
	/**The only ports in use are Ports 0 to 127. 
	 * Will go through all the ports to find a free port to use.
	 * @return Will return the port number that is free.
	 */
	public int findPort(){
		int PORT = 0;
		while(checkPort(PORT) == false){
			PORT++;
			if(PORT >= 128){
				PORT = -1;
			}
		}
		return PORT;
	}

	public void send(TCPpackets mail) {
		  System.out.println("sending mail: " + mail);
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
	
	/*
	 * #########################
	 * # THE ACK-SYN STUFF PART
	 * #########################
	 */
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

	public TCPpackets receives(TCPpackets pckt){
		
		return pckt;
	}
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