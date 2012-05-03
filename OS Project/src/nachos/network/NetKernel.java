package nachos.network;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import nachos.network.*;
import nachos.network.Sockets.socketStates;


/**
 * A kernel with network support.
 */
public class NetKernel extends UserKernel {
	/**
	 * Allocate a new networking kernel.
	 */
	public NetKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		transport = new TransportLayer();
		//postOffice = new PostOffice();
	}

	/**
	 * Test the network. Create a server thread that listens for pings on port
	 * 1 and sends replies. Then ping one or two hosts. Note that this test
	 * assumes that the network is reliable (i.e. that the network's
	 * reliability is 1.0).
	 */
	public void selfTest2() {
		//super.selfTest();

		KThread serverThread = new KThread(new Runnable() {
			public void run() { pingServer(); }
		});

		//serverThread.fork();

		System.out.println("Press any key to start the network test...");
		console.readByte(true);

		int local = Machine.networkLink().getLinkAddress();

		// ping this machine first
		ping(local);

		// if we're 0 or 1, ping the opposite
		if (local <= 1)
			ping(1-local);
	}
	public void selfTest(){

		KThread test2 = new KThread(new Runnable() {
			public void run(){
				socketTest2();
			}
		});
		KThread test1 = new KThread(new Runnable() {
			public void run(){
				socketTest();
			}
		});

		test1.fork();
		test2.fork();



		// KThread.yield();

	}
	public void socketTest(){
		int srcLink = Machine.networkLink().getLinkAddress();
		Sockets connectPlease = new Sockets(0);
		//connectPlease.destID = dstLink;
		//connectPlease.destPort = 1;
		System.out.println("Attempting to connect!");
		if(!transport.createConnection(srcLink, 1, connectPlease) ){
			System.out.println("Did not connect");
		}
		System.out.println("Connected with info: DestID: " + connectPlease.destID + " at port: " + connectPlease.destPort );


		connectPlease.close();
	}
	private void socketTest2(){
		if (test==0){
			timeout();
		}
		Sockets SockemBoppers = NetKernel.transport.socketQueues[1].pollFirst();
		if(SockemBoppers==null)
			System.out.println("fail to grab socket");
		SockemBoppers.states = socketStates.SYNRECEIVED;
		System.out.println("Attempting to Accept");
		//  Sockets scktRcv = new Sockets(1);
		if(SockemBoppers != null && transport.acceptConnection(SockemBoppers))
			System.out.println("Accept has worked");
		System.out.print("Accept failed");
	}

	private void ping(int dstLink) {
		int srcLink = Machine.networkLink().getLinkAddress();
		Sockets connectPlease = new Sockets(0);
		//connectPlease.destID = dstLink;
		//connectPlease.destPort = 1;
		if(!transport.createConnection(dstLink, 1, connectPlease) ){
			System.out.println("Did not connect");
		}
		System.out.println("Connected with info: DestID: " + connectPlease.destID + " at port: " + connectPlease.destPort );

		long startTime = Machine.timer().getTime();

		TCPpackets ping;

		try {
			//ping = new MailMessage(dstLink, 1,
			//      Machine.networkLink().getLinkAddress(), 0,
			//new byte[0]);
			ping =  new TCPpackets(dstLink,1,Machine.networkLink().getLinkAddress(),0, new byte[0],true,false,false,false,0);
		}
		catch (MalformedPacketException e) {
			Lib.assertNotReached();
			return;
		}
		Sockets pingSocket = new Sockets(0);
		pingSocket.destID = dstLink;
		pingSocket.destPort = 1;
		//  pingSocket.sendSYN();
		pingSocket.states = socketStates.SYNSENT;
		transport.activeSockets.put(pingSocket.getKey(), pingSocket);
		System.out.println("Socket key: " + pingSocket.getKey());
		// transport.send(ping);

		TCPpackets ack = transport.receive(1);

		long endTime = Machine.timer().getTime();

		System.out.println("time=" + (endTime-startTime) + " ticks");   
	}

	private void pingServer() {
		while (true) {
			int port = 1;
			//TCPpackets ping = postOffice.receive(1);
			//create socket at port 1


			Sockets acceptPlease = new Sockets(port);
			transport.socketQueues[port].add(acceptPlease);
			TCPpackets ping = transport.receive(1);
			TCPpackets ack;

			try {
				//ack = new MailMessage(ping.packet.srcLink, ping.srcPort,
				//      ping.packet.dstLink, ping.dstPort,
				//ping.contents);
				ack = new TCPpackets(ping.packet.srcLink,ping.srcPort,ping.packet.dstLink,ping.dstPort, new byte[0],false,true,false,false,0);
			}
			catch (MalformedPacketException e) {
				// should never happen...
				continue;
			}

			// transport.send(ack);
		}       
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}
	public static void timeout(){
		alarm.waitUntil(20000);
		test = 1;
	}
	
	//private PostOffice postOffice;
	public static TransportLayer transport;
	public static int test = 0;
	// dummy variables to make javac smarter
	private static NetProcess dummy1 = null;
}

