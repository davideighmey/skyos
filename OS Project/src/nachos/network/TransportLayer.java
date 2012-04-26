package nachos.network;

import java.util.Hashtable;
import java.util.LinkedList;

import nachos.machine.*;
import nachos.network.Sockets.socketStates;
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
		queues = new SynchList[MailMessage.portLimit];
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

		KThread t = new KThread(new Runnable() {
			public void run() { packetDelivery(); }
		});

		t.fork();
	}

	public void rememberSocket(Sockets sckt, int portnum){

	}
	public TCPpackets receives(int port) {
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
	private void packetDelivery() {
		while (true) {
			messageReceived.P();

			Packet p = Machine.networkLink().receive();

			MailMessage mail;

			try {
				mail = new MailMessage(p);
			}
			catch (MalformedPacketException e) {
				continue;
			}

			// atomically add message to the mailbox and wake a waiting thread
			queues[mail.dstPort].add(mail);
		}
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
		while(sckt.states== socketStates.SYNSENT && count < TransportLayer.maxRetry){
			try {
				alarm.wait(TransportLayer.timeoutLength);
			} catch (InterruptedException e) {
				return false;
			}
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
	
}